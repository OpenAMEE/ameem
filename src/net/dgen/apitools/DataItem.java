/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dgen.apitools;

import com.amee.client.AmeeException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dgen.apiexamples.AmeeXMLHelper;
import net.dgen.apiexamples.Main;

/**
 *
 * @author nalu
 */
public class DataItem {
    public static String AMPERSAND="_AMPERSAND_";
    public static int MAX_VALUE_LENGTH = 255;
    String key;
    private String[] localValues,  apiValues;
    String units, source, algorithm;
    boolean isDefault;
    String dataItemUid;
    Map keyMap = new LinkedHashMap();
    DataCategory dataCategory;

    public DataItem(DataCategory dataCategory, String key, String[] localValues, String units, String source, String algorithm, String isDefault) {
        try {
            this.dataCategory = dataCategory;
            this.key = key;
            this.localValues = localValues;
            this.units = units;
            this.source = source;
            this.algorithm = algorithm;
            this.isDefault = Boolean.getBoolean(isDefault);
            String[] drillChoices = key.split("&");
            for (int i = 0; i < dataCategory.drillNames.length; i++) {
                keyMap.put(dataCategory.drillNames[i], drillChoices[i].replaceAll(AMPERSAND, "&"));
            }
        } catch (Exception e) {
            System.err.println("Error reading data row: " + e);
        }
    }

    public boolean isOK() {
        /* This restriction is lifted now on stage.
        for(int i=0;i<localValues.length;i++){
        if(localValues[i].length()>MAX_VALUE_LENGTH){
        System.err.println("WARNING: This item value is over the max length of "+MAX_VALUE_LENGTH+":");
        System.err.println(dataCategory.itemNames[i] + "=" + localValues[i]);
        localValues[i]=localValues[i].substring(0,MAX_VALUE_LENGTH-5);
        return false;
        }
        }*/
        return true;
    }

    public String getCSVLine(int where) {
        String s = "";

        String[] drillChoices = key.split("&");
        for (int i = 0; i < drillChoices.length; i++) {
            s += quotify(drillChoices[i]) + ",";
        }

        Object[] values;
        if (where == DataCategory.LOCAL) {
            values = localValues;
        } else {
            values = apiValues;
        }
        for (int i = 0; i < values.length; i++) {
            s += quotify(""+values[i]) + ",";
        }
        //NOTE: this backup doesn't work properly if where==API cos these values below
        //are always read from the CSV.
        s += quotify(units) + "," + quotify(source) + "," + quotify(algorithm) + ",";

        return s;
    }

    /** Replaces double quotes inside the given strong and places a double
     *  quote at each end and returns it.
     * @param s
     * @return
     */
    static String quotify(String s){
        s=s.replaceAll("\"", DataCategory.QUOTE);
        s="\""+s+"\"";
        return s;
    }

