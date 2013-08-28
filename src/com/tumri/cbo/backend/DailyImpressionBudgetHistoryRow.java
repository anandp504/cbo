package com.tumri.cbo.backend;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Sexpression;

import java.util.*;


public class DailyImpressionBudgetHistoryRow {
    Date eventTime;
    Long budget;

    public String toString()
    {
        return "["+ AppNexusUtils.afterDot(this.getClass().getName()) + ": "
                  + eventTime + ", " + budget +"]";
    }

    DailyImpressionBudgetHistoryRow (Sexpression eventTime, Sexpression budget)
    {
        this.eventTime = eventTime.unboxDate();
	    this.budget = budget.unboxLong();
    }
}
