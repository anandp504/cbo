package com.tumri.cbo.backend;

/** When CBO detects that a new campaign has started it may
 * automatically change its bidding policy to the 
 * Adjustable Daily Impressions bidding policy.
 * <p>
 * This class contains constants that are used in the local.properties
 * file to determine when and if the bidder should change the bidding
 * policy.  
 * <ul>
 * <li>AFTER_DELAY means let the bidder bid at ECP a day before changing the policy</li>.
 * <li>IMMEDIATE means change the bidding policy as soon as a new campaign is detected</li>.
 * <li>DO_NOTHING means don't change the bidding policy automatically.</li>
 * </ul>
 */
public enum NewCampaignBidImpositionPolicy 
{
    AFTER_DELAY,
    IMMEDIATE,
    DO_NOTHING
}
