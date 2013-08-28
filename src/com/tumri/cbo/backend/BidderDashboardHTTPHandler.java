package com.tumri.cbo.backend;

import com.tumri.cbo.monitor.Messages;
import com.tumri.cbo.monitor.ShowMessagesHTTPHandler;
import com.tumri.mediabuying.zini.*;
import org.apache.log4j.Level;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;

public class BidderDashboardHTTPHandler extends BidderGrapherHTTPHandler {

    static final String DAILY_IMPRESSIONS_TARGET = 
            BidderInstruction.DAILY_IMPRESSIONS_TARGET;
    static final String DAILY_IMPRESSIONS_LIMIT =
            BidderInstruction.DAILY_IMPRESSIONS_LIMIT;
    // static final String MAX_BID = BidderInstruction.MAX_BID;
    static final String CURRENT_OR_MAX_BID = BidderInstruction.CURRENT_OR_MAX_BID;
    static final String ADVERTISER_NAME = BidderInstruction.ADVERTISER_NAME;
    static final String CAMPAIGN_NAME   = BidderInstruction.CAMPAIGN_NAME;
    static final String BIDDING_POLICY  = BidderInstruction.BIDDING_POLICY;
    public static final String BIDDING_POLICY_PARAM = BIDDING_POLICY;
    static int DEFAULT_TEXT_BOX_WIDTH = 10;
    static final String  ASCENDING = "Ascending";
    static final String DESCENDING = "Descending";
    static final String SAVE_BIDDER_DATA = "Save bidder dashboard data";

