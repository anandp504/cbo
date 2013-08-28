package com.tumri.cbo.backend;

import au.com.bytecode.opencsv.CSVParser;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.zini.SynchDateFormat;
import com.tumri.mediabuying.zini.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

public class AmpInterface {

    // There are ~4600 advertisers and ~2500 sites, so set this to a value
    // large enough that we can get them in one page.
    public static int defaultAmpFetchPageSize = 10000;
    static final String ADVERTISER_REPORT_KEY = "advertiser";
    static final String SITE_REPORT_KEY = "site";
    static final String AppNexus_SITE_NAME = "AppNexus";
    static SynchDateFormat ampDateFormat =
            new SynchDateFormat("yyyy-MM-dd");

    @SuppressWarnings("EmptyCatchBlock")
    public static String fetchURLInternal
            (String host, String method, URL serverAddress,
             HttpURLConnection conn, boolean verbose)
            throws IOException
    {
        String res;
        //Set up the initial connection
        conn = (HttpURLConnection)serverAddress.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);
        conn.setReadTimeout(0); // Wait forever if need be!
        conn.setRequestProperty("Content-Length", "0");
        conn.setRequestProperty
                ("Content-Type", "application/x-www-form-urlencoded");
        if(verbose)
            System.out.println("Amp Interface request: " + serverAddress);
        try { conn.connect(); }
        catch (java.net.UnknownHostException he)
        {
            throw new Error("Unknown host: " + host);
        }
        BufferedReader in = null;
        //read the result from the server
        int code = -1;
        String errorMessage = null;
        try { code = conn.getResponseCode(); }
        // Should never happen, but, ....
        catch (Exception exc)
        {
            errorMessage = exc.getMessage();
            System.out.println
                    ("Got exception when getting a response code:" +
                            errorMessage);
        }
        if(code == 200 || code == 201)
            in = new BufferedReader
                    (new InputStreamReader(conn.getInputStream()));
        else
        {
            InputStream er = conn.getErrorStream();
            if(er == null)
            {
                try
                {
                    in = new BufferedReader
                            (new InputStreamReader(conn.getInputStream()));
                }
                catch (Exception e) {}
                String body =
                        (in == null ? "" : AppNexusInterface.readToEOF(in));
                if(errorMessage == null)
                    throw new Error
                            ("" + code + " Server error " +
                                    "(No error message supplied) " + body);
                else throw new Error
                        ("" + code + " Server error: " + errorMessage);
            }
            else in  = new BufferedReader(new InputStreamReader(er));
        }
        res = AppNexusInterface.readToEOF(in);
        if(verbose)
        {
            System.out.println("Amp Interface response:\n");
            System.out.println("------------------------------------------\n");
            System.out.println(res);
            System.out.println("------------------------------------------\n");
        }
        return res;
    }

    @SuppressWarnings("EmptyCatchBlock")
    public static String fetchURL(String protocol, String host, int port,
                                  String url, String method, boolean verbose)
    {
        HttpURLConnection conn = null;
        String res = null;
        try
            {
                URL serverAddress = new URL
                        (protocol + host + ":" + port +
                            (url.length() > 0 && url.substring(0,1).equals("/")
                                        ? ""
                                        : "/")
                            + url);
                //Set up the initial connection
                conn = (HttpURLConnection)serverAddress.openConnection();
                res = fetchURLInternal(host, method, serverAddress, conn,
                                       verbose);
            }
        catch (MalformedURLException e) {
                System.err.println("MalformedURLException : " + e);
                e.printStackTrace();
            }
        catch (ProtocolException e) {
                System.err.println("ProtocolException : " + e);
                e.printStackTrace();
            }
        catch (IOException e) {
                System.err.println("IOException : " + e);
                e.printStackTrace();
            }
        finally
            {
                //close the connection, set all objects to null
                if(conn != null) conn.disconnect();
                // in = null; sb = null; wr = null; conn = null;
            }
    	return res;
    }

    @SuppressWarnings("unused")
    public static JSONArray csvToJSONArray(String csv)
    {
        JSONArray res = new JSONArray();
        return csvToJSONArray(csv, res);
    }

    @SuppressWarnings("unchecked")
    public static JSONArray csvToJSONArray(String csv, JSONArray res)
    {
        CSVParser parser = new CSVParser();
        String[] lines = csv.split("\n");
        for(int lineIdx = 0; lineIdx < lines.length; lineIdx++)
        {
          lines[lineIdx] = lines[lineIdx].trim();
        }
        int startLine = 0;
        for(int lineIdx = 0; lineIdx < lines.length; lineIdx++)
        {
            startLine = lineIdx;
            if(lines[lineIdx].length() > 0) break;
        }
        String[] cols = lines[startLine].split(",");
        try
        {
            for(int i = startLine + 1; i < lines.length; i++)
            {
                String line = lines[i];
                if(line.length() > 0)
                {
                    String[] vals = parser.parseLineMulti(line);
                    JSONObject obj = new JSONObject();
                    int j = 0;
                    for(String col: cols)
                    {
                        String val = vals[j];
                        obj.put(col, val);
                        j = j + 1;
                    }
                    res.add(obj);
                }
            }
        }
        catch (IOException e) { throw new Error(e); }
        return res;
    }

    public static XMLExtract csvToJSON(String s)
    {
        XMLExtract res = new XMLExtract();
        return csvToJSON(s, res);
    }

    public static XMLExtract csvToJSON(String s, XMLExtract res)
    {
        Pattern p = Pattern.compile
          (".* pagestart=\"(\\d*)\" fullcount=\"(\\d*)\".*\\[CDATA\\[(.*)\\]\\]",
           Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(s);
        if(m.find())
        {
            String pageStart = m.group(1);
            String fullCount = m.group(2);
            String csv = m.group(3);
            res.values = csvToJSONArray(csv, res.values);
            res.pageStart = Integer.parseInt(pageStart);
            res.fullCount = Integer.parseInt(fullCount);
            return res;
        }
        return res;
    }

    public static MapExtract csvToMap
            (String csv, Object idKey, Object valueKey,
             MapExtract res)
    {
        XMLExtract x = csvToJSON(csv);
        for(Object obj: x.values)
        {
            if(obj instanceof JSONObject)
            {
                JSONObject jo = (JSONObject) obj;
                Object key = jo.get(idKey);
                Object value = jo.get(valueKey);
                res.addValue(key, value);
            }
        }
        res.fullCount = x.fullCount;
        res.pageStart = x.pageStart;
        return res;
    }

    static Date daysBefore(Date d, int days)
    {
        return new Date(d.getTime() - (days * 24L * 3600L * 1000L));
    }

    public static Map<Object, Object> thingToIdMap
            (String thingType, int userId, boolean verbose, String token)
    {
        // Note: date values are arbitrary, but are needed by the API, and it
        // seems to have to be at least two days ago to be safe.  If we don't
        // do this, there's a danger of getting back zero advertisers/sites.
        // We'd like to set it to new Date(0); so that we pick up values from
        // the beginning of time, but then AMP generates errors.  So, .....
        // pick an absolute value from before Collective came into existence.
        Date now = new Date();
        // Date twoDaysAgo = daysBefore(now, 2);
        SynchDateFormat format = new SynchDateFormat("yyyy-MM-dd");
        Date fromDate;
        try
        {
            fromDate = format.parse("2000-01-01");
        }
        catch(ParseException e)
        {
            throw Utils.barf(e, null);
        }
        String fromDateString = toAmpDateString(fromDate);
        String toDateString   = toAmpDateString(now);
        MapExtract res = new MapExtract();
        int pageStart = 0;
        int pageSize = defaultAmpFetchPageSize;
        while(true)
        {
            String url = "/delivery/detailfilter"
                    + "?filtertype="
                    + "&sortDirection=ascending"
                    + "&format=csv"
                    + "&mg=none"
                    + "&type=est"
                    + "&filter="
                    + "&calltype=data"
                    + "&tkn=" + token
                    + "&usage=0"
                    + "&sortField=name"
                    + "&filterid="
                    + "&userid=" + userId
                    + "&tm=" + now.getTime()
                    + "&queryid=" + now.getTime()
                    + "&timestamp=" + now.getTime()
                    + "&listtype=" + thingType
                    + "&detailtype=" + thingType
                    + "&item=" + thingType
                    + "&grp=" + thingType
                    + "&pagesize=" + pageSize
                    + "&pagestart=" + pageStart
                    + "&fromdate=" + fromDateString
                    + "&todate=" + toDateString;
            String resString = fetchURL
                    ("HTTP://", "cm.cdb.collective-media.net", 80, url,
                     AppNexusInterface.GET, verbose);
            res = csvToMap(resString, "name", "id", res);
            if(res.rowsFound >= res.fullCount) break;
            pageStart = pageStart + pageSize;
        }
        return res.map;
    }

    public static String toAmpDateString(Date date)
    {
        return ampDateFormat.format(date).replace("-", "%2D");
    }

    public static String makeNewAmpToken(String userid, String network_id)
    {
        // These both look like ints, but there's nothing obvious about
        // the token generating process requiring this.
        // Example network_id = "6";
        // Example userid = "3975";
        TimeZone easternTimeZone = TimeZone.getTimeZone("est5edt");
        SynchDateFormat format = new SynchDateFormat("yyyy-MM-dd");
        Calendar etCal = Calendar.getInstance(easternTimeZone);
        format.setCalendar(etCal);
        String dateString = format.format(new Date());
        String tokenString = network_id + ":" + userid + ":" + dateString;
        // MD5 conversion taken from http://www.mkyong.com/java/java-md5-hashing-example/
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(tokenString.getBytes());
            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for (byte b: byteData)
            {
              sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }
        catch(NoSuchAlgorithmException e)
        {
            throw Utils.barf(e, null);
        }
    }

    public static JSONArray getZonesSpreadsheet
            (int advertiserId, int appnexusSiteId, int userId,
             Date fromDate, Date toDate, boolean verbose,
             String token)
    {
        Date now = new Date();
        String fromDateString = toAmpDateString(fromDate);
        String toDateString   = toAmpDateString(  toDate);
        XMLExtract res = new XMLExtract();
        int pageStart = 0;
        int pageSize = defaultAmpFetchPageSize;
        while(true)
        {
            String url = "/delivery/data"
                    + "?filtertype=OR"
                    + "&sortDirection=ascending"
                    + "&grp=advertiser"
                    + "&format=csv"
                    + "&item=zone"
                    + "&detailfilter="
                    + "&filter=site"
                    + "&calltype=data"
                    + "&listtype=advertiser"
                    + "&listfilter="
                    + "&userid=" + userId
                    + "&tm=" + now.getTime()
                    + "&queryid=" + now.getTime()
                    + "&timestamp=" + now.getTime()
                    + "&detailid=" + advertiserId
                    + "&filterid=" + appnexusSiteId
                    + "&parentid=" + advertiserId
                    + "&pagestart=" + pageStart
                    + "&pagesize=" + pageSize
                    + "&todate=" + toDateString
                    + "&fromdate=" + fromDateString
                    + "&tkn=" + token
                    + "&detailtype=zone"
                    + "&sortField=name"
                    + "&parenttype=advertiser";
            String resString = fetchURL
                    ("HTTP://", "cm.cdb.collective-media.net", 80, url,
                     AppNexusInterface.GET, verbose);
            res = csvToJSON(resString, res);
            if(res.values.size() >= res.fullCount) break;
            pageStart = pageStart + pageSize;
        }
        return res.values;
    }

    @SuppressWarnings("unchecked")
    public static Integer getAppNexusSiteId(int userId, boolean verbose,
                                            String token)
    {
        Map<Object, Object> map =
                thingToIdMap(SITE_REPORT_KEY, userId, verbose, token);
        String idString = (String)map.get(AppNexus_SITE_NAME);
        if(idString == null) return null;
        else return Integer.parseInt(idString);
    }

    @SuppressWarnings("unchecked")
    public static Integer getAdvertiserId
            (String name, int userId, boolean verbose, String token)
    {
        Map<Object, Object> map =
                thingToIdMap(ADVERTISER_REPORT_KEY, userId, verbose, token);
        String idString = (String)map.get(name);
        if(idString == null) return null;
        else return Integer.parseInt(idString);
    }

    @SuppressWarnings("unused")
    public static Map<String, Number> getZoneReportFor
            (int advertiserId, int appNexusId, String attribute, int userId,
             Date forDay, boolean verbose, String token)
    {
        JSONArray zoneData = getZonesSpreadsheet
            (advertiserId, appNexusId, userId, forDay, forDay, verbose, token);
        return getZoneReportFor(zoneData, attribute);
    }

    @SuppressWarnings("EmptyCatchBlock")
    public static Map<String, Number> getZoneReportFor
            (JSONArray zoneData, String attribute)
    {
        Map<String, Number> res = new HashMap<String, Number>();
        for(Object o: zoneData)
        {
            if(o instanceof JSONObject)
            {
                JSONObject jo = (JSONObject) o;
                Object v = jo.get(attribute);
                String zoneName = (String)jo.get("name");
                Number n = null;
                if(v instanceof String)
                {
                    try { n = Long.parseLong((String) v); }
                    catch (NumberFormatException e) {}
                    if(n == null)
                    {
                        try { n = Double.parseDouble((String) v); }
                        catch (NumberFormatException e) {}
                    }
                    if(n != null) res.put(zoneName, n);
                }
            }
        }
        return res;
    }

    public static void main(String[] args)
    {
        // Where will we get this from? I think this is for JK.
        int userId = 9104;
        // This seems to be just a magic number, and presumably
        // shouldn't change.
        int network_id = 6;
        String advertiserName =
                "New England Toyota Dealers";
                // "Virginia State Lottery";
        String token = makeNewAmpToken(Integer.toString(userId),
                                       Integer.toString(network_id));
        System.out.println("Token: " + token);
        Date now = new Date();
        Date   toDate = daysBefore(now, 1);
        Date fromDate = daysBefore(now, 100);
        boolean verbose = true;
        Map<Object, Object> advertisers =
                thingToIdMap(ADVERTISER_REPORT_KEY, userId, verbose, token);
        Map<Object, Object> sites =
                thingToIdMap(SITE_REPORT_KEY, userId, verbose, token);
        Integer appNexusId = getAppNexusSiteId(userId, verbose, token);
        if(appNexusId == null) 
            throw new Error("Couldn't find AppNexus ID", null);
        Integer advertiserId =
                getAdvertiserId(advertiserName, userId, verbose, token);
        if(advertiserId == null)
            throw new Error("Couldn't find an Advertiser ID for " +
                            advertiserName);
        JSONArray zoneData =
                getZonesSpreadsheet(advertiserId, appNexusId, userId, fromDate,
                                    toDate, verbose, token);
        Map<String, Number> clickMap = getZoneReportFor(zoneData, "clicks");
        System.out.println("Advertisers: " + advertisers);
        System.out.println("Sites: " + sites);
        System.out.println("AppNexus ID: " + appNexusId);
        System.out.println("Advertiser ID: " + advertiserId);
        System.out.println("Zone Data: " + zoneData);
        System.out.println("Click Map: " + clickMap);
    }

}

class XMLExtract {
    JSONArray values = new JSONArray();
    int pageStart;
    int fullCount;
}

class MapExtract {
    Map<Object, Object> map = new HashMap<Object, Object>();
    int pageStart;
    int fullCount;
    int rowsFound = 0;

    public void addValue(Object key, Object value)
    {
        map.put(key, value);
        rowsFound = rowsFound + 1;
    }

}