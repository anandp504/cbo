package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.quartz.*;

import com.tumri.af.utils.DateUtils;

import java.util.Date;


public class BidderScheduler
{
    private static int triggerCounter = -1;
    
    public static synchronized int getNextSchedulerUID()
    {
        triggerCounter = triggerCounter + 1;
        return triggerCounter;
    }

    @SuppressWarnings("unused")
	public static Trigger makeImmediateTrigger ()
	{
		return new SimpleTrigger("immediateTrigger_" + getNextSchedulerUID(),
                                 "group1", new Date());
	}

    @SuppressWarnings("unused")
    public static Trigger makeMinuteTrigger ()
    {
        return makeMinuteTrigger("minuteTrigger_" + getNextSchedulerUID());
    }

    public static Trigger makeMinuteTrigger (String name)
    {
    	// Starts in 1 min.
        Trigger trigger = TriggerUtils.makeSecondlyTrigger(60);
        trigger.setName(name);
        trigger.setGroup("group1");
        trigger.setStartTime(getOneMinuteFromNow());
        return trigger;
    }

    public static Trigger makeThreeMinuteTrigger (String name)
    {
    	// Starts in 1 min.
        Trigger trigger = TriggerUtils.makeSecondlyTrigger(180);
        trigger.setName(name);
        trigger.setGroup("group1");
        trigger.setStartTime(getOneMinuteFromNow());
        return trigger;
    }

    public static Trigger makeFiveMinuteTrigger (String name)
    {
    	// Starts in 1 minute.
        Trigger trigger = TriggerUtils.makeSecondlyTrigger(300);
        trigger.setName(name);
        trigger.setGroup("group1");
        trigger.setStartTime(getOneMinuteFromNow());
        return trigger;
    }

    public static Trigger makeTenMinuteTrigger (String name)
    {
    	// Starts in 1 minute.
        Trigger trigger = TriggerUtils.makeSecondlyTrigger(600);
        trigger.setName(name);
        trigger.setGroup("group1");
        trigger.setStartTime(getOneMinuteFromNow());
        return trigger;
    }

    @SuppressWarnings("unused")
	public static Trigger makeHourlyTrigger ()
	{
        return makeHourlyTrigger("hourlyTrigger");
	}

	public static Trigger makeHourlyTrigger (String name)
	{
        return makeHourlyTrigger(name, DEFAULT_HOURLY_TRIGGER_GROUP, 0);
	}

	public static Trigger makeHourlyTrigger
            (String name, String group, int minute)
	{
		// Triggers each hour.
    	// Starts at the next Minute after 5 min from now.
		Trigger trigger = TriggerUtils.makeHourlyTrigger(name);
		trigger.setName(name);
		trigger.setGroup(group);
		Date startTime =
           new Date((1000 * 60 * minute) +
                    TriggerUtils.getEvenHourDate
                            (getNMinutesFromNow(5)).getTime());
		Utils.logThisPoint(Level.DEBUG, "Making 1 hour trigger for " + name +
                                 " that will start at " + startTime);
		System.out.println
                ("Making 1 hour trigger for " + name +
                 " that will start at " + startTime);
		trigger.setStartTime(startTime);
		return trigger;
	}

    @SuppressWarnings("unused")
	public static Trigger makeDailyTrigger ()
	{
        return makeDailyTrigger("dailyTrigger");
	}

    static final int DEFAULT_DAILY_TRIGGER_HOUR = 1;
    static final int DEFAULT_DAILY_TRIGGER_MINUTE = 0;
    static final int DAILY_LATE_TRIGGER_HOUR = 3;
    static final int DAILY_LATE_TRIGGER_MINUTE = 40;
    public static final String DEFAULT_HOURLY_TRIGGER_GROUP = "group1";
    public static final String DEFAULT_DAILY_TRIGGER_GROUP = "group1";

	public static Trigger makeDailyTrigger (String name)
	{
		// Triggers at 1am in the current time zone every day.
		// Starts at the next 1 AM after 5 min from now.
        return makeDailyTrigger(name, DEFAULT_DAILY_TRIGGER_GROUP,
                                DEFAULT_DAILY_TRIGGER_HOUR,
                                DEFAULT_DAILY_TRIGGER_MINUTE);
	}

	public static Trigger makeDailyTrigger
            (String name, String group, int hour, int minute)
	{
        Utils.logThisPoint(Level.DEBUG, "Making daily trigger for " + name +
                                 " that will start at " + hour + ":" + minute);
		System.out.println
                ("Making daily trigger for " + name +
                  " that will start at " + hour + ":" + minute);
        Trigger trigger = TriggerUtils.makeDailyTrigger(name, hour, minute);
		trigger.setName(name);
		trigger.setGroup(group);
		trigger.setStartTime(getNMinutesFromNow(5));
		return trigger;
	}

	public static Trigger makeWeeklyTrigger (String name)
	{
		// Triggers at 1am Sunday in the current time zone once a week day.
		// Starts at the next occurrence of 1 AM Sunday following 5 min from now.
		Trigger trigger = TriggerUtils.makeWeeklyTrigger
                                (name, TriggerUtils.SUNDAY, 1, 0);
		trigger.setGroup("group1");
		trigger.setStartTime(getNMinutesFromNow(5));
		return trigger;
	}

	
	/** Gets a Date that is 1 minute from now.
	 * @return A date that is 1 minute from now.
	 */
	private static Date getOneMinuteFromNow() {
		return getNMinutesFromNow(1);
	}
	
	/** Gets a Date that is n minutes from now.
	 * @param n The number of minutes from now.
	 * @return A date that is n minutes from now.
	 */
	private static Date getNMinutesFromNow(int n) {
		return new Date(System.currentTimeMillis() + n*DateUtils.ONE_MINUTE_MS);
	}
}
