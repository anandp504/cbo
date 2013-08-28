package com.tumri.cbo.monitor;

public abstract class NonUrgentNotifyMonitor extends NotifyMonitor {

    public NonUrgentNotifyMonitor(MessageReport[] applicableReports,
                                  ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.NON_URGENT_REPORTS
                : applicableReports,
              includedColumns);
    }
}
