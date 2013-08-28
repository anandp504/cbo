package com.tumri.cbo.backend;

/** An interface for all classes that listen to bidder state changes.
 */
public interface BidderStateChangeListener {
	
	/** This method is called after the bidder state has changed.
	 * @param e The bidder state change event.
	 */
	public void bidderStateChanged(BidderStateChangedEvent e);
}
