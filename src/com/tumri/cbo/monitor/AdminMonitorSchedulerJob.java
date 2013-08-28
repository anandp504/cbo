package com.tumri.cbo.monitor;

import com.tumri.af.exceptions.BusyException;
import com.tumri.cbo.backend.Bidder;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.quartz.*;
import java.util.List;

public class AdminMonitorSchedulerJob extends MonitorSchedulerJob
{
    // --------------------------- Public methods -------------------------

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void executeForAdmin() throws JobExecutionException
    {
        boolean finishedOk = false;
    	try
        {
            owningThread = Thread.currentThread();
            Utils.logThisPoint(Level.INFO, "Executing admin message job");
            Bidder bidder = Bidder.getInstance();
            if(bidder != null)
            {
                List<Messages> messages =
                        Messages.checkAll(bidder, true, true,
                                          MessageReport.FOR_ADMINS);
                Utils.logThisPoint
                        (Level.INFO,
                         "Admin messages found: " + messages.size());
            }
            else Utils.logThisPoint
                    (Level.INFO,
                     "Admin message job not running because "  +
                     "no bidder was found.");
            finishedOk = true;
    	}
        catch(BusyException t)
        {
            Utils.logThisPoint(Level.WARN, "Messages class was busy.");
            throw new JobExecutionException(t);
    	}
        catch(Throwable t)
        {
            Utils.logThisPoint
                    (Level.ERROR, "Unhandled error checking for messages.");
            Utils.barf(t, null);
            throw new JobExecutionException(t);
    	}
        finally
        {
            owningThread = null;
            Utils.logThisPoint
                    (Level.INFO,
                     "Finished admin message job" +
                     (finishedOk ? " OK" : " with Error!"));
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void executeForUrgent()
                        throws JobExecutionException
    {
        boolean finishedOk = false;
    	try
        {
            owningThread = Thread.currentThread();
            Utils.logThisPoint(Level.INFO,"Executing urgent user message job");
            Bidder bidder = Bidder.getInstance();
            if(bidder != null)
            {
                List<Messages> messages =
                        Messages.checkAll(bidder, true, true,
                                          MessageReport.URGENT_FOR_USERS);
                Utils.logThisPoint
                        (Level.INFO,
                         "Urgent user messages found: " + messages.size());
            }
            else Utils.logThisPoint
                    (Level.INFO,
                     "Urgent user message job not running because "  +
                     "no bidder was found.");
            finishedOk = true;
    	}
        catch(BusyException t)
        {
            Utils.logThisPoint(Level.WARN, "Messages class was busy.");
            throw new JobExecutionException(t);
    	}
        catch(Throwable t)
        {
            Utils.logThisPoint
                    (Level.ERROR, "Unhandled error checking for messages.");
            Utils.barf(t, null);
            throw new JobExecutionException(t);
    	}
        finally
        {
            owningThread = null;
            Utils.logThisPoint
                    (Level.INFO,
                     "Finished urgent user message job" +
                     (finishedOk ? " OK" : " with Error!"));
        }
    }


    /** Implementation of the Job interface.
     * @param context The job execution context.
     * @exception JobExecutionException If error executing the job.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void execute(JobExecutionContext context)
                        throws JobExecutionException
    {
        executeForUrgent();
        executeForAdmin();
    }

}
