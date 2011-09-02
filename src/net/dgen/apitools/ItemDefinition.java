/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.twicom.qdparser.TaggedElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dgen.apiexamples.AmeeXMLHelper;
import net.dgen.apiexamples.Main;

/**
 *
 * v2 t2d
 * - itemdef.csv load/save
 * - API creating
 * - API updating
 * - API fetching
 * Class that represents an AMEE item def. It can load details either from
 * the API or a CSV file.
 * @author nalu
 */
public class ItemDefinition {
    public static final String NOTSET = "___NOTSET___";
    String path;//e.g. /home/lighting
    String name;
    String dataItemDefUid;
    String drillDown = "";
    String algorithm, algorithmName = "default", algorithmUid;
    Map<String, ValueDefinition> valueMap = new LinkedHashMap();
    Map<String, ValueDefinition> valueMapByUid = new LinkedHashMap();
    private boolean isCsvVersion2 = true;

    class DrillComparator implements Comparator<ValueDefinition> {

        // Comparator interface requires defining compare method.
        public int compare(ValueDefinition vd1, ValueDefinition vd2) {
            int i1 = drillDown.indexOf(vd1.path);
            int i2 = drillDown.indexOf(vd2.path);
            if (i1 >= 0 && i2 >= 0) {//both drill downs
                return i1 - i2;
            } else if (i1 >= 0) {//vd1 is a drill down
                return -1;
            } else if (i2 >= 0) {//vd2 is a drill down {
                return 1;
            } else if (vd1.fromData && !vd1.fromProfile) {//pure data item values first
                return -1;
            } else if (vd2.fromData && !vd2.fromProfile) {
                return 1;
            } else if (vd1.fromProfile && !vd1.fromData) {//pure profile item values last
                return 1;
            } else if (vd2.fromProfile && !vd2.fromData) {
                return -1;
            } else {//alphabetical sort
                return vd1.path.compareTo(vd2.path);
            }
        }
    }

    public class ValueDefinition {
        private boolean fromData,  fromProfile;
        String name, path;
        String type; //see ItemDefinitionSync.SERVERValueDefMap for possible options
        String uid;
        String defaultValue = NOTSET;
        String choices = NOTSET;
        //v2.0 features
        String unit = NOTSET, perUnit = NOTSET;
        String versions = NOTSET;
        String aliasedTo = NOTSET;

        public ValueDefinition(String name, String path, String type, String dataProfileValue, boolean isDrill) {
            this.name = name;
            this.path = path;
            this.type = type;
            setDataItemValue(dataProfileValue);
            if (isDrill) {
                addToDrill(path);
            }
        }

        /** Creates the def from a csv file */
        public ValueDefinition(String csv) {
            //System.err.println(csv);
            String[] csvSs = csv.split(",");
            String[] ss;
            if (csvSs.length < 8) {
                ss = new String[8];
                for (int i = 0; i < ss.length; i++) {
                    if (i < csvSs.length) {
                        ss[i] = csvSs[i];
                    } else {
                        ss[i] = NOTSET;
                    }
                }
            } else {
                ss = csvSs;
            }


            name = ss[0].trim();
            path = ss[1].trim();
            type = ss[2].trim();
            setDataItemValue(ss[3].trim());
            boolean isDrill = Boolean.parseBoolean(ss[4].trim());
            if (isDrill) {
                addToDrill(path);
            }
//          bw.write("name,path,type,isDataItemValue,isDrillDown,unit,perUnit,default,choices\n");
            int defaultCol = 7;
            if (isCsvVersion2) {
                unit = ss[5].trim();
                if (unit.length() == 0) {
                    unit = NOTSET;
                }
                perUnit = ss[6].trim();
                if (perUnit.length() == 0) {
                    perUnit = NOTSET;
                }
            } else {
                defaultCol = 5;
            }
            if (ss.length > defaultCol) {//default is optional
                defaultValue = ss[defaultCol].trim();
                if (defaultValue.length() == 0) {
                    defaultValue = NOTSET;
                }
                if (ss.length > defaultCol + 1) {//choices is optional, but can have arbitrary fields in CSV
                    choices = "";
                    for (int i = defaultCol + 1; i < ss.length; i++) {
                        if (i > defaultCol + 1) {
                            choices += ",";
                        }
                        choices += ss[i].trim();
                        if (ss[i].indexOf(NOTSET) >= 0) {
                            break;
                        }
                    }
                }
            }
        }

