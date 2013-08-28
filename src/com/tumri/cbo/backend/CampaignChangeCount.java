package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;

import java.sql.ResultSet;
import java.sql.SQLException;


public class CampaignChangeCount
{
    @SuppressWarnings("unused")
    public static final String firstSlotName =
            "attribute_changed_but_will_not_affect_delivery";
    long attribute_Changed_But_Will_Not_Affect_Delivery = 0;
    long attribute_Changed_With_Unknown_Effect_On_Delivery = 0;
    long attribute_Changed_Increases_Delivery   = 0;
    long attribute_Changed_Decreases_Delivery   = 0;
    long attribute_Increased_Increases_Delivery = 0;
    long attribute_Decreased_Decreases_Delivery = 0;
    long attribute_Increased_Decreases_Delivery = 0;
    long attribute_Decreased_Increases_Delivery = 0;
    long targeting_Widened_Increases_Delivery   = 0;
    long targeting_Narrowed_Decreases_Delivery  = 0;
    long unchanged = 0;

    public CampaignChangeCount() { }

    public long getCountOfChanges ()
    {
        return attribute_Changed_But_Will_Not_Affect_Delivery +
               attribute_Changed_With_Unknown_Effect_On_Delivery +
               attribute_Changed_Increases_Delivery   +
               attribute_Changed_Decreases_Delivery   +
               attribute_Increased_Increases_Delivery +
               attribute_Decreased_Decreases_Delivery +
               attribute_Increased_Decreases_Delivery +
               attribute_Decreased_Increases_Delivery +
               targeting_Widened_Increases_Delivery   +
               targeting_Narrowed_Decreases_Delivery;
    }

    public boolean isNonTrivial ()
    {
        return attribute_Changed_But_Will_Not_Affect_Delivery > 0 ||
               attribute_Changed_With_Unknown_Effect_On_Delivery > 0 ||
               attribute_Changed_Increases_Delivery   > 0 ||
               attribute_Changed_Decreases_Delivery   > 0 ||
               attribute_Increased_Increases_Delivery > 0 ||
               attribute_Decreased_Decreases_Delivery > 0 ||
               attribute_Increased_Decreases_Delivery > 0 ||
               attribute_Decreased_Increases_Delivery > 0 ||
               targeting_Widened_Increases_Delivery   > 0 ||
               targeting_Narrowed_Decreases_Delivery  > 0;
    }

    public CampaignChangeCount(ResultSet rs, int offset)
            throws SQLException
    {
        attribute_Changed_But_Will_Not_Affect_Delivery = rs.getLong(offset);
        attribute_Changed_With_Unknown_Effect_On_Delivery = rs.getLong(offset + 1);
        attribute_Changed_Increases_Delivery   = rs.getLong(offset + 2);
        attribute_Changed_Decreases_Delivery   = rs.getLong(offset + 3);
        attribute_Increased_Increases_Delivery = rs.getLong(offset + 4);
        attribute_Decreased_Decreases_Delivery = rs.getLong(offset + 5);
        attribute_Increased_Decreases_Delivery = rs.getLong(offset + 6);
        attribute_Decreased_Increases_Delivery = rs.getLong(offset + 7);
        targeting_Widened_Increases_Delivery   = rs.getLong(offset + 8);
        targeting_Narrowed_Decreases_Delivery  = rs.getLong(offset + 9);
    }

    public static CampaignChangeCount accumulate
            (CampaignChangeCount a, CampaignChangeCount b)
    {
        if(a == null) return b;
        else if (b == null) return a;
        else
        {
            CampaignChangeCount res = new CampaignChangeCount();
            res.attribute_Changed_But_Will_Not_Affect_Delivery =
                    a.attribute_Changed_But_Will_Not_Affect_Delivery +
                            b.attribute_Changed_But_Will_Not_Affect_Delivery;
            res.attribute_Changed_With_Unknown_Effect_On_Delivery =
                    a.attribute_Changed_With_Unknown_Effect_On_Delivery +
                            b.attribute_Changed_With_Unknown_Effect_On_Delivery;
            res.attribute_Changed_Increases_Delivery =
                    a.attribute_Changed_Increases_Delivery +
                            b.attribute_Changed_Increases_Delivery;
            res.attribute_Changed_Decreases_Delivery =
                    a.attribute_Changed_Decreases_Delivery +
                            b.attribute_Changed_Decreases_Delivery;
            res.attribute_Increased_Increases_Delivery =
                    a.attribute_Increased_Increases_Delivery +
                            b.attribute_Increased_Increases_Delivery;
            res.attribute_Decreased_Decreases_Delivery =
                    a.attribute_Decreased_Decreases_Delivery +
                            b.attribute_Decreased_Decreases_Delivery;
            res.attribute_Increased_Decreases_Delivery =
                    a.attribute_Increased_Decreases_Delivery +
                            b.attribute_Increased_Decreases_Delivery;
            res.attribute_Decreased_Increases_Delivery =
                    a.attribute_Decreased_Increases_Delivery +
                            b.attribute_Decreased_Increases_Delivery;
            res.targeting_Widened_Increases_Delivery =
                    a.targeting_Widened_Increases_Delivery +
                            b.targeting_Widened_Increases_Delivery;
            res.targeting_Narrowed_Decreases_Delivery =
                    a.targeting_Narrowed_Decreases_Delivery +
                            b.targeting_Narrowed_Decreases_Delivery;
            return res;
        }
    }

