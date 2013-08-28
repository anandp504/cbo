package com.tumri.cbo.monitor;

public abstract class UrgentNotifyMonitor extends NotifyMonitor {

    public UrgentNotifyMonitor(MessageReport[] applicableReports,
                               ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.URGENT_REPORTS
                : applicableReports,
               includedColumns);
    }
}
