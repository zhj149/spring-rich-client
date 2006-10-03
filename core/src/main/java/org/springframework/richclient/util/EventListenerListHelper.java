/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.springframework.core.NestedRuntimeException;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.CachingMapDecorator;

/**
 * Helper implementation of an event listener list.
 * <p>
 * Provides methods for maintaining a list of listeners and firing events on
 * that list. This class is thread safe and serializable.
 * <p>
 * Usage Example:
 * 
 * <pre>
 * private ListenerListHelper fooListeners = new ListenerListHelper(FooListener.class);
 * 
 * public void addFooListener(FooListener listener) {
 * 	  fooListeners.add(listener);
 * }
 * 
 * public void removeFooListener(FooListener listener) {
 * 	  fooListeners.remove(listener);
 * }
 * 
 * protected void fireFooXXX() {
 * 	  fooListeners.fire(&quot;fooXXX&quot;, new Event());
 * }
 * 
 * protected void fireFooYYY() {
 * 	  fooListeners.fire(&quot;fooYYY&quot;);
 * }
 * </pre>
 * 
 * @author Oliver Hutchison
 * @author Keith Donald
 */
public class EventListenerListHelper implements Serializable {

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	private static final Iterator EMPTY_ITERATOR = new Iterator() {
		public boolean hasNext() {
			return false;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public Object next() {
			throw new UnsupportedOperationException();
		}
	};

	private static final Map methodCache = new CachingMapDecorator() {
		protected Object create(Object o) {
			MethodCacheKey key = (MethodCacheKey)o;
			Method fireMethod = null;

			Method[] methods = key.listenerClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				if (method.getName().equals(key.methodName) && method.getParameterTypes().length == key.numParams) {
					if (fireMethod != null) {
						throw new UnsupportedOperationException("Listener class [" + key.listenerClass
								+ "] has more than 1 implementation of method [" + key.methodName + "] with ["
								+ key.numParams + "] parameters.");
					}
					fireMethod = method;
				}
			}

			if (fireMethod == null) {
				throw new IllegalArgumentException("Listener class [" + key.listenerClass
						+ "] does not implement method [" + key.methodName + "] with [" + key.numParams
						+ "] parameters.");
			}
			return fireMethod;
		}
	};

	private final Class listenerClass;

	private volatile Object[] listeners = EMPTY_OBJECT_ARRAY;

	/**
	 * Create new <code>ListenerListHelper</code> instance that will maintain
	 * a list of event listeners of the given class.
	 */
	public EventListenerListHelper(Class listenerClass) {
		Assert.notNull(listenerClass, "The listenerClass argument is required");
		this.listenerClass = listenerClass;
	}

	/**
	 * Returns whether or not any listeners are registered with this list.
	 */
	public boolean hasListeners() {
		return listeners.length > 0;
	}

	/**
	 * Returns true if there are no listeners registered with this list.
	 */
	public boolean isEmpty() {
		return !hasListeners();
	}

	/**
	 * Returns the total number of listeners registered with this list.
	 */
	public int getListenerCount() {
		return listeners.length;
	}

	/**
	 * Returns an array of all the listeners registered with this list. This
	 * method is intended for use in subclasses that require the fastest possible
	 * access to the listener list. It is recommended that unless performance is
	 * absolutely critical access to the listener list should be through the
	 * <code>iterator</code>,<code>forEach</code> and <code>fire</code>
	 * methods only.
	 * <p>
	 * NOTE: The array returned by this method is used internally by this class
	 * and must NOT be modified.
	 */
	protected Object[] getListeners() {
		return listeners;
	}

	/**
	 * Returns an iterator over the list of listeners registered with this list.
	 */
	public Iterator iterator() {
		if (listeners == EMPTY_OBJECT_ARRAY)
			return EMPTY_ITERATOR;

        return new ObjectArrayIterator(listeners);
	}

	/**
	 * Invokes the specified method on each of the listeners registered with
	 * this list.
	 * 
	 * @param methodName the name of the method to invoke.
	 */
	public void fire(String methodName) {
		if (listeners != EMPTY_OBJECT_ARRAY) {
			fireEventByReflection(methodName, EMPTY_OBJECT_ARRAY);
		}
	}

	/**
	 * Invokes the specified method on each of the listeners registered with
	 * this list.
	 * 
	 * @param methodName the name of the method to invoke.
	 * @param arg the single argument to pass to each invocation.
	 */
	public void fire(String methodName, Object arg) {
		if (listeners != EMPTY_OBJECT_ARRAY) {
			fireEventByReflection(methodName, new Object[] { arg });
		}
	}

	/**
	 * Invokes the specified method on each of the listeners registered with
	 * this list.
	 * 
	 * @param methodName the name of the method to invoke.
	 * @param arg1 the first argument to pass to each invocation.
	 * @param arg2 the second argument to pass to each invocation.
	 */
	public void fire(String methodName, Object arg1, Object arg2) {
		if (listeners != EMPTY_OBJECT_ARRAY) {
			fireEventByReflection(methodName, new Object[] { arg1, arg2 });
		}
	}

