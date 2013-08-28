package com.tumri.cbo.backend;

public class AdvertiserCombinedDataChangesPerspective
    extends AdvertiserDataChangesPerspective {

    public static AdvertiserCombinedDataChangesPerspective PERSPECTIVE =
	                    new AdvertiserCombinedDataChangesPerspective();

    private AdvertiserCombinedDataChangesPerspective()
    {
        super("Combined Changed campaigns for advertiser", 25);
        returnCombinedP = true;
    }

}
