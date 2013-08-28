package com.tumri.cbo.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import com.tumri.cbo.backend.BidderSchedulerJob;
import com.tumri.cbo.utils.CBOConfigurator;

public class DataCleanUpScheduler implements Job {

	private static Scheduler dataCleanUpScheduler;
	private final static Logger logger = Logger.getLogger(DataCleanUpScheduler.class);

	public static Scheduler getDataCleanUpScheduler() {
		return dataCleanUpScheduler;
	}

	public static void setDataCleanUpScheduler(Scheduler dataCleanUpScheduler) {
		DataCleanUpScheduler.dataCleanUpScheduler = dataCleanUpScheduler;
	}

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		BufferedReader inputStream = null;
		try {
			logger.info("Executing data clean up task...");
			logger.info("Pausing all Bidder jobs if running...");
			BidderSchedulerJob.getBidderScheduler().pauseTriggerGroup("group1");
			Runtime runTime = Runtime.getRuntime();
			logger.info("Running data cleanup script "
					+ CBOConfigurator.getDataCleanUpScriptFile());
			Process dataCleanUpProcess = runTime.exec(CBOConfigurator
					.getDataCleanUpScriptFile());

			inputStream = new BufferedReader(new InputStreamReader(
					dataCleanUpProcess.getInputStream()));
			try {
				dataCleanUpProcess.waitFor();
			} catch (InterruptedException e) {
				logger.error("Data clean up process interrupted: " + e);
			}

			String cleanUpScriptOutput = null;
			while ((cleanUpScriptOutput = inputStream.readLine()) != null) {
				logger.info(cleanUpScriptOutput);
			}

			inputStream.close();

		} catch (JobExecutionException ex) {
			logger.error("Error occurred during data clean up execution: " + ex);
		} catch (IOException ex) {
			logger.error("IOException occurred when closing the inputstream from dataclean up output: "
					+ ex);
		} catch (SchedulerException ex) {
			logger.error("Error occurred when pausing the Bidder Jobs. SchedulerException message: " + ex);
		} catch (Exception ex){
			logger.error("Exception thrown from execute() method: " + ex);
		}
		finally {
			try {
				logger.info("Resuming all Bidder jobs...");
				BidderSchedulerJob.getBidderScheduler().resumeTriggerGroup(
						"group1");
			} catch (SchedulerException ex) {
				logger.error("Error occurred when resuming the Bidder Jobs"
						+ ex);
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ex) {
					logger.error("IOException occurred when closing the inputstream from dataclean up output: " + ex);
				}
			}
		}
	}
	
	public static void scheduleDataCleanUp(String cronExpression) {

		try {
			JobDetail jobDetail = new JobDetail("Data Cleanup Job", "cleanup_group",
					DataCleanUpScheduler.class);
			logger.info("Data clean up scheduler cron expression: " + cronExpression);
			CronTrigger dataCleanUpTrigger = new CronTrigger(
					"CboDataCleanUp_Triggger", "cleanup_group", cronExpression);
			Scheduler scheduler = instantiateScheduler();
			scheduler.scheduleJob(jobDetail, dataCleanUpTrigger);
			
		} catch (ParseException ex) {
			logger.error("Data CleanupScheduler cron expression parsing failed:" + ex.getMessage());
		} catch (SchedulerException ex) {
			logger.error("Data CleanupScheduler scheduler exception occurred:" + ex.getMessage());
		}

	}
	
	private static Scheduler instantiateScheduler() throws SchedulerException
    {
		logger.info("Starting Data clean up scheduler...");
		if (getDataCleanUpScheduler() == null) {
			SchedulerFactory sf = new StdSchedulerFactory();
			dataCleanUpScheduler = sf.getScheduler();
		}
		if(!dataCleanUpScheduler.isStarted()){
			dataCleanUpScheduler.start();
		}
        return dataCleanUpScheduler;
    }
	
	/** Checks if the scheduler is ok.
     * @return True if the scheduler is ok or false if not.
     */
	public static boolean isSchedulerOK() {
		boolean result = false;
		Scheduler scheduler = dataCleanUpScheduler;
		try {
			result = (scheduler != null) && scheduler.isStarted();
			logger.info("Data Cleanup Scheduler is ok");
		} catch (SchedulerException e) {
			logger.error("Data CleanupScheduler is NOT OK" + e);
		}
		return result;
	}
}
