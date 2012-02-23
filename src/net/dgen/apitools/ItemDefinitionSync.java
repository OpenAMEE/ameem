/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.amee.client.AmeeException;
import com.twicom.qdparser.TaggedElement;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dgen.apiexamples.AmeeXMLHelper;
import net.dgen.apiexamples.Main;

/**
 * Class sync'ing ItemDefinition object with an item def on API. This class can create
 * an item definition in AMEE.
 * to do: compare ItemDefintion with one in AMEE and update item values and alg
 * in AMEE accordingly.
 * @author nalu
 */
public class ItemDefinitionSync {
    public static final String DECIMAL = "DECIMAL";
    public static final String BOOLEAN = "BOOLEAN";
    public static final String INTEGER = "INTEGER";
    public static final String TEXT = "TEXT";
    public static final String UNKNOWN = "UNKNOWN";
    public static Map<String, String> stageValueDefMap = new HashMap();
    public static Map<String, String> liveValueDefMap = new HashMap();
    static boolean testMode = false;
    static boolean isToUpdateAlgorithm = true;
    static boolean isToUpdateDifferingValues = true;
    /** The from item is only used in updating. It is never altered. */
    private ItemDefinition fromItemDef;
    /** The to item will be either created or updated in the API/. */
    private ItemDefinition toItemDef;


    static {
        stageValueDefMap.put(DECIMAL, "45433E48B39F");//double,float
        stageValueDefMap.put(BOOLEAN, "4594CFEC6A20");//boolean
        stageValueDefMap.put(INTEGER, "537270B92F6E");//integer
        stageValueDefMap.put(TEXT, "CCEB59CACE1B");//string

        liveValueDefMap.put(DECIMAL, "45433E48B39F");//double,float
        liveValueDefMap.put(BOOLEAN, "B36A711611F3");//boolean
        liveValueDefMap.put(INTEGER, "537270B92F6E");//integer
        liveValueDefMap.put(TEXT, "CCEB59CACE1B");//string
    }

    /** Constructor for creating an item definition in the API. */
    private ItemDefinitionSync(ItemDefinition to) {
        fromItemDef = null;
        toItemDef = to;
    }

    /** Constructor for syncing an item definition in the API. */
    private ItemDefinitionSync(ItemDefinition from, ItemDefinition to) {
        fromItemDef = from;
        toItemDef = to;
    }

    static boolean isResponseOK(String response) {
        boolean success = false;
        if (response.toLowerCase().indexOf("200 ok") >= 0) {
            //System.err.println("response = " + response);
            success = true;
        } else {
            System.err.println("response = " + response);
        }
        return success;
    }

    static String sendRequest(String request, String body) throws AmeeException {
        String response;
        if (testMode) {
            System.err.println("TEST Mode (no change to AMEE):\n" + request + "\n" + body);
            response = "TEST MODE 200 ok";
        } else {
            response = Main.sendRequest(request, body);
        }

        return response;
    }

    private boolean create() throws AmeeException {
        boolean success = false;
        String request;
        request = "POST /admin/itemDefinitions";
        String body = "name=" + Main.urlEncode(toItemDef.name);
        String response = sendRequest(request, body);

        System.err.println("Request: " + request + "\n" + "Response:" + response);

        if (!testMode) {
            ArrayList al = AmeeXMLHelper.getElement(response, "ItemDefinition");
            TaggedElement te = (TaggedElement) al.get(0);
            toItemDef.dataItemDefUid = te.getAttribute("uid");
        } else {
            toItemDef.dataItemDefUid = "TESTMODE_NOUID";
        }
        success = isResponseOK(response);

        if (!success) {
            System.err.println("ERROR: failed to create item def:\n" + request + "\n" + body + "\n" + response);
        }

        success = updateDrillDown(true);
        if (!success) {
            System.err.println("ERROR: failed to create drill down");
        }

        Iterator<ItemDefinition.ValueDefinition> iter = toItemDef.valueMap.values().iterator();
        while (iter.hasNext()) {
            ItemDefinition.ValueDefinition vd = iter.next();
            success = createValue(vd);
            if (!success) {
                System.err.println("ERROR: failed to create item value def:\n" + vd);
            }
        }

        success = createAlgorithm(toItemDef.algorithmName, toItemDef.algorithm);
        if (!success) {
            System.err.println("ERROR: failed to create algorithm");
        }

        return success;
    }

