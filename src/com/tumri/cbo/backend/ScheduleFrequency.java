package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.Utils;


public enum ScheduleFrequency {
    FREQUENCY_IMMEDIATE(-1),
    FREQUENCY_ONE_MIN(1),
    FREQUENCY_THREE_MINS(3),
    FREQUENCY_FIVE_MINS(5),
    FREQUENCY_TEN_MINS(10),
    FREQUENCY_HOURLY(60),
    FREQUENCY_HOURLY_LATE(60),
    FREQUENCY_DAILY(24 * 60),
    FREQUENCY_DAILY_LATE(24 * 60),
    FREQUENCY_WEEKLY(7 * 24 * 60);

    private int frequency;

    public int getFrequency()
    {
        return frequency;
    }

    public static ScheduleFrequency get(int i)
    {
        for(ScheduleFrequency f: ScheduleFrequency.values())
        {
            if(i == f.getFrequency()) return f;
        }
        throw Utils.barf("Cannot find frequency", null, i);
    }

    public static ScheduleFrequency get(String s)
    {
        for(ScheduleFrequency f: ScheduleFrequency.values())
        {
            if(s.equals(f.name())) return f;
        }
        throw Utils.barf("Cannot find frequency", null, s);
    }

    private ScheduleFrequency(int freq)
    {
        this.frequency = freq;
    }

}
