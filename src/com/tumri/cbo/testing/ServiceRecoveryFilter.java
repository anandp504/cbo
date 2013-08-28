package com.tumri.cbo.testing;

import com.mongodb.*;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.Utils;
import org.json.simple.JSONObject;
import java.util.*;

public class ServiceRecoveryFilter extends AbstractServiceFilter{

    @SuppressWarnings("unused")
    private ServiceRecoveryFilter()
    {
        super();
    }

    @SuppressWarnings("unused")
    public ServiceRecoveryFilter(DB db)
    {
        super(db);
    }

    public ServiceRecoveryFilter(DB db, String collectionMangulation)
    {
        super(db, collectionMangulation);
    }

    public AppNexusReturnValue filter
            (AppNexusReturnValue arg, AppNexusThunk thunk)
    {
        return arg; // NoOp.
    }

    DBObject findOne
            (long dateOffset, DBCollection collection, Object... kvPairs)
    {
        DBObject query = new BasicDBObject();
        for(int i = 0; i < kvPairs.length; i = i + 2)
        {
            Object k = kvPairs[i];
            Object v = kvPairs[i + 1];
            if(k instanceof String)
                query.put((String) k, v);
        }
        DBObject res = collection.findOne(query);
        if(res == null) return null;
        else
        {
            for(String key: res.keySet())
            {
                Object v = res.get(key);
                res.put(key, maybeAdjustDate(key, v, dateOffset,
                                             AdjustDirection.FORWARDS,
                                             TranslateDirection.MONGO_TO_JSON,
                                             db));
            }
            return res;
        }
    }

    List<DBObject> findAllInternal
            (long dateOffset, DBCollection collection, DBObject query)
    {
        DBCursor cur = null;
        try
        {
            cur = collection.find(query);
            if(cur == null) return null;
            else
            {
                List<DBObject> resList = new Vector<DBObject>();
                for(DBObject res: cur)
                {
                    for(String key: res.keySet())
                    {
                        Object v = res.get(key);
                        Object adjusted =
                             maybeAdjustDate(key, v, dateOffset,
                                             AdjustDirection.FORWARDS,
                                             TranslateDirection.MONGO_TO_JSON,
                                             db);
                        res.put(key, adjusted);
                    }
                    resList.add(res);
                }
                return resList;
            }
        }
        finally
        {
            if(cur != null) cur.close();
        }
    }

    List<DBObject> findAll
            (long dateOffset, DBCollection collection, Object... kvPairs)
    {
        List<DBObject> res;
        DBObject query = new BasicDBObject();
        for(int i = 0; i < kvPairs.length; i = i + 2)
        {
            Object k = kvPairs[i];
            Object v = kvPairs[i + 1];
            if(k instanceof String)
                query.put((String) k, v);
        }
        res = findAllInternal(dateOffset, collection, query);
        return res;
    }

    static Object translateFilterValue(Object value)
    {
        if(value instanceof List)
        {
            DBObject res;
            res = new BasicDBObject("$in", value);
            return res;
        }
        else if(value instanceof Date)
            return dateTimeFormat.format((Date)value);
        else return value;
    }

    List<DBObject> findFiltered
            (long dateOffset, DBCollection collection,
             AbstractAppNexusServiceFilter filter)
    {
        List<DBObject> res;
        DBObject query = new BasicDBObject();
        Map<String, Object> literalMap = filter.getLiteralMap();
        Map<String, Object> maxMap = filter.getMaxMap();
        Map<String, Object> minMap = filter.getMinMap();
        for(String key: literalMap.keySet())
        {
            Object value = literalMap.get(key);
            query.put(key, translateFilterValue(value));
        }
        for(String key: minMap.keySet())
        {
            Object value = minMap.get(key);
            DBObject valObj =
                        new BasicDBObject("$gte", translateFilterValue(value));
            query.put(key, valObj);
        }
        for(String key: maxMap.keySet())
        {
            Object value = maxMap.get(key);
            DBObject valObj =
                        new BasicDBObject("$lte", translateFilterValue(value));
            query.put(key, valObj);
        }
        res = findAllInternal(dateOffset, collection, query);
        return res;
    }

