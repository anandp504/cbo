package com.tumri.cbo.testing;

import com.mongodb.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.FetchReportService;
import com.tumri.mediabuying.zini.SynchDateFormat;
import com.tumri.mediabuying.zini.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.InputStream;
import java.util.*;
import org.apache.commons.io.IOUtils;


public abstract class AbstractServiceFilter implements ResultFilter {

    public static final SynchDateFormat dateTimeFormat =
            new SynchDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SynchDateFormat yyyyMMddDateFormat =
            new SynchDateFormat("yyyy-MM-dd");
    public static final String BASE_DATE_STRING = "2012-08-21";
    public static final String GRID_VALUE_COOKIE = "GRID_FS_VALUE_";
    public static final String GRID_BUCKET = "APPNEXUS";
    public static final int MAX_STRING_LENGTH = 2000000;
    public static final String QUERY_URL = "QUERY_URL";
    public static final String QUERY_PARAMETER_BLOCK = "QUERY_PARAMETER_BLOCK";
    public static final Date BASE_DATE = yyyymmddParse(BASE_DATE_STRING);
    public static final String START_DATE_KEY = "start_date";
    public static final String END_DATE_KEY = "end_date";
    public static final String DATE_KEY = "date";
    public static final Set<String> DATE_KEYS =
            new HashSet<String>
                    (Arrays.asList(START_DATE_KEY, END_DATE_KEY, DATE_KEY));

    DB db;
    String collectionMangulation = "";
    Map<String, DBCollection> knownCollections =
            new HashMap<String, DBCollection>();

    public AbstractServiceFilter() {}

    public AbstractServiceFilter(DB db)
    {
        this.db = db;
        for(String name: db.getCollectionNames())
        {
            knownCollections.put(name, db.getCollection(name));
        }
    }

