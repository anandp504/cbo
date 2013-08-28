package com.tumri.cbo.backend;

import com.tumri.af.context.TimeScale;
import com.tumri.mediabuying.zini.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class TimeScaleIterator implements Iterator<String>{

    TimeScale ts;
    Date startDate;
    Date endDate;
    Date currentDate;
    long endTime;
    long hourOffset;
    SimpleDateFormat format;
    TimeZone wrtTimeZone;
    Calendar calendar;
    public static SimpleDateFormat    hourlyFormat() { return new SimpleDateFormat("yyyy-MM-dd HH"); }
    public static SimpleDateFormat     dailyFormat() { return new SimpleDateFormat("yyyy-MM-dd"); }
    public static SimpleDateFormat    weeklyFormat() { return new SimpleDateFormat("yyyy-ww"); }
    public static SimpleDateFormat   monthlyFormat() { return new SimpleDateFormat("yyyy-MM"); }
    public static SimpleDateFormat quarterlyFormat() { return new SimpleDateFormat("yyyy-"); }
    public static SimpleDateFormat    yearlyFormat() { return new SimpleDateFormat("yyyy"); }
    private static ThreadLocal<Map<TimeScale, SimpleDateFormat>> formats =
            new ZThreadLocal<Map<TimeScale, SimpleDateFormat>>();

    private static HashMap<TimeScale, SimpleDateFormat> makeFormats()
    {
        return
            new HashMap<TimeScale, SimpleDateFormat>()
            {
                private static final long serialVersionUID =
                        7526472295622776147L;
                {
                    put(TimeScale.HOURLY, hourlyFormat());
                    put(TimeScale.DAILY, dailyFormat());
                    put(TimeScale.WEEKLY, weeklyFormat());
                    put(TimeScale.MONTHLY, monthlyFormat());
                    put(TimeScale.QUARTERLY, quarterlyFormat());
                    put(TimeScale.YEARLY, yearlyFormat());
                }
            };
    }

    public static Map<TimeScale, SimpleDateFormat> getFormats()
    // Will be ThreadLocal
    {
        Map<TimeScale, SimpleDateFormat> f = formats.get();
        if(f == null)
        {
            f = makeFormats();
            formats.set(f);
        }
        return f;
    }

    @SuppressWarnings("unused")
    public static SimpleDateFormat getFormatFor(TimeScale ts)
    {
        return getFormats().get(ts);
    }

    private static Map<TimeZone, Map<TimeScale, SimpleDateFormat>>
            timeZoneFormats =
                new HashMap<TimeZone, Map<TimeScale, SimpleDateFormat>>();

    public static SimpleDateFormat getFormatFor(TimeScale ts, TimeZone tz)
    {
        Map<TimeScale, SimpleDateFormat> formatEntry = timeZoneFormats.get(tz);
        if(formatEntry == null)
        {
            formatEntry = new HashMap<TimeScale, SimpleDateFormat>();
            timeZoneFormats.put(tz, formatEntry);
        }
        SimpleDateFormat f = formatEntry.get(ts);
        if(f == null)
        {
            f = (SimpleDateFormat)getFormats().get(ts).clone();
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(tz);
            f.setCalendar(cal);
            formatEntry.put(ts, f);
        }
        return f;
    }

    public TimeScaleIterator
            (TimeScale theTs, Date theStartDate, Date theEndDate,
             TimeZone wrtTimeZone, Date now)
    {
        // We have local times, and we need to offset them to make them look
        // like they come from the wrtTimeZone.
        long localTimeZoneOffset =
                Calendar.getInstance().getTimeZone().getOffset(now.getTime());
        long wrtTimeZoneOffset = wrtTimeZone.getOffset(now.getTime());
        hourOffset = (localTimeZoneOffset - wrtTimeZoneOffset) / 3600000;
        Calendar cal = Calendar.getInstance();
        this.wrtTimeZone = wrtTimeZone;
        ts = theTs;
        format = getFormatFor(ts, wrtTimeZone);
        startDate = theStartDate;
        endDate = theEndDate;
        //-------------
        endTime = endDate.getTime();
        Date offsetDate = new Date(startDate.getTime() - (hourOffset * 3600000));
        cal.setTime(offsetDate); // startDate);
        timeScaleFloor(ts, cal);
        currentDate = new Date(cal.getTime().getTime() + (hourOffset * 3600000));
    }

    void timeScaleFloor(TimeScale ts, Calendar cal)
    {
        if(ts == TimeScale.HOURLY)
        {
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
        }
        else if(ts == TimeScale.DAILY)
        {
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        }
        else if(ts == TimeScale.WEEKLY)
        {
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        }
        else if(ts == TimeScale.MONTHLY)
        {
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        else if(ts == TimeScale.QUARTERLY)
        {
            int month = cal.get(Calendar.MONTH);
            int newMonth = (((month - 1) / 3) * 3) + 1;
            cal.set(Calendar.MONTH, newMonth);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        else if(ts == TimeScale.YEARLY)
        {
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.MONTH, 1);
        }
        else { throw Utils.barf("Unhandled timescale", null, ts); }
    }

    public static String rollUpDate(Date thisDate, TimeZone tz, TimeScale ts)
    {
        boolean dst = tz.inDaylightTime(thisDate);
        DateFormat df = TimeScaleIterator.getFormatFor(ts, tz);
        String res;
        if(ts == TimeScale.QUARTERLY)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(thisDate);
            int month = cal.get(Calendar.MONTH); // Months are ZERO-based.
            int quarter = (month / 3) + 1;
            res = df.format(thisDate) + "Q" + quarter;
        }
        else res = df.format(thisDate);
        // Add the TZ if we're in hourly mode.  Since DST is important
        // for hourly time resolution, we have to supply yht TZ, otherwise
        // we can roll up to >= daily timescale.
        res = res + (ts == TimeScale.HOURLY
                        ? " " + tz.getDisplayName(dst, TimeZone.SHORT)
                        : "");
        return res;
    }

    String formatDate(Date date)
    {
        return rollUpDate(date, wrtTimeZone, ts);
    }

    public boolean hasNext()
    {
        return currentDate != null;
    }

    public String next()
    {
        if(currentDate == null) return null;
        else
        {
            String res = formatDate(currentDate);
            currentDate = incrementDate(currentDate);
            if(currentDate.getTime() > endTime) currentDate = null;
            return res;
        }
    }

    public void remove()
    {
        throw Utils.barf("Not implemented", this);
    }

    Date incrementDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(wrtTimeZone);
        if(ts == TimeScale.HOURLY) return new Date(date.getTime() + 3600000L);
        else if(ts == TimeScale.DAILY)
            return new Date(date.getTime() + 24 * 3600000L);
        else if(ts == TimeScale.WEEKLY)
            return new Date(date.getTime() + 7 * 24 * 3600000L);
        else if(ts == TimeScale.MONTHLY)
        {
            cal.add(Calendar.MONTH, 1);
            return cal.getTime();
        }
        else if(ts == TimeScale.QUARTERLY)
        {
            cal.add(Calendar.MONTH, 3);
            return cal.getTime();
        }
        else if(ts == TimeScale.YEARLY)
        {
            cal.add(Calendar.YEAR, 1);
            return cal.getTime();
        }
        else { throw Utils.barf("Unhandled timescale", null, ts); }
    }

    public static Date timeCeiling(Date time, TimeScale ts, TimeZone tz)
    {
        Date res;
        Calendar cal = Calendar.getInstance();
        if(tz != null) cal.setTimeZone(tz);
        cal.setTime(time);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        switch(ts)
        {
            case YEARLY:    cal.set(Calendar.HOUR_OF_DAY, 23);
                            cal.set(Calendar.MINUTE, 59);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.MONTH, Calendar.JANUARY);
                            cal.add(Calendar.YEAR, 1);
                            cal.add(Calendar.DAY_OF_MONTH, -1);
                            break;
            case QUARTERLY: cal.set(Calendar.HOUR_OF_DAY, 23);
                            cal.set(Calendar.MINUTE, 59);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            int m = cal.get(Calendar.MONTH);
                            if(m > Calendar.SEPTEMBER)
                            {
                                cal.set(Calendar.MONTH, Calendar.JANUARY);
                                cal.add(Calendar.YEAR, 1);
                            }
                            else if(m > Calendar.JUNE)
                                cal.set(Calendar.MONTH, Calendar.OCTOBER);
                            else if(m > Calendar.MARCH)
                                cal.set(Calendar.MONTH, Calendar.JULY);
                            else cal.set(Calendar.MONTH, Calendar.APRIL);
                            cal.add(Calendar.DAY_OF_MONTH, -1);
                            break;
            case MONTHLY:   cal.set(Calendar.HOUR_OF_DAY, 23);
                            cal.set(Calendar.MINUTE, 59);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.add(Calendar.MONTH, 1);
                            cal.add(Calendar.DAY_OF_MONTH, -1);
                            break;
            case WEEKLY:    cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                            cal.set(Calendar.HOUR_OF_DAY, 23);
                            break;
            case DAILY:     cal.set(Calendar.HOUR_OF_DAY, 23);
                            break;
            case HOURLY:    break;
            default:
                throw Utils.barf("Unhandled timescale", null, time, ts, tz);
        }
        res = cal.getTime();
        return res;
    }

    public static Date timeFloor(Date time, TimeScale ts, TimeZone tz)
    {
        Date res;
        int m;
        Calendar cal = Calendar.getInstance();
        if(tz != null) cal.setTimeZone(tz);
        cal.setTime(time);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        switch(ts)
        {
            case YEARLY:    cal.set(Calendar.MONTH, Calendar.JANUARY);
                            m = cal.get(Calendar.MONTH);
                            if(m > 9) cal.set(Calendar.MONTH, Calendar.OCTOBER);
                            else if(m > 6) cal.set(Calendar.MONTH, Calendar.JULY);
                            else if(m > 3) cal.set(Calendar.MONTH, Calendar.APRIL);
                            else cal.set(Calendar.MONTH, Calendar.JANUARY);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            break;
            case QUARTERLY: m = cal.get(Calendar.MONTH);
                            if(m > 9) cal.set(Calendar.MONTH, Calendar.OCTOBER);
                            else if(m > 6) cal.set(Calendar.MONTH, Calendar.JULY);
                            else if(m > 3) cal.set(Calendar.MONTH, Calendar.APRIL);
                            else cal.set(Calendar.MONTH, Calendar.JANUARY);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            break;
            case MONTHLY:   cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            break;
            case WEEKLY:    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            break;
            case DAILY:     cal.set(Calendar.HOUR_OF_DAY, 0);
                            break;
            case HOURLY:    break;
            default:
                throw Utils.barf("Unhandled timescale", null, time, ts, tz);
        }
        res = cal.getTime();
        return res;
    }

}