    /** The drilldown can only be updated, as it's part of the item definition in AMEE. */
    private boolean updateDrillDown(boolean isCreate) throws AmeeException {
        String request;
        request = "PUT /admin/itemDefinitions";
        request += "/" + toItemDef.dataItemDefUid;
        ItemDefinition id = fromItemDef;
        if (isCreate) {
            id = toItemDef;
        }
        String body = "drillDown=" + Main.urlEncode(id.drillDown);

        return isResponseOK(sendRequest(request, body));
    }

    private String getBody(ItemDefinition.ValueDefinition vd) {
        String body = "";
        body += "name=" + Main.urlEncode(vd.name);
        if (vd.isDataItemValue().equals("true")) { //data item value
            body += "&fromData=true";
            body += "&fromProfile=false";
        } else if (vd.isDataItemValue().equals("false")) { //profile item value
            body += "&fromData=false";
            body += "&fromProfile=true";
        } else if (vd.isDataItemValue().equals("both")) {
            body += "&fromData=true";
            body += "&fromProfile=true";
        }

        if (!vd.unit.equals(ItemDefinition.NOTSET)) {
            body += "&unit=" + vd.unit;
        } else {
            body += "&unit=";
        }

        if (!vd.perUnit.equals(ItemDefinition.NOTSET)) {
            body += "&perUnit=" + vd.perUnit;
        } else {
            body += "&perUnit=";
        }

        if (!vd.defaultValue.equals(ItemDefinition.NOTSET)) {
            body += "&value=" + vd.defaultValue;
        } else {
            body += "&value=";
        }

        if (!vd.choices.equals(ItemDefinition.NOTSET)) {
            body += "&choices=" + vd.choices;
        } else {
            body += "&choices=";
        }

        if (vd.versions.indexOf("1.0") >= 0) {
            body += "&apiversion-1.0=true";
        } else {
            //body += "&apiversion-1.0=false";
        }

        if (vd.versions.indexOf("2.0") >= 0) {
            body += "&apiversion-2.0=true";
        } else {
            //body += "&apiversion-2.0=false";
        }

        return body;
    }

    private boolean createValue(ItemDefinition.ValueDefinition vd) throws AmeeException {
        String request;
        request = "POST /admin/itemDefinitions";
        request += "/" + toItemDef.dataItemDefUid + "/itemValueDefinitions";
        String body = "valueDefinitionUid=" + Main.urlEncode(vd.getValueDefinitionUid());
        body += "&path=" + Main.urlEncode(vd.path);
        body += "&" + getBody(vd);
        System.err.println("body=" + body);
        if (!vd.aliasedTo.equals(ItemDefinition.NOTSET)) {
            System.err.println("WARNING: This IVD contains an AliasedTo path=" + vd.path + ": must be created manually");
        }
        return isResponseOK(sendRequest(request, body));
    }

    private boolean updateValue(String uid, ItemDefinition.ValueDefinition vd) throws AmeeException {
        String request;
        request = "PUT /admin/itemDefinitions";
        request += "/" + toItemDef.dataItemDefUid + "/itemValueDefinitions/" + uid;
        String body = getBody(vd);
        //System.err.println("body=" + body);
        return isResponseOK(sendRequest(request, body));
    }

    private boolean deleteValue(ItemDefinition.ValueDefinition vd) {
        return false;
    /* Far too dangerous just now - do it manually.
    String request;
    request = "DELETE /environments/" + ApiTools.adminEnvironmentUid + "/itemDefinitions";
    request += "/" + toItemDef.dataItemDefUid + "/itemValueDefinitions/";
    request += vd.uid;

    return isResponseOK(sendRequest(request, ""));*/
    }

    private boolean createAlgorithm(String name, String content) throws AmeeException {
        String request;
        request = "POST /admin/itemDefinitions";
        request += "/" + toItemDef.dataItemDefUid + "/algorithms";
        String body = "name=" + name;
        body += "&content=" + Main.urlEncode(content);

        return isResponseOK(sendRequest(request, body));
    }

    private boolean updateAlgorithm() throws AmeeException {
        String request;
        request = "PUT /admin/itemDefinitions";
        request += "/" + toItemDef.dataItemDefUid + "/algorithms/" + toItemDef.algorithmUid;
        String body = "content=" + Main.urlEncode(fromItemDef.algorithm);

        return isResponseOK(sendRequest(request, body));
    }

