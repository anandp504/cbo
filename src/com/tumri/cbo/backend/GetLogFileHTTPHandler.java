package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.DocHTTPHandler;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;

import java.io.*;
import java.util.*;

public class GetLogFileHTTPHandler extends HTTPHandler {

    public static String urlName = "GETLOGFILE";

    public GetLogFileHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, null);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }

    public boolean isAdminUserCommand() { return true; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String fileName = httpParams.get("name");
        File logDir = new File(LogFileListHTTPHandler.getLogDirPath());
        File f = new File(logDir, fileName);
        return DocHTTPHandler.handleForFile(f, stream, os, returnHeaders);
    }

    static { HTTPListener.registerHandlerClass(GetLogFileHTTPHandler.class); }
}

