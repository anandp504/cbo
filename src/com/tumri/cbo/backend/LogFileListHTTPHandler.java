package com.tumri.cbo.backend;

import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.mediabuying.zini.HTMLifier;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.HTTPListener;
import com.tumri.mediabuying.zini.LogTailHTTPHandler;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.*;
import java.util.*;

public class LogFileListHTTPHandler extends HTTPHandler {

    static String urlName = "LOGFILELIST";
    static String prettyName = "Log File List";

    public LogFileListHTTPHandler(HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }
    
    public static String getLogDirPath()
    {
        String dirPath = CBOConfigurator.getLogDirectoryPath();
        if(dirPath == null)
        {
            for(String s: LogTailHTTPHandler.logFilePaths())
            {
                File f = new File(s).getParentFile();
                dirPath = f.getAbsolutePath();
                String[] fns = f.list(new Filter());
                if(fns != null && fns.length > 0) break;
            }
        }
        return dirPath;
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
                handlerPageSetup
                        (stream, "Log file list", null, stylesheetUrl,
                                null, returnHeaders, prettyName, httpParams);
        File logDir = new File(getLogDirPath());
        List<String> names = new ArrayList<String>();
        String[] fileNames = logDir.list(new Filter());
        if(fileNames != null)
            names.addAll(Arrays.asList(fileNames));
        Collections.sort(names, LogFileComparator.logFileComparator);
        boolean first = true;
        for(String name: names)
        {
            if(first) first = false;
            else stream.append("\n<BR>\n");
            stream.append("\n<A HREF=\"../");
            stream.append(GetLogFileHTTPHandler.urlName);
            stream.append("/");
            stream.append(GetLogFileHTTPHandler.urlName);
            stream.append("?name=");
            stream.append(name);
            stream.append("\">");
            stream.append(htmlify(name));
            stream.append("</A>");
            stream.append("&nbsp;(<A HREF=\"../");
            stream.append(LogFileFilterHTTPHandler.urlName);
            stream.append("/");
            stream.append(LogFileFilterHTTPHandler.urlName);
            stream.append("?");
            stream.append(LogFileFilterHTTPHandler.PATH_PARAM);
            stream.append("=");
            stream.append(escapeHtml(new File(logDir, name).toString()));
            stream.append("\">Filter</A>)");
        }
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static { HTTPListener.registerHandlerClass(LogFileListHTTPHandler.class); }
}

class Filter implements FilenameFilter {

    private final static String LOG_FILE_SUFFIX = ".log";

    public boolean accept(File dir, String name) {
        return (name != null) && name.contains(LOG_FILE_SUFFIX);
    }

}

class LogFileComparator implements Comparator<String>
{
    public static LogFileComparator logFileComparator =
            new LogFileComparator();
    private final static String LOG_FILE_SUFFIX = ".log";

    public int compare(String x, String y)
    {
        if(x.endsWith(LOG_FILE_SUFFIX))
            return (y.endsWith(LOG_FILE_SUFFIX) ? 0 : -1);
        else if(y.endsWith(LOG_FILE_SUFFIX)) return 1;
        else return -x.compareTo(y);
    }
}