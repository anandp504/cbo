package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;
import org.apache.log4j.Level;
import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;

public class LogFileFilterHTTPHandler extends HTTPHandler {

    static String urlName = "LOGFILEFILTER";
    static String prettyName = "Log File Filter";

    public LogFileFilterHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }
    
    static final int DEFAULT_MAX_LINES = 500;
    static SynchDateFormat dateTimeParserMS =
            new SynchDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    static SynchDateFormat timeParserMS =
            new SynchDateFormat("HH:mm:ss,SSS");
    static SynchDateFormat hmsParser = new SynchDateFormat("HH:mm:ss");
    static SynchDateFormat dateParser =
            new SynchDateFormat("yyyy-MM-dd HH:mm:ss");
    static SynchDateFormat dateParserHM =
            new SynchDateFormat("yyyy-MM-dd HH:mm");
    static SynchDateFormat dayParser = new SynchDateFormat("yyyy-MM-dd");
    static int REGEXP_FLAGS = Pattern.DOTALL | Pattern.MULTILINE;
    static final Level[] LEVELS = new Level[]
            { null, Level.ALL, Level.DEBUG, Level.INFO, Level.WARN,
                    Level.ERROR, Level.FATAL, Level.OFF };



    static Integer readParamPositiveInteger
            (String paramName, Map<String, String> httpParams)
    {
        return readParamInteger(paramName, httpParams, 1, null);
    }

    @SuppressWarnings("EmptyCatchBlock")
    static Integer readParamInteger
            (String paramName, Map<String, String> httpParams,
             Integer minValue, Integer maxValue)
    {
        String iString = httpParams.get(paramName);
        Integer i = null;
        if(iString != null && !"".equals(iString))
        {
            try
            {
                i = Integer.parseInt(iString);
                if(minValue != null) i = Math.max(minValue, i);
                if(maxValue != null) i = Math.min(maxValue, i);
            }
            catch (Exception e) {}
        }
        return i;
    }

    static Date dateFromHour(String t, Date wrtDate)
            throws ParseException
    {
        if(wrtDate == null || t == null || "".equals(t)) return null;
        else
        {
            try
            {
                return dateTimeParserMS.parse(dayParser.format(wrtDate) + " " +t);
            }
            catch(Exception e) { return null; }
        }
    }

    static DecimalFormat  twoDigits = new DecimalFormat("00");
    static DecimalFormat fourDigits = new DecimalFormat("0000");

    static Date dateFromHour(long t, Date wrtDate)
            throws ParseException
    {
        long hours = t / 3600000;
        long mins = (t - (hours * 3600000)) / 60000;
        long secs = (t - (hours * 3600000) - (mins * 60000)) / 1000;
        long millisecs =
                t - (hours * 3600000) - (mins * 60000) - (secs * 1000);
        if(wrtDate == null) return null;
        else return dateTimeParserMS.parse
                (dayParser.format(wrtDate) + " " + twoDigits.format(hours) +
                 ":" + twoDigits.format(mins) + ":" + twoDigits.format(secs) +
                 "," + fourDigits.format(millisecs));
    }

    @SuppressWarnings("EmptyCatchBlock")
    static Date readParamDateTime
            (String paramName, Map<String, String> httpParams, Date wrtDate)
    {
        String dString = httpParams.get(paramName);
        Date d = null;
        if(dString != null)
        {
            try
            {
                d = dateParser.parse(dString);
            }
            catch (Exception e1)
            {
                try
                {
                    d = dayParser.parse(dString);
                }
                catch (Exception e2)
                {
                    try
                    {
                        d = dateParser.parse
                                (dayParser.format(wrtDate) + " " + dString);
                    }
                    catch (Exception e3)
                    {
                        try
                        {
                            d = dateParserHM.parse
                                   (dayParser.format(wrtDate) + " " + dString);
                        }
                        catch (Exception e4) {}
                    }
                }
            }
        }
        return d;
    }

    @SuppressWarnings("unused")
    List<LogChunk> filterLogFiles
            (String path, Writer stream, Integer maxEntries,
             Integer entriesAfterHit, Date startDate, Date endDate,
             Date wrtDate, Level levelGTE, Level levelEQ,
             String filterRegexp)
            throws IOException
    {
        List<String> paths = new Vector<String>();
        paths.add(path);
        return filterLogFiles(paths, stream, maxEntries, entriesAfterHit,
                              startDate, endDate, wrtDate, levelGTE, levelEQ,
                              filterRegexp);
    }

    @SuppressWarnings("EmptyCatchBlock")
    List<LogChunk> filterLogFiles
            (List<String> paths, Writer stream, Integer maxEntries,
             Integer entriesAfterHit, Date startDate, Date endDate,
             Date wrtDate, Level levelGTE, Level levelEQ, String filterRegexp)
            throws IOException
    {
        int lineCount = 0;
        int entryCount = 0;
        int entriesSinceHit = -1;
        boolean maxExceeded = false;
        List<LogChunk> res = new Vector<LogChunk>();
        for(String path: paths)
        {
            FileReader fr = null;
            BufferedReader reader = null;
            System.out.println("\nProcessing log file: " + path);
            if(stream != null && paths.size() > 1)
            {
                stream.append("\n************* ");
                stream.append(new File(path).getName());
                stream.append(" *************\n");
            }
            try
            {
                fr = new FileReader(path);
                reader = new BufferedReader(fr);
                String line;
                LogChunk chunk = null;
                Pattern filterPattern =
                        (filterRegexp == null
                                ? null
                                : Pattern.compile(filterRegexp, REGEXP_FLAGS));
                List<LogChunkExtractor> applicableExtractors =
                        new Vector<LogChunkExtractor>();
                applicableExtractors.addAll(LogChunk.extractors);
                do
                {
                    Matcher knownMatcherForNextLine;
                    if(chunk == null || chunk.nextLine == null)
                    {
                        line = reader.readLine();
                        knownMatcherForNextLine = null;
                    }
                    else
                    {
                        line = chunk.nextLine;
                        knownMatcherForNextLine =
                                chunk.knownMatcherForNextLine;
                    }
                    if(line == null) break;
                    chunk = LogChunk.getNextChunk
                            (line, knownMatcherForNextLine, reader, startDate,
                             endDate, wrtDate, levelGTE, levelEQ,
                             filterPattern, path, entriesAfterHit,
                             entriesSinceHit, applicableExtractors);
                    if(chunk.passesFilter)
                    {
                        if(stream == null) res.add(chunk);
                        else
                        {
                            stream.append("{");
                            stream.append(AppNexusUtils.intToString
                                                (entryCount, 5));
                            stream.append("|");
                            stream.append(AppNexusUtils.intToString
                                                (lineCount,  5));
                            stream.append("}: ");
                            stream.append(chunk.getText());
                            stream.append("\n");
                        }
                        entryCount = entryCount + 1;
                        if(chunk.passedOnFreeRide)
                            entriesSinceHit = entriesSinceHit + 1;
                        else entriesSinceHit = 0;
                    }
                    else entriesSinceHit = -1;
                    lineCount  = lineCount + chunk.lineCount;
                    if(entryCount >= maxEntries)
                    {
                        maxExceeded = true;
                        break;
                    }
                }
                while (true);

            }
            finally
            {
                if(reader != null) reader.close();
                if(fr != null) fr.close();
            }
            if(stream != null) stream.flush();
            if(maxExceeded) break;
        }
        return ((stream == null) ? res : null);
    }

    static void emitLevelsMenu
            (Writer stream, String param, String title, Level level)
            throws IOException
    {
        stream.append("<TD>");
        stream.append(title);
        stream.append("</TD><TD>\n<SELECT NAME=\"");
        stream.append(param);
        stream.append("\" onChange=\"{ form.submit(); }\">");
        for(Level lev: LEVELS)
        {
            stream.append("\n  <OPTION VALUE=\"");
            if(lev == null)
            {
                if(level == null)
                    stream.append("\" SELECTED>");
                else stream.append("\">");
            }
            else
            {
                stream.append(Integer.toString(lev.toInt()));
                stream.append("\"");
                if(lev.equals(level))
                    stream.append(" SELECTED");
                stream.append(">");
                stream.append(lev.toString());
            }
        }
        stream.append("\n</SELECT></TD>");
    }

    HeadersAndFormat emitHTMLStuff
            (Writer stream, String stylesheetUrl, List<String> names,
             File logDir, String path, Level levelGTE, Level levelEQ,
             Date startDate, Date endDate, Integer maxEntries,
             Integer linesAfterHit, String regexp, boolean returnHeaders,
             String currentFormat, int currentFormatIndex,
             Map<String, String> httpParams)
            throws IOException
    {
        Map<String, String> headers =
                handlerPageSetup
                        (stream, "Log file filter", null, stylesheetUrl,
                                null, returnHeaders, prettyName, httpParams);
        stream.append("\n<FORM METHOD=\"POST\" ACTION=\"");
        stream.append(urlName);
        stream.append("\">\n");
        stream.append("\n<H3><TABLE BORDER=\"0\">");

        stream.append("<TR><TD>Log&nbsp;file:</TD><TD>" +
                      "\n<SELECT NAME=\"");
        stream.append(PATH_PARAM);
        stream.append("\" onChange=\"{ form.submit(); }\">");
        for(String name: names)
        {
            String fullPath =
                    escapeHtml(new File(logDir, name).toString());
            stream.append("\n  <OPTION VALUE=\"");
            stream.append(fullPath);
            stream.append("\"");
            if(fullPath.equals(path)) stream.append(" SELECTED");
            stream.append(">");
            stream.append(name);
        }
        if(names.size() > 1)
        {
            stream.append("\n  <OPTION VALUE=\"");
            stream.append(ALL_FILES_PARAM);
            stream.append("\">** All **");
        }
        stream.append("\n</SELECT></TD>");

        stream.append("<TD>Max&nbsp;entries:</TD><TD>" +
                      "<INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(MAX_ENTRIES_PARAM);
        stream.append("\" VALUE=\"");
        stream.append(maxEntries.toString());
        stream.append("\"></TD></TR>");

        stream.append("<TR>");

        emitLevelsMenu(stream, LEVEL_GTE_PARAM, "Level&gt;=", levelGTE);

        stream.append("<TD>Entries&nbsp;after&nbsp;match:</TD><TD>" +
                      "<INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(ENTRIES_AFTER_HIT_PARAM);
        stream.append("\" VALUE=\"");
        if(linesAfterHit != null) stream.append(linesAfterHit.toString());
        stream.append("\"></TD></TR>");

        stream.append("<TR>");

        emitLevelsMenu(stream, LEVEL_EQ_PARAM, "Level=", levelEQ);

        stream.append("<TD>Start&nbsp;time:</TD><TD>" +
                      "<INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(START_DATE_PARAM);
        stream.append("\" VALUE=\"");
        if(startDate != null)
            stream.append(hmsParser.format(startDate));
        stream.append("\"></TD></TR>");

        stream.append("<TR><TD>Regexp:</TD><TD>" +
                      "<INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(REGEXP_PARAM);
        stream.append("\" VALUE=\"");
        if(regexp != null)
            stream.append(escapeHtml(regexp));
        stream.append("\"></TD>");

        stream.append("<TD>End&nbsp;time:</TD><TD>" +
                      "<INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(END_DATE_PARAM);
        stream.append("\" VALUE=\"");
        if(endDate != null)
            stream.append(hmsParser.format(endDate));
        stream.append("\"></TD></TR>");

        stream.append("\n<TR><TD COLSPAN=\"4\">" +
                      "\n<INPUT TYPE=\"SUBMIT\" NAME=\"" + DOIT_PARAM +
                                "\" " + "VALUE=\"Do It\">");
        stream.append("\n<INPUT TYPE=\"SUBMIT\" NAME=\"" + EXPORT_PARAM +
                                "\" " + "VALUE=\"Export\"" +
                      " onClick=\"{ var ix = form." +
                      FORMAT_PARAM + ".selectedIndex; " +
                      "form.action = 'export.' + form." +
                      FORMAT_PARAM + "[ix].text.toLowerCase();"+
                      " return true; }\">\n");
        currentFormat = ACLHTTPHandler.outputFormatWidget
                                   (stream, currentFormatIndex, currentFormat);
        stream.append("</TD></TR>");
        stream.append("\n</TABLE></H3>");
        stream.append("\n</FORM>");
        stream.append("<PLAINTEXT>\n");
        return new HeadersAndFormat(headers, currentFormat);
    }

    public boolean isAdminUserCommand() { return true; }

    @SuppressWarnings("EmptyCatchBlock")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        Map<String, String> headers;
        String path = httpParams.get(PATH_PARAM);
        List<String> paths = new Vector<String>();
        Date wrtDate = AppNexusUtils.dayFloor(new Date());
        String formatStr = httpParams.get(FORMAT_PARAM);
        String currentFormat = saveFormats[0];
        int currentFormatIndex = -1;
        try
        {
            currentFormatIndex = Integer.parseInt(formatStr);
            currentFormat = saveFormats[currentFormatIndex];
        }
        catch(NumberFormatException e) {}
        if(currentFormatIndex == -1)
        {
            currentFormatIndex = TEXT_POSITION;
            currentFormat = saveFormats[currentFormatIndex];
        }
        Integer maxEntries = readParamPositiveInteger
                (MAX_ENTRIES_PARAM, httpParams);
        Integer entriesAfterHit = readParamPositiveInteger
                (ENTRIES_AFTER_HIT_PARAM, httpParams);
        Integer levelGTENumber = readParamInteger
                (LEVEL_GTE_PARAM, httpParams, null, null);
        Integer levelEQNumber = readParamInteger
                (LEVEL_EQ_PARAM, httpParams, null, null);
        Level levelGTE =
               (levelGTENumber == null ? null : Level.toLevel(levelGTENumber));
        Level levelEQ =
               (levelEQNumber == null ? null : Level.toLevel(levelEQNumber));
        boolean exportP = httpParams.get(EXPORT_PARAM) != null;
        File logDir = new File(LogFileListHTTPHandler.getLogDirPath());
        List<String> names = new ArrayList<String>();
        String[] fileNames = logDir.list(new Filter());
        if(fileNames != null)
            names.addAll(Arrays.asList(fileNames));
        Collections.sort(names);
        if (maxEntries == null) maxEntries = DEFAULT_MAX_LINES;
        if(names.size() == 0)
        {
            headers = handlerPageSetup
                            (stream, "Log file filter", null, stylesheetUrl,
                             null, returnHeaders, prettyName, httpParams);
            stream.append("<H1>No log files!</H1>");
            return headers;
        }
        else
        {
            if(path == null)
                path = new File(logDir, names.get(0)).toString();
            else path = unescapeHtml(path);
            if(ALL_FILES_PARAM.equals(path))
            {
                for(String name: names)
                {
                    paths.add(new File(logDir, name).toString());
                }
            }
            else paths.add(path);
            int dotPos = path.lastIndexOf(".");
            if(dotPos >= 0)
            {
                try
                {
                    wrtDate = dayParser.parse(path.substring(dotPos + 1));
                }
                catch (ParseException e) {}
            }
            Date startDate = readParamDateTime
                                (START_DATE_PARAM, httpParams, wrtDate);
            Date endDate = readParamDateTime
                                (END_DATE_PARAM, httpParams, wrtDate);
            String regexp = httpParams.get(REGEXP_PARAM);
            if(exportP)
            {
                System.out.println("Format: " + currentFormat);
                if(currentFormat == null ||
                   "".equals(currentFormat) ||
                   TEXT.equals(currentFormat))
                {
                    StringWriter sw = new StringWriter();
                    filterLogFiles(paths, sw, maxEntries, entriesAfterHit,
                                   startDate, endDate, wrtDate, levelGTE,
                                   levelEQ, regexp);
                    sw.flush();
                    String res = sw.toString();
                    headers = HTTPListener.emitStandardHeaders
                            (stream, 200, HTTPListener.application_binary,
                             (long)res.length(), null, false, returnHeaders);
                    stream.append(res);
                    stream.flush();
                }
                else
                {
                    List<LogChunk> chunks =
                            filterLogFiles(paths, null, maxEntries,
                                           entriesAfterHit, startDate, endDate,
                                           wrtDate, levelGTE, levelEQ, regexp);
                    SexpLoc sl = new SexpLoc();
                    for(LogChunk chunk: chunks)
                    {
                        sl.collect(chunk.toSexp());
                    }
                    if(XLS.equals(currentFormat))
                    {
                        ByteArrayOutputStream baos
                                = new ByteArrayOutputStream();
                        ExcelColSchema.dumpToXLSStream
                                (baos, sl.getSexp(), LogChunk.COLUMNS);
                        baos.flush();
                        baos.close();
                        byte[] ba = baos.toByteArray();
                        headers = HTTPListener.emitStandardHeaders
                                (stream, 200, HTTPListener.application_binary,
                                 (long)ba.length, null, false, returnHeaders);
                        stream.flush();
                        os.write(ba);
                        os.flush();
                    }
                    else
                    {
                        StringWriter sw = new StringWriter();
                        ACLHTTPHandler.dumpToTextStream
                           (sw, sl.getSexp(), currentFormat, LogChunk.COLUMNS);
                        String s = sw.toString();
                        headers = HTTPListener.emitStandardHeaders
                                (stream, 200, HTTPListener.application_binary,
                                 (long)s.length(), null, false, returnHeaders);
                        stream.write(s);
                        stream.flush();
                    }
                }
            }
            else
            {
                HeadersAndFormat pair =
                        emitHTMLStuff(stream, stylesheetUrl, names, logDir,
                                      path, levelGTE, levelEQ, startDate,
                                      endDate, maxEntries, entriesAfterHit,
                                      regexp, returnHeaders,
                                      currentFormat, currentFormatIndex,
                                      httpParams);
                headers = pair.headers;
                // currentFormat = pair.currentFormat;
                filterLogFiles(paths, stream, maxEntries, entriesAfterHit,
                               startDate, endDate, wrtDate, levelGTE, levelEQ,
                               regexp);
            }
            // Not needed if we're in plaintext mode.
            // HTMLifier.finishHTMLPage(stream);
            return headers;
        }
    }

    static {HTTPListener.registerHandlerClass(LogFileFilterHTTPHandler.class);}
}

class LogChunk {
    boolean passesFilter = false;
    String nextLine = null;
    Matcher knownMatcherForNextLine = null;
    int lineCount = 0;
    long linesRead = 0;
    Level level = null;
    String className = null;
    String methodName = null;
    int lineNumber = 0;
    Date eventTime = null;
    String path;
    String prelude;
    String firstLine;
    String body = null;
    boolean passedOnFreeRide = false;

    static List<LogChunkExtractor> extractors =
            Arrays.asList(new DeploymentLogChunkExtractor(),
                          new SimpleLogChunkExtractor());

    String getText()
    {
        return prelude + body;
    }

    public static final String[] COLUMNS = new String[]
            { "Time", "Severity", "Class", "Method", "Line", "Text" };

    public Sexpression toSexp()
    {
        SexpLoc res = new SexpLoc();
        res.collect(new DateAtom(eventTime));
        res.collect(new StringAtom(level.toString()));
        res.collect(new StringAtom(className));
        res.collect(new StringAtom(methodName == null ? "" : methodName));
        res.collect(new NumberAtom(lineNumber));
        res.collect(new StringAtom(body == null ? "" : body));
        return res.getSexp();
    }

    static Pattern fixUpPattern =
            Pattern.compile("\\[(.*)\\:(\\d+)\\] (.*)",
                            Pattern.MULTILINE | Pattern.DOTALL);

    void fixUpClassAndMethod()
    {
        if(body != null)
        {
            Matcher m = fixUpPattern.matcher(body);
            if(m.find())
            {
                String spec = m.group(1);
                int pos = spec.lastIndexOf(".");
                if(eventTime != null && pos > 0)
                {
                    className = spec.substring(0, pos);
                    methodName = spec.substring(pos + 1);
                    lineNumber = Integer.parseInt(m.group(2));
                    body = m.group(3);
                    // Now, reconstruct the prelude.
                    prelude = LogFileFilterHTTPHandler.timeParserMS.format
                                (eventTime) +
                              " " + level + " " + className +
                              "." + methodName +
                              ":" + lineNumber + " - ";
                }
            }
        }
    }

    static LogChunk getNextChunk(String line, Matcher knownMatcherForLine,
                                 BufferedReader reader, Date startDate,
                                 Date endDate, Date wrtDay,
                                 Level filterLevelGTE, Level filterLevelEQ,
                                 Pattern filterPattern, String filePath,
                                 Integer entriesAfterHit, int entriesSinceHit,
                                 List<LogChunkExtractor> applicableExtractors)
            throws IOException
    {
        // Line is always non-null.
        LogChunk res = new LogChunk();
        res.path = filePath;
        Matcher m = null;
        LogChunkExtractor e = null;
        if(knownMatcherForLine == null)
        {
            for(LogChunkExtractor thisExtractor: applicableExtractors)
            {
                m = thisExtractor.matches(line);
                if(m != null)
                {
                    e = thisExtractor;
                    break;
                }
                else m = null;
            }
            if(e != null && applicableExtractors.size() > 1)
            {
                // We've found a match for this file, so remove all others
                // (but just for this file).  Avoid concurrent modification errors.
                List<LogChunkExtractor> toRemove = new Vector<LogChunkExtractor>();
                for(LogChunkExtractor thisExtractor: applicableExtractors)
                {
                    if(thisExtractor != e)
                        toRemove.add(thisExtractor);
                }
                applicableExtractors.removeAll(toRemove);
            }
        }
        else
        {
            m = knownMatcherForLine;
            e = applicableExtractors.get(0);
        }
        // Find the first pattern that matches this line and use it
        // consistently.
        if(e != null)
        {
            res = e.initChunk(res, m, line, startDate, endDate, wrtDay,
                              filterLevelGTE, filterLevelEQ, filterPattern,
                              reader, entriesAfterHit != null &&
                                      entriesSinceHit >= 0 &&
                                      entriesSinceHit < entriesAfterHit);
            if(res != null)
                res.fixUpClassAndMethod();
        }
        return res;
    }
}

abstract class LogChunkExtractor {
    Pattern pattern;

    public abstract Matcher matches(String line);

    public abstract LogChunk initChunk
            (LogChunk chunk, Matcher m, String line, Date startDate,
             Date endDate, Date wrtDay, Level filterLevelGTE,
             Level filterLevelEQ, Pattern filterPattern, BufferedReader reader,
             boolean freeRideOK)
            throws IOException;

}

class DeploymentLogChunkExtractor extends LogChunkExtractor {
// This handles cases that look like this:
// 00:12:41,604  INFO Status:17 - Fetching advertiser data for 11346
    public DeploymentLogChunkExtractor()
    {
        pattern = Pattern.compile
               ("(\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d)\\s*(.*) (.*):(\\d+) - (.*)");
    }

    public Matcher matches(String line)
    {
        Matcher m = pattern.matcher(line);
        if(m.find()) return m;
        else return null;
    }

    public LogChunk initChunk(LogChunk res, Matcher m, String line,
                              Date startDate, Date endDate, Date wrtDay,
                              Level filterLevelGTE, Level filterLevelEQ,
                              Pattern filterPattern, BufferedReader reader,
                              boolean freeRideOK)
            throws IOException
    {
        String timePart = m.group(1);
        try
        {
            res.eventTime =
                   LogFileFilterHTTPHandler.dateFromHour(timePart, wrtDay);
        }
        catch (ParseException e)
        {
            return res;
        }
        String levelString = m.group(2);
        res.level = Level.toLevel(levelString);
        res.className = m.group(3);
        res.lineNumber = Integer.parseInt(m.group(4));
        res.firstLine = m.group(5);
        res.prelude =
                line.substring(0, line.length() - res.firstLine.length());
        StringBuffer b = new StringBuffer(res.firstLine);
        res.lineCount = res.lineCount + 1;
        while(true)
        {
            res.nextLine = reader.readLine();
            res.linesRead = res.linesRead + 1;
            if(res.nextLine == null) break;
            Matcher m2 = pattern.matcher(res.nextLine);
            if(m2.find())
            {
                res.knownMatcherForNextLine = m2;
                break;
            }
            else
            {
                b.append("\n");
                b.append(res.nextLine);
                res.nextLine = null;
                res.lineCount = res.lineCount + 1;
                res.knownMatcherForNextLine = null;
            }
        }
        res.body = b.toString();
        if((startDate == null ||
            (res.eventTime.getTime() >= startDate.getTime())) &&
           (endDate == null ||
            (res.eventTime.getTime() <= endDate.getTime())) &&
           (filterLevelGTE == null ||
            (res.level != null &&
             res.level.toInt() >= filterLevelGTE.toInt())) &&
           (filterLevelEQ == null ||
            (res.level != null &&
             res.level.toInt() == filterLevelEQ.toInt())) &&
           (filterPattern == null || filterPattern.matcher(m.group(5)).find()))
        {
            res.passesFilter = true;
        }
        else if(freeRideOK)
        {
            res.passesFilter = true;
            res.passedOnFreeRide = true;
        }
        else {}
        return res;
    }
}

class SimpleLogChunkExtractor extends LogChunkExtractor {
// This handles cases that look like this:
// 1469 [main] INFO com.tumri.mediabuying.zini.Utils  - Fetching advertiser data for 17191
    public SimpleLogChunkExtractor()
    {
        pattern = Pattern.compile
               ("(\\d+) \\[(.*)\\] (.*) (.*)\\s*- (.*)");
    }

    public Matcher matches(String line)
    {
        Matcher m = pattern.matcher(line);
        if(m.find()) return m;
        else return null;
    }

    public LogChunk initChunk(LogChunk res, Matcher m, String line,
                              Date startDate, Date endDate, Date wrtDay,
                              Level filterLevelGTE, Level filterLevelEQ,
                              Pattern filterPattern, BufferedReader reader,
                              boolean freeRideOK)
            throws IOException
    {
        String timePart = m.group(1);
        try
        {
            res.eventTime =
                   LogFileFilterHTTPHandler.dateFromHour
                           (Integer.parseInt(timePart), wrtDay);
        }
        catch (ParseException e)
        {
            return res;
        }
        String levelString = m.group(3);
        res.level = Level.toLevel(levelString);
        res.className = m.group(2);
        res.lineNumber = -1; // Not available.
        res.firstLine = m.group(5);
        res.prelude =
                line.substring(0, line.length() - res.firstLine.length());
        StringBuffer b = new StringBuffer(res.firstLine);
        res.lineCount = res.lineCount + 1;
        while(true)
        {
            res.nextLine = reader.readLine();
            res.linesRead = res.linesRead + 1;
            if(res.nextLine == null) break;
            Matcher m2 = pattern.matcher(res.nextLine);
            if(m2.find()) break;
            else
            {
                b.append("\n");
                b.append(res.nextLine);
                res.nextLine = null;
                res.lineCount = res.lineCount + 1;
            }
        }
        res.body = b.toString();
        if((startDate == null ||
            (res.eventTime.getTime() >= startDate.getTime())) &&
           (endDate == null ||
            (res.eventTime.getTime() <= endDate.getTime())) &&
           (filterLevelGTE == null ||
            (res.level != null &&
             res.level.toInt() >= filterLevelGTE.toInt())) &&
           (filterLevelEQ == null ||
            (res.level != null &&
             res.level.toInt() == filterLevelEQ.toInt())) &&
           (filterPattern == null || filterPattern.matcher(m.group(5)).find()))
        {
            res.passesFilter = true;
        }
        else if(freeRideOK)
        {
            res.passesFilter = true;
            res.passedOnFreeRide = true;
        }
        else {}
        return res;
    }
}

@SuppressWarnings("unused")
class BroadcastWriter extends Writer {
    List<Writer> streams = new Vector<Writer>();

    public void flush() throws IOException
    {
        for(Writer w: streams) { w.flush(); }
    }

    public void close() throws IOException
    {
        for(Writer w: streams) { w.close(); }
    }

    public void write(char[] buff, int off, int len) throws IOException
    {
        for(Writer w: streams) { w.write(buff, off, len); }
    }

    @SuppressWarnings("unused")
    public void addStream(Writer w)
    {
        streams.add(w);
    }
}

class HeadersAndFormat {
    Map<String, String> headers;
    String currentFormat;

    public HeadersAndFormat(Map<String, String> headers,
                            String currentFormat)
    {
        this.headers = headers;
        this.currentFormat = currentFormat;
    }
}