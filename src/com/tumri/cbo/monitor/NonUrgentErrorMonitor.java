package com.tumri.cbo.monitor;

public abstract class NonUrgentErrorMonitor extends ErrorMonitor {

    public NonUrgentErrorMonitor(MessageReport[] applicableReports,
                                 ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.NON_URGENT_REPORTS
                : applicableReports,
              includedColumns);
    }
}
