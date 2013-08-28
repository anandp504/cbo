package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.BidderSchedulerJob;
import com.tumri.cbo.backend.ScheduleFrequency;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.List;
import java.util.Vector;

public abstract class MonitorSchedulerJob implements Job
{
     // Should be false for real config, true to disable.
    protected Thread owningThread = null;
    private static boolean initialised = false;
    public static final ScheduleFrequency DEFAULT_USER_FREQUENCY =
            ScheduleFrequency.FREQUENCY_DAILY_LATE;
    public static final ScheduleFrequency DEFAULT_ADMIN_FREQUENCY =
            ScheduleFrequency.FREQUENCY_HOURLY_LATE;
    public static final ScheduleFrequency DEFAULT_URGENT_FREQUENCY =
            ScheduleFrequency.FREQUENCY_HOURLY_LATE;
    // OK, so we tenure these, but there should only ever be two of them, and
    // hanging on to them should help debugging!
    static List<JobDetail> jobDetails = new Vector<JobDetail>();
    static List<Trigger> triggers = new Vector<Trigger>();

    static Scheduler messageScheduler = null;

    // --------------------------- Public static methods -------------------------

    /** Schedules a job that periodically recomputes the user monitor messages.
     * @param frequency One of the ScheduleFrequency frequency constants.
     */
    public static void scheduleUserMessageJob(ScheduleFrequency frequency)
    {
        JobDetail jd = new JobDetail("UserMessageDaemon", "group1",
                                     UserMonitorSchedulerJob.class);
        jobDetails.add(jd);
        Trigger trigger = BidderSchedulerJob.makeAppropriateTrigger(frequency);
        triggers.add(trigger);
        try { ensureScheduler().scheduleJob(jd, trigger);}
        catch (SchedulerException e) { throw Utils.barf(e, null); }
    }

    /** Schedules a job that periodically recomputes the admin monitor messages.
     * @param frequency One of the ScheduleFrequency frequency constants.
     */
    public static void scheduleAdminMessageJob(ScheduleFrequency frequency)
    {
        JobDetail jd = new JobDetail("AdminMessageDaemon", "group1",
                                     AdminMonitorSchedulerJob.class);
        jobDetails.add(jd);
        Trigger trigger = BidderSchedulerJob.makeAppropriateTrigger(frequency);
        triggers.add(trigger);
        try { ensureScheduler().scheduleJob(jd, trigger);}
        catch (SchedulerException e) { throw Utils.barf(e, null); }
    }

    /** Checks if the scheduler is ok.
     * @return True if the scheduler is ok or false if not.
     */
    public static boolean isSchedulerOK() {
    	boolean result = false;
    	Scheduler s = messageScheduler;
    	try {
			result = (s != null) && s.isStarted();
			Utils.logThisPoint(Level.DEBUG, "Scheduler is ok");
		} catch (SchedulerException e) {
			Utils.logThisPoint(Level.ERROR, "Scheduler is NOT OK" + e);
		}
		return result;
    }
    
    public static void main(String[] args)
    {
        try { init(args); }
        catch (Throwable t)
        {
            // Log the error, but proceed.  I think that this should
            // be truly fatal, but, ....
            Utils.logThisPoint
                    (Level.INFO,
                     "Failed to finished loading message scheduler jobs");
            Utils.logThisPoint(Level.ERROR, t);
            t.printStackTrace();
        }
    }
    
    // -------------------------- Private methods -------------------------

    private static Scheduler instantiateScheduler() throws SchedulerException
    {
        // the 'default' scheduler is defined in "quartz.properties" found
        // in the current working directory, in the classpath, or
        // resorts to a fall-back default that is in the quartz.jar

        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler scheduler = sf.getScheduler();

        // Scheduler will not execute jobs until it has been started
        // (though they can be scheduled before start())
        scheduler.start();
        return scheduler;
    }

    private static Scheduler ensureScheduler() throws SchedulerException
    {
        if(messageScheduler == null)
            messageScheduler = instantiateScheduler();
        return messageScheduler;
    }

    // ----------------------- Command line operation --------------------
    
    private static void init(String[] commandLineArgs)
    {
        if(!initialised)
        {
            if(commandLineArgs != null)
            {
                Integer userFreq = AppNexusUtils.commandLineGetInteger
                        ("-usermessagedaemonfrequency", commandLineArgs);
                ScheduleFrequency userFrequency;
                if(userFreq == null) {
                	userFrequency = DEFAULT_USER_FREQUENCY;
                }
                else userFrequency = ScheduleFrequency.get(userFreq);
                try {
                	scheduleUserMessageJob(userFrequency);
                } catch (Exception e) {
                	throw Utils.barf(e, null, (Object)commandLineArgs);
                }

                Integer adminFreq = AppNexusUtils.commandLineGetInteger
                        ("-adminmessagedaemonfrequency", commandLineArgs);
                ScheduleFrequency adminFrequency;
                if(adminFreq == null) {
                	adminFrequency = DEFAULT_ADMIN_FREQUENCY;
                }
                else adminFrequency = ScheduleFrequency.get(adminFreq);
                try {
                	scheduleAdminMessageJob(adminFrequency);
                } catch (Exception e) {
                	throw Utils.barf(e, null, (Object)commandLineArgs);
                }
            }
            Utils.logThisPoint
                    (Level.INFO,
                     "Finished loading monitor scheduler jobs");
            initialised = true;
        }
    }
}