        private String getCSVLine() {
            String s = name + "," + path + "," + type;
            boolean isDrill = false;
            String[] dd = drillDown.split(",");
            for (int i = 0; i < dd.length; i++) {
                if (path.equals(dd[i])) {
                    isDrill = true;
                }
            }
            s += "," + isDataItemValue() + "," + isDrill;

            s += "," + unit + "," + perUnit;
            s += "," + defaultValue + "," + choices;
            s = s.replaceAll(NOTSET, "");

            return s;
        }

        private String getVersionLine() {
            String s = path + "," + versions + "," + aliasedTo;
            s = s.replaceAll(NOTSET, "");

            return s;
        }

        public String toString() {
            String s = "(";
            s += getCSVLine();
            s += ") (" + getVersionLine();
            s += ")";
            return s;
        }

        /** Create from xml. */
        private ValueDefinition(TaggedElement te) {
            uid = te.getAttribute("uid");
            path = te.find("Path").getChild(0).toString();
            name = te.find("Name").getChild(0).toString();
            fromData = Boolean.parseBoolean(te.find("FromData").getChild(0).toString());
            fromProfile = Boolean.parseBoolean(te.find("FromProfile").getChild(0).toString());
            type = te.find("ValueDefinition").find("ValueType").getChild(0).toString();
        }

        /** Loads the defaultValue and choices. */
        private boolean loadFromAPI() {
            boolean success = false;
            String request = "GET /admin/itemDefinitions";
            request += "/" + dataItemDefUid + "/itemValueDefinitions/" + uid;

            String response = Main.sendRequest(request, "");
            success = ItemDefinitionSync.isResponseOK(response);
            //System.err.println("ivd detail xml:\n" + response);
            ArrayList<TaggedElement> al = AmeeXMLHelper.getElement(response, "ItemValueDefinition");
            TaggedElement te = al.get(0);

            TaggedElement valueElement = te.find("Value");
            if (valueElement.hasElements()) {
                defaultValue = valueElement.getChild(0).toString();
            }

            TaggedElement choicesElement = te.find("Choices");
            if (choicesElement != null && choicesElement.hasElements()) {
                choices = choicesElement.getChild(0).toString();
            }

            TaggedElement unitElement = te.find("Unit");
            if (unitElement != null && unitElement.hasElements()) {
                unit = unitElement.getChild(0).toString();
            }

            unitElement = te.find("PerUnit");
            if (unitElement != null && unitElement.hasElements()) {
                perUnit = unitElement.getChild(0).toString();
            }

            TaggedElement vte = te.find("APIVersions");
            versions = "";
            for (int i = 0; i < vte.elements(); i++) {
                TaggedElement vtei = (TaggedElement) vte.getChild(i);
                String sv = vtei.getChild(0).toString();
                versions += sv;
                if (i < (vte.elements() - 1)) {
                    versions += "|";
                }
            }

            String att = te.find("AliasedTo").getAttribute("uid");
            if (att != null) {
                aliasedTo = att;
            }

            return success;
        }

        /** NOTE: It's either a data item value or a profile item value.
         * or both, i.e. never neither. */
        String isDataItemValue() {
            if (fromData && fromProfile) {
                return "both";
            } else if (fromData) {
                return "true";
            } else {
                return "false";
            }
        }

        private void setDataItemValue(String s) {
            if (s.equalsIgnoreCase("both")) {
                fromData = true;
                fromProfile = true;
            } else {
                boolean b = Boolean.parseBoolean(s);
                fromData = b;
                fromProfile = !b;
            }
        }

        String getValueDefinitionUid() {
            return ApiTools.valueDefMap.get(type);
        }

