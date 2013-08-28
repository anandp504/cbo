package com.tumri.cbo.backend;

import java.util.ArrayList;
import java.util.List;

/** This class represents a recommendation to change a numeric value.
 * The new value and a list of reasons is provided.
 * Typically the list starts with the most detail and ends with the least.
 * If the new value is null it means the recommendation is to not change anything.
 */
public class NumericChange {
    private Number m_newValue;
	private List<String> m_reasons = new ArrayList<String>();

    // ---------------------- Constructors -----------------------
    
    /** Default constructor that constructs a "null" suggestion
     * with a null change and no reasons.
     */
    public NumericChange() {
    	this(null, (String)null);
    }
    
    /** A constructor that specifies a new value and one or more reasons.
     * @param newValue The new value or null to leave the old value alone.
     * @param reasons The reasons (ignored).
     */
    public NumericChange(Number newValue, String... reasons) {
    	setNewValue(newValue);
    }
    
    // ---------------- Public methods -------------------------
    
    /** Sets the new value.
     * This may be set to null to indicate the value should not be changed.
     * @param newValue The new value or null to leave the old value alone.
     */
	public void setNewValue(Number newValue) {
		m_newValue = newValue;
	}

	/** Gets the new value
	 * @return The new value or null if none.
	 */
    public Number getNewValue() {
		return m_newValue;
	}

    /** Sets the reasons.
	 * The reasons may be non-null even if there is no new value.
     * @param reasons The reasons.
     */
	public void setReasons(String... reasons) {
		m_reasons.clear();
		for(String reason : reasons) {
			m_reasons.add(reason);
		}
	}
	
    /** Sets the reasons as a list.
	 * Copies the values from the list.
     * @param reasons The reasons.
     */
	public void setReasons(List<String> reasons) {
		m_reasons.clear();
		for(String reason : reasons) {
			m_reasons.add(reason);
		}
	}
	
    /** Gets the reasons as a list.
     * This returns the internal list so that changes made to the list
     * will change the list of reasons associated with this change.
     * @reasons The reasons as a list (never null).
     */
	public List<String> getReasons() {
		return m_reasons;
	}

    /** Appends a new reason to the end of the reasons list.
     * @param reason The reason to add.
     */
    public void addReason(String reason) {
    	m_reasons.add(reason);
    }
    
	public String toString() {
		return "NumericChange[" + getNewValue() +
		       ",reasons=" + getReasons() + "]";
	}
	
	// --------------------- Package private methods ---------------

	// ---------------------- Private methods ------------------
}

