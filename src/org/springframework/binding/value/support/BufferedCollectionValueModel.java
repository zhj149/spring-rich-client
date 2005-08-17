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
package org.springframework.binding.value.support;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.binding.value.ValueModel;
import org.springframework.richclient.list.ListListModel;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A <code>BufferedValueModel</code> that wraps a value model containing a
 * <code>Collection</code> or <code>array</code> with a
 * <code>ListListModel</code>. The list model acts as a buffer for changes to
 * and a representation of the state of the underlying collection.
 * <p>
 * On commit the following steps occur: 
 * <ol>
 * <li>a new instance of the backing collection type is created</li>
 * <li>the contents of the list model is inserted into this new collection</li>
 * <li>the new collection is saved into the underlying collection's value model</li>
 * <li>the structure of the list model is compared to the structure of the 
 * new underlying collection and if they differ the list model is updated to
 * reflect the new structure.</li>
 * </ol>
 * <p>
 * NOTE: Between calls to commit the list model adheres to the contract defined
 * in <code>java.util.List</code> NOT the contract of the underlying
 * collection's type. This can result in the list model representing a state
 * that is not possible for the underlying collection. 
 * 
 * 
 * @author oliverh
 */
public class BufferedCollectionValueModel extends BufferedValueModel {
    private ListListModel listListModel;

    private Class wrappedType;

    private Class wrappedConcreteType;

    /**
     * Constructs a new BufferedCollectionValueModel.
     * 
     * @param wrappedModel
     *            the value model to wrap
     * @param wrappedType
     *            the class of the value contained by wrappedModel; this must be
     *            assignable to <code>java.util.Collection</code> or
     *            <code>Object[]</code>.
     */
    public BufferedCollectionValueModel(ValueModel wrappedModel, Class wrappedType) {
        super(wrappedModel);
        Assert.notNull(wrappedType);
        this.wrappedType = wrappedType;
        this.wrappedConcreteType = getConcreteCollectionType(wrappedType);
        updateListModel(getWrappedValue());
        if (getValue() != listListModel) {
            super.setValue(listListModel);
        }
    }

    public void setValue(Object value) {
        if (value != listListModel) {
            if (!hasSameStructure()) {
                updateListModel(value);
            }
        }
    }
    
    protected Object getValueToCommit() {
        Object wrappedValue = getWrappedValue();
        // If the wrappedValue is null and the buffer is empty 
        // just return null rather than an empty collection
        if (wrappedValue == null && listListModel.size() == 0) {
            return null;
        }
        else {
            return createCollection(wrappedValue);
        }
    }

    //    protected void doBufferedValueCommit(Object bufferedValue) {
    //        if (hasSameStructure()) {
    //            return;
    //        }
    //        getWrappedValueModel().setValue(createCollection());
    //        if (hasSameStructure()) {
    //            return;
    //        }
    //        updateListModel(getWrappedValue());
    //    }

    public static Class getConcreteCollectionType(Class wrappedType) {
        Class class2Create;
        if (wrappedType.isArray()) {
            if (BeanUtils.isPrimitiveArray(wrappedType)) {
                throw new IllegalArgumentException("wrappedType can not be an array of primitive types");
            }
            class2Create = wrappedType;
        }
        else if (wrappedType == Collection.class) {
            class2Create = ArrayList.class;
        }
        else if (wrappedType == List.class) {
            class2Create = ArrayList.class;
        }
        else if (wrappedType == Set.class) {
            class2Create = HashSet.class;
        }
        else if (wrappedType == SortedSet.class) {
            class2Create = TreeSet.class;
        }
        else if (Collection.class.isAssignableFrom(wrappedType)) {
            if (wrappedType.isInterface()) {
                throw new IllegalArgumentException("unable to handle Collection of type [" + wrappedType
                        + "]. Do not know how to create a concrete implementation");
            }
            class2Create = wrappedType;
        }
        else {
            throw new IllegalArgumentException("wrappedType [" + wrappedType + "] must be an array or a Collection");
        }
        return class2Create;
    }