        public boolean isTheSameAs(ValueDefinition vd) {
            String diffString = "";//if this remains blank then there's no diff
            if (path.compareTo(vd.path) != 0) {
                System.err.println("SHOULD NEVER GET HERE COS IVDs ARE MATCHED ON path!!!");
            }
            if (type.compareTo(vd.type) != 0) {
                System.err.println("WARNING: types differ for " + vd.path + " - THIS CODE CAN'T UPDATE THIS - DO IT MANUALLY");
            }
            if (name.compareTo(vd.name) != 0) {
                diffString += ("|" + name + ":name:" + vd.name);
            }
            if (fromData != vd.fromData) {
                diffString += ("|" + fromData + ":fromData:" + vd.fromData);
            }
            if (fromProfile != vd.fromProfile) {
                diffString += ("|" + fromProfile + ":fromProfile:" + vd.fromProfile);
            }
            if (defaultValue.compareTo(vd.defaultValue) != 0) {
                diffString += ("|" + defaultValue + ":defaultValue:" + vd.defaultValue);
            }
            if (choices.compareTo(vd.choices) != 0) {
                diffString += ("|" + choices + ":choices:" + vd.choices);
            }
            if (unit.compareTo(vd.unit) != 0) {
                diffString += ("|" + unit + ":unit:" + vd.unit);
            }
            if (perUnit.compareTo(vd.perUnit) != 0) {
                diffString += ("|" + perUnit + ":perUnit:" + vd.perUnit);
            }
            if (versions.compareTo(vd.versions) != 0) {
                diffString += ("|" + versions + ":versions:" + vd.versions);
            }
            if (aliasedTo.compareTo(vd.aliasedTo) != 0) {
                System.err.println("WARNING: AliasedTo differ for path=" + path + ": this must be fixed manually.");
            }
            if (diffString.length() == 0) {
                return true;
            } else {
                System.err.println("Differences |API-value:property:CSV-value| found in IVD with path=" + path + ":\n" + diffString);
                return false;
            }
        }
    }

    private ItemDefinition(File csvFile) {
        loadFromCSV(csvFile);
    }

    ItemDefinition() {
    }

    private ItemDefinition(String path) {
        this.path = path;
    }

    void addValue(String name, String path, String type, String dataProfileValue, boolean isDrill) {
        valueMap.put(path, new ValueDefinition(name, path, type, dataProfileValue, isDrill));
    }

    private void addToDrill(String path) {
        if (drillDown.length() == 0) {
            drillDown += path;
        } else {
            drillDown += "," + path;
        }
    }

