package org.springframework.rules.constraint.property;

import org.springframework.binding.PropertyAccessStrategy;
import org.springframework.rules.constraint.IfTrue;
import org.springframework.util.Assert;

/**
 * <p>
 * Provides a way to trigger rules for propertyB when propertyA satisfies a
 * certain condition:
 * </p>
 * 
 * <pre>
 * 	if (propertyA satisfies the conditional constraint)
 * 	{
 * 		check the rules for propertyB
 * 	}
 * </pre>
 * 
 * with an optional part:
 * 
 * <pre>
 *  else
 *  {
 *      check the rules for propertyC
 *  }
 * </pre>
 * 
 * <p>
 * More complex situations are possible by using compound constraints which
 * leverages the previous to:
 * </p>
 * 
 * <pre>
 * if (constraint(propertyA, propertyB,...) == true)
 * {
 *     checkConstraint(property1, property2,...);
 * }
 * \\ optional part
 * else
 * {
 *     checkConstraint(propertyX, propertyY,...);
 * }
 * </pre>
 * 
 * <p>
 * This class can be compared to the {@link IfTrue} class: it applies the same
 * pattern but on different <b>properties</b> instead of on a <b>property value</b>.
 * </p>
 * 
 * @author jh
 * 
 */
public class ConditionalPropertyConstraint extends AbstractPropertyConstraint {

	/** The condition which triggers further rules to be checked. */
	private final PropertyConstraint ifConstraint;

	/** The constraint to be checked when the condition is satisfied. */
	private final PropertyConstraint thenConstraint;

	/** The constraint to be checked when the condition is <b>NOT</b> satisfied. */
	private final PropertyConstraint elseConstraint;

	/**
	 * Create a constraint which simulates the if...then pattern applied
	 * on separate properties.
	 * 
	 * @param ifConstraint the PropertyConstraint that triggers the test
	 * (satisfying a certain condition).
	 * @param thenConstraint the PropertyConstraint to test in the specified
	 * condition.
	 */
	public ConditionalPropertyConstraint(PropertyConstraint ifConstraint, PropertyConstraint thenConstraint) {
		this(ifConstraint, thenConstraint, null);
	}
	
	/**
	 * Create a constraint which simulates the if...then...else pattern applied
	 * on separate properties.
	 * 
	 * @param ifConstraint the PropertyConstraint that triggers the test
	 * (satisfying a certain condition).
	 * @param thenConstraint the PropertyConstraint to test in the specified
	 * condition.
	 * @param elseConstraint the PropertyConstraint to test if the condition is
	 * <b>NOT</b> satisfied. May be <code>null</code>.
	 */
	public ConditionalPropertyConstraint(PropertyConstraint ifConstraint, PropertyConstraint thenConstraint,
			PropertyConstraint elseConstraint) {
		super(ifConstraint.getPropertyName());
		Assert.notNull(ifConstraint);
		Assert.notNull(thenConstraint);
		this.ifConstraint = ifConstraint;
		this.thenConstraint = thenConstraint;
		this.elseConstraint = elseConstraint;
	}

	@Override
	public boolean isCompoundRule() {
		return true;
	}

	@Override
	public boolean isDependentOn(String propertyName) {
		if (elseConstraint == null)
			return ifConstraint.isDependentOn(propertyName) || thenConstraint.isDependentOn(propertyName);

		return ifConstraint.isDependentOn(propertyName) || thenConstraint.isDependentOn(propertyName)
				|| elseConstraint.isDependentOn(propertyName);
	}

	@Override
	protected boolean test(PropertyAccessStrategy domainObjectAccessStrategy) {
		if (ifConstraint.test(domainObjectAccessStrategy))
			return thenConstraint.test(domainObjectAccessStrategy);
		if (elseConstraint != null)
			return elseConstraint.test(domainObjectAccessStrategy);

		return true;
	}

}