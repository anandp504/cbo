package com.tumri.cbo.backend;

import com.tumri.af.exceptions.BusyException;
import com.tumri.mediabuying.zini.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class RunBidderHTTPHandler extends HTTPHandler {

    public static final String URL_NAME = "RUNBIDDER";
    public static final String RUN_BIDDER_PARAM = "RunBidder";
    static final String PRETTYNAME = null;
    Bidder bidder;

    @SuppressWarnings("unused")
    public RunBidderHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, URL_NAME, PRETTYNAME);
        this.bidder = bidder;
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(URL_NAME, this);
    }

    public boolean isAdminUserCommand() { return true; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        Map<String, String> headers =
                handlerPageSetup(stream, "Run Bidder", null, stylesheetUrl,
                        null, returnHeaders, PRETTYNAME, httpParams);
        BidderState state = bidder.getBidderState();
        if(state == BidderState.IDLE)
        {
            Thread thread = new Thread(new BidderRunner());
            thread.start();
            stream.append("<H2>Bidder started</H2>");
        }
        else
        {
            stream.append("<H2>Bidder is not idle.  Please try later.</H2>");
        }
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static { HTTPListener.registerHandlerClass(RunBidderHTTPHandler.class); }
}

class BidderRunner implements Runnable {

    boolean finished = false;

    public BidderRunner() {}

    public void run()
    {
        Bidder bidder = Bidder.getInstance();
        if(bidder == null) {
        	throw new IllegalStateException("Bidder has not been initialized");
        }
        try
        {
            bidder.processBidInstructions();
        }
        catch (BusyException e)
        {
            throw Utils.barf(e, this, bidder);
        }
        finished = true;
    }
}
