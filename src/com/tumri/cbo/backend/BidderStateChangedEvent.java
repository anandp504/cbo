package com.tumri.cbo.backend;

/** An event that indicates the bidder state has changed.
 */
public class BidderStateChangedEvent {
	
	private long m_eventTime;
	private Bidder m_bidder;
	private BidderState m_bidderState;
	
	/** Constructor that specifies the bidder and the state it was changed to.
	 * @param b The bidder.
	 * @param bs The state the bidder was changed to.
	 */
	public BidderStateChangedEvent(Bidder b, BidderState bs) {
		m_bidder = b;
		m_eventTime = System.currentTimeMillis();
		m_bidderState = bs;
	}
	
	/** Gets the event time in mS.
	 * This is always the mS in UTC since the start of the 1970 era.
	 * @return The event time in mS.
	 */
	public final long getEventTime() {
		return m_eventTime;
	}
	
	/** Gets the bidder whose state was changed.
	 * @return The bidder whose state was changed.
	 */
	public Bidder getBidder() {
		return m_bidder;
	}
	
	/** Gets the state that the bidder was changed to at the time of the change.
	 * Note the bidder state may have changed between the time
	 * this event was created and time it is received by the listener.
 	 * @return The new bidder state that bidder was changed to.
	 */
	public final BidderState getBidderState() {
		return m_bidderState;
	}	
}
