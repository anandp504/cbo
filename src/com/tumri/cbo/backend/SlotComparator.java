package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.util.*;

public class SlotComparator implements Comparator<AdvCampDataPair> {
    ExcelSlotSchema sortSlot;
    String slotName;
    boolean descendingP;
    MethodMapper methodMapper;
    QueryContext qctx;

    public SlotComparator(ExcelSlotSchema sortSlot, String sortDirection,
                          MethodMapper methodMapper, QueryContext qctx)
    {
        this.sortSlot = sortSlot;
        descendingP = BidderDashboardHTTPHandler.DESCENDING.equals
                (sortDirection);
        this.methodMapper = methodMapper;
        this.qctx = qctx;
        slotName = sortSlot.getSlotName();
    }

    @SuppressWarnings("unchecked")
    public int compare(AdvCampDataPair x, AdvCampDataPair y)
    {
        CampaignData cdx = x.campaign;
        CampaignData cdy = y.campaign;
        Comparable vx = (Comparable)methodMapper.getValue(cdx, slotName, qctx);
        Comparable vy = (Comparable)methodMapper.getValue(cdy, slotName, qctx);
        int res;
        if(vx == null)
        {
            if(vy == null) res = 0;
            else res = 1;
        }
        else if(vy == null) res = -1;
        else res = vx.compareTo(vy);
        if(descendingP) res = -res;
        return res;
    }

    public boolean equals(Object obj)
    {
        return obj instanceof SlotComparator && this == obj;
    }
}
