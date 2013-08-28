package com.tumri.cbo.monitor;

public abstract class NonUrgentWarningMonitor extends WarningMonitor {

    public NonUrgentWarningMonitor
            (MessageReport[] applicableReports,
             ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.NON_URGENT_REPORTS
                : applicableReports, 
              includedColumns);
    }
}