     public static ExcelFileSchema dashboardSchema =
         new ExcelFileSchema("Dashboard Schema", new String[][]
         {
           // Pretty Name      Slot Name                       Constraint                      Legal Values Method            Menu Values Method            CellStyle
           { "Adv Name",       "advertiserName",               BidderInstruction.READ_ONLY,    null,                          null,                         null},
           { "Adv Id",         "advertiserId",                 BidderInstruction.READ_ONLY,    null,                          null,                         null},
           // { "L/I Name",    "lineItemName",                 BidderInstruction.READ_ONLY,    null,                          null,                         null},
           // { "L/I Id",      "lineItemId",                   BidderInstruction.READ_ONLY,    null,                          null,                         null},
           { "Camp Name",      "campaignName",                 BidderInstruction.READ_ONLY,    null,                          null,                         null},
           { "Camp Id",        "campaignId",                   BidderInstruction.READ_ONLY,    null,                          null,                         null},
           { "Starts",         "startDate",                    BidderInstruction.READ_ONLY,    null,                          null,                         "dateOnly"},
           { "Ends",           "endDate",                      BidderInstruction.READ_ONLY,    null,                          null,                         "dateOnly"},
           { "Daily Imp Lmt",  DAILY_IMPRESSIONS_LIMIT,        BidderInstruction.NON_NEGATIVE, null,                          null,                         "numberWithCommas"},
           { "Max Bid",        CURRENT_OR_MAX_BID,             BidderInstruction.NON_NEGATIVE, null,                          null,                         "dollarCurrency"},
           { "Curr Bid",       "currentBid",                   BidderInstruction.READ_ONLY,    null,                          null,                         "dollarCurrency"},
           { "Bidding Policy", BIDDING_POLICY,                 BidderInstruction.LEGAL_VALUES, "getBiddingPolicyLegalValues", "biddingPolicyMenuValues",    null},
           { "Life Imp Tgt",   "lifetimeImpressionsTarget",    BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           { "Imps to date",   "lifetimeImpressionsServed",    BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           // { "Life Pacing", "pacing",                       BidderInstruction.READ_ONLY,    null,                          null,                         "getPacingCellStyle"},
           { "Projected Imps", "lifetimeProjectedImpressions", BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           { "Projected %age", "lifetimePacingPercentage",     BidderInstruction.READ_ONLY,    null,                          null,                         "getLifetimePacingCellStyleWithColour"},
           { "L/B",            "lifetimePacingLookback",       BidderInstruction.READ_ONLY,    null,                          null,                         "getLifetimePacingLookbackCellStyle"},
           { "Daily Imp Tgt",  DAILY_IMPRESSIONS_TARGET,       BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           { "Imps Ystd",      "yesterdayImpressionsServed",   BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           { "Daily Pacing",   "dailyPacing",                  BidderInstruction.READ_ONLY,    null,                          null,                         "getDailyPacingCellStyleWithColour"},
           { "Imps Today",     "dailyImpressionsServed",       BidderInstruction.READ_ONLY,    null,                          null,                         "numberWithCommas"},
           { "Entropy",        "yesterdayEntropy",             BidderInstruction.READ_ONLY,    null,                          null,                         "getYesterdayEntropyStyle"}
           // { "Sugg Bid",    "suggestedBid",                 BidderInstruction.READ_ONLY,    null,                          null,                         "dollarCurrency"},
           // { "Bid Reason",  "bidReason",                    BidderInstruction.READ_ONLY,    null,                          null,                         "getBidReasonCellStyle"},
         },
         "com.tumri.cbo.backend.CampaignData",
         // BidderDashboardHTTPHandler.class
         DashboardInstruction.class);

    static final String slotCookie = "SLOT_";

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "BIDDERDASHBOARD";
    static String prettyName = "Bidder Dashboard";
    static String NON_NEGATIVE_CHECKER = " onChange=\"{ return checkNumberP(this, true, true, true, 0, null, null, false, null); }\"";
    DBValueExtractor extractor = null;

    @SuppressWarnings("unused")
    public BidderDashboardHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public BidderDashboardHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
        extractor = new DBValueExtractor(bidder);
    }

    static int emitColumnHeaders
            (Writer stream, int rowIndex, ExcelFileSchema schema,
             String sortColumnName, String sortDirection)
        throws IOException
    {
        stream.append("\n  <TR>");
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec != null)
            {
                stream.append("\n    <TH nowrap=\"nowrap\">");
                String direction =
                        (spec.getSlotName().equals(sortColumnName)
                                ? (ASCENDING.equals(sortDirection)
                                      ? DESCENDING
                                      : ASCENDING)
                                : ASCENDING);
                String anchorString =
                        "<A HREF=\"javascript:void(0)\" onClick=\"" +
                         "{ var form = window.document." + DEFAULT_FORM_NAME +
                                "; "+
                           "form." + SORT_COLUMN_PARAM + ".value = '" +
                                     spec.getSlotName() + "'; " +
                           "form." + SORT_DIRECTION_PARAM + ".value = '" +
                                     direction + "'; " +
                           "form.submit(); }\">";
                stream.append(anchorString);
                stream.append(HTTPHandler.htmlify(spec.getLabel()));
                stream.append("</A></TH>");
            }
        }
        stream.append("\n  </TR>");
        return rowIndex + 1;
    }

    public static NumberFormat makePercentageFormat()
    {
        NumberFormat nbFmt = NumberFormat.getPercentInstance();
        nbFmt.setMaximumFractionDigits(1);
        return nbFmt;
    }

    public static NumberFormat makeTwoDecminalsFormat()
    {
        NumberFormat nbFmt = NumberFormat.getInstance();
        nbFmt.setMaximumFractionDigits(2);
        return nbFmt;
    }

    static String makeParamName(ExcelSlotSchema spec, Long advertiserId,
                                Long campaignId)
    {
        return slotCookie + spec.getSlotName() +
                "_" + advertiserId.toString() +
                "_" + campaignId.toString();
    }

    // Ultimately, this should be more declarative!
    @SuppressWarnings("unused")
    static boolean checkIsEditable
            (ExcelSlotSchema spec, CampaignData campaignData)
    {
        if(DAILY_IMPRESSIONS_LIMIT.equals(spec.getSlotName()))
            return true;
                // campaignData.getBiddingPolicyObj() ==
                //        DailyImpressionsBidStrategy.STRATEGY;
        else if(CURRENT_OR_MAX_BID.equals(spec.getSlotName()))
            return true;
        /*
                   !(campaignData.getBiddingPolicyObj() ==
                     NoOptimizationBidStrategy.STRATEGY ||
                     campaignData.getBiddingPolicyObj() ==
                     NotSelectedBidStrategy.STRATEGY);
                     */
        else
            return true;
    }

    static String onChangeScript
         (ExcelSlotSchema spec, CampaignData campaignData, Object[] menuValues)
    {
        if(BIDDING_POLICY.equals(spec.getSlotName()))
        {
            String res = " onChange=\"{ ";
            String[] targets = { CURRENT_OR_MAX_BID, DAILY_IMPRESSIONS_LIMIT };
            for(String target: targets)
            {
                res = res + "var tgt_" + target + " = form." +
                        slotCookie + target + "_" +
                        campaignData.getAdvertiserId() + "_" +
                        campaignData.getCampaignId() +
                        "; var val = form." + slotCookie + BIDDING_POLICY +
                        "_" + campaignData.getAdvertiserId() + "_" +
                        campaignData.getCampaignId() +
                        ".value; tgt_" + target + ".disabled = !(";
                int i = 0;
                boolean emitted = false;
                for(Object mv: menuValues)
                {
                    String mvString = mv.toString();
                    if(mvString.equals
                       (NoOptimizationBidStrategy.STRATEGY.getPrimaryName()) ||
                       mvString.equals
                            (NotSelectedBidStrategy.STRATEGY.getPrimaryName()) ||
                       (mvString.equals
                            (DailyImpressionsBidStrategyWithCapChange.STRATEGY.getPrimaryName()) &&
                        DAILY_IMPRESSIONS_LIMIT.equals(target)))
                    {}
                    else
                    {
                        if(!emitted) emitted = true;
                        else res = res + " || ";
                        res = res + "val == " + i;
                    }
                    i = i + 1;
                }
                res = res + "); ";
            }
            res = res + "return true; }\"";
            return res;
        }
        else return "";
    }

    // Ultimately, this should be more declarative!
    @SuppressWarnings("SimplifiableIfStatement")
    static boolean checkIsDisabled
            (ExcelSlotSchema spec, CampaignData campaignData)
    {
        if(CURRENT_OR_MAX_BID.equals(spec.getSlotName()))
            return (campaignData.getBiddingPolicyObj() ==
                    NoOptimizationBidStrategy.STRATEGY ||
                    campaignData.getBiddingPolicyObj() ==
                    NotSelectedBidStrategy.STRATEGY);
        else if(DAILY_IMPRESSIONS_LIMIT.equals(spec.getSlotName()))
            return (campaignData.getBiddingPolicyObj() ==
                    DailyImpressionsBidStrategyWithCapChange.STRATEGY ||
                    campaignData.getBiddingPolicyObj() ==
                    NotSelectedBidStrategy.STRATEGY||
                    campaignData.getBiddingPolicyObj() ==
                    NotSelectedBidStrategy.STRATEGY);
        else return false;
    }

    // Ultimately, this should be more declarative!
    static List<ObjectAndPerspective> slotValueAnchor
          (ExcelSlotSchema spec, AdvertiserData ad, CampaignData campaignData)
    {
        List<ObjectAndPerspective> res = new Vector<ObjectAndPerspective>();
        if(ADVERTISER_NAME.equals(spec.getSlotName()))
            res.add(new ObjectAndPerspective
                      (ad, AdvertiserDataChangesPerspective.PERSPECTIVE));
        else if(CAMPAIGN_NAME.equals(spec.getSlotName()))
        {
            res.add(new ObjectAndPerspective
                      (campaignData,
                       CampaignDataHistoryPerspective.PERSPECTIVE));

            String chartUrl = BidHistoryHTTPHandler.makeURL(campaignData);
            String chartIconUrl = "/cbo/assets/chart-icon3.gif";
            res.add(new ObjectAndPerspective(chartUrl, chartIconUrl));
            Messages messages = Messages.getLastMessages();
            if(messages != null)
            {
                Map<Object, String> key = messages.getKey();
                if(key.get(campaignData.getCampaignId()) != null)
                {
                    String messagesUrl =
                            ShowMessagesHTTPHandler.makeURL(campaignData);
                    String messagesIconUrl = "/cbo/assets/alert-icon0.gif";
                    res.add(new ObjectAndPerspective
                                    (messagesUrl, messagesIconUrl));
                }
            }
        }
        else return null;
        return res;
    }

    static ExcelSlotSchema slotSpecFromName(String name, ExcelFileSchema schema)
    {
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec.getSlotName().equals(name)) return spec;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    int dumpCampaignDataToHTML(AdvertiserData advertiserData,
                               CampaignData campaignData,
                               Writer stream, int rowIndex, QueryContext qctx,
                               MethodMapper methodMapper,
                               ExcelFileSchema schema, boolean editable,
                               boolean admin, String sortColumnName,
                               String sortDirection)
            throws IOException
    {
        methodMapper.assertInitialised();
        if(rowIndex == 0)
        {
            stream.append("<TABLE BORDER=\"1\">");
            emitColumnHeaders(stream, 0, schema, sortColumnName,
                              sortDirection);
        }
        int colIndex = 0;
        if(rowIndex % 2 == 0) stream.append("\n  <TR>");
        else stream.append("\n  <TR BGCOLOR=\"#E0E0E0\">");
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec != null)
            {
                Object value = methodMapper.getValue
                        (campaignData, spec.getSlotName(), qctx);
                CellStyleName styleName = spec.getStyleName();
                Object styleNameObj =
                        methodMapper.getStyleNameObj
                          (campaignData, styleName, spec.getStyleNameMethod(),
                           qctx);
                ExcelConstraintType constraint = spec.getConstraint();
                String bgColor = null;
                String widgetString = null;
                String tdPart = "";
                DecimalFormat thousandsFormat = new DecimalFormat("#,##0");
                DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
                NumberFormat percentFormat = makePercentageFormat();
                String start = "";
                String end = "";
                TimeZone tz;
                boolean editableInThisCase =
                        checkIsEditable(spec, campaignData);
                boolean disabledInThisCase =
                        checkIsDisabled(spec, campaignData);
                tz = campaignData.getTimeZone();
                if(editable && editableInThisCase &&
                   constraint == ExcelConstraintType.NON_NEGATIVE)
                {
                    tdPart = tdPart + " align=\"right\"";
                    widgetString =
                        "\n  " +
                        (styleName == CellStyleName.dollarCurrency ? "$" : "")+
                        "<INPUT TYPE=\"TEXT\" STYLE=\"text-align: right\"" +
                              " NAME=\"" +
                        makeParamName(spec, campaignData.getAdvertiserId(),
                                      campaignData.getCampaignId()) +
                        "\" VALUE=\"" +
                        (value == null ? "" : escapeHtml(value.toString())) +
                        "\"" + NON_NEGATIVE_CHECKER + " SIZE=\"" +
                        DEFAULT_TEXT_BOX_WIDTH + "\"" +
                        (disabledInThisCase ? " DISABLED" : "") +">";
                }
                else if(editable && editableInThisCase &&
                        constraint == ExcelConstraintType.LEGAL_VALUES)
                {
                    // Object[] menuValues = spec.menuValuesFor(schema);
                    Object[] menuValues = 
                            schema.getExcelInstruction().menuValuesFor
                                    (spec.getSlotName(), schema);
                    // Object[] menuValues =
                       //      spec.invokeMenuValuesFor
                          //        (methodMapper, schema.getExcelInstruction());
                    String onChange =
                            onChangeScript(spec, campaignData, menuValues);
                    widgetString =
                        "\n  <SELECT NAME=\"" +
                            makeParamName(spec, campaignData.getAdvertiserId(),
                                          campaignData.getCampaignId()) + "\""
                            // Changes to bidding policy can affect the
                            // other widgets for this campaign.
                            // However, this change must be saved for
                            // things to take effect.
                            // + "\" onChange=\"{ form.submit(); }\">";
                            + onChange + ">";
                    if(menuValues != null)
                    {
                        int i = 0;
                        for(Object mv: menuValues)
                        {
                            String mvString = mv.toString();
                            widgetString = widgetString +
                               "\n    <OPTION VALUE=\"" + Integer.toString(i) +
                               "\"" +
                               (value != null && value.equals(mv)
                                       ? " SELECTED"
                                       : "") +
                               ">" + escapeHtml(mvString);
                            i = i + 1;
                        }
                    }
                    widgetString = widgetString + "\n  </SELECT>";
                }
                else if(styleNameObj == null) {}
                else
                {
                    List styles;
                    if(styleNameObj instanceof List)
                        styles = (List)styleNameObj;
                    else
                    {
                        styles = new Vector();
                        styles.add(styleNameObj);
                    }
                    for(Object style: styles)
                    {
                        if(style instanceof CellStyleName)
                        {
                            styleName = (CellStyleName) style;
                            switch(styleName)
                            {
                                case dateOnly:
                                    if(value instanceof Date)
                                    {
                                        SimpleDateFormat dateOnlyFormat =
                                                new SimpleDateFormat
                                                        ("yyyy-MM-dd");
                                        Calendar cal =
                                                Calendar.getInstance(tz);
                                        dateOnlyFormat.setCalendar(cal);
                                        value = dateOnlyFormat.format(value);
                                    }
                                    break;
                                case dateAndTime:
                                    if(value instanceof Date)
                                    {
                                        SimpleDateFormat dateOnlyFormat =
                                                new SimpleDateFormat
                                                      ("yyyy-MM-dd HH:mm:ss");
                                        Calendar cal =
                                                Calendar.getInstance(tz);
                                        dateOnlyFormat.setCalendar(cal);
                                        value = dateOnlyFormat.format(value);
                                    }
                                    break;
                                case boldText:
                                    start = start + "<B>";
                                    end = end + "</B>";
                                    break;
                                case numberWithCommas:
                                    if(value instanceof Number)
                                    {
                                        value = thousandsFormat.format(value);
                                        tdPart = tdPart + " align=\"right\"";
                                    }
                                    break;
                                case dollarCurrency:
                                    if(value instanceof Number)
                                    {
                                        value = "$" +
                                                currencyFormat.format(value);
                                        tdPart = tdPart + " align=\"right\"";
                                    }
                                    break;
                                case percentage:
                                    if(value instanceof Number)
                                    {
                                        value = percentFormat.format(value);
                                        tdPart = tdPart + " align=\"right\"";
                                    }
                                    break;
                                case redBackground:
                                    bgColor =
                                        (bgColor == null ? "ED1C24" : bgColor);
                                    break;
                                case orangeBackground:
                                    bgColor =
                                        (bgColor == null ? "F7941D" : bgColor);
                                    break;
                                case greenBackground:
                                    bgColor =
                                        (bgColor == null ? "00A651" : bgColor);
                                    break;
                                default:
                            }
                        }
                        else if(style instanceof String)
                        {
                            bgColor = (String) style;
                        }
                        else {} // Nothing to do.
                    }
                }
                stream.append("\n  <TD");
                if(bgColor != null)
                {
                    stream.append(" BGCOLOR=\"#");
                    stream.append(bgColor);
                    stream.append("\"");
                }
                if(tdPart != null) stream.append(tdPart);
                stream.append(" nowrap=\"nowrap\">");
                stream.append(start);

                if(widgetString != null) stream.append(widgetString);
                else if(value == null) stream.append("&nbsp;");
                else
                {
                    StringWriter sw = new StringWriter();
                    if(value instanceof Float ||
                            value instanceof Double)
                        sw.append(makeTwoDecminalsFormat().format(value));
                    else sw.append(value.toString());
                    List<ObjectAndPerspective> objAndPs =
                           slotValueAnchor(spec, advertiserData, campaignData);
                    if(objAndPs == null)
                        stream.append(HTTPHandler.htmlify(sw.toString()));
                    else
                    {
                        boolean first = true;
                        for(ObjectAndPerspective objAndP: objAndPs)
                        {
                            if(first) first = false;
                            else stream.append("&nbsp;&nbsp;");
                            if(objAndP.iconURL == null)
                            {
                                if(objAndP.explicitURL == null)
                                    stream.append
                                    (HTMLifier.anchorIfReasonable
                                     (objAndP.object, sw.toString(),
                                      "../" + InspectHTTPHandler.urlName + "/",
                                      null, objAndP.perspective, admin, null));
                                else
                                {
                                    stream.append("<A HREF=\"");
                                    stream.append(objAndP.explicitURL);
                                    stream.append("\">");
                                    stream.append(HTTPHandler.htmlify(sw.toString()));
                                    stream.append("</A>");
                                }
                            }
                            else
                            {
                                if(objAndP.explicitURL == null)
                                    throw Utils.barf
                                            ("Explicit URL not supplied.",
                                                    this, objAndP);
                                else
                                {
                                    stream.append("<A HREF=\"");
                                    stream.append(objAndP.explicitURL);
                                    stream.append("\"><IMG SRC=\"");
                                    stream.append(objAndP.iconURL);
                                    stream.append("\"></A>");
                                }
                            }
                        }
                    }
                }
                stream.append(end);
                stream.append("</TD>");
                colIndex = colIndex + 1;
            }
        }
        stream.append("\n  </TR>");
        return rowIndex + 1;
    }

