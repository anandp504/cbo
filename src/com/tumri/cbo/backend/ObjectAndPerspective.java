package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.Perspective;

public class ObjectAndPerspective {
    Object object = null;
    Perspective perspective = null;
    String iconURL = null;
    String explicitURL = null;

    public ObjectAndPerspective(Object object, Perspective perspective)
    {
        this.object = object;
        this.perspective = perspective;
        this.explicitURL = null;
        this.iconURL = null;
    }

    public ObjectAndPerspective(String explicitURL, String iconURL)
    {
        this.object = null;
        this.perspective = null;
        this.explicitURL = explicitURL;
        this.iconURL = iconURL;
    }
}

