package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;

import java.io.*;
import java.util.*;

public class BidderStatusHTTPHandler extends HTTPHandler {

    public static final String URL_NAME = "BIDDERSTATUS";
    static final String STATUS_PRETTYNAME = null;
    Bidder bidder;

    @SuppressWarnings("unused")
    public BidderStatusHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, URL_NAME, STATUS_PRETTYNAME);
        this.bidder = bidder;
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(URL_NAME, this);
    }

    public static final String STATE_FORM_NAME = "StateForm";
    public static final String STATE_ELEMENT_NAME = "State";

    public boolean isAdminUserCommand() { return false; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        Status status = bidder.getStatus();
        String statusString = status.printedRepresentation();
        Map<String, String> headers =
            handlerPageSetup
                (stream, null, null, stylesheetUrl,
                 (statusString != null &&
                         !statusString.toUpperCase().contains("FINISHED"))
                    ? "<meta http-equiv=\"refresh\" content=\"" +
                         Integer.toString(Bidder.STATUS_FRAME_REFRESH_SECONDS)
                         + "\">"
                    : null,
                 returnHeaders, STATUS_PRETTYNAME, httpParams);
        if(statusString != null)
        {
            stream.append(statusString);
        }
        stream.append("\n<FORM id=\"" + STATE_FORM_NAME + "\" NAME=\"" +
                                        STATE_FORM_NAME + "\">");
        stream.append("<INPUT TYPE=\"HIDDEN\" NAME=\"" + STATE_ELEMENT_NAME +
                          "\" VALUE=\"");
        stream.append((statusString != null &&
                       statusString.toUpperCase().contains("IDLE")
                        ? Bidder.IDLE_STATE
                        : statusString != null &&
                            statusString.toUpperCase().contains("UNINITIALISED")
                            ? Bidder.UNINITIALISED_STATE
                            : Bidder.ACTIVE_STATE));
        stream.append("\">");
        stream.append("</FORM>");
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static { HTTPListener.registerHandlerClass(StatusHTTPHandler.class); }
}

