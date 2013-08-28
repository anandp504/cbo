package com.tumri.cbo.backend;

public class CombinedDataHistoryPerspective
        extends CampaignDataHistoryPerspective {

    public static CombinedDataHistoryPerspective PERSPECTIVE =
	                    new CombinedDataHistoryPerspective();

    private CombinedDataHistoryPerspective()
    {
        super("Combined change history", 25);
        returnCombinedP = true;
    }

    CombinedDataHistoryPerspective(String name, int priority)
    {
        super(name, priority);
        returnCombinedP = true;
    }

}