    /** Checks for differences between the two in the same way as update.
     * @return true If to and from are the same, false if not.
     */
    private boolean check() {
        boolean areSame = true;
        if (!fromItemDef.drillDown.equals(toItemDef.drillDown)) {
            System.err.println("WARNING: Drill downs differ.");
            areSame = false;
        }

        if (toItemDef.algorithmUid == null) {
            System.err.println("WARNING: \"to\" algorithm is null.");
            areSame = false;
        } else if (fromItemDef.algorithm.compareTo(toItemDef.algorithm) != 0) {
            System.err.println("WARNING: Algorithms differ.");
            areSame = false;
        }

        //values that are in "from", but not in "to"
        Set<ItemDefinition.ValueDefinition> missing;
        missing = toItemDef.getMissingValueDefinitions(fromItemDef);

        if (!missing.isEmpty()) {
            System.err.println("WARNING: \"from\" contains values that are not in \"to\":\n" + missing);
            areSame = false;
        }

        //values that are in "to", but not in "from"
        Set<ItemDefinition.ValueDefinition> extra;
        extra = fromItemDef.getMissingValueDefinitions(toItemDef);

        if (!extra.isEmpty()) {
            System.err.println("WARNING: \"to\" contains values that are not in \"from\":\n" + extra);
            areSame = false;
        }

        return areSame;
    }

    /** This method WON'T delete values and it can't rename existing values.
     *  Those have to be done manually.
     * @return
     */
    private boolean update() throws AmeeException {
        boolean success = true, overallSuccess = true;
        if (!fromItemDef.drillDown.equals(toItemDef.drillDown)) {
            success = updateDrillDown(false);
            System.err.println("\nUpdating drill down to " + fromItemDef.drillDown + " : " + success);
        }
        overallSuccess = overallSuccess && success;

        if (toItemDef.algorithmUid == null) {
            success = createAlgorithm(fromItemDef.algorithmName, fromItemDef.algorithm);
            System.err.println("\nAlgorithm create success = " + success);
        } else if (fromItemDef.algorithm.replaceAll("(\r)", "").compareTo(toItemDef.algorithm.replaceAll("(\r)", "")) != 0) {
            //System.err.println("from alg=[" + fromItemDef.algorithm + "]");
            //System.err.println("  to alg=[" + toItemDef.algorithm + "]");
            if (isToUpdateAlgorithm) {
                success = updateAlgorithm();
                System.err.println("\nAlgorithm update success = " + success);
            } else {
                System.err.println("\nWARNING: Not updating/creating algorithm because isToUpdateAlgorithm=false");
            }
        }
        overallSuccess = overallSuccess && success;

        //values that are in "from", but not in "to"
        Set<ItemDefinition.ValueDefinition> missing;
        missing = toItemDef.getMissingValueDefinitions(fromItemDef);

        Iterator<ItemDefinition.ValueDefinition> iter = missing.iterator();
        while (iter.hasNext()) {
            ItemDefinition.ValueDefinition ivd = iter.next();
            success = createValue(ivd);
            overallSuccess = success && overallSuccess;
            System.err.println("\nCreated value " + ivd.path + ": success = " + success);
        }

        //values that are in "to", but not in "from"
        Set<ItemDefinition.ValueDefinition> extra;
        extra = fromItemDef.getMissingValueDefinitions(toItemDef);

        iter = extra.iterator();
        while (iter.hasNext()) {
            System.err.println("\nDELETE THIS ITEM VALUE MANUALLY: " + iter.next());
        //success = deleteValue(iter.next());
        //overallSuccess = success && overallSuccess;
        }

        //values that have the same path in both but differ in some other respect.
        //path and type CANNOT be changed this way, must be done manually
        Map<String, ItemDefinition.ValueDefinition> differing;
        differing = fromItemDef.getDifferingValueDefinitions(toItemDef);

        if (isToUpdateDifferingValues) {
            Iterator<String> diffIter = differing.keySet().iterator();
            while (diffIter.hasNext()) {
                String uid = diffIter.next();
                ItemDefinition.ValueDefinition ivd = differing.get(uid);
                success = updateValue(uid, ivd);
                overallSuccess = success && overallSuccess;
                System.err.println("\nUpdated value " + ivd.path + ": success = " + success);
            }
        } else {
            System.err.println("\nWARNING: Not updating differing values because isToUpdateDifferingValues=false");
        }
        return overallSuccess;
    }

    /** Creates the ItemDef in the API. */
    public static boolean createInAPI(int site, ItemDefinition itemDef) throws AmeeException {
        boolean success;
        ApiTools.isAdmin = true;
        ApiTools.init(site);

        ItemDefinitionSync dc = new ItemDefinitionSync(itemDef);
        success = dc.create();

        return success;
    }