    public String describe()
    {
        StringBuffer s = new StringBuffer();
        boolean emitted = false;
        if(attribute_Changed_But_Will_Not_Affect_Delivery > 0)
        {
            // if(emitted) s.append(", ");
            s.append("attribute_Changed_But_Will_Not_Affect_Delivery: ");
            s.append(attribute_Changed_But_Will_Not_Affect_Delivery);
            emitted = true;
        }
        if(attribute_Changed_With_Unknown_Effect_On_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Changed_With_Unknown_Effect_On_Delivery: ");
            s.append(attribute_Changed_With_Unknown_Effect_On_Delivery);
            emitted = true;
        }
        if(attribute_Changed_Increases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Changed_Increases_Delivery: ");
            s.append(attribute_Changed_Increases_Delivery);
            emitted = true;
        }
        if(attribute_Changed_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Changed_Decreases_Delivery: ");
            s.append(attribute_Changed_Decreases_Delivery);
            emitted = true;
        }
        if(attribute_Increased_Increases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Increased_Increases_Delivery: ");
            s.append(attribute_Increased_Increases_Delivery);
            emitted = true;
        }
        if(attribute_Decreased_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Decreased_Decreases_Delivery: ");
            s.append(attribute_Decreased_Decreases_Delivery);
            emitted = true;
        }
        if(attribute_Increased_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Increased_Decreases_Delivery: ");
            s.append(attribute_Increased_Decreases_Delivery);
            emitted = true;
        }
        if(attribute_Decreased_Increases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("attribute_Decreased_Increases_Delivery: ");
            s.append(attribute_Decreased_Increases_Delivery);
            emitted = true;
        }
        if(targeting_Widened_Increases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("targeting_Widened_Increases_Delivery: ");
            s.append(targeting_Widened_Increases_Delivery);
            emitted = true;
        }
        if(targeting_Narrowed_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("targeting_Narrowed_Decreases_Delivery: ");
            s.append(targeting_Narrowed_Decreases_Delivery);
            emitted = true;
        }
        return (emitted ? "{ " + s.toString() + " }" : null);
    }

    public String summarise()
    {
        StringBuffer s = new StringBuffer();
        boolean emitted = false;
        if(targeting_Widened_Increases_Delivery > 0)
        {
            // if(emitted) s.append(", ");
            s.append("Targeting widened");
            emitted = true;
        }
        if(targeting_Narrowed_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("Targeting narrowed");
            emitted = true;
        }
        if(attribute_Changed_Increases_Delivery > 0 ||
           attribute_Decreased_Increases_Delivery > 0 ||
           attribute_Increased_Increases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("Changes increase delivery");
            emitted = true;
        }
        if(attribute_Changed_Decreases_Delivery > 0 ||
           attribute_Decreased_Decreases_Delivery > 0 ||
           attribute_Increased_Decreases_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("Changes decrease delivery");
            emitted = true;
        }
        if(attribute_Changed_With_Unknown_Effect_On_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("Changes with unknown effect");
            emitted = true;
        }
        if(attribute_Changed_But_Will_Not_Affect_Delivery > 0)
        {
            if(emitted) s.append(", ");
            s.append("Changes, but delivery unaffected");
            emitted = true;
        }
        if(emitted)
            return s.toString();
        else return null;
    }

    public String toString()
    {
        long changes = getCountOfChanges();
        if(changes > 0)
             return "[" + AppNexusUtils.afterDot(this.getClass().getName()) +
                    " - Non-trivial (" + Long.toString(changes) +  ")]";
        else return "[" + AppNexusUtils.afterDot(this.getClass().getName()) +
                    "]";
    }
}
