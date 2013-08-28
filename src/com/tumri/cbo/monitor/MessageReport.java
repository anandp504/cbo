package com.tumri.cbo.monitor;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public enum MessageReport 
{
    @SuppressWarnings("unused")
    FOR_USERS(ReportApplicabilityColumn.FOR_USERS), 
    @SuppressWarnings("unused")
    FOR_ADMINS(ReportApplicabilityColumn.FOR_ADMINS),
    @SuppressWarnings("unused")
    URGENT_FOR_USERS(ReportApplicabilityColumn.FOR_USERS),
    @SuppressWarnings("unused")
    URGENT_FOR_ADMINS(ReportApplicabilityColumn.FOR_ADMINS);

    public static final MessageReport[] NON_URGENT_REPORTS = 
            { FOR_USERS, FOR_ADMINS };
    public static final MessageReport[] URGENT_REPORTS =
            { URGENT_FOR_ADMINS, URGENT_FOR_USERS };

    List<ReportApplicabilityColumn> includeColumns =
            new Vector<ReportApplicabilityColumn>();
    List<ReportApplicabilityColumn> excludeColumns =
            new Vector<ReportApplicabilityColumn>();

    List<ReportApplicabilityColumn> getIncludeColumns()
    {
        return includeColumns;
    }

    @SuppressWarnings("unused")
    List<ReportApplicabilityColumn> getExcludeColumns()
    {
        return excludeColumns;
    }

    MessageReport(ReportApplicabilityColumn... columns)
    {
        this.includeColumns.addAll(Arrays.asList(columns));
    }
    
    @SuppressWarnings("unused")
    MessageReport(List<ReportApplicabilityColumn> includeColumns,
                  List<ReportApplicabilityColumn> excludeColumns)
    {
        this.includeColumns.addAll(includeColumns);
        this.excludeColumns.addAll(excludeColumns);
    }

    String toConjuncts()
    {
        StringBuffer b = new StringBuffer();
        for(ReportApplicabilityColumn col: includeColumns)
        {
            b.append("\nAND   ");
            b.append(col.getColumnName());
            b.append(" = true;");
        }
        for(ReportApplicabilityColumn col: excludeColumns)
        {
            b.append("\nAND   ");
            b.append(col.getColumnName());
            b.append(" = true;");
        }
        return b.toString();
    }
    
}
