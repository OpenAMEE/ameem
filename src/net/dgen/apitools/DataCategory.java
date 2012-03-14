/*
 * DataCategory.java
 *
 * Created on 19 March 2007, 09:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.amee.client.AmeeException;
import java.util.*;
import java.io.*;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import net.dgen.apiexamples.AmeeXMLHelper;
import net.dgen.apiexamples.Main;

/**
 * Adapted from CalculatorSection in CarbonTest.
 * Divorced from UserData.
 * @author nalu
 */
public class DataCategory {
    static final int LOCAL = 1,  API = 2 ;
    public static final String QUOTE = "'";
    public final static int DEFRA = 0,  STAGE = 1,  LIVE = 2,  DEV = 3,  SANDBOX = 4,  JB = 5,  SCIENCE = 6, FREE = 7;
    /** Status for a data item */
    public final static int NOTHING = 0,  AUTO = 1,  POST = 2,  PUT = 3,  PUT_SOURCE = 4,  PUT_BOTH = 5;
    protected boolean isStrict = true;
    protected boolean checkSource = false;
    protected boolean useItemDef = true;
    protected boolean doBackup = false;
    protected boolean doBatch = true;
    protected double localResult = 0.;
    protected double apiResult = 0.;
    protected String apiParams;
    protected String apiPath;
    protected int limit = Integer.MAX_VALUE,  startIndex = 1,  stopIndex = Integer.MAX_VALUE;
    protected String[] drillNames;//names of drill items, e.g. for car, size and fuel etc.
    protected String[] itemNames;//names of data item values, e.g. for car, size and fuel etc.
    protected File csvFile;
    protected String saveName;
    protected String sourceName;
    protected boolean useDataItemUidCache = false;    //Results
    protected HashMap apiMap = new LinkedHashMap(),  localMap = new LinkedHashMap();
    private File backupDir;
    protected Map cacheMap = new HashMap();
    protected BufferedWriter backupWriter;
    protected int[] valueColumns;
    protected ItemDefinition itemDefinition;
    /** Container for storing values from the user. */
    //protected UserData userData;
    /** Container for storing carbon data, mirroring data-base entries. */
    private LinkedHashMap carbonDataMap = new LinkedHashMap();

    /**
     * Creates a new instance of DataCategory where item def is to be loaded
     */
    public DataCategory(String apiPath) {
        this(apiPath, null);
        csvFile = new File(ApiTools.csvDir, apiPath);
        csvFile = new File(csvFile, "data.csv");
        saveName = apiPath.replaceAll("/", "_");
        sourceName = "source";
        backupDir = new File(ApiTools.csvDir, "backup");
    }

    /**
     * Creates a new instance of DataCategory with no item def
     */
    public DataCategory(String apiPath, int[] valueColumns) {
        this.apiPath = apiPath;
        this.valueColumns = valueColumns;
    }

    /** NOTHING means this key will be ignored.
     *  AUTO means that the api will be checked and the appropriate action taken
     *  POST means that only a POST will be attempted, saves on looking for a non-existent uid */
    private int getDefaultAction() {
        return defaultAction;
    }
    private int defaultAction = AUTO;

    public void setDefaultAction(int action) {
        defaultAction = action;
    }

    public void setUseCache(boolean b) {
        useDataItemUidCache = b;
    }

    public Map getApiMap() {
        return apiMap;
    }

    public Map getLocalMap() {
        return localMap;
    }

    public void checkAPI() throws AmeeException {
        updateAPI(false);
    }

    public void updateAPI() throws AmeeException {
        updateAPI(true);
    }

    /** Sets the index to start from, defaults to 1 */
    public void setStart(int index) {
        startIndex = index;
    }

