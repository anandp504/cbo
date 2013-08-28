package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;

public class BidderControlHTTPHandler extends AbstractControlHTTPHandler {

    static final String URL_NAME = "BIDDERCONTROL";
    static final String PRETTYNAME = null;
    static final String FORM_NAME = "BidderControl";
    static final String CONTINUATION = StatusHTTPHandler.STATUS;
    static final String STYLE_SHEET_URL = null;

    public static final String PAUSED_PARAM = "Paused";
    public static final String DEBUG_MODE_PARAM = "Debug";
    public static final String TRACE_SQL_PARAM = "TraceSQL";
    public static final String MUFFLE_SQL_TRACE_PARAM = "MuffleSQLTrace";
    public static final String TENURE_ERRORS_PARAM = "TenureErrors";
    public static final String PRINT_APPNEXUS_DEBUGS = "PrintAppNexusDebugs";
    public static final String PRINT_APPNEXUS_JSON = "PrintAppNexusJSON";
    public static final String UPDATE_APPNEXUS = "UpdateAppNexus";
    public static final String FORCE_BID_UPDATING = "ForceBidUpdating";
    public static final String FETCH_HISTORICAL_DATA = "FetchHistoricalData";
    public static final String EFFECTUATE_BIDS = "EffectuateBids";
    public static final String APPNEXUS_READ_ONLY = "AppNexusReadOnly";
    public static final String NEW_CAMPIGN_BID_IMPOSITION_POLICY = "NewCampaignBidImpositionPolicy";

    public static ControlParameter[] bidderControlParams =
        {
         new ControlParameter(RadioBooleanWidget.WIDGET, "Paused:", PAUSED_PARAM, "isPaused", "setPaused"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Debug:", DEBUG_MODE_PARAM, "getDebugMode", "setDebugMode"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Trace SQL:", TRACE_SQL_PARAM, "getTraceSQL", "setTraceSQL"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Muffle SQL trace results:", MUFFLE_SQL_TRACE_PARAM, "getMuffleSQLTrace", "setMuffleSQLTrace"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Tenure errors:", TENURE_ERRORS_PARAM, "getTenureErrors", "setTenureErrors"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Print AppNexus debugs:", PRINT_APPNEXUS_DEBUGS, "getPrintAppNexusDebugs", "setPrintAppNexusDebugs"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Print AppNexus JSON:", PRINT_APPNEXUS_JSON, "getPrintAppNexusJSON", "setPrintAppNexusJSON"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Update AppNexus:", UPDATE_APPNEXUS, "getUpdateAppNexus", "setUpdateAppNexus"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Force Bid Updates:", FORCE_BID_UPDATING, "getForceBidUpdating", "setForceBidUpdating"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Fetch Historical Data:", FETCH_HISTORICAL_DATA, "getFetchHistoricalData", "setFetchHistoricalData"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "Effectuate Bids:", EFFECTUATE_BIDS, "getEffectuateBids", "setEffectuateBids"),
         new ControlParameter(RadioBooleanWidget.WIDGET, "AppNexus Read Only:", APPNEXUS_READ_ONLY, "getAppNexusReadOnly", "setAppNexusReadOnly"),
         new ControlParameter
                 (new EnumDropDownWidget(NewCampaignBidImpositionPolicy.class),
                  "New campaign bid imposition policy:",
                  NEW_CAMPIGN_BID_IMPOSITION_POLICY,
                  "getNewCampaignBidImpositionPolicy",
                  "setNewCampaignBidImpositionPolicy"),
         /*
         new ControlParameter(TextBoxWidget.WIDGET, "xxTest1:", "xxTest1", "xxTest1"),
         new ControlParameter(TextBoxNumberWidget.NUMBER_WIDGET, "xxTest2:", "xxTest2", "xxTest2"),
         new ControlParameter(TextBoxNumberWidget.LONG_WIDGET, "xxTest3:", "xxTest3", "xxTest3"),
         new ControlParameter(TextBoxNumberWidget.NON_NEGATIVE_LONG_WIDGET, "xxTest4:", "xxTest4", "xxTest4"),
         new ControlParameter(TextBoxNumberWidget.POSITIVE_LONG_WIDGET, "xxTest5:", "xxTest5", "xxTest5"),
         new ControlParameter(TextBoxNumberWidget.INTEGER_WIDGET, "xxTest6:", "xxTest6", "xxTest6"),
         new ControlParameter(TextBoxNumberWidget.NON_NEGATIVE_INTEGER_WIDGET, "xxTest7:", "xxTest7", "xxTest7"),
         new ControlParameter(TextBoxNumberWidget.POSITIVE_INTEGER_WIDGET, "xxTest8:", "xxTest8", "xxTest8"),
         new ControlParameter(TextBoxNumberWidget.DOUBLE_WIDGET, "xxTest9:", "xxTest9", "xxTest9"),
         new ControlParameter(TextBoxDateTimeWidget.DATE_TIME_WIDGET, "xxTest10:", "xxTest10", "xxTest10"),
         new ControlParameter(TextBoxDateTimeWidget.DATE_WIDGET, "xxTest11:", "xxTest11", "xxTest11"),
         new ControlParameter(TextBoxDateTimeWidget.TIME_WIDGET, "xxTest12:", "xxTest12", "xxTest12"),
         new ControlParameter(new DropDownWidget(new String[]{ "A", "B", "C", "D"}), "xxTest13:", "xxTest13", "xxTest13"),
         new ControlParameter(new MultiChoiceWidget(new String[]{ "A", "B", "C", "D"}), "xxTest14:", "xxTest14", "xxTest14"),
         */
         null
        };

    public boolean isAdminUserCommand() { return false; }
    
    @SuppressWarnings("unused")
    public BidderControlHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, URL_NAME, PRETTYNAME, FORM_NAME,
              CONTINUATION, bidder, bidderControlParams, STYLE_SHEET_URL);
    }

    static { HTTPListener.registerHandlerClass(StatusHTTPHandler.class); }
}

