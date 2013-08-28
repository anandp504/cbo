package com.tumri.cbo.testing;

import com.mongodb.*;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.FetchReportService;
import com.tumri.mediabuying.zini.ProgressNoter;
import com.tumri.mediabuying.zini.Utils;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;


public class ServicePersistenceFilter extends AbstractServiceFilter {

    @SuppressWarnings("unused")
    private ServicePersistenceFilter()
    {
        super();
    }

    public ServicePersistenceFilter(DB db, String collectionMangulation)
    {
        super(db, collectionMangulation);
    }

    static List<String> boringKeys =
            Arrays.asList
                    ("status",
                     "dbg_info",
                     "execution_status");

    public AppNexusReturnValue filterServiceList(AppNexusReturnValue arg)
    {
        List<AbstractAppNexusService> list =
                arg.coerceToServiceList(AbstractAppNexusService.class);
        if(list.size() > 0)
        {
            AbstractAppNexusService elt = list.get(0);
            String collectionName =
                    collectionMangulation +
                            AppNexusUtils.afterDot(elt.getClass().getName());
            ProgressNoter noter =
                    new ProgressNoter("Persisting " + collectionName);
            DBCollection collection =
                    ensureCollection(collectionName, elt);
            for(AbstractAppNexusService obj: list)
            {
                Object converted =
                        toMongoObject(obj.serviceToJSONUnwrapped());
                if(converted instanceof DBObject)
                    collection.insert((DBObject) converted);
                else throw Utils.barf
                        ("Not a DBObject", this, obj, converted);
                noter.event();
            }
            noter.finish();
        }
        return arg;
    }

    public AppNexusReturnValue filterJSONObject(AppNexusReturnValue arg)
    {
        JSONObject jo = arg.getJSONObject();
        Object resp = jo.get("response");
        if(resp instanceof JSONObject)
        {
            JSONObject res = (JSONObject) resp;
            AbstractAppNexusService selected = null;
            for(Object key: res.keySet())
            {
                if(key instanceof String &&
                        !boringKeys.contains((String)key))
                {
                    String keys = (String) key;
                    Object val = res.get(key);
                    if(val instanceof JSONObject)
                    {
                        Object javo =
                                AbstractAppNexusService.appNexusObjectToJava
                                        (keys, (JSONObject) val, null);
                        if(javo instanceof AbstractAppNexusService)
                            selected = (AbstractAppNexusService) javo;
                        break;
                    }
                }
            }
            if(selected != null)
            {
                String collectionName =
                        collectionMangulation +
                                AppNexusUtils.afterDot
                                        (selected.getClass().getName());
                DBCollection collection =
                        ensureCollection(collectionName, selected);
                Object converted =
                        toMongoObject(selected.serviceToJSONUnwrapped());
                if(converted instanceof DBObject)
                {
                    DBObject convertedObj = (DBObject) converted;
                    if(arg.getUrl() != null)
                        convertedObj.put(QUERY_URL, arg.getUrl());
                    if(arg.getParameterBlock() != null)
                        convertedObj.put(QUERY_PARAMETER_BLOCK,
                                         arg.getParameterBlock());
                    List<PendingValue> pvs = null;
                    for(String key: convertedObj.keySet())
                    {
                        Object value = convertedObj.get(key);
                        if(value instanceof String)
                        {
                            String valueS = (String) value;
                            if(valueS.length() > MAX_STRING_LENGTH)
                            {
                                if(pvs == null)
                                    pvs = new Vector<PendingValue>();
                                pvs.add(new PendingValue(key, valueS));
                            }
                        }
                    }
                    if(pvs != null)
                    {
                        for(PendingValue pv: pvs)
                        {
                            String bucket = GRID_BUCKET;
                            ObjectId id = new ObjectId();
                            String newFileName = id.toString();
                            File localFile =
                                    new File("/tmp/" + newFileName + ".tmp");
                            FileWriter fos;
                            try
                            {
                                fos = new FileWriter(localFile);
                                fos.write(pv.value);
                                fos.close();
                                GridFS gfsString = new GridFS(db, bucket);
                                GridFSInputFile gfsFile =
                                        gfsString.createFile(localFile);
                                gfsFile.setFilename(newFileName);
                                gfsFile.save();
                                if(!localFile.delete())
                                    throw Utils.barf
                                            ("Failed to delete grid temp file.",
                                             this, id, localFile);
                            }
                            catch (IOException e)
                            {
                                throw Utils.barf(e, this, "Error emitting "
                                        + " a GridFS value for "
                                        + pv.key);
                            }
                            convertedObj.put
                                    (pv.key, GRID_VALUE_COOKIE + id.toString());
                        }
                    }
                    if(((DBObject)converted).keySet().size() > 0)
                        collection.insert((DBObject) converted);
                }
                else throw Utils.barf
                        ("Not a DBObject", this, collection, converted);
            }
        }
        return arg;
    }

    public AppNexusReturnValue filterStatus
            (AppNexusReturnValue arg, AppNexusThunk thunk)
    {
        AbstractAppNexusService prototype = thunk.getPrototype();
        String status = arg.getStatus();
        if(prototype instanceof FetchReportService)
        {
            String collectionName =
                    collectionMangulation +
                            AppNexusUtils.afterDot
                                    (prototype.getClass().getName());
            DBCollection collection =
                    ensureCollection(collectionName, prototype);
            DBObject dbObj = new BasicDBObject("status", status);
            if(arg.getUrl() != null)
                dbObj.put(QUERY_URL, arg.getUrl());
            if(arg.getParameterBlock() != null)
                dbObj.put(QUERY_PARAMETER_BLOCK,
                          arg.getParameterBlock());
            collection.insert(dbObj);
        }
        else System.out.println("????");
        return arg;
    }

    public AppNexusReturnValue filter
            (AppNexusReturnValue arg, AppNexusThunk thunk)
    {
        if(arg.isGeneralisedServiceList()) return filterServiceList(arg);
        else if(arg.isJSONObject()) return filterJSONObject(arg);
        else if(arg.isStatus()) return filterStatus(arg, thunk);
        else
        {
            System.out.println("????");
        }
        return arg;
    }

    public AppNexusReturnValue preFilter
            (AbstractAppNexusService prototype, String operationName,
             Identity ident, String[] argNames, Object... arguments)
    {
        return null;
    }
}

class PendingValue {
    String key;
    String value;

    public PendingValue(String key, String value)
    {
        this.key = key;
        this.value = value;
    }
}