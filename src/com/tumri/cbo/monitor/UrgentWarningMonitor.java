package com.tumri.cbo.monitor;

public abstract class UrgentWarningMonitor extends WarningMonitor {

    public UrgentWarningMonitor(MessageReport[] applicableReports,
                                ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports == null
                ? MessageReport.URGENT_REPORTS
                : applicableReports,
              includedColumns);
    }
}
