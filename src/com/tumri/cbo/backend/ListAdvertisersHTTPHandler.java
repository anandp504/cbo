package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;

public class ListAdvertisersHTTPHandler extends BidderGrapherHTTPHandler {

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "CAMPAIGNCHANGEHISTORYBYADVERTISER";
    static String prettyName = "Campaign Change History by Advertiser";

    @SuppressWarnings("unused")
    public ListAdvertisersHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    @SuppressWarnings("unused")
    public ListAdvertisersHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    public boolean isAdminUserCommand() { return false; }

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
                        (stream, "Campaign Change History by Advertiser", null,
                         stylesheetUrl, null, returnHeaders, prettyName,
                         httpParams);
        Bidder bidder = ensureBidder();
        if(bidder.getAdvertiserMap() != null)
        {
            stream.append("<TABLE BORDER=\"1\">");
            List<AdvertiserData> advertisers =
                    new Vector<AdvertiserData>
                            (bidder.getAdvertiserMap().values());
            Collections.sort(advertisers, AdvertiserData.nameComparator);
            for(AdvertiserData advertiser: advertisers)
            {
                String printed = advertiser.prettyString();
                
                stream.append("  \n<TR><TD>");
                stream.append
                    (SQLHTTPHandler.itemise
                       (advertiser, printed,
                        AdvertiserCombinedDataChangesPerspective.PERSPECTIVE));
                stream.append("</TD></TR>");
            }
            /*
            HTMLifier.htmlifyCollection(stream, advertisers, true, true,
                                        null, null, null, "../INSPECT/", null,
                                        0, null, true);
                                        */
            stream.append("</TABLE>");
        }
        else stream.append("<i>No known advertisers!</i>");        
        return headers;
    }

    Map<String, String> outputHeaderStuff
            (Writer stream, String stylesheetUrl, boolean returnHeaders,
             Map<String, String> httpParams)
            throws IOException
    {
        String title = prettyName;
        return handlerPageSetup
                (stream, title, urlName, stylesheetUrl, returnHeaders,
                 httpParams);
    }

    public static void register()
    {
        HTTPListener.registerHandlerClass(ListAdvertisersHTTPHandler.class);
    }
    static
    { register(); }
}

