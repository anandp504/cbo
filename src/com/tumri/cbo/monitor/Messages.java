package com.tumri.cbo.monitor;

import com.tumri.af.email.MailSender;
import com.tumri.af.exceptions.BusyException;
import com.tumri.af.utils.PropertyException;
import com.tumri.cbo.backend.Bidder;
import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.cbo.utils.CBOMailConfig;
import com.tumri.mediabuying.zini.*;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.*;

public class Messages{

    String text;
    String html;
    Map<Object, String> key = new HashMap<Object, String>();
    MessageReport forReport = null;
    
    private static boolean m_checkingMessages = false;
    
    private static final Object MESSAGES_LOCK = new Object();

    public Messages(String text, String html, MessageReport forReport)
    {
        this(text, html);
        this.forReport = forReport;
    }

    public Messages(String text, String html)
    {
        if(text == null && html != null ||
           text != null && html == null)
            throw Utils.barf("Both args should be null or neither should be.",
                             this, text, html);
        this.text = text;
        this.html = html;
    }

    public Messages(String text, String html, Map<Object, String> key,
                    MessageReport forReport)
    {
        this(text, html, key);
        this.forReport = forReport;
    }

    public Messages(String text, String html, Map<Object, String> key)
    {
        this(text, html);
        this.key = key;
    }

    public Map<Object, String> getKey()
    {
        return key;
    }

    public String getText()
    {
        return text;
    }

    public String getHTML()
    {
        return html;
    }

    public MessageReport getForReport()
    {
        return forReport;
    }

    private static Messages lastMessages = null;

    public static Messages getLastMessages()
    {
        return lastMessages;
    }

    static SynchDateFormat format = new SynchDateFormat("yyyy-MM-dd");
    // Add to config vars.

    static List<InternetAddress> getToAddresses
            (SQLConnector connector, QueryContext qctx, MessageReport type)
            throws AddressException
    {
        List<InternetAddress> toAddresses = new Vector<InternetAddress>();
        StringBuffer query = new StringBuffer();
        query.append("SELECT DISTINCT email_address");
        query.append("\nFROM users");
        query.append("\nWHERE 1 = 1");
        query.append(type.toConjuncts());
        query.append(";");
        Sexpression addresses = connector.runSQLQuery(query.toString(), qctx);
        while(addresses != Null.nil)
        {
            InternetAddress toAddress =
                    new InternetAddress(addresses.car().car().unboxString());
            toAddresses.add(toAddress);
            addresses = addresses.cdr();
        }
        return toAddresses;
    }

    static String absolutifyHTML(String body) throws PropertyException
    {
        StringBuffer res = new StringBuffer();
        res.append("<HEAD>\n");
        res.append("</HEAD>\n");
        res.append("<BODY>\n");
        res.append(body.replace(AbstractReporter.URL_PRELUDE,
                                CBOConfigurator.getExternalURLPrefix()));
        res.append("\n</BODY>\n");
        return res.toString();
    }

    static void mailOutMessagesForToday
            (Bidder bidder, Messages messages,
             MessageReport toAddressesWrtReport)
    {
        Date now = new Date();
        CBOMailConfig mailConfig = null;
        String host = null;
        Integer port = null;
        String userName = null;
        String password = null;
        String fromAddressString = null;
        try
        {
            mailConfig = CBOConfigurator.getMailConfig();
            host = mailConfig.getHost();
            port = mailConfig.getPort();
            userName = mailConfig.getUsername();
            password = mailConfig.getPassword();
            fromAddressString = mailConfig.getFrom();
            if(host == null)
                throw Utils.barf("SMTP host not specified.", null);
            if(port == null)
                throw Utils.barf("SMTP port not specified.", null);
            if(fromAddressString == null)
                throw Utils.barf("SMTP fromAddress not specified.", null);
            InternetAddress fromAddress =
                    new InternetAddress(fromAddressString);
            SQLConnector connector = bidder.ensureBidderSQLConnector();
            QueryContext qctx = new BasicQueryContext
                                    (null, bidder.getAppNexusTheory());
                List<InternetAddress> toAddresses =
                        getToAddresses(connector, qctx, toAddressesWrtReport);
                if(toAddresses.size() > 0)
                {
                    String subject;
                    subject = "Bid Optimizer Messages for " +
                                    format.format(now);
                    String text = messages.getText();
                    if(text != null)
                    {
                        String htmlText = messages.getHTML();
                        if(htmlText != null)
                            htmlText = absolutifyHTML(htmlText);
                        List<DataSource> attachments = null;
                        MailSender.sendMail(host, port, userName, password,
                                fromAddress, toAddresses, subject, text,
                                htmlText, attachments);
                    }
                }
        }
        catch(MessagingException e)
        {
            throw Utils.barf(e, null, messages, mailConfig, host, port,
                             userName, password, fromAddressString);
        }
        catch(PropertyException e)
        {
            if(HTTPHandler.getRunningInTomcat())
                throw Utils.barf(e, null, messages, mailConfig, host, port,
                                 userName, password, fromAddressString);
            else Utils.warn(e, null, messages, mailConfig, host, port,
                            userName, password, fromAddressString);
        }
    }

