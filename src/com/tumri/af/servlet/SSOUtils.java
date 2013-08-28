package com.tumri.af.servlet;

import java.net.SocketException;


public class SSOUtils {
    public static final String CONNECTION_RESET_MSG = "Connection reset";
    public static final String BROKEN_PIPE_MSG = "Broken pipe";

    public static boolean isBoringSocketException(SocketException se)
    {
        String dm = se.getMessage();
        return CONNECTION_RESET_MSG.equals(dm) ||
               BROKEN_PIPE_MSG.equals(dm);
    }

}
