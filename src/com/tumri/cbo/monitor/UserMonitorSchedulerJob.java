package com.tumri.cbo.monitor;

import com.tumri.af.exceptions.BusyException;
import com.tumri.cbo.backend.Bidder;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.quartz.*;
import java.util.List;

public class UserMonitorSchedulerJob extends MonitorSchedulerJob
{

    // --------------------------- Public methods -------------------------

    /** Implementation of the Job interface.
     * @param context The job execution context.
     * @exception JobExecutionException If error executing the job.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void execute(JobExecutionContext context)
                        throws JobExecutionException
    {
        boolean finishedOk = false;
    	try
        {
            owningThread = Thread.currentThread();
            Utils.logThisPoint(Level.INFO, "Executing user message job");
            Bidder bidder = Bidder.getInstance();
            if(bidder != null)
            {
    		    List<Messages> messages =
                        Messages.checkAll(bidder, true, true,
                                          MessageReport.FOR_USERS);
                Utils.logThisPoint
                        (Level.INFO,
                         "User messages found: " + messages.size());
            }
            else Utils.logThisPoint
                    (Level.INFO,
                     "User message job not running because "  +
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
                     "Finished user message job" +
                     (finishedOk ? " OK" : " with Error!"));
        }
    }
}