    void collectAdvertiserBidData(AdvertiserData advertiser,
                                  Sexpression currentBidPolicy,
                                  List<AdvCampDataPair> campaignsToShow)
            throws IOException
    {
        List<CampaignData> campaigns =
                new ArrayList<CampaignData>(advertiser.campaignData);
        Collections.sort(campaigns, CampaignData.nameComparator);
        for(CampaignData campaignData: campaigns)
        {
            if(campaignData != null &&
               (currentBidPolicy == Null.nil ||
                currentBidPolicy.unboxString().equals
                        (campaignData.getBiddingPolicy()) ||
                (NotSelectedBidStrategy.NOT_SELECTED.equals(campaignData.getBiddingPolicy()) &&
                 (currentBidPolicy.unboxString().equals(NoOptimizationBidStrategy.NO_OPTIMIZATION) ||
                  currentBidPolicy.unboxString().equals(NoOptimizationBidStrategy.NO_OPTIMIZATION_SECONDARY)))))
            {
                extractor.setCampaignId(campaignData.campaignId);
                campaignsToShow.add(new AdvCampDataPair(advertiser, campaignData));
            }
        }
    }

    @SuppressWarnings("EmptyCatchBlock")
    void processCampaignUpdates
            (StringBuffer sb, CampaignData campaignData, QueryContext qctx,
             MethodMapper methodMapper, ExcelFileSchema schema,
             Map<String, String> httpParams)
    {
        methodMapper.assertInitialised();
        int colIndex = 0;
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec != null)
            {
                ExcelConstraintType constraint = spec.getConstraint();
                Object currentVal = null;
                Object newVal = null;
                String slotName = spec.getSlotName();
                if(constraint == ExcelConstraintType.NON_NEGATIVE)
                {
                    String val = httpParams.get
                           (makeParamName(spec, campaignData.getAdvertiserId(),
                                          campaignData.getCampaignId()));
                    if(val != null)
                    {
                        List<Object> possibilities = new Vector<Object>();
                        try
                        {
                            Long l = Long.parseLong(unescapeHtml(val));
                            possibilities.add(l);
                        }
                        catch (NumberFormatException e) {}
                        try
                        {
                            Double d = Double.parseDouble(unescapeHtml(val));
                            possibilities.add(d);
                        }
                        catch (NumberFormatException e) {}
                        currentVal = methodMapper.getValue
                                (campaignData, slotName, qctx);
                        methodMapper.setValue(campaignData, slotName,
                                              qctx, possibilities);
                        newVal = methodMapper.getValue
                                      (campaignData, slotName, qctx);
                    }
                }
                else if(constraint == ExcelConstraintType.LEGAL_VALUES)
                {
                    Object[] menuValues = spec.menuValuesFor(schema);
                    String val = httpParams.get
                           (makeParamName(spec, campaignData.getAdvertiserId(),
                                          campaignData.getCampaignId()));
                    if(menuValues != null && val != null)
                    {
                        int index = Integer.parseInt(val);
                        currentVal = methodMapper.getValue
                                (campaignData, slotName, qctx);
                        methodMapper.setValue(campaignData, slotName,
                                              qctx, menuValues[index]);
                        newVal = methodMapper.getValue
                                      (campaignData, slotName, qctx);
                    }
                }
                else {}
                if((currentVal != null && !currentVal.equals(newVal)) ||
                   (currentVal == null && newVal != null))
                {
                    // Then this campaignData has actually been updated, so
                    // stash the updates.  These may have to be applied to
                    // any equivalent campaignData that's being processed
                    // in parallel by the bidder.
                    campaignData.stashUpdate(methodMapper, slotName,
                                             qctx, newVal);
                    String str = "\n    " + campaignData.getIdPair() +
                                 "." + slotName +
                                 " = '" + newVal + "' was '" +
                                 currentVal + "'";
                    sb.append(str);
                }
                colIndex = colIndex + 1;
            }
        }
    }

    void processUpdates(StringBuffer sb, AdvertiserData advertiser,
                        QueryContext qctx, MethodMapper methodMapper,
                        ExcelFileSchema schema, Map<String, String> httpParams)
    {
        List<CampaignData> campaigns =
                new ArrayList<CampaignData>(advertiser.campaignData);
        // Collections.sort(campaigns, CampaignData.nameComparator);
        for(CampaignData campaignData: campaigns)
        {
            if(campaignData != null)
            {
                extractor.setCampaignId(campaignData.campaignId);
                processCampaignUpdates
                    (sb, campaignData, qctx, methodMapper, schema, httpParams);
            }
        }
    }

    public static Sexpression outputBidPolicyMenu
            (Writer stream, Map<String, String> httpParams, String param)
            throws IOException
    {
        Sexpression options =
                Sexpression.boxArray
                        (BidderInstruction.getBiddingPolicyLegalValues
                                (true, true));
        return outputMenu(stream, options, httpParams, "Bidding policy:",
                          param, true, false, true, "Any", false, null);
    }

    static Comparator<AdvCampDataPair> getComparator
            (ExcelSlotSchema sortSlot, String sortDirection,
             MethodMapper methodMapper, QueryContext qctx)
    {
        return new SlotComparator(sortSlot, sortDirection, methodMapper, qctx);
    }


    public boolean isAdminUserCommand() { return false; }

    @SuppressWarnings("unchecked")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        String perpetrator = getCurrentUser(httpParams);
        ExcelFileSchema schema = dashboardSchema;
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = new BasicQueryContext(null, bidder.appNexusTheory);
        MethodMapper methodMapper = schema.getMethodMapper();
        bidder.ensureLastReportTime(qctx);
        String stylesheetUrl = null;
        int rowIndex;
        Map<String, String> headers;
        List<String> extraHeaders = new Vector<String>();
        extraHeaders.add("\n<SCRIPT TYPE=\"text/javascript\" " +
                         "SRC=\"/cbo/assets/form-verification.js\"></SCRIPT>");
        Bidder bidder = ensureBidder();
        boolean saveP = httpParams.get(SAVE_PARAM) != null;
        boolean downloadP = httpParams.get(DOWNLOAD_PARAM) != null;
        String outputPath = bidder.getOutputSpreadsheetPath();
        Map<Long, AdvertiserData> advertiserDataMap =
                bidder.getAdvertiserMap();
        boolean editable = outputPath != null;
        String sortColumnName = httpParams.get(SORT_COLUMN_PARAM);
        String sortDirection  = httpParams.get(SORT_DIRECTION_PARAM);
        if(downloadP && advertiserDataMap != null)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bidder.downloadSpreadsheetForSchema
                (baos, advertiserDataMap, qctx, dashboardSchema);
            baos.flush();
            baos.close();
            byte[] ba = baos.toByteArray();
            headers = HTTPListener.emitStandardHeaders
                            (stream, 200, HTTPListener.application_xls,
                             (long) ba.length, null, false, returnHeaders);
            stream.flush();
            os.write(ba);
            os.flush();
        }
        else if(advertiserDataMap != null)
        {
            headers = handlerPageSetup
                    (stream, "Dashboard", null, stylesheetUrl,
                     extraHeaders, returnHeaders, prettyName, httpParams);
            List<AdvertiserData> advertisers =
                    new ArrayList(advertiserDataMap.values());
            Sexpression advertisersSexp = getAdvertisers(connector, qctx);
            Sexpression currentAdvertiser = Null.nil;
            Sexpression currentBidPolicy = Null.nil;
            stream.append("\n<FORM METHOD=\"POST\" NAME=\"" + DEFAULT_FORM_NAME
                          + "\" ACTION=\"");
            stream.append(urlName);
            stream.append("?\">");
            stream.append("\n<INPUT TYPE=\"HIDDEN\" NAME=\"Format\" VALUE=\"");
            stream.append(Integer.toString
                            (HTTPHandler.formatIndex(HTTPHandler.XLS)));
            stream.append("\">");
            stream.append("\n  <INPUT TYPE=\"HIDDEN\" NAME=\"");
            stream.append(SORT_COLUMN_PARAM);
            stream.append("\" VALUE=\"");
            if(sortColumnName != null) stream.append(sortColumnName);
            stream.append("\">");
            stream.append("\n  <INPUT TYPE=\"HIDDEN\" NAME=\"");
            stream.append(SORT_DIRECTION_PARAM);
            stream.append("\" VALUE=\"");
            if(sortDirection != null) stream.append(sortDirection);
            stream.append("\">");
            if(editable)
            {
                if(saveP && editable)
                {
                    StringBuffer sb = new StringBuffer(outputPath);
                    for(AdvertiserData advertiser: advertisers)
                    {
                        extractor.setAdvertiserId(advertiser.id);
                        processUpdates(sb, advertiser, qctx, methodMapper,
                                       schema, httpParams);
                    }
                    synchronized(Utils.internFile(outputPath))
                    {
                        bidder.backupSpreadsheet(outputPath);
                        Utils.logThisPoint
                             (Level.INFO, "Writing spreadsheet " + outputPath);
                        bidder.dumpBidData(outputPath, advertiserDataMap,qctx);
                    }
                    File f = new File(outputPath);
                    long wd = f.lastModified();
                    bidder.recordPerpetrator(perpetrator, SAVE_BIDDER_DATA,
                                             new Date(wd), sb.toString(),qctx);
                }
                stream.append("<INPUT TYPE=\"SUBMIT\" NAME=\"");
                stream.append(SAVE_PARAM);
                stream.append("\" VALUE=\"Save Changes\">");
                outputDownloadButton
                        (stream, "Download Spreadsheet", "dashboard");
                currentAdvertiser =
                        outputAdvertiserMenu
                                (stream, advertisersSexp, httpParams,
                                 "Advertiser:", ADVERTISER_ID_PARAM,
                                 true, false, true, true);
                currentBidPolicy =
                        outputBidPolicyMenu
                                (stream, httpParams, BIDDING_POLICY_PARAM);
            }
            if(currentAdvertiser == Null.nil)
            {
                advertisers = new Vector<AdvertiserData>();
            }
            else if(currentAdvertiser != Syms.All)
            {
                advertisers = new Vector<AdvertiserData>();
                for(AdvertiserData ad: advertiserDataMap.values())
                {
                    if(ad.id.equals(currentAdvertiser.car().unboxLong()))
                    {
                        advertisers.add(ad);
                        break;
                    }
                }
            }
            if(advertisers.size() > 0)
            {
                List<AdvCampDataPair> campaignsToShow = new Vector<AdvCampDataPair>();
                Collections.sort(advertisers, AdvertiserData.nameComparator);
                for(AdvertiserData advertiser: advertisers)
                {
                    extractor.setAdvertiserId(advertiser.id);
                    collectAdvertiserBidData(advertiser, currentBidPolicy,
                                             campaignsToShow);
                }
                // Campaigns are presorted by Adv-name/Camp-name.
                rowIndex = 0;
                // Apply the sort here!
                ExcelSlotSchema sortSlot =
                        slotSpecFromName(sortColumnName, schema);
                if(sortSlot != null)
                    Collections.sort(campaignsToShow,
                            getComparator(sortSlot, sortDirection,
                                    methodMapper, qctx));
                AdvertiserData lastAdvertiser = null;
                for(AdvCampDataPair pair: campaignsToShow)
                {
                    AdvertiserData advertiser = pair.advertiser;
                    CampaignData campaignData = pair.campaign;
                    extractor.setAdvertiserId(advertiser.id);
                    extractor.setCampaignId(campaignData.campaignId);
                    if(lastAdvertiser != null &&
                            !lastAdvertiser.id.equals(advertiser.id))
                        stream.append("\n  <TR height=5></TR>");
                    rowIndex = dumpCampaignDataToHTML
                            (advertiser, campaignData, stream, rowIndex, qctx,
                                    methodMapper, schema, editable, admin,
                                    sortColumnName, sortDirection);
                    lastAdvertiser = advertiser;
                }
                if(rowIndex > 0)
                    stream.append("\n</TABLE>");
                else stream.append
                      ("\n<H3>No campaigns match the filter settings</H3>");
            }
            else stream.append
                    ("\n<H3>No campaigns match the filter settings</H3>");
            stream.append("\n</FORM>");
            HTMLifier.finishHTMLPage(stream);
        }
        else
        {
            headers = handlerPageSetup
                    (stream, "Dashboard", null, stylesheetUrl,
                     extraHeaders, returnHeaders, prettyName, httpParams);
            stream.append
                ("<i>The Bidder has not run, so the advertisers/campaigns " +
                 "have not been loaded.  Please try again later.</i>");
            HTMLifier.finishHTMLPage(stream);
        }
        return headers;
    }

    Map<String, String> outputHeaderStuff
            (Writer stream, String stylesheetUrl, boolean returnHeaders,
             Map<String, String> httpParams)
            throws IOException
    {
        String title = prettyName;
        return handlerPageSetup
            (stream, title, urlName, stylesheetUrl, returnHeaders, httpParams);
    }

    public static void register()
    {
        HTTPListener.registerHandlerClass(BidderDashboardHTTPHandler.class);
    }
    static
    { register(); }
}