    /** Loads values from the API.
     *  @return A suggested action, POST if item wasn't present in API */
    int loadValuesFromAPI() throws AmeeException {
        int action = DataCategory.NOTHING;
        apiValues = new String[dataCategory.itemNames.length];
        String response;
        if (dataCategory.useDataItemUidCache) {
            dataItemUid = (String) dataCategory.cacheMap.get(key);
        }
        if (dataItemUid == null) {
            response = ApiTools.getDataItemFromKeyMap(dataCategory.apiPath, keyMap);
            if (ApiTools.lastDataItemUid != null) {
                dataItemUid = ApiTools.lastDataItemUid;
                ApiTools.lastDataItemUid = null;
                if (dataCategory.useDataItemUidCache) {
                    dataCategory.cacheMap.put(key, dataItemUid);
                }
            }
        } else {
            System.err.println("    " + "Got uid from cache");
            response = ApiTools.getDataItemFromUid(dataCategory.apiPath, dataItemUid);
        }
        if (response == null) {//Couldn't find the item, suggest a POST
            action = DataCategory.POST;
        } else {
            Map map = AmeeXMLHelper.getDataItemValues(response);
            for (int i = 0; i < dataCategory.itemNames.length; i++) {
                if (!map.containsKey(dataCategory.itemNames[i])) {
                    System.err.println("ERROR: this value isn't present in AMEE xml: " + dataCategory.itemNames[i]);
                }
                String itemValue = map.get(dataCategory.itemNames[i]).toString();
                apiValues[i] = itemValue;
                if (isDifferent(i)) {//Difference, suggest a PUT
                    action = DataCategory.PUT;
                    System.err.println("this value differs" + i + " = " + dataCategory.itemNames[i]);
                    System.err.println("local value=" + localValues[i]);
                    System.err.println("api value=" + apiValues[i]);
                }
            }
            if (dataCategory.checkSource && source.equals(map.get(dataCategory.sourceName)) == false) {
                if (action == DataCategory.PUT) {
                    action = DataCategory.PUT_BOTH;
                } else {
                    action = DataCategory.PUT_SOURCE;
                }
            }
            if (dataCategory.doBackup) {
                try {
                    dataCategory.backupWriter.write(getCSVLine(DataCategory.API) + "\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return action;
    }

    private boolean isDifferent(int i) {
        return !apiValues[i].equals(localValues[i]);
    }

    String getUpdatedValues(int action) {
        String s = "";
        if (action == DataCategory.PUT_SOURCE || action == DataCategory.PUT_BOTH) {
            s += dataCategory.sourceName + "=" + Main.urlEncode(source);
        }
        if (action != DataCategory.PUT_SOURCE) {
            for (int i = 0; i < dataCategory.itemNames.length; i++) {
                if (isDifferent(i)) {
                    if (s.length() > 0) {
                        s += "&";
                    }
                    s += dataCategory.itemNames[i] + "=" + localValues[i];
                }
            }
        }
        return s;
    }

    String getPostString() {
        String postString = "newObjectType=DI&" + AmeeXMLHelper.getDrillString(keyMap);
        postString += "&" + dataCategory.sourceName + "=" + Main.urlEncode(source);
        for (int i = 0; i < dataCategory.itemNames.length; i++) {
            postString += "&" + Main.urlEncode(dataCategory.itemNames[i]) + "=" + localValues[i];
        }
        return postString;
    }


    private String xmlEncode(String s){
        s=s.replaceAll("&","&amp;");
        s=s.replaceAll("<","&lt;");
        s=s.replaceAll(">","&gt;");

        return s;
    }

    String getPutXML(int action) {
        String xml = "<DataItem>\n";
        //Stuff doesn't need to be url encoded in XML
        xml += "<dataItemUid>" + dataItemUid + "</dataItemUid>\n";

        if (action == DataCategory.PUT_SOURCE || action == DataCategory.PUT_BOTH) {
            xml += "<" + dataCategory.sourceName + ">";
            xml += xmlEncode(source);
            xml += "</" + dataCategory.sourceName + ">\n";
        }

        if (action != DataCategory.PUT_SOURCE) {
            for (int i = 0; i < dataCategory.itemNames.length; i++) {
                if (isDifferent(i)) {
                    xml += "<" + dataCategory.itemNames[i] + ">";
                    xml += xmlEncode(localValues[i]);
                    xml += "</" + dataCategory.itemNames[i] + ">\n";
                }
            }
        }
        xml += "</DataItem>\n";

        return xml;
    }

    String getPostXML() {
        String xml = "<DataItem>\n";
        //Stuff doesn't need to be url encoded in XML
        Iterator<String> iter = keyMap.keySet().iterator();
        while (iter.hasNext()) {
            String drillName = iter.next();
            xml += "<" + drillName + ">";
            xml += xmlEncode(keyMap.get(drillName).toString());
            xml += "</" + drillName + ">\n";
        }
        xml += "<" + dataCategory.sourceName + ">";
        xml += xmlEncode(source);
        xml += "</" + dataCategory.sourceName + ">\n";
        for (int i = 0; i < dataCategory.itemNames.length; i++) {
            xml += "<" + dataCategory.itemNames[i] + ">";
            xml += xmlEncode(localValues[i]);
            xml += "</" + dataCategory.itemNames[i] + ">\n";
        }
        xml += "</DataItem>\n";

        return xml;
    }

    public String toString() {
        String vs = "localValues=";
        for (int i = 0; i < dataCategory.itemNames.length; i++) {
            vs += dataCategory.itemNames[i] + "=" + localValues[i];
        }
        vs += "\napiValues=";
        if (apiValues == null) {
            vs += "NOT LOADED YET, CALL checkAPI() to load API data";
        } else {
            for (int i = 0; i < dataCategory.itemNames.length; i++) {
                vs += dataCategory.itemNames[i] + "=" + apiValues[i];
            }
        }
        vs += "\n";
        return vs + "units=" + units + "source=" + source + "alg=" + algorithm + "isDefault=" + isDefault;
    }
}