    /** Sets the index to stop at, defaults to max integer */
    public void setStop(int index) {
        stopIndex = index;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @param update If true the API will be changed, if false it will
     * just be checked.
     */
    private void updateAPI(boolean update) throws AmeeException {
        if (update) {
            System.err.println("Opening confirmation dialog...");
            //int res = javax.swing.JOptionPane.showConfirmDialog(null, "Are you sure you want to update the API?");
            //if (res != javax.swing.JOptionPane.YES_OPTION) {
            //    System.err.println("...update aborted");
            //    return;
            //}
            System.err.println("...update confirmed.");
        }

        if (doBackup) {
            DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String now = df.format(new Date());
            File backupFile = new File(backupDir, saveName + "_" + now + ".csv");
            backupWriter = ApiTools.getBufferedWriter(backupFile);
            try {
                backupWriter.write(getCSVHeaderLine() + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        String charSet = Main.apiWriteCharSet;//"ISO-8859-1"; //was UTF-8
		ArrayList<String> putItems = new ArrayList<String>();
		ArrayList<String> postItems = new ArrayList<String>();
        Iterator iter = carbonDataMap.keySet().iterator();
        if (startIndex > 1) {
            System.err.println("SKIPPING TO " + startIndex);
        }
        int i = 1, iDiff = 0, iMissing = 0, iFail = 0;
        long time = System.currentTimeMillis();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            DataItem de = (DataItem) carbonDataMap.get(key);
            try {
                int action = getDefaultAction();
                if (action != NOTHING) {
                    String response = null;
                    if (i < startIndex) {
                        //do nothing
                    } else {
                        System.err.println(">>>>>>>>>>>>>>>>>>>>>>>> i = " + i + "    key       = " + key + " num fails = " + iFail + "    num diffs = " + iDiff + " num missing = " + iMissing + "  time = " + (System.currentTimeMillis() - time) / 1000);
                        if (action == AUTO) {
                            action = de.loadValuesFromAPI();
                        }
                        if (action == POST) {
                            System.err.println("    MISSING*****************************");
                            iMissing++;
                            boolean success = false;
                            for (int tries = 0; tries < 3; tries++) {
                                if (doBatch) {
                                    postItems.add(de.getPostXML());
                                    success = true;
                                } else {
                                    success = ApiTools.createDataItem(apiPath, de.getPostString(), update);
                                }
                                if (success) {
                                    break;
                                }
                            }
                            if (!success) {
                                throw (new Exception("Failed to POST exception"));
                                //iFail++;
                            }
                        } else if (action >= PUT) {//Any of the PUT actions
                            String updatedValues = de.getUpdatedValues(action);
                            if (updatedValues.length() > 0) {
                                iDiff++;
                                System.err.println("    DIFF*****************************");
                                boolean success = false;
                                for (int tries = 0; tries < 3; tries++) {
                                    if (doBatch) {
                                        putItems.add(de.getPutXML(action));
                                        success = true;
                                    } else {
                                        success = ApiTools.updateDataItem(apiPath, de.keyMap, updatedValues, update);
                                    }
                                    if (success) {
                                        break;
                                    }
                                }
                                if (!success) {
                                    throw (new Exception("Failed to PUT exception"));
                                    //iFail++;
                                }
                            } else {
                                System.err.println("FATAL ERROR - ABORTING: PUT requested, but updatedValues string is empty in DataCategory.updateAPI");
                                System.exit(0);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                iFail++;
                ApiTools.sleep(1l);//wait for one sec
            }
            if (i >= stopIndex) {
                break;
            }
            i++;
        }
        if (iDiff > 0 || iMissing > 0) {
			int batchSize = 1; // Turned right down because of timeout problems.
            if (iDiff > 0) {
                if (doBatch) {
					for (int itemStart=0; itemStart<putItems.size(); itemStart+=batchSize)
					{
						time = System.currentTimeMillis();
	                    System.err.println("Sending PUT batch...");
				        String putXML = "<?xml version=\"1.0\" encoding=\"" + charSet + "\"?>\n" + "<DataCategory>\n" + "<DataItems>\n";
						int itemEnd = itemStart+batchSize;
						if (itemEnd > putItems.size())
							itemEnd = putItems.size();
						for (String item: putItems.subList(itemStart, itemEnd))
							putXML += item;
				        putXML += "</DataItems>\n" + "</DataCategory>";
	                    boolean success = ApiTools.batchDataItems("PUT", apiPath, putXML, update);
	                    if (success) {
	                        System.err.println("...PUT batch sent successfully");
	                        System.err.println("<<<< PUT time = " + (System.currentTimeMillis() - time) / 1000);
	                    } else {
	                        System.err.println("ERROR - PUT batch failed");
	                    }
					}
                }
                System.err.println("number of updated items = " + iDiff);
            }
            if (iMissing > 0) {
                if (doBatch) {
					for (int itemStart=0; itemStart<postItems.size(); itemStart+=batchSize)
					{
	                    time = System.currentTimeMillis();
	                    System.err.println("Sending POST batch...");
				        String postXML = "<?xml version=\"1.0\" encoding=\"" + charSet + "\"?>\n" + "<DataCategory>\n" + "<DataItems>\n";
						int itemEnd = itemStart+batchSize;
						if (itemEnd > postItems.size())
							itemEnd = postItems.size();
						for (String item: postItems.subList(itemStart, itemEnd))
							postXML += item;
				        postXML += "</DataItems>\n" + "</DataCategory>";
	                    boolean success = ApiTools.batchDataItems("POST", apiPath, postXML, update);
	                    if (success) {
	                        System.err.println("...POST batch sent successfully");
	                        System.err.println("<<<< POST time = " + (System.currentTimeMillis() - time) / 1000);
	                    } else {
	                        System.err.println("ERROR - POST batch failed");
	                    }
					}
                }
                System.err.println("number of created items = " + iMissing);
            }
        } else if (update) {
            System.err.println("NO CHANGES NEEDED TO BE MADE TO API");
        } else {
            System.err.println("CHECK PASSED - NO API UPDATE NEEDED");
        }
        if (useDataItemUidCache) {
            saveDataItemUidCache();
        }
        if (doBackup) {
            try {
                backupWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Note: This must be called before checkAPI or updateAPI */
    public void loadLocalData() throws AmeeException {
        if (useDataItemUidCache) {
            loadDataItemUidCache();
        }

        if (useItemDef) {
            File itemDefCSV = new File(csvFile.getParentFile(), "itemdef.csv");
            if (itemDefCSV.exists()) {
                System.err.println("Loading local data item def...");
                itemDefinition = ItemDefinition.fetchItemDefFromCSV(itemDefCSV);
            } else {
                System.err.println("No itemdef at:" + csvFile.getParentFile());
                System.err.println("Loading API data item def...");
                itemDefinition = ItemDefinition.fetchItemDefFromAPI(ApiTools.currentSite, apiPath);
                System.err.println("Saving item def locally...");
                itemDefinition.save(csvFile.getParentFile());
            }
        }

        if (!load()) {
            System.err.println("ABORTING: failed to load local data.");
            System.exit(666);
        }

        System.err.println("...local loading done");
    }

    private boolean setValueColumnsFromItemDef(String headerLine) {
        String[] ss = splitCSVLine(headerLine);
        int firstValue = -1, lastValue = -10;
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].equalsIgnoreCase("units")) {
                lastValue = i - 1;
                break;
            } else if (firstValue < 0 && itemDefinition.drillDown.indexOf(ss[i]) < 0) {
                firstValue = i;
            }
        }

        if (firstValue < 0 && lastValue >= 0) {//there is no data item column
            firstValue = lastValue;
            System.err.println("WARNING: DataCategory.setValueColumnsFromItemDef:  no data item value columns:\n" + headerLine);
            valueColumns = new int[1];
            valueColumns[0] = -1 * (firstValue + 1);//load needs to know where drill down ends, but make it -ve so it knows there are no vals
            return true;
        } else if (firstValue < 0 || lastValue < 0 || lastValue < firstValue) {
            System.err.println("DataCategory.setValueColumnsFromItemDef:  item def inconsistent with this header line:\n" + headerLine);
            return false;
        } else {
            String s = "valueColumns={";
            valueColumns = new int[lastValue - firstValue + 1];
            for (int i = firstValue; i <= lastValue; i++) {
                valueColumns[i - firstValue] = i;
                s += i + ",";
            }
            s += "}";
            System.err.println(s);
            return true;
        }
    }

    private boolean load() {
        System.err.println("Loading local data...");
        try {
            BufferedReader br = ApiTools.getBufferedReader(csvFile);

            String line = br.readLine(); //first line is header line

            if (useItemDef && !setValueColumnsFromItemDef(line)) {
                br.close();
                return false;
            }
            int firstValue = valueColumns[0], lastValue = valueColumns[valueColumns.length - 1];
            int nDrill = Math.abs(firstValue);//-ve means there are no vals
            String[] ss = splitCSVLine(line);
            drillNames = new String[nDrill];
            System.err.println("nDrill=" + nDrill);
            for (int i = 0; i < nDrill; i++) {
                drillNames[i] = ApiTools.cleanString(ss[i]);
            }
            if (firstValue < 0) {
                itemNames = new String[0];
                lastValue = nDrill - 1;
            } else {
                itemNames = new String[valueColumns.length];
                for (int i = firstValue; i <= lastValue; i++) {
                    itemNames[i - firstValue] = ApiTools.cleanString(ss[i]);
                    //System.err.println("itemName" + (i - firstValue) + "=" + itemNames[i - firstValue]);
                }
            }
            line = br.readLine();
            while (line != null) {
                ss = splitCSVLine(line);
                while (ss == null) { //deal with CSV fields containing newlines
                    line += br.readLine();
                    ss = splitCSVLine(line);
                }
                //System.err.print(" ===== " + line + "\n");
                String key = "";
                // Bugs due to empty lines in data files fell here
                if (ss.length <= 1) {
                    line = br.readLine();
                    continue;
                }
                for (int i = 0; i < nDrill; i++) {
                    String s = ApiTools.cleanString(ss[i]);
                    //if(s.compareTo("-")!=0){
                    if (i > 0) {
                        key += "&";
                    }
                    key += s.replaceAll("&", DataItem.AMPERSAND);
                    //}
                }
                //System.err.println("ss.length=" + ss.length);
                //System.err.println("lastValue=" + lastValue);
                int i = lastValue;
                String units = "";
                if (i + 1 < ss.length && ss[i + 1].length() > 0) {
                    units = ss[i + 1];
                }
                String source = "";
                if (i + 2 < ss.length && ss[i + 2].length() > 0) {
                    source = ss[i + 2];
                } else {
                    System.err.println("WARNING: Source field was blank - ALWAYS quote a source!");
                    if (isStrict) {
                        System.err.println("ABORTING because source was blank - set isStrict=false to override.");
                        System.exit(666);
                    }
                }
                String alg = "";
                if (i + 3 < ss.length && ss[i + 3].length() > 0) {
                    alg = ss[i + 3];
                }
                String isDefault = "false";
                if (i + 4 < ss.length && ss[i + 4].length() > 0) {
                    isDefault = "true";
                }
                DataItem dr = new DataItem(this, key, getValues(ss), units, source, alg, isDefault);
                if (isStrict && !dr.isOK()) {
                    System.err.println("ABORTING because data item is invalid - set isStrict=false to override.");
                    System.exit(666);
                }
                carbonDataMap.put(key, dr);
                if (carbonDataMap.size() >= limit) {
                    break;
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Error reading/writing CSV files " + e);
            return false;
        }
        return true;
    }

    /**
     * Parses a line from a csv file that may contain quotes. If entries are
     * quoted it is assumed that there are no spaces between commas and quotes.
     * @param line line from CSV file
     * @return array of entries or null if the line contains an unclosed quote
     */
    public static String[] splitCSVLine(String line) {
        ArrayList<String> al = new ArrayList();
        if (line.indexOf('"') < 0) {
            line = line.replaceAll("_QUOTE_", "\"");
            return line.split(",");
        } else {
            int cur = 0;
            while (cur < line.length()) {
                //System.err.println("cur=" + cur + ":" + line.substring(cur));
                int cNext = line.indexOf(',', cur);
                int qNext = line.indexOf('"', cur);
                if (cNext < 0) {//last entry
                    cNext = line.length();
                }

                if (qNext < 0 || cNext < qNext) {//unquote entry
                    al.add(line.substring(cur, cNext));
                    cur = cNext + 1; //skip comma
                } else {//quoted entry
                    cur = qNext + 1; //skip opening quote
                    qNext = line.indexOf('"', cur);
                    if (qNext < 0) {//no closing quote
                        if (Main.debug) {
                            System.err.println("DataCategory.splitCSVLine: unclosed quote at pos" + cur + ":\n" + line);
                        }
                        return null;
                    }
                    al.add(line.substring(cur, qNext));
                    cur = qNext + 2; //skip closing quote plus next comma
                }
            }
        }
        return al.toArray(new String[0]);
    }

    protected String[] getValues(String[] ss) {
        if (itemNames.length == 0) { //no values
            return new String[0];
        }

        String[] values = new String[valueColumns.length];
        for (int i = 0; i < values.length; i++) {
            int col = valueColumns[i];
            values[i] = ss[col].trim();//values from API seemed to get trimmed, so trim here
        }

        return values;
    }

    public double calculate() {
        return -999.0;
    }

    private void saveDataItemUidCache() {
        System.err.println("Saving item cache...");
        Iterator iter = cacheMap.keySet().iterator();
        BufferedWriter bw = ApiTools.getBufferedWriter(new File(ApiTools.cachePath, saveName + ".csv"));
        if (bw == null) {
            System.err.println("dataItemUid cache couldn't be opened for saving: " + csvFile.getAbsolutePath());
            return;
        }
        while (iter.hasNext()) {
            String key = (String) iter.next();
            try {
                String uid = (String) cacheMap.get(key);
                if (uid != null) {
                    bw.write(key + "," + uid + "\n");
                }
            } catch (IOException e) {
                System.err.println("Problem saving uid_cache: " + e);
            }
        }
        try {
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.err.println("...done");
    }

    private void loadDataItemUidCache() throws AmeeException {
        File file = new File(ApiTools.cachePath, saveName + ".csv");
        System.err.println("Loading item cache..." + file.getAbsolutePath());
        BufferedReader br = ApiTools.getBufferedReader(file);
        if (br == null) {
            System.err.println("dataItemUid cache not found: " + file.getAbsolutePath());
            return;
        }
        boolean firstOne = true;
        try {
            String line = br.readLine();
            while (line != null) {
                String[] ss = line.split(",");
                cacheMap.put(ss[0], ss[1]);
                if (firstOne) {
                    String response = ApiTools.getDataItemFromUid(apiPath, ss[1]);
                    if (response == null || response.toLowerCase().indexOf("200 ok") < 0) {
                        System.err.println("FATAL ERROR - ABORTING: cache is out of date or server unavailable");
                        System.err.println("response=\n" + response);
                        System.exit(0);
                    } else {
                        System.err.println("Cache check OK");
                        firstOne = false;
                    }
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.err.println("...done");
        System.err.println("cache map=\n" + cacheMap);
    }

    public String getCSVHeaderLine() {
        String s = "";
        for (int i = 0; i < drillNames.length; i++) {
            s += drillNames[i] + ",";
        }
        for (int i = 0; i < itemNames.length; i++) {
            s += itemNames[i] + ",";
        }
        s += "units," + sourceName + ",algorithm,default";
        return s;
    }

    public String getCSV(int where) {
        String s = getCSVHeaderLine() + "\n";

        Iterator iter = carbonDataMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            DataItem de = (DataItem) carbonDataMap.get(key);
            s += de.getCSVLine(where) + "\n";
        }

        return s;
    }

    public String toString() {
        String s = "Data category: " + apiPath + "\n";
        for (int i = 0; i < drillNames.length; i++) {
            s += drillNames[i];
            if (i < drillNames.length - 1) {
                s += ",";
            }
        }
        s += "\n";
        Iterator iter = carbonDataMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            s += key + "," + carbonDataMap.get(key) + "\n";
        }
        return s;
    }

    /** Gets the data category object for instance (DEFRA or AMEE)
     *  for category called name.
     *  @param path The path e.g. /home/lighting, unless instance==DEFRA, the name of the category, lighting.
     */
    public static DataCategory getDataCategory(int instance, String path) {
        if (instance == DEFRA) {
            return getDEFRADataCategory(path);
        } else if (instance == STAGE || instance == LIVE || instance == DEV) {
            return getDataCategory(path);
        } else {
            System.err.println("No such instance = " + instance);
            return null;
        }
    }

    static DataCategory getDataCategory(String path) {
        DataCategory cat = null;
        cat = new DataCategory(path);
        return cat;
    }

    /** This way of doing things is legacy, now we should just use the itemDef.
     * @deprecated
     * @param name
     * @return
     */
    private static DataCategory getAMEEDataCategory(String name) {
        DataCategory cat = null;
        File csvDir = new File("/home/nalu/docs/amee/api_csvs");
        if (name.equals("major_airports")) {
            cat = new DataCategory("/transport/plane/generic/airports/major");
            cat.csvFile = new File(csvDir, "/transport/plane/generic/airports/major/data.csv");
        } else if (name.equals("hob")) {
            int[] vc = {1, 2};
            cat = new DataCategory("/home/appliances/cooking/hob", vc);
            cat.csvFile = new File(csvDir, "/home/appliances/cooking/hob/data.csv");
        } else if (name.equals("oven")) {
            int[] vc = {1, 2};
            cat = new DataCategory("/home/appliances/cooking/oven", vc);
            cat.csvFile = new File(csvDir, "/home/appliances/cooking/oven/data.csv");
        } else if (name.equals("insulation")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/insulation", vc);
            cat.csvFile = new File(csvDir, "home/energy/insulation/data.csv");
        } else if (name.equals("personal_generic")) {
            int[] vc = {1};
            cat = new DataCategory("/personal/generic", vc);
            cat.csvFile = new File(csvDir, "personal/generic/data.csv");
        } else if (name.equals("us_price")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/us/price", vc);
            cat.csvFile = new File(csvDir, "home/energy/us/price/data.csv");
        } else if (name.equals("uk_price")) {
            int[] vc = {2};
            cat = new DataCategory("/home/energy/uk/price", vc);
            cat.csvFile = new File(csvDir, "home/energy/uk/price/data.csv");
        } else if (name.equals("computers")) {
            int[] vc = {2};//vc ={1};
            cat = new DataCategory("/home/appliances/computers/generic", vc);
            cat.csvFile = new File(csvDir, "home/appliances/computers/generic/data.csv");
        } else if (name.equals("kitchen")) {
            int[] vc = {4, 5};//vc ={1};
            cat = new DataCategory("/home/appliances/kitchen/generic", vc);
            cat.csvFile = new File(csvDir, "home/appliances/kitchen/generic/data.csv");
        } else if (name.equals("car_bands_ireland")) {
            int[] vc = {1, 2, 3};//vc ={1};
            cat = new DataCategory("/transport/car/bands/ireland", vc);
            cat.csvFile = new File(csvDir, "transport/car/bands/ireland/data.csv");
        } else if (name.equals("heating")) {
            int[] vc = {5};//vc ={1};
            cat = new DataCategory("/home/heating", vc);
            cat.csvFile = new File(csvDir, "home/heating/data.csv");
        } else if (name.equals("heating_us")) {
            int[] vc = {4, 5, 6};//vc ={1};
            cat = new DataCategory("/home/heating/us", vc);
            cat.csvFile = new File(csvDir, "home/heating/us/data.csv");
        } else if (name.equals("lighting")) {
            int[] vc = {1};//vc ={1};
            cat = new DataCategory("/home/lighting", vc);
            cat.csvFile = new File(csvDir, "home/lighting/data.csv");
        } else if (name.equals("car_specific_uk")) {
            int[] vc = {6, 7, 8, 9, 10, 11, 12, 13, 14};//vc ={1};
            cat = new DataCategory("/transport/car/specific/uk", vc);
            cat.csvFile = new File(csvDir, "transport/car/specific/uk/data.csv");
        } else if (name.equals("car_specific_uk_stats")) {
            int[] vc = {6, 7, 8, 9, 10, 11, 12, 13, 14};//vc ={1};
            cat = new DataCategory("/transport/car/specific/uk/stats", vc);
            cat.csvFile = new File(csvDir, "transport/car/specific/uk/stats/data.csv");
        } else if (name.equals("van_generic")) {
            int[] vc = {2, 3};//vc ={1};
            cat = new DataCategory("/transport/van/generic", vc);
            cat.csvFile = new File(csvDir, "transport/van/generic/data.csv");
        } else if (name.equals("car_generic")) {
            int[] vc = {2, 3};//vc ={1};
            cat = new DataCategory("/transport/car/generic", vc);
            cat.csvFile = new File(csvDir, "transport/car/generic/data.csv");
        } else if (name.equals("minibus_generic")) {
            int[] vc = {2, 3};//vc ={1};
            cat = new DataCategory("/transport/minibus/generic", vc);
            cat.csvFile = new File(csvDir, "transport/minibus/generic/data.csv");
        } else if (name.equals("bus_generic")) {
            int[] vc = {1, 2};//vc ={1};
            cat = new DataCategory("/transport/bus/generic", vc);
            cat.csvFile = new File(csvDir, "transport/bus/generic/data.csv");
        } else if (name.equals("train_generic")) {
            int[] vc = {1, 2};//vc ={1};
            cat = new DataCategory("/transport/train/generic", vc);
            cat.csvFile = new File(csvDir, "transport/train/generic/data.csv");
        } else if (name.equals("energy_quantity")) {
            int[] vc = {1, 2, 3};//vc ={1};
            cat = new DataCategory("/home/energy/quantity", vc);
            cat.csvFile = new File(csvDir, "home/energy/quantity/data.csv");
        } else if (name.equals("uk_suppliers")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/uk/suppliers", vc);
            cat.csvFile = new File(csvDir, "home/energy/uk/suppliers/data.csv");
        } else if (name.equals("ireland_suppliers")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/ireland/suppliers", vc);
            cat.csvFile = new File(csvDir, "home/energy/ireland/suppliers/data.csv");
        } else if (name.equals("electricityISO")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/electricityISO", vc);
            cat.csvFile = new File(csvDir, "home/energy/electricityISO/data.csv");
        } else if (name.equals("uk_reductions")) {
            int[] vc = {1};
            cat = new DataCategory("/home/energy/uk/reductions", vc);
            cat.csvFile = new File(csvDir, "home/energy/uk/reductions/data.csv");
        }

        if (cat != null) {
            cat.saveName = name;
            cat.sourceName = "source";
            cat.backupDir = new File(csvDir, "backup");
        }
        return cat;
    }

    private static DataCategory getDEFRADataCategory(String name) {
        DataCategory cat = null;
        File csvDir = new File("/home/nalu/docs/amee/clients/defra/csvs");
        if (name.equals("fuel")) {
            int[] vc = {1, 2, 3};//vc ={1};
            cat = new DataCategory("/home/fuel", vc);
            cat.csvFile = new File(csvDir, "data-fuel.csv");
            cat.sourceName = "fuelSource";
        } else if (name.equals("fuel-price")) {
            int[] vc = {2};//vc ={1};
            cat = new DataCategory("/home/fuel_price", vc);
            cat.csvFile = new File(csvDir, "data-fuel-price.csv");
            cat.sourceName = "fuelPriceSource";
        } else if (name.equals("lighting")) {
            int[] vc = {1};//vc ={1};
            cat = new DataCategory("/home/lighting", vc);
            cat.csvFile = new File(csvDir, "data-lighting.csv");
            cat.sourceName = "lightingSource";
        } else if (name.indexOf("appliances") >= 0) {
            int[] vc = {3, 4};//vc ={1};
            cat = new DataCategory("/home/appliances", vc);
            cat.csvFile = new File(csvDir, "data-appliances.csv");
            cat.sourceName = "appliancesSource";
        } else if (name.equals("heating")) {
            int[] vc = {5};//vc ={1};
            cat = new DataCategory("/home/heating", vc);
            cat.csvFile = new File(csvDir, "data-heating.csv");
            cat.sourceName = "heatingSource";
        } else if (name.equals("cooking")) {
            int[] vc = {2};//vc ={1};
            cat = new DataCategory("/home/cooking", vc);
            cat.csvFile = new File(csvDir, "data-cooking.csv");
            cat.sourceName = "cookingSource";
        } else if (name.equals("transport")) {
            int[] vc = {4, 5};//vc ={1};
            cat = new DataCategory("/transport/transport", vc);
            cat.csvFile = new File(csvDir, "data-transport.csv");
            cat.sourceName = "transportSource";
        } else if (name.equals("tvs")) {
            int[] vc = {3};//vc ={1};
            cat = new DataCategory("/home/television", vc);
            cat.csvFile = new File(csvDir, "data-tvs.csv");
            cat.sourceName = "tvsSource";
        }

        if (cat != null) {
            cat.saveName = name;
            cat.backupDir = new File(csvDir, "backup");
            //cat.source = "Source";
            AmeeXMLHelper.dataItemUidXPath = "Resources/DrillDownResource/Choices/Choice";
        }
        return cat;
    }

    public static void main(String[] args) throws AmeeException {
        Main.setLogin(args[0]);
        Main.setPassword(args[1]);


        String path = args[2];
        String command = args[3]; // add, fetch, put
        String mode = args[4]; // itemdef, data
        String test = args[5]; // test only if true
        String siteString = args[6];
        String adminSiteString=args[7];
        int site = -1;
        if (siteString.equals("DEFRA")) {
            site = DEFRA;
        } else if (siteString.equals("STAGE")) {
            site = STAGE;
        } else if (siteString.equals("SCIENCE")) {
            site = SCIENCE;
        } else if (siteString.equals("LIVE")) {
            site = LIVE;
        } else if (siteString.equals("DEV")) {
            site = DEV;
        } else if (siteString.equals("SANDBOX")) {
            site = SANDBOX;
        } else if (siteString.equals("JB")) {
            site = JB;
        } else {
            site = FREE;
            ApiTools.freeHost=siteString;
            ApiTools.freeAdminHost=adminSiteString;
        }
        String localCSVROOT = args[8];
        ApiTools.init(site);
        ApiTools.csvDir = new File(localCSVROOT);
        boolean testMode = (test.equals("true"));
        System.out.println("Server:" + site);
        System.out.println("Path:" + path);
        System.out.println("Command:" + command);
        System.out.println("Mode:" + mode);
        System.out.println("Test:" + testMode);

        File csvDir = new File(ApiTools.csvDir, path);

        //Map<String,Charset> map = Charset.availableCharsets();
        //System.err.println("map="+map.keySet());
        //Charset charset = Charset.forName("UTF-8");

        /*String drill="type=majority%20lel%20bulbs";
        ArrayList<String> al = ApiTools.getDataItemsForDrill("/data"+path, drill);
        String uid = al.get(0);
        //String response = ApiTools.getDataItemFromUid(path, uid);
        String response = Main.sendRequest("GET /data"+path+"/"+uid,"");
        System.err.println(response);*/

        //Main.debug = true;
        boolean success = false;
        if (mode.equals("itemdef")) {
            System.out.println("Operating on item definition and algorithm");
            //NOTE: You CAN use updateInAPI now, but bear in mind:
            // - you MUST delete itemdefs manually - it won't do it for you
            // - it can't rename an existing item def - must be done manually
            ItemDefinitionSync.testMode = testMode; //don't actually change AMEE
            if (command.equals("add")) {
                System.out.println("Adding new item definition and algorithm");
                //Loads an item def from a local csv file
                File csvFile = new File(csvDir, "itemdef.csv");
                ItemDefinition id = ItemDefinition.fetchItemDefFromCSV(csvFile);
                id.algorithmName = "default";
                ItemDefinitionSync.isToUpdateAlgorithm = true;
                ItemDefinitionSync.isToUpdateDifferingValues = true;
                //ItemDefinitionSync.isToSetApiVersion1 = false; ajc 11sept09, versions handled properly now
                //ItemDefinitionSync.isToSetApiVersion2 = true;
                success = ItemDefinitionSync.createInAPI(site, id);
            } else if (command.equals("fetch")) {
                System.out.println("Fetch current item definition and algorithm");
                ItemDefinition id = ItemDefinition.fetchItemDefFromAPI(site, path);
                System.err.println("id:\n" + id);
                System.err.println("save=" + id.save(csvDir));
                success = true;
            } else if (command.equals("put")) {

                File csvFile = new File(csvDir, "itemdef.csv");
                ItemDefinitionSync.isToUpdateAlgorithm = true;
                ItemDefinitionSync.isToUpdateDifferingValues = true;
                //ItemDefinitionSync.isToSetApiVersion1 = false; ajc 11sept09, versions handled properly now
                //ItemDefinitionSync.isToSetApiVersion2 = true;
                success = ItemDefinitionSync.updateInAPI(site, csvFile, path);
            }
            System.err.println("result = " + success);
        } else if (mode.equals("data")) {
            System.out.println("Operating on data items");
            DataCategory cat = DataCategory.getDataCategory(path);
            if (command.equals("fetch")) {
                System.err.println("Data fetch not supported");
                return;
            } else {
                if (command.equals("add")) {
                    System.out.println("Adding new data");
                    cat.setDefaultAction(DataCategory.POST);//set this if you know the data items aren't in the db
                }
                //cat.setLimit(5); //Only 10 entries will be loaded from local data file
                //cat.setStart(5); //Start from nth entry, default is 1
                //cat.setUseCache(true);
                cat.useItemDef = true; //load/save item def? otherwise use simple old system
                cat.doBackup = false; //saves to backup in csv dir
                cat.checkSource = true; //check the source fields? make sure there's a dummy "units" col before source!
                cat.isStrict = true; //ABORT if certain problems occur, e.g. no source is given
                cat.doBatch = true; //batch things up in xml and send in one go at the end
                //Main.charSetName="UTF-8";
                //Note, if any of the data contains double quotes, replace each one with _QUOTE_
                //in the CSVs and it will be converted to a double quotes after loading.
                cat.loadLocalData();
                //ApiTools.charSetName="ISO-8859-1";
                //just check, don't alter AMEE
                if (testMode) {
                    cat.checkAPI();
                } else {
                    cat.updateAPI();
                }
            }
        }
    }
}
