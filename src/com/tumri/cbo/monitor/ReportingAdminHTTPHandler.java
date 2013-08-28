package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.Bidder;
import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;

public class ReportingAdminHTTPHandler extends HTTPHandler {

    static String urlName = "REPORTINGADMIN";
    static String prettyName = "Reporting Administration";
    static final String DELETE_PARAM = "DELETE";
    static final String EMAIL_PARAM = "EMAIL";
    static final String UNKNOWN_EMAIL = "Unknown";
    static final int NEW_ID = -1;
    static int emailBoxWidth = 40;


    public ReportingAdminHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(urlName, this);
    }
    
    public boolean isAdminUserCommand() { return true; }

    static Sexpression getAdminRows(SQLConnector connector, QueryContext qctx)
    {
        StringBuffer query = new StringBuffer();
        query.append("SELECT DISTINCT id, email_address");
        for(ReportApplicabilityColumn col: ReportApplicabilityColumn.values())
        {
            query.append(", ");
            query.append(col.getColumnName());
        }
        query.append("\nFROM users");
        query.append("\nWHERE 1 = 1");
        query.append(";");
        Sexpression rows;
        rows = connector.runSQLQuery(query.toString(), qctx);
        return rows;
    }

    static int getId(String key, String cookie)
    {
        if(key.startsWith(cookie)) // Allow for the "_"!
            return Integer.parseInt(key.substring(cookie.length() + 1));
        else throw Utils.barf("Cookie not found.", null, key, cookie);
    }

    static void deleteRow(int id, SQLConnector connector, QueryContext qctx)
    {
        String query = "DELETE FROM users WHERE id = " +
                            Integer.toString(id) + ";";
        connector.runSQLUpdate(query, qctx);
    }

    static boolean processDeletes(Map<String, String> httpParams,
                                  SQLConnector connector, QueryContext qctx)
    {
        boolean deleteP = false;
        for(String key: httpParams.keySet())
        {
            if(key.startsWith(DELETE_PARAM))
            {
                int id = getId(key, DELETE_PARAM);
                deleteRow(id, connector, qctx);
                deleteP = true;
                break;
            }
        }
        return deleteP;
    }

    static String escape(String arg)
    {
        return arg.replace("'", "''");
    }

    static void saveRow(int id, Map<String, String> httpParams,
                        SQLConnector connector, QueryContext qctx)
    {
        StringBuffer query = new StringBuffer("UPDATE users");
        String emailKey = EMAIL_PARAM + "_" + Integer.toString(id);
        String email = httpParams.get(emailKey);
        if(email == null || "".equals(email))
            email = UNKNOWN_EMAIL; // Should never happen!
        query.append("\n  SET email_address = '");
        query.append(escape(email));
        query.append("'");
        for(ReportApplicabilityColumn col: ReportApplicabilityColumn.values())
        {
            String name = col.getColumnName();
            query.append(",\n      ");
            query.append(name);
            query.append(" = ");
            String key = name + "_" + Integer.toString(id);
            boolean val = httpParams.get(key) != null;
            query.append(val ? "true" : "false");
        }
        query.append("\nWHERE id = ");
        query.append(Integer.toString(id));
        query.append(";");
        connector.runSQLUpdate(query.toString(), qctx);
    }

    static void maybeSaveNewRow(Map<String, String> httpParams,
                                SQLConnector connector, QueryContext qctx)
    {
        String emailKey = EMAIL_PARAM + "_" + NEW_ID;
        String email = httpParams.get(emailKey);
        if(email != null && !"".equals(email))
        {
            StringBuffer query =
                    new StringBuffer("INSERT INTO users (email_address");
            for(ReportApplicabilityColumn col:
                    ReportApplicabilityColumn.values())
            {
                String name = col.getColumnName();
                query.append(", ");
                query.append(name);
            }
            query.append(") VALUES ('");
            query.append(escape(email));
            query.append("'");
            for(ReportApplicabilityColumn col:
                    ReportApplicabilityColumn.values())
            {
                String name = col.getColumnName();
                query.append(", ");
                String key = name + "_" + Integer.toString(NEW_ID);
                boolean val = httpParams.get(key) != null;
                query.append(val ? "true" : "false");
            }
            query.append(");");
            connector.runSQLUpdate(query.toString(), qctx);
        }
    }

    static void processSaves(Map<String, String> httpParams,
                             SQLConnector connector, QueryContext qctx)
    {
        List<Integer> idsToSave = new Vector<Integer>();
        for(String key: httpParams.keySet())
        {
            if(key.startsWith(EMAIL_PARAM))
            {
                int id = getId(key, EMAIL_PARAM);
                if(id != NEW_ID) idsToSave.add(id);
            }
        }
        for(Integer id: idsToSave)
        {
            saveRow(id, httpParams, connector, qctx);
        }
    }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        // Real IDs are never negative, so -1 (NEW_ID) indicates a new row.
        String stylesheetUrl = null;
        boolean saveP = httpParams.get(SAVE_PARAM) != null;
        Bidder bidder = Bidder.getInstance();
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = new BasicQueryContext
                                (null, bidder.getAppNexusTheory());
        boolean deletePerformedP = processDeletes(httpParams, connector, qctx);
        if(!deletePerformedP && saveP)
        {
            processSaves(httpParams, connector, qctx);
            maybeSaveNewRow(httpParams, connector, qctx);
        }
        // Now emit he page itself.
        Map<String, String> headers =
                handlerPageSetup
                        (stream, "Reporting Administration", null,
                                stylesheetUrl, null, returnHeaders,
                                prettyName, httpParams);
        String header ="<H2><FORM ACTION=\"../" +
                ReportingAdminHTTPHandler.urlName + "/" +
                ReportingAdminHTTPHandler.urlName + "?" +
                "\" METHOD=\"POST\">";
        stream.append(header);
        Sexpression rows = getAdminRows(connector, qctx);
        stream.append("\n<TABLE BORDER=1>");
        stream.append("\n  <TR>");
        stream.append("\n    <TH>Action</TH>");
        stream.append("\n    <TH>EMail Address</TH>");
        for(ReportApplicabilityColumn col: ReportApplicabilityColumn.values())
        {
            stream.append("\n    <TH>");
            stream.append(htmlify(col.getPrettyName()));
            stream.append("</TH>");
        }
        stream.append("\n  </TR>");
        while(rows != Null.nil)
        {
            Sexpression row = rows.car();
            stream.append("\n  <TR>");
            String id = row.car().unboxLong().toString();
            stream.append("\n    <TD><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                                    DELETE_PARAM + "_");
            stream.append(id);
            stream.append("\" VALUE=\"Delete\"></TD>");
            stream.append("\n    <TD><INPUT TYPE=\"TEXT\" NAME=\"" +
                                    EMAIL_PARAM + "_");
            stream.append(id);
            stream.append("\" VALUE=\"");
            stream.append(htmlify(row.second().unboxString()));
            stream.append("\" SIZE=\"");
            stream.append(Integer.toString(emailBoxWidth));
            stream.append("\"></TD>");
            row = row.cdr().cdr();
            for(ReportApplicabilityColumn col:
                    ReportApplicabilityColumn.values())
            {
                Sexpression cell = row.car();
                stream.append("\n    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"");
                stream.append(col.getColumnName());
                stream.append("_");
                stream.append(id);
                stream.append("\"");
                if(cell.unboxLong() == 1)
                    stream.append(" CHECKED");
                stream.append("></TD>");
                row = row.cdr();
            }
            stream.append("\n  </TR>");
            rows = rows.cdr();
        }
        stream.append("\n  <TR>");
        stream.append("\n    <TD>Add new user:</TD>");
        stream.append("\n    <TD><INPUT TYPE=\"TEXT\" NAME=\"");
        stream.append(EMAIL_PARAM);
        stream.append("_");
        stream.append(Integer.toString(NEW_ID));
        stream.append("\" VALUE=\"\" SIZE=\"");
        stream.append(Integer.toString(emailBoxWidth));
        stream.append("\"></TD>");
        for(ReportApplicabilityColumn col: ReportApplicabilityColumn.values())
        {
            stream.append("\n    <TD><INPUT TYPE=\"CHECKBOX\" NAME=\"");
            stream.append(col.getColumnName());
            stream.append("_" + NEW_ID + "\"></TD>");
        }
        stream.append("\n  </TR>");
        stream.append("\n</TABLE>");
        stream.append("\n<INPUT TYPE=\"SUBMIT\" NAME=\"Save\" VALUE=\"");
        stream.append(SAVE_PARAM);
        stream.append("\">");
        stream.append("</FORM></H2>");
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static { HTTPListener.registerHandlerClass(ReportingAdminHTTPHandler.class); }
}
