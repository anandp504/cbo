package com.tumri.cbo.testing;

import com.mongodb.*;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.zini.Utils;
import java.net.UnknownHostException;

public class TestCore {

    static Identity appNexusSetup(String[] args)
    {
        Identity ident = AppNexusUtils.identityFromCommandLine(args);
        // AppNexusPersistenceDAO appNexusPersistence =
        //        AppNexusInterface.getAppNexusPersister();
        AppNexusInterface.enableDebugPrinting();
        return ident;
    }

    static Mongo ensureConnection(String host, int port)
    {
        try
        {
            return new Mongo(host, port);
        }
        catch (UnknownHostException e)
        {
            throw Utils.barf(e, null);
        }
    }

    static DB ensureDB(Mongo connection, String name)
    {
        DB db;
        db = connection.getDB(name);
        return db;
    }

    static String sanitiseMongoName(String name)
    {
        StringBuffer out = new StringBuffer();
        for(int i=0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if(c == '.' ||
               c == '@' ||
               c == '/' ||
               c == '\\' ||
               c == ':' ||
               c == '*' ||
               c == '?' ||
               c == '"' ||
               c == '<' ||
               c == '>' ||
               c == '|' ||
               c == ' ')
                out.append('_');
            else out.append(c);
        }
        return out.toString();
    }

    public static DB init(Identity ident, String mongoHost, int mongoPort)
    {
        String dbName;
        String appNexusHost = AppNexusInterface.getAppNexusApiHost(ident);
        String appNexusUser = AppNexusInterface.getAppNexusUserId(ident, true);
        dbName = sanitiseMongoName(appNexusHost + "_" + appNexusUser + "_db");
                // "local";
        Mongo connection = ensureConnection(mongoHost, mongoPort);
        DB db;
        db = ensureDB(connection, dbName);
        return db;
    }

    public static DB populate(DB db, Identity ident, Long advertiserId)
    {
        String collectionMangulation = "";
                // sanitiseMongoName(appNexusHost + "_" + appNexusUser + "_");
        ServicePersistenceFilter filter =
                new ServicePersistenceFilter(db, collectionMangulation);
        AppNexusReturnValue advertisers = AdvertiserService.viewAll
                (ident, AppNexusInterface.INTERVAL_VALUE_LIFETIME, null, null,
                 filter);
        System.out.println(advertisers);
        ServiceListMap<AdvertiserService> map =
                advertisers.coerceToServiceMap(AdvertiserService.class);
        AdvertiserService adv = map.get(advertiserId);
        System.out.println(adv);
        CampaignService.viewAll
               (ident, advertiserId, AppNexusInterface.INTERVAL_VALUE_LIFETIME,
                null, null, filter);
        return db;
    }

    public static void main(String[] args)
    {
        Long advertiserId = 15167L; // ResponseLogix
        String mongoHost = "localhost";
        int mongoPort = 27017;
        Identity ident = appNexusSetup(args);
        DB db = init(ident, mongoHost, mongoPort);
        populate(db, ident, advertiserId);
    }
}
