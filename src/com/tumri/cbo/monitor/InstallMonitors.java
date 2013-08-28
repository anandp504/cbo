package com.tumri.cbo.monitor;

public class InstallMonitors {
    
    static ReportApplicabilityColumn forUsers = 
            ReportApplicabilityColumn.FOR_USERS;
    static ReportApplicabilityColumn forAdmins =
            ReportApplicabilityColumn.FOR_ADMINS;

    public static void init()
    {
	    AbstractMonitor.addMonitor(new BadSiteDistributionWarning(forUsers));
	    AbstractMonitor.addMonitor(new BidStrategyChangedNotification(forUsers));
        AbstractMonitor.addMonitor(new BidOscillationWarning(forUsers));
 	    AbstractMonitor.addMonitor(new CampaignEndedNotification(forUsers));
        AbstractMonitor.addMonitor(new CampaignEndingSoonNotification(forUsers));
        AbstractMonitor.addMonitor(new DailyImpressionTargetLargePctChangeNotification(forUsers));
        AbstractMonitor.addMonitor(new DailyImpressionBudgetChangedNotification(forUsers));
        AbstractMonitor.addMonitor(new ImpressionsOscillationWarning(forUsers));
 	    AbstractMonitor.addMonitor(new LoggedCampaignNotification(forUsers));
 	    AbstractMonitor.addMonitor(new LoggedError(forAdmins));
 	    AbstractMonitor.addMonitor(new LifetimeImpressionBudgetChangedNotification(forUsers));
 	    AbstractMonitor.addMonitor(new MaxBidChangedNotification(forUsers));
        AbstractMonitor.addMonitor(new NoCampaignTZWarning(forUsers));
 	    AbstractMonitor.addMonitor(new StartedDeliveryLateNotification(forUsers));
 	    AbstractMonitor.addMonitor(new StoppedDeliveryEarlyNotification(forUsers));
 	    AbstractMonitor.addMonitor(new TargetingChangedNotification(forUsers));
        AbstractMonitor.addMonitor(new UnderOrOverPacingWarning(forUsers));
    }
}