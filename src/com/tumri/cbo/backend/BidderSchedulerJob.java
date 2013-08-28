package com.tumri.cbo.backend;

import com.tumri.af.exceptions.BusyException;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class BidderSchedulerJob implements Job
{
     // Should be false for real config, true to disable.
    private static boolean initialised = false;
    public static final ScheduleFrequency DEFAULT_FREQUENCY =
            ScheduleFrequency.FREQUENCY_HOURLY;

    private static Scheduler bidderScheduler = null;
    

    // --------------------------- Public static methods -------------------------

    public static Scheduler getBidderScheduler() {
		return bidderScheduler;
	}

	public static void setBidderScheduler(Scheduler bidderScheduler) {
		BidderSchedulerJob.bidderScheduler = bidderScheduler;
	}

	/** Schedules a job that calls the bidder at the specified frequency.
     * Note that the bidder must be initialized with all the required control
     * and debug parameters before this is called.
     * @param frequency One of the ScheduleFrequency frequency constants.
     */
    public static void scheduleBidderJob(ScheduleFrequency frequency)
    {
        JobDetail jd = new JobDetail("Bidder", "group1",
                                     BidderSchedulerJob.class);
        Trigger trigger = makeAppropriateTrigger(frequency);
        try { ensureScheduler().scheduleJob(jd, trigger);}
        catch (SchedulerException e) { throw Utils.barf(e, null); }
    }
    
    /** Checks if the scheduler is ok.
     * @return True if the scheduler is ok or false if not.
     */
    public static boolean isSchedulerOK() {
    	boolean result = false;
    	Scheduler s = bidderScheduler;
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
                     "Failed to finished loading bidder scheduler jobs");
            Utils.logThisPoint(Level.ERROR, t);
            t.printStackTrace();
        }
    }
    
    // --------------------------- Public methods -------------------------

    /** Implementation of the Job interface.
     * @param context The job execution context.
     * @exception JobExecutionException If error executing the job.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void execute(JobExecutionContext context) 
                        throws JobExecutionException
    {
    	try {
    		Bidder.getInstance().processBidInstructions();
    	} catch(BusyException be) {
            Utils.logThisPoint
                    (Level.WARN,
                     "Not running scheduled bidder job because bidder is already running.");
    	} catch(Throwable t) {
    		Utils.logThisPoint
                    (Level.ERROR,
                     "Unhandled error processing bid instructions.  " + t);
            Utils.barf(t, null);
    		throw new JobExecutionException(t);
    	}
    }
    
    // -------------------------- Private methods -------------------------

    public static Trigger makeAppropriateTrigger(ScheduleFrequency frequency)
    {
        Trigger trigger = null;
        switch(frequency)
        {
            case FREQUENCY_IMMEDIATE:
                trigger = BidderScheduler.makeImmediateTrigger();
                break;
            case FREQUENCY_ONE_MIN:
                trigger = BidderScheduler.makeMinuteTrigger
                            ("One minute trigger");
                break;
            case FREQUENCY_THREE_MINS:
                trigger = BidderScheduler.makeThreeMinuteTrigger
                            ("Three minute trigger");
                break;
            case FREQUENCY_FIVE_MINS:
                trigger = BidderScheduler.makeFiveMinuteTrigger
                            ("Five minute trigger");
                break;
            case FREQUENCY_TEN_MINS:
                trigger = BidderScheduler.makeTenMinuteTrigger
                            ("Ten minute trigger");
                break;
            case FREQUENCY_HOURLY:
                trigger = BidderScheduler.makeHourlyTrigger("Hourly trigger");
                break;
            case FREQUENCY_HOURLY_LATE:
                trigger = BidderScheduler.makeHourlyTrigger
                        ("Hourly trigger late",
                         BidderScheduler.DEFAULT_HOURLY_TRIGGER_GROUP,
                         BidderScheduler.DAILY_LATE_TRIGGER_MINUTE);
                break;
            case FREQUENCY_DAILY:
                trigger = BidderScheduler.makeDailyTrigger("Daily trigger");
                break;
            case FREQUENCY_DAILY_LATE:
                trigger = BidderScheduler.makeDailyTrigger
                        ("Daily trigger late",
                         BidderScheduler.DEFAULT_DAILY_TRIGGER_GROUP,
                         BidderScheduler.DAILY_LATE_TRIGGER_HOUR,
                         BidderScheduler.DAILY_LATE_TRIGGER_MINUTE);
                break;
            case FREQUENCY_WEEKLY:
                trigger = BidderScheduler.makeWeeklyTrigger("Weekly trigger");
                break;
        }
        if(trigger == null)
            throw Utils.barf("Trigger frequency not known: " + frequency,null);
        return trigger;
    }

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
        if(bidderScheduler == null)
            bidderScheduler = instantiateScheduler();
        return bidderScheduler;
    }

    // ----------------------- Command line operation --------------------
    
    private static void init(String[] commandLineArgs)
    {
        if(!initialised)
        {
            if(commandLineArgs != null)
            {
                Integer freq = AppNexusUtils.commandLineGetInteger
                        ("-frequency", commandLineArgs);
                ScheduleFrequency frequency;
                if(freq == null) {
                	frequency = DEFAULT_FREQUENCY;
                }
                else frequency = ScheduleFrequency.get(freq);
                Bidder b = Bidder.initializeFromCommandLineArgs
                                        (false, commandLineArgs);
                try { 
                	scheduleBidderJob(frequency);
                } catch (Exception e) {
                	throw Utils.barf(e, null, b, commandLineArgs); 
                }
            }
            Utils.logThisPoint(Level.DEBUG, "Finished loading recurring jobs");
            initialised = true;
        }
    }

}