    /** Returns ValueDefinitions where the path matches between this and itemDef,
     *  but the itemDef differs in any other way.
     */
    Map<String, ValueDefinition> getDifferingValueDefinitions(ItemDefinition itemDef) {
        Map<String, ValueDefinition> map = new HashMap();
        Iterator<String> iter = itemDef.valueMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            ValueDefinition vd = itemDef.valueMap.get(key);
            ValueDefinition vdThis = valueMap.get(key);
            if (vdThis != null && !vd.isTheSameAs(vdThis)) {
                map.put(vd.uid, vdThis);
            }
        }
        return map;
    }

    /** Returns ValueDefinitions that are in itemDef, but not in this. */
    HashSet<ValueDefinition> getMissingValueDefinitions(ItemDefinition itemDef) {
        HashSet<ValueDefinition> missingSet = new HashSet();
        Iterator<String> iter = itemDef.valueMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            ValueDefinition vd = itemDef.valueMap.get(key);
            ValueDefinition vdThis = valueMap.get(key);
            //System.err.println("vdThis="+vdThis);
            if (vdThis == null) {// CATERED FOR IN DifferingValueDefinitions || !vd.isTheSameAs(vdThis)) {
                missingSet.add(vd);
            }
        }
        return missingSet;
    }

    private String fetchDefinitionUid() {
        String request;
        request = "GET /data" + path;
        String response = Main.sendRequest(request, "");
        if (response.indexOf("404 NOT_FOUND") >= 0) {
            System.err.println("NOT FOUND: " + request);
            return null;
        }

        ArrayList al = AmeeXMLHelper.getElement(response, "ItemDefinition");
        if (!al.isEmpty()) {
            TaggedElement te = (TaggedElement) al.get(0);
            dataItemDefUid = te.getAttribute("uid");
        } else {
            return null;
        }

        //System.err.println("get def uid = " + ItemDefinitionSync.isResponseOK(response));

        return dataItemDefUid;
    }

    private boolean loadFromCSV(File file) {
        boolean success = false;
        BufferedReader br = ApiTools.getBufferedReader(file);
        try {
            String line = br.readLine();
            String[] ss = line.split(",");
            name = ss[1].trim();
            line = br.readLine();
            ss = line.split(",");
            success = loadAlgorithmFromFile(new File(file.getParent(), ss[1].trim()));
            valueMap = new LinkedHashMap();
            drillDown = "";
            line = br.readLine();//skip value def header line
            if (line.indexOf("unit") < 0) {
                isCsvVersion2 = false;
            }
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    ValueDefinition vd = new ValueDefinition(line);
                    valueMap.put(vd.path, vd);
                }
            }
            br.close();
            loadVersionFile(new File(file.getParent(), "version.csv"));
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean loadVersionFile(File file) {
        boolean success = false;
        if (!file.exists()) {//if file doesn't exist, just assume all v2
            System.err.println("WARNING: v2 will be assumed in all IVDs as this file doesn't exist: " + file.getName());
            success = false;
        } else {
            BufferedReader br = ApiTools.getBufferedReader(file);
            try {
                String line = br.readLine();//skip header line
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        String[] ss = line.split(",");
                        ValueDefinition vd = valueMap.get(ss[0].trim());
                        if (vd == null) {
                            System.err.println("ERROR: " + file.getName() + " specifies an IVD that is not in itemdef.csv: path=" + ss[0]);
                            System.exit(666);
                        } else {
                            if (ss.length > 1 && ss[1].trim().length() > 0) {
                                vd.versions = ss[1].trim();
                            }

                            if (ss.length > 2 && ss[2].trim().length() > 0) {
                                vd.aliasedTo = ss[2].trim();
                            }
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("ERROR: problem reading " + file.getName());
                System.exit(667);

                success = false;
            }
        }
        Iterator<ValueDefinition> vdIter = valueMap.values().iterator();
        while (vdIter.hasNext()) {
            ValueDefinition vd = vdIter.next();
            if (vd.versions.equals(NOTSET)) {
                System.err.println("WARNING: version for " + vd.path + " is not defined in " + file.getName() + ": assuming 2.0 ONLY");
                vd.versions = "2.0";
            }
        }
        return success;
    }

    boolean save(File dir) {
        boolean success = true;
        File csvFile = new File(dir, "itemdef.csv");
        File versionFile = new File(dir, "version.csv");
        File algFile = new File(dir, algorithmName + ".js");
        try {
            BufferedWriter bw = ApiTools.getBufferedWriter(csvFile);
            BufferedWriter vbw = ApiTools.getBufferedWriter(versionFile);
            bw.write("name," + name + "\n");
            bw.write("algFile," + algFile.getName() + "\n");
            bw.write("name,path,type,isDataItemValue,isDrillDown,unit,perUnit,default,choices\n");
            vbw.write("path,versions,aliasedTo\n");
            Iterator<ValueDefinition> iter = valueMap.values().iterator();
            while (iter.hasNext()) {
                ValueDefinition vd = iter.next();
                bw.write(vd.getCSVLine() + "\n");
                vbw.write(vd.getVersionLine() + "\n");
            }
            bw.close();
            bw = ApiTools.getBufferedWriter(algFile);
            //write the alg
            if (algorithm != null) {
                bw.write(algorithm.replaceAll("(\r)", ""));
            } else {
                bw.write("//no alg in API\n");
            }
            bw.close();
            vbw.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean loadAlgorithmFromFile(File file) {
        boolean success = false;
        BufferedReader br = ApiTools.getBufferedReader(file);
        algorithm = "";
        try {
            String line;
            while ((line = br.readLine()) != null) {
                algorithm += line + "\n";
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        if (algorithm.length() > 0) {
            algorithm = fullTrim(algorithm);
        }
        return success;
    }

    private boolean loadFromAPI() {
        boolean success = false;
        String request;
        request = "GET /admin/itemDefinitions";
        request += "/" + dataItemDefUid;

        String response = Main.sendRequest(request, "");
        success = ItemDefinitionSync.isResponseOK(response);
        //System.err.println("id xml:\n" + response);
        ArrayList al = AmeeXMLHelper.getElement(response, "ItemDefinition");
        TaggedElement te = (TaggedElement) al.get(0);
        name = te.find("Name").getChild(0).toString();
        //System.err.println("name=" + name);
        TaggedElement ddte = te.find("DrillDown");
        if (ddte.hasElements()) {
            drillDown = te.find("DrillDown").getChild(0).toString();
        } else {
            System.err.println("WARNING: no drill down specfied in API - OK if it's the metadata item.");
        }
        //System.err.println("drillDown=" + drillDown);

        success = loadValuesFromAPI();
        success = loadAlgorithmFromAPI();
        return success;
    }

    private boolean loadValuesFromAPI() {
        boolean success = false;
        String request = "GET /admin/itemDefinitions";
        request += "/" + dataItemDefUid + "/itemValueDefinitions";

        String response = Main.sendRequest(request, "");
        success = ItemDefinitionSync.isResponseOK(response);
        //System.err.println("ivd xml:\n" + response);

        ArrayList<ValueDefinition> vdlist = new ArrayList<ValueDefinition>();
        ArrayList<TaggedElement> al = AmeeXMLHelper.getElement(response, "ItemValueDefinition");
        Iterator<TaggedElement> iter = al.iterator();
        while (iter.hasNext()) {
            TaggedElement te = iter.next();
            ValueDefinition vd = new ValueDefinition(te);
            success = vd.loadFromAPI();//loads default and choices, not in this level of xml
            if (!success) {
                System.err.println("ABORTING: Failed to load default and choices from API");
                break;
            }
            vdlist.add(vd);
        //valueMap.put(vd.path, vd);
        //System.err.println("vd  = " + vd);
        }
        Collections.sort(vdlist, new DrillComparator());
        Iterator<ValueDefinition> vditer = vdlist.iterator();
        while (vditer.hasNext()) {
            ValueDefinition vd = vditer.next();
            valueMap.put(vd.path, vd);
            valueMapByUid.put(vd.uid, vd);
        }
        translateAliasedToUids();

        return success;
    }

    /** AliasedTo in the platform is stored as a UID, but in the CSV files
     * and in this cose it is stored as the IVD's path.
     */
    private void translateAliasedToUids() {
        Iterator<ValueDefinition> vditer = valueMapByUid.values().iterator();
        while (vditer.hasNext()) {
            ValueDefinition vd = vditer.next();
            if (!vd.aliasedTo.equals(NOTSET)) {
                ValueDefinition vda = valueMapByUid.get(vd.aliasedTo);
                vd.aliasedTo = vda.path;
            }
        }
    }

    private boolean loadAlgorithmFromAPI() {
        boolean success = false;
        String request = "GET /admin/itemDefinitions";
        request += "/" + dataItemDefUid + "/algorithms";

        String response = Main.sendRequest(request, "");
        success = ItemDefinitionSync.isResponseOK(response);

        ArrayList<TaggedElement> al = AmeeXMLHelper.getElement(response, "Algorithm");
        Iterator<TaggedElement> iter = al.iterator();
        while (iter.hasNext()) {
            TaggedElement te = iter.next();
            if (te.find("Name").getChild(0).toString().equals(algorithmName)) {
                algorithm = te.find("Content").getChild(0).toString();
                algorithmUid = te.getAttribute("uid");
            //System.err.println("algorithm  = " + algorithm);
            }
        }
        if (algorithm != null) {
            algorithm = fullTrim(algorithm);
        }
        return success;
    }

    /** Trims and removes trailing returns. */
    private static String fullTrim(String s) {
        s = s.trim();
        int i;
        for (i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) != '\n') {
                break;
            }
        }
        s = s.substring(0, i + 1);
        return s;
    }

    public static ItemDefinition fetchItemDefFromCSV(File csvFile) {
        return new ItemDefinition(csvFile);
    }

    /** This method should be used to fetch the item def from the API.
     *  @return null if path has no data item def
     */
    public static ItemDefinition fetchItemDefFromAPI(int site, String path) {
        boolean saveIsAdmin = ApiTools.isAdmin;
        int saveSite = ApiTools.currentSite;

        ApiTools.isAdmin = false;
        ApiTools.init(site);

        //testMode = true;
        ItemDefinition id = new ItemDefinition(path);
        id.fetchDefinitionUid();
        //System.err.println("get def uid = " + id.dataItemDefUid);

        if (id.dataItemDefUid == null) {
            id = null;
        } else {

            //System.err.println("create def success = " + dc.create());
            //dc.dataItemDefUid="9E2C2158CBCA";

            ApiTools.isAdmin = true;
            ApiTools.init(site);

            id.loadFromAPI();
        }

        //restore settings
        ApiTools.isAdmin = saveIsAdmin;
        ApiTools.init(saveSite);

        return id;
    }

    public String toString() {
        String s = "name=" + name + "\n";
        s += "drillDown=" + drillDown + "\n";
        s += "algorithmName=" + algorithmName + "\n";
        if (algorithm == null) {
            s += "algorithm is null\n";
        } else {
            s += "algorithm.length()=" + algorithm.length() + "\n";
        }
        Iterator<ItemDefinition.ValueDefinition> iter = valueMap.values().iterator();
        while (iter.hasNext()) {
            s += iter.next().toString() + "\n";
        }
        return s;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //ItemDefinition id = fetchItemDefFromAPI("/home/lighting");
        File dir = new File("/home/nalu/dev/amee/src/net/dgen/apitools");
        ItemDefinition id = new ItemDefinition(new File(dir, "itemDefTest.csv"));
        System.err.println("id = " + id);
        File saveDir = new File("/home/nalu/Desktop/itemdef");
        id.save(saveDir);
    }
}
