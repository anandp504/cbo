package com.tumri.cbo.backend;

/** An exception that is thrown if an AppNexus campaign is not configured properly.
 */
public class CampaignConfigurationException extends Exception {

	private static final long serialVersionUID = -4552245877406549013L;

	CampaignConfigurationException() {
		super();
	}

	CampaignConfigurationException(String msg) {
		super(msg);
	}
	
}