    @SuppressWarnings("unchecked")
    public AppNexusReturnValue preFilter
            (AbstractAppNexusService prototype, String operationName,
             Identity ident, String[] argNames, Object... arguments)
    {
        long dateOffset = today().getTime() - BASE_DATE.getTime();
        String serviceName =
                AppNexusUtils.afterDot(prototype.getClass().getName());
        AppNexusReturnValue retVal;
        if(prototype instanceof FetchReportService &&
           "getReportById".equals(operationName) &&
           argNames.length == 3 &&
           "REPORT_TOKEN".equals(argNames[0]))
        {
            Object token = arguments[0];
            Object start = arguments[1];
            Object numElements = arguments[2];
            if(token instanceof String)
            {
                if(start == null) start = 0;
                if(numElements == null) numElements = 100;
                String queryUrl = "/report?id=" + token + "&start_element=" +
                        start + "&num_elements=" + numElements;
                DBCollection collection =
                        ensureCollection("ReportService", prototype);
                List<DBObject> res = findAll(dateOffset, collection,
                                       QUERY_URL, queryUrl);
                DBObject selected = null;
                for(DBObject o: res)
                {
                    if(o.get("data") != null)
                    {
                        selected = o;
                        break;
                    }
                }
                Object jo =
                       (selected == null ? null : mongoToJSONObject(selected));
                AppNexusReturnValue resVal;
                if(selected == null)
                    resVal = AppNexusReturnValue.val(null, "No URL", null);
                else
                {
                    JSONObject wrapper = new JSONObject();
                    JSONObject innerWrapper = new JSONObject();
                    innerWrapper.put("report", jo);
                    wrapper.put("response", innerWrapper);
                    resVal = AppNexusReturnValue.val(wrapper, queryUrl, null);
                    resVal.setAllocatedTicket(Ticket.VACUOUS_TICKET);
                }
                return resVal;
            }
            else throw Utils.barf("Token is not a String", this, token,
                                  start, numElements);
        }
        else if(prototype instanceof FetchReportService &&
                argNames.length == 1 &&
                "parameterBlock".equals(argNames[0]))
        {
            Object params = arguments[0];
            if(params instanceof JSONObject)
            {
                JSONObject jo = (JSONObject) params;
                Object mongoArg = adjustDates(jo, dateOffset,
                                    AdjustDirection.BACKWARDS,
                                    TranslateDirection.JSON_TO_MONGO, db);
                DBCollection collection =
                        ensureCollection(serviceName, prototype);
                DBObject res = findOne(dateOffset, collection,
                                       QUERY_PARAMETER_BLOCK, mongoArg);
                AppNexusReturnValue resVal;
                if(res == null)
                    resVal = AppNexusReturnValue.val(null, "No URL", null);
                else
                {
                    resVal = AppNexusReturnValue.val
                                           (res.get("status"), "No URL", null);
                    resVal.setAllocatedTicket(Ticket.VACUOUS_TICKET);
                }
                return resVal;
            }
            else throw Utils.barf("Not a JSONObject", this, params,
                                  arguments[0]);
        }
        else if((prototype instanceof ProfileService && argNames.length > 0) ||
                (prototype instanceof LineItemService && argNames.length > 0))
        {
            if("viewAll".equals(operationName))
            {
                Long advertiserId =
                        getArgLong("ADVERTISER_ID", argNames, arguments);
                DBCollection collection =
                        ensureCollection(serviceName, prototype);
                List<DBObject> res = findAll(dateOffset, collection,
                                             "advertiser_id", advertiserId);
                Object adjusted =
                        toServiceList(res, prototype, dateOffset, null,
                                      TranslateDirection.MONGO_TO_JSON);
                retVal = AppNexusReturnValue.val(adjusted, "No URL", null);
                return retVal;
            }
            else return null;
        }
        else if((prototype instanceof AdvertiserService && argNames.length > 0) ||
                (prototype instanceof CampaignService && argNames.length > 0))
        {
            if("viewFiltered".equals(operationName))
            {
                AbstractAppNexusServiceFilter filter =
                        (AbstractAppNexusServiceFilter)
                                getArg("filter", argNames, arguments);
                DBCollection collection =
                        ensureCollection(serviceName, prototype);
                List<DBObject> res = findFiltered(dateOffset, collection,
                                                  filter);
                Object converted =
                        toServiceList(res, prototype, dateOffset, null,
                                      TranslateDirection.MONGO_TO_JSON);
                retVal = AppNexusReturnValue.val(converted, "No URL", null);
                return retVal;
            }
            else return null;
        }
        else return null;
    }


}

enum AdjustDirection { FORWARDS, BACKWARDS }