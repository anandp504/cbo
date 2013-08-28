package com.tumri.cbo.monitor;

// The names of these enums match the constraint column names in the
// USERS table.
public enum ReportApplicabilityColumn
{
    FOR_USERS("for_users", "User"),
    FOR_ADMINS("for_admins", "Admin");

    String columnName;
    String prettyName;
    
    ReportApplicabilityColumn(String columnName, String prettyName)
    {
        this.columnName = columnName;
        this.prettyName = prettyName;
    }
    
    String getColumnName()
    {
        return columnName;
    }

    String getPrettyName()
    {
        return prettyName;
    }
}