	/**
	 * Invokes the specified method on each of the listeners registered with
	 * this list.
	 * 
	 * @param methodName the name of the method to invoke.
	 * @param args an array of arguments to pass to each invocation.
	 */
	public void fire(String methodName, Object[] args) {
		if (listeners != EMPTY_OBJECT_ARRAY) {
			fireEventByReflection(methodName, args);
		}
	}

	/**
	 * Adds <code>listener</code> to the list of registered listeners. If
	 * listener is already registered this method will do nothing.
	 */
	public boolean add(Object listener) {
		if (listener == null) {
			return false;
		}
		checkListenerType(listener);
		synchronized (this) {
			if (listeners == EMPTY_OBJECT_ARRAY) {
				listeners = new Object[] { listener };
			}
			else {
				int listenersLength = listeners.length;
				for (int i = 0; i < listenersLength; i++) {
					if (listeners[i] == listener) {
						return false;
					}
				}
				Object[] tmp = new Object[listenersLength + 1];
				tmp[listenersLength] = listener;
				System.arraycopy(listeners, 0, tmp, 0, listenersLength);
				listeners = tmp;
			}
		}
		return true;
	}

	/**
	 * @param listeners
	 */
	public boolean addAll(Object[] listeners) {
		if (listeners == null) {
			return false;
		}
		boolean changed = false;
		for (int i = 0; i < listeners.length; i++) {
			if (add(listeners[i])) {
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Removes <code>listener</code> from the list of registered listeners.
	 */
	public void remove(Object listener) {
		checkListenerType(listener);
		synchronized (this) {
			if (listeners == EMPTY_OBJECT_ARRAY)
				return;

            int listenersLength = listeners.length;
			int index = 0;
			for (; index < listenersLength; index++) {
				if (listeners[index] == listener) {
					break;
				}
			}
			if (index < listenersLength) {
				if (listenersLength == 1) {
					listeners = EMPTY_OBJECT_ARRAY;
				}
				else {
					Object[] tmp = new Object[listenersLength - 1];
					System.arraycopy(listeners, 0, tmp, 0, index);
					if (index < tmp.length) {
						System.arraycopy(listeners, index + 1, tmp, index, tmp.length - index);
					}
					listeners = tmp;
				}
			}
		}
	}

	/**
	 * Remove all listeners
	 */
	public void clear() {
		synchronized (this) {
			if (this.listeners == EMPTY_OBJECT_ARRAY)
				return;

            this.listeners = EMPTY_OBJECT_ARRAY;
		}
	}

	private void fireEventByReflection(String eventName, Object[] events) {
		Method fireMethod = (Method)methodCache.get(new MethodCacheKey(listenerClass, eventName, events.length));
		Object[] listenersCopy = listeners;
		for (int i = 0; i < listenersCopy.length; i++) {
			try {
				fireMethod.invoke(listenersCopy[i], events);
			}
			catch (InvocationTargetException e) {
				throw new EventBroadcastException("Exception thrown by listener", e.getCause());
			}
			catch (IllegalAccessException e) {
				throw new EventBroadcastException("Unable to invoke listener", e);
			}
		}
	}

	public static class EventBroadcastException extends NestedRuntimeException {
		public EventBroadcastException(String msg, Throwable ex) {
			super(msg, ex);
		}
	}

	private void checkListenerType(Object listener) {
		if (!listenerClass.isInstance(listener)) {
			throw new IllegalArgumentException("Listener [" + listener + "] is not an instance of [" + listenerClass
					+ "].");
		}
	}

	private static class ObjectArrayIterator implements Iterator {
		private final Object[] array;

		private int index;

		public ObjectArrayIterator(Object[] array) {
			this.array = array;
			this.index = 0;
		}

		public boolean hasNext() {
			return index < array.length;
		}

		public Object next() {
			return array[index++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class MethodCacheKey {
		public final Class listenerClass;

		public final String methodName;

		public final int numParams;

		public MethodCacheKey(Class listenerClass, String methodName, int numParams) {
			this.listenerClass = listenerClass;
			this.methodName = methodName;
			this.numParams = numParams;
		}

		public boolean equals(Object o2) {
			if (o2 == null) {
				return false;
			}
			MethodCacheKey k2 = (MethodCacheKey)o2;
			return listenerClass.equals(k2.listenerClass) && methodName.equals(k2.methodName)
					&& numParams == k2.numParams;
		}

		public int hashCode() {
			return listenerClass.hashCode() ^ methodName.hashCode() ^ numParams;
		}
	}

	public Object toArray() {
		if (listeners == EMPTY_OBJECT_ARRAY)
			return Array.newInstance(listenerClass, 0);

        Object[] listenersCopy = listeners;
		Object copy = Array.newInstance(listenerClass, listenersCopy.length);
		System.arraycopy(listenersCopy, 0, copy, 0, listenersCopy.length);
		return copy;
	}

	public String toString() {
		return new ToStringCreator(this).append("listenerClass", listenerClass).append("listeners", listeners)
				.toString();
	}
}