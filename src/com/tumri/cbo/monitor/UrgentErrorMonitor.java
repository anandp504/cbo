package com.tumri.cbo.monitor;

public abstract class UrgentErrorMonitor extends ErrorMonitor {

    public UrgentErrorMonitor(MessageReport[] applicableReports,
                              ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.URGENT_REPORTS
                : applicableReports, 
              includedColumns);
    }
}