    /** This method WON'T delete values and it can't rename existing values.
     * Those have to be done manually.
     * @param site DataCategory.STAGE or .LIVE
     * @param csvFile File containing the local data item def
     * @param path Path on API to look up the current data item def to update
     * @return
     */
    public static boolean updateInAPI(int site, File csvFile, String path) throws AmeeException {
        boolean success = false;

        boolean saveIsAdmin = ApiTools.isAdmin;
        int saveSite = ApiTools.currentSite;

        ApiTools.isAdmin = true;
        ApiTools.init(site);

        ItemDefinition localItemDef = ItemDefinition.fetchItemDefFromCSV(csvFile);
        ItemDefinition apiItemDef = ItemDefinition.fetchItemDefFromAPI(site, path);

        //System.err.println("csv id=\n"+localItemDef);
        //System.err.println("api id=\n"+apiItemDef);


        ItemDefinitionSync ids = new ItemDefinitionSync(localItemDef, apiItemDef);
        success = ids.update();

        //restore settings
        ApiTools.isAdmin = saveIsAdmin;
        ApiTools.init(saveSite);

        return success;
    }
    private static ItemDefinition apiItemDef,  localItemDef;

    /**
     * @param site DataCategory.STAGE or .LIVE
     * @param csvFile File containing the local data item def
     * @param path Path on API to look up the current data item def to check against
     * @return true if they are the same or if category doesn't have an item def in api
     */
    public static boolean checkInAPI(int site, File csvFile, String path) throws AmeeException {
        boolean isSame = false;

        apiItemDef = ItemDefinition.fetchItemDefFromAPI(site, path);
        if (apiItemDef == null) {
            System.err.println("This category has no item def:" + path);
            return true;
        }

        localItemDef = null;

        if (csvFile.exists()) {
            localItemDef = ItemDefinition.fetchItemDefFromCSV(csvFile);
            ItemDefinitionSync ids = new ItemDefinitionSync(localItemDef, apiItemDef);
            isSame = ids.check();
        } else {
            System.err.println("local csv doesn't exist: " + csvFile);
        }

        /*if (!isSame) {
        System.err.println("local item def:\n" + localItemDef);
        System.err.println("API item def:\n" + apiItemDef);
        }*/

        return isSame;
    }

    /* Starting from the specified path, item definitions stored locally are
     * compared with those in the API. If they differ, the api is saved locally.
     * @param site DataCategory.STAGE or .LIVE
     * @param path Path on API to look up the current data item def to check against
     * @param save If true, the api def is saved locally if the item def differs
     * or doesn't exist locally.
     */
    public static void recurseSaveFromAPI(int site, String path, boolean save) throws AmeeException {
        File dir = new File(ApiTools.csvDir, path);
        File csvFile = new File(dir, "itemdef.csv");
        System.err.println("======= " + path);

        boolean isSame = checkInAPI(site, csvFile, path);
        if (!isSame) {
            System.err.println("item defs are DIFFERENT");
            if (save && localItemDef == null) { //latter stops saving if local itemdef.csv exists
                System.err.println("API version saved locally.");
                apiItemDef.save(dir);
            }
        } else {
            System.err.println("item defs are OK");
        }
        File[] subdirs = dir.listFiles();
        for (int i = 0; i < subdirs.length; i++) {
            if (subdirs[i].isDirectory() && !subdirs[i].getName().equals(".svn")) {
                recurseSaveFromAPI(site, path + File.separator + subdirs[i].getName(), save);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws AmeeException {
        Main.setLogin(args[0]);
        Main.setPassword(args[1]);
        int site = DataCategory.STAGE;

        ApiTools.csvDir = new File("/home/jamespjh/devel/amee/svn.amee.com/internal/api_csvs");

        //File dir = new File(ApiTools.csvDir,"home/water");
        //checkInAPI(site, new File(dir,"itemdef.csv"), "/home/water");

        recurseSaveFromAPI(site, "/home", true);


    //Fetches and item def from the API
//        ItemDefinition id = ItemDefinition.fetchItemDefFromAPI(site, "/home");
//        System.err.println(id);

    //Loads an item from a local csv file
        /*File dir = new File(ApiTools.csvDir, "home/heating");
    id = ItemDefinition.fetchItemDefFromCSV(new File(dir, "itemdef.csv"));
    System.err.println("id = " + id);*/

    //Creates the item in the 
    //System.err.println("create success = " + createInAPI(site, id));
    }
}