    public AbstractServiceFilter(DB db, String collectionMangulation)
    {
        this(db);
        this.collectionMangulation = collectionMangulation;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    DBCollection ensureCollection(String name, Object elt)
    {
        DBCollection existing = knownCollections.get(name);
        if(existing == null)
        {
            synchronized(db)
            {
                existing = knownCollections.get(name);
                if(existing == null)
                {
                    DBObject collectionOptions = new BasicDBObject();
                    existing = db.createCollection(name, collectionOptions);
                    if(elt instanceof AbstractAppNexusServiceWithId)
                    {
                        DBObject keys = new BasicDBObject("id", 1);
                        DBObject indexOptions = new BasicDBObject("unique", 1);
                        existing.ensureIndex(keys, indexOptions);
                    }
                    else if(elt instanceof FetchReportService)
                    {
                        DBObject keys = new BasicDBObject("status", 1);
                        DBObject indexOptions = new BasicDBObject("unique", 1);
                        existing.ensureIndex(keys, indexOptions);
                    }
                    knownCollections.put(name, existing);
                }
            }
        }
        return existing;
    }

    @SuppressWarnings({"unchecked", "unused"})
    Object mongoToJSONObject(Object o)
    {
        if(o instanceof List)
        {
            JSONArray res = new JSONArray();
            for(Object val: (List)o)
            {
                res.add(mongoToJSONObject(val));
            }
            return res;
        }
        else if(o instanceof Map)
        {
            JSONObject res = new JSONObject();
            Map map = (Map)o;
            for(Object key: map.keySet())
            {
                res.put(key, mongoToJSONObject(map.get(key)));
            }
            return res;
        }
        else return o;
    }

    Object toMongoObject(Object obj)
    {
        if(obj instanceof JSONObject)
        {
            JSONObject jo = (JSONObject) obj;
            DBObject res = new BasicDBObject();
            for(Object key: jo.keySet())
            {
                if(key instanceof String)
                    res.put((String)key, toMongoObject(jo.get(key)));
            }
            return res;
        }
        else if(obj instanceof JSONArray)
        {
            JSONArray ja = (JSONArray) obj;
            BasicDBList res = new BasicDBList();
            for(Object v: ja)
            {
                res.add(toMongoObject(v));
            }
            return res;
        }
        else return obj;
    }

    static Long getArgLong(String name, String[] argNames, Object[] arguments)
    {
        return (Long)getArg(name, argNames, arguments);
    }

    static Object getArg(String name, String[] argNames, Object[] arguments)
    {
        for(int i = 0; i < argNames.length; i++)
        {
            if(name.equals(argNames[i])) return arguments[i];
        }
        return null;
    }

    List<AbstractAppNexusService> toServiceList
            (List<DBObject> lst, AbstractAppNexusService prototype,
             long dateOffset, AdjustDirection direction,
             TranslateDirection transDir)
    {
        List<AbstractAppNexusService> res =
                new Vector<AbstractAppNexusService>();
        Class<? extends AbstractAppNexusService> myClass =prototype.getClass();
        try
        {
            Constructor constructor = myClass.getConstructor(JSONObject.class);
            for(DBObject obj: lst)
            {
                Object adjusted =
                        adjustDates(obj, dateOffset, direction, transDir, db);
                Object o = constructor.newInstance(adjusted);
                res.add((AbstractAppNexusService) o);
            }
        }
        catch (InvocationTargetException e)
        {
            throw Utils.barf(e, this, prototype);
        }
        catch (IllegalAccessException e)
        {
            throw Utils.barf(e, this, prototype);
        }
        catch (InstantiationException e)
        {
            throw Utils.barf(e, this, prototype);
        }
        catch (NoSuchMethodException e)
        {
            throw Utils.barf(e, this, prototype);
        }
        return res;
    }

    static Date yyyymmddParse(String s)
    {
        try
        {
            return yyyyMMddDateFormat.parse(s);
        }
        catch (java.text.ParseException e)
        {
            return null;
        }
    }

    static Date dateParse(String s)
    {
        try
        {
            return dateTimeFormat.parse(s);
        }
        catch (java.text.ParseException e)
        {
            return null;
        }
    }

    static Date today()
    {
        return AppNexusUtils.dayFloor(new Date());
    }

    static Object maybeAdjustDate
            (String key, Object v, long dateOffset, AdjustDirection dir,
             TranslateDirection transDir, DB db)
    {
        if(dir == null) return adjustDates(v, dateOffset, dir, transDir, db);
        else if(DATE_KEYS.contains(key)) return adjustDate(v, dateOffset, dir);
        else if(db != null && v instanceof String &&
                ((String) v).startsWith(GRID_VALUE_COOKIE))
        {
            String vs = (String) v;
            String token = vs.substring(GRID_VALUE_COOKIE.length());
            return gridFSValue(GRID_BUCKET, token, db);
        }
        else return adjustDates(v, dateOffset, dir, transDir, db);
    }

    static String gridFSValue(String bucket, String token, DB db)
    {
        String theString;
        GridFS gfs = new GridFS(db, bucket);
        GridFSDBFile fileForReturn = gfs.findOne(token);
        InputStream inputStream = fileForReturn.getInputStream();
        StringWriter writer = new StringWriter();
        try
        {
            IOUtils.copy(inputStream, writer, "UTF-8");
        }
        catch (IOException e)
        {
            throw Utils.barf(e, bucket, token, db, inputStream);
        }
        theString = writer.toString();
        return theString;
    }

    static Object adjustDate(Object d, long dateOffset, AdjustDirection dir)
    {
        if(d instanceof Date) return adjustDate((Date) d, dateOffset, dir);
        else if(d instanceof String)
            return adjustDate((String) d, dateOffset, dir);
        else return d;
    }

    static Date adjustDate(Date d, long dateOffset, AdjustDirection dir)
    {
        Date res;
        switch(dir)
        {
            case FORWARDS:  res = new Date(d.getTime() + dateOffset); break;
            case BACKWARDS: res = new Date(d.getTime() - dateOffset); break;
            default: throw Utils.barf("Unknown direction", null, dir);
        }
        return res;
    }

    static String adjustDate(String s, long dateOffset, AdjustDirection dir)
    {
        if(s == null) return null;
        else
        {
            Date d = dateParse(s);
            if(d != null)
                return dateTimeFormat.format(adjustDate(d, dateOffset,dir));
            else
            {
                d = yyyymmddParse(s);
                if(d != null)
                    return yyyyMMddDateFormat.format
                            (adjustDate(d, dateOffset,dir));
                else throw Utils.barf("Not parsable as a date", null, s,
                                      dateOffset, dir);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Object adjustDates(Object obj, long dateOffset, AdjustDirection dir,
                              TranslateDirection transDir, DB db)
    {
        if(obj instanceof Map)
        {
            Map jo = (Map) obj;
            Map res;
            switch(transDir)
            {
                case JSON_TO_MONGO: res = new BasicDBObject(); break;
                case MONGO_TO_JSON: res = new JSONObject(); break;
                case IDENTITY:
                    if(obj instanceof JSONObject) res = new JSONObject();
                    else if(obj instanceof DBObject) res = new BasicDBObject();
                    else throw Utils.barf("Unhandled type", null, obj,
                                          dateOffset, dir, transDir);
                    break;
                default: throw Utils.barf("Unhandled transDir", null, obj,
                                          dateOffset, dir, transDir);
            }
            for(Object key: jo.keySet())
            {
                Object v = jo.get(key);
                if(key instanceof String)
                {
                    String k = (String) key;
                    v = maybeAdjustDate(k, v, dateOffset, dir, transDir, db);
                    res.put(k, v);
                }
            }
            return res;
        }
        else if(obj instanceof List)
        {
            List res;
            switch(transDir)
            {
                case JSON_TO_MONGO: res = new BasicDBList(); break;
                case MONGO_TO_JSON: res = new JSONArray(); break;
                case IDENTITY:
                    if(obj instanceof JSONObject) res = new JSONArray();
                    else if(obj instanceof DBObject) res = new BasicDBList();
                    else throw Utils.barf("Unhandled type", null, obj,
                                          dateOffset, dir, transDir);
                    break;
                default: throw Utils.barf("Unhandled transDir", null, obj,
                                          dateOffset, dir, transDir);
            }

            List ja = (List) obj;
            for(Object v: ja)
            {
                res.add(adjustDates(v, dateOffset, dir, transDir, db));
            }
            return res;
        }
        else return obj;
    }
}

enum TranslateDirection { MONGO_TO_JSON, JSON_TO_MONGO, IDENTITY }