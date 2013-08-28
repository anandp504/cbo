package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.HTMLifier;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;

import java.io.*;
import java.util.*;

public class ShowMessagesHTTPHandler extends HTTPHandler {

    static String urlName = "SHOWMESSAGES";
    static String prettyName = "Show Messages";
    public static final String NO_PARAM = "";
    public static final String SEND_MAIL_TO_ADMINS_PARAM = "SEND_MAIL_TO_ADMINS";
    public static final String SEND_MAIL_TO_ALL_PARAM = "SEND_MAIL_TO_ALL";
    public static final String SEND_MAIL_PARAM = "SEND_MAIL";
    public static final String RECENT_DAYS_PARAM = "daysCountingAsRecent";

    public ShowMessagesHTTPHandler(HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }
    
    public boolean isAdminUserCommand() { return false; }

    public static String makeURL(CampaignData cd)
    {
        StringBuffer b = new StringBuffer();
        b.append("../");
        b.append(urlName);
        b.append("/");
        b.append(urlName);
        b.append("?#");
        b.append(cd.getAdvertiserId());
        b.append("/");
        b.append(cd.getCampaignId());
        return b.toString();
    }

    static int maxDaysLookBack = 60;

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        Map<String, String> headers =
                handlerPageSetup
                        (stream, "Messages", null, stylesheetUrl,
                                null, returnHeaders, prettyName, httpParams);
        Messages messages = Messages.getLastMessages();
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        String header ="<FORM ACTION=\"../" +
                                  FetchMessagesHTTPHandler.urlName + "/" +
                                  FetchMessagesHTTPHandler.urlName + "?" +
                                  "\" METHOD=GET>";
        stream.append(header);
        if(admin)
        {
            stream.append
                    ("<H4>");
            stream.append("\nDon't&nbsp;send&nbsp;mail:" +
                          "<INPUT TYPE=RADIO NAME=\"" +
                          SEND_MAIL_PARAM + "\" " +
                          "VALUE=\"" + NO_PARAM + "\" CHECKED>&nbsp;");
            stream.append("\nSend&nbsp;mail&nbsp;to&nbsp;admins&nbsp;only:" +
                          "<INPUT TYPE=RADIO NAME=\"" +
                          SEND_MAIL_PARAM + "\" " +
                          "VALUE=\"" + SEND_MAIL_TO_ADMINS_PARAM + "\">");
            stream.append("\nSend&nbsp;mail&nbsp;to&nbsp;all:" +
                          "<INPUT TYPE=RADIO NAME=\"" +
                          SEND_MAIL_PARAM + "\" " +
                          "VALUE=\"" + SEND_MAIL_TO_ALL_PARAM + "\">");
            stream.append("<BR>\nLookback&nbsp;(days):<SELECT NAME=\""
                            + RECENT_DAYS_PARAM
                            + "\">");
            for(int i=1; i <= maxDaysLookBack; i++)
            {
                stream.append("\n  <OPTION VALUE=\"");
                stream.append(Integer.toString(i));
                stream.append("\">");
                stream.append(Integer.toString(i));
            }
            stream.append("\n</SELECT>");
            stream.append("<BR>\n<INPUT TYPE=SUBMIT NAME=\"DoIt\" " +
                                   "VALUE=\"Fetch messages\">&nbsp;" +
                                   "(may&nbsp;take&nbsp;about&nbsp;ten" +
                                   "&nbsp;minutes)");
            stream.append("\n</H4>");
        }
        stream.append("</FORM>");
        if(messages == null)
            stream.append("<H2>No messages have been recorded.</H2>  ");
        else
        {
            String html = messages.getHTML();
            if(html == null)
                stream.append("<H3>Nothing to report at present!</H3>");
            else stream.append(html);
        }
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static { HTTPListener.registerHandlerClass(ShowMessagesHTTPHandler.class); }
}
