package com.tumri.cbo.backend;

import org.json.simple.JSONObject;

import java.util.Date;


public class Observation {

    public JSONObject campaignJSON;
    public JSONObject profileJSON;
    public Date observationTime;

    public Observation() {}

    @SuppressWarnings("unused")
    public Observation(JSONObject campaignJSON, JSONObject profileJSON,
                       Date observationTime)
    {
        this.campaignJSON = campaignJSON;
        this.profileJSON = profileJSON;
        this.observationTime = observationTime;
    }

    public JSONObject campaignObject()
    {
        if(campaignJSON == null) return null;
        else return (JSONObject)campaignJSON.get("campaign");
    }

    public JSONObject profileObject()
    {
        if(profileJSON == null) return null;
        else return (JSONObject)profileJSON.get("profile");
    }

}