    public static final long defaultDaysCountingAsRecent = 2;

    public static List<Messages> checkAll
            (Bidder bidder, boolean admin, boolean mailReports,
             MessageReport report)
            throws BusyException
            // Return the txt and html formats of the messages.
    {
        return checkAll(bidder, admin, mailReports, report,
                        defaultDaysCountingAsRecent, false);
    }

    @SuppressWarnings("unused")
    public static List<Messages> checkAll
            (Bidder bidder, boolean admin, boolean mailReports,
             MessageReport report, boolean forceMailToAdmins)
            throws BusyException
            // Return the txt and html formats of the messages.
    {
        return checkAll(bidder, admin, mailReports, report,
                        defaultDaysCountingAsRecent, forceMailToAdmins);
    }

    public static List<Messages> checkAll
            (Bidder bidder, boolean admin, boolean mailReports,
             MessageReport report, long daysCountingAsRecent,
             boolean forceMailToAdmins)
            throws BusyException
            // Return the txt and html formats of the messages.
    {
        MessageReport[] reports = new MessageReport[] { report };
        ProblemReporter reporter = new OrderByCampaignProblemReporter();
        return checkAll(bidder, admin, mailReports, reports, reporter,
                        daysCountingAsRecent, forceMailToAdmins);
    }

    public static List<Messages> checkAll
            (Bidder bidder, boolean admin, boolean mailReports,
             MessageReport[] reports, ProblemReporter reporter,
             long daysCountingAsRecent, boolean forceMailToAdmins)
            throws BusyException
            // Return the txt and html formats of the messages.
    {
        List<Messages> res = new Vector<Messages>();
        Date now = bidder.getCurrentTime();
        TimeZone localTz = TimeZone.getDefault();
        InstallMonitors.init();
        boolean outerDefeat = bidder.getDefeatFetchingAppNexusReports();
        
    	synchronized(MESSAGES_LOCK) {
    		if(m_checkingMessages) {
    			throw new BusyException
                  ("Tried to check messages before finishing previous check.");
    		}
    		m_checkingMessages = true;
    	}
    	
        try
        {
            bidder.setDefeatFetchingAppNexusReports(true);
            
            for(MessageReport report: reports)
            {
                Messages messages;
                messages = AbstractMonitor.checkAllInternal
                               (bidder, now, localTz, admin, report, reporter,
                                daysCountingAsRecent);
                lastMessages = messages;
                if(mailReports)
                    mailOutMessagesForToday
                            (bidder, messages,
                             (forceMailToAdmins
                                     ? MessageReport.FOR_ADMINS
                                     : report));
                res.add(messages);
            }
            return res;
        }
        finally
        {
        	synchronized(MESSAGES_LOCK) {
        		m_checkingMessages = false;
        	}
            bidder.setDefeatFetchingAppNexusReports(outerDefeat);
        }
    }

    public static void main(String[] args)
    {
        long daysCountingAsRecent = 1; // Just for testing.
        boolean sendMail = false;
        boolean forceMailToAdmins = true;
        Bidder bidder = Bidder.initializeFromCommandLineArgs(false, args);
        bidder.initializeFromCommandLineArgsLevel2(args);
        System.out.println("******** Starting monitor run *********");
        ProblemReporter reporter =
                            // new OrderByTypeProblemReporter();
                            new OrderByCampaignProblemReporter();
        try
        {
            List<Messages> messagesList =
                    checkAll(bidder, true, sendMail, MessageReport.values(),
                             reporter, daysCountingAsRecent,
                             forceMailToAdmins);
            for(Messages messages: messagesList)
            {
                System.out.println("FOR REPORT: " +
                        messages.getForReport().name() +
                        "\n======================\n");
                System.out.println("HTML\n====\n");
                System.out.println(messages.getHTML());
                System.out.println("TEXT\n====\n");
                System.out.println(messages.getText());
            }
            System.out.println("******** Finished running monitors *********");
        }
        catch(BusyException e)
        {
            System.out.println("Messages were busy!!!");
        }
    }

}