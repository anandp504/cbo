package com.tumri.cbo.monitor;

import com.tumri.af.exceptions.BusyException;
import com.tumri.cbo.backend.Bidder;
import com.tumri.mediabuying.zini.HTMLifier;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;

import java.io.*;
import java.util.*;

public class FetchMessagesHTTPHandler extends HTTPHandler {

    static String urlName = "FETCHMESSAGES";
    static String prettyName = null;

    public FetchMessagesHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }
    
    public boolean isAdminUserCommand() { return false; }

    @SuppressWarnings("EmptyCatchBlock")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        Bidder bidder = Bidder.getInstance();
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        String sendMailVal =
               httpParams.get(ShowMessagesHTTPHandler.SEND_MAIL_PARAM);
        String daysCountingAsRecentString =
                httpParams.get(ShowMessagesHTTPHandler.RECENT_DAYS_PARAM);
        long daysCountingAsRecent = Messages.defaultDaysCountingAsRecent;
        boolean forceMailToAdmins;
        boolean sendMail;
        if(ShowMessagesHTTPHandler.NO_PARAM.equals(sendMailVal))
        {
            sendMail = false;
            forceMailToAdmins = false;
        }
        else if(ShowMessagesHTTPHandler.SEND_MAIL_TO_ADMINS_PARAM.equals
                (sendMailVal))
        {
            sendMail = true;
            forceMailToAdmins = true;
        }
        else if(ShowMessagesHTTPHandler.SEND_MAIL_TO_ALL_PARAM.equals
                (sendMailVal))
        {
            sendMail = true;
            forceMailToAdmins = false;
        }
        else throw Utils.barf("Unhandled send mail param", this, sendMailVal);
        if(daysCountingAsRecentString != null)
        {
            daysCountingAsRecent = Long.parseLong(daysCountingAsRecentString);
        }
        if(bidder != null)
        {
            boolean checkedOk = false;
            int maxChecks = 6;
            int sleepSecs = 10;
            int checks = 0;
            HTTPHandler handler = null;
            // Will check for it being free for a minute, and if things are
            // still busy, then log it, and return to the user.
            while(!checkedOk && checks < maxChecks)
            {
                try
                {
                    // if(!checkedOk) throw new BusyException(); // test
                    Messages.checkAll(bidder, admin, sendMail,
                                      MessageReport.FOR_USERS,
                                      daysCountingAsRecent, forceMailToAdmins);
                    handler = HTTPListener.getHandler
                                    (ShowMessagesHTTPHandler.urlName);
                    checkedOk = true;
                }
                catch(BusyException t)
                {
                    try
                    {
                        Thread.sleep(sleepSecs * 1000);
                    }
                    catch (InterruptedException e) {}
                }
                checks = checks + 1;
            }
            if(handler != null)
                return handler.handle(parsed, command, inputLine, in, stream, os,
                                  httpParams, returnHeaders);
            else
            {
                Utils.logThisPoint
                        (Level.WARN, "Messages class was busy when running "
                                      + this.getClass().getName());
                Map<String, String> headers =
                        handlerPageSetup
                                (stream, "Messages", null, stylesheetUrl,
                                 null, returnHeaders, prettyName, httpParams);
                stream.append("<H3>The system is busy computing the current " +
                              "set of messages.  Please try again in a " +
                              "few minutes.</H3>");
                HTMLifier.finishHTMLPage(stream);
                return headers;
            }
        }
        else throw Utils.barf
               ("Call to FetchMessages, when the bidder doesn't exist.", this);
    }

    static { HTTPListener.registerHandlerClass(FetchMessagesHTTPHandler.class); }
}
