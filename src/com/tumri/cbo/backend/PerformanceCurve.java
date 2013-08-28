package com.tumri.cbo.backend;

import java.util.List;


class PerformanceCurve {
    List<PerformanceHistoryRow> points;
    Long campaignId;
    String campaignName;

    public PerformanceCurve
	(List<PerformanceHistoryRow> points, Long campaignId,
	 String campaignName)
    {
        this.points = points;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
    }
}