    /*
     * Checks if the structure of the ListListModel is the same as the wrapped
     * collection. "same structure" is defined as having the same elements in the
     * same order with the one exception that NULL == empty list.
     */
    private boolean hasSameStructure() {
        Object wrappedCollection = getWrappedValue();
        if (wrappedCollection == null) {
            return listListModel.size() == 0;
        }
        else if (wrappedCollection instanceof Object[]) {
            Object[] wrappedArray = (Object[])wrappedCollection;
            if (wrappedArray.length != listListModel.size()) {
                return false;
            }
            for (int i = 0; i < listListModel.size(); i++) {
                if (!ObjectUtils.nullSafeEquals(wrappedArray[i], listListModel.get(i))) {
                    return false;
                }
            }
        }
        else {
            if (((Collection)wrappedCollection).size() != listListModel.size()) {
                return false;
            }
            for (Iterator i = ((Collection)wrappedCollection).iterator(), j = listListModel.iterator(); i.hasNext();) {
                if (!ObjectUtils.nullSafeEquals(i.next(), j.next())) {
                    return false;
                }
            }
        }
        return true;
    }

    private Object createCollection(Object wrappedCollection) {
        return populateFromListModel(createNewCollection(wrappedCollection));
    }

    private Object createNewCollection(Object wrappedCollection) {
        if (wrappedConcreteType.isArray()) {
            return Array.newInstance(wrappedConcreteType.getComponentType(), listListModel.size());
        }
        else {
            Object newCollection;
            if (SortedSet.class.isAssignableFrom(wrappedConcreteType) && wrappedCollection instanceof SortedSet
                    && ((SortedSet)wrappedCollection).comparator() != null) {
                try {
                    Constructor con = wrappedConcreteType.getConstructor(new Class[] {Comparator.class});
                    newCollection = BeanUtils.instantiateClass(con,
                            new Object[] {((SortedSet)wrappedCollection).comparator()});
                }
                catch (NoSuchMethodException e) {
                    throw new FatalBeanException("Could not instantiate SortedSet class ["
                            + wrappedConcreteType.getName() + "]: no constructor taking Comparator found", e);
                }
            }
            else {
                newCollection = BeanUtils.instantiateClass(wrappedConcreteType);
            }
            return newCollection;
        }
    }

    private Object populateFromListModel(Object collection) {
        if (collection instanceof Object[]) {
            Object[] wrappedArray = (Object[])collection;
            for (int i = 0; i < listListModel.size(); i++) {
                wrappedArray[i] = listListModel.get(i);
            }
        }
        else {
            Collection wrappedCollection = ((Collection)collection);
            wrappedCollection.clear();
            wrappedCollection.addAll(listListModel);
        }
        return collection;
    }

    /**
     * Gets the list value associated with this value model, creating a list
     * model buffer containing its contents, suitable for manipulation.
     * 
     * @return The list model buffer
     */
    private Object updateListModel(final Object wrappedCollection) {
        if (listListModel == null) {
            listListModel = new ListListModel();
            listListModel.addListDataListener(new ListDataListener() {
                public void contentsChanged(ListDataEvent e) {
                    fireListModelChanged();
                }

                public void intervalAdded(ListDataEvent e) {
                    fireListModelChanged();
                }

                public void intervalRemoved(ListDataEvent e) {
                    fireListModelChanged();
                }
            });            
            setValue(listListModel);
        }
        if (wrappedCollection == null) {
            listListModel.clear();
        }
        else {
            if (wrappedType.isAssignableFrom(wrappedCollection.getClass())) {
                Collection buffer = null;
                if (wrappedCollection instanceof Object[]) {
                    Object[] wrappedArray = (Object[])wrappedCollection;
                    buffer = Arrays.asList(wrappedArray);
                }
                else {
                    buffer = (Collection)wrappedCollection;
                }
                listListModel.replaceWith(buffer);
            }
            else {
                throw new IllegalArgumentException("wrappedCollection must be assignable from " + wrappedType.getName());
            }

        }
        return listListModel;
    }

    private Object getWrappedValue() {
        return getWrappedValueModel().getValue();
    }

    protected void fireListModelChanged() {
        if (isBuffering()) {
            super.fireValueChange(listListModel, listListModel);
        }
        else {
            super.setValue(listListModel);
        }
    }

    protected boolean hasValueChanged(Object oldValue, Object newValue) {
        return (oldValue == listListModel && newValue == listListModel) 
            || super.hasValueChanged(oldValue, newValue);
    }
}