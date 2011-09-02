/*
 * This file is part of the AMEE java example code.
 *
 * The AMEE java example code is free software; you can redistribute it and/or mo
dify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * The AMEE php calculator is free software is distributed in the hope that it
will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package net.dgen.apiexamples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * To do:
 * - make case insensitive
 * - implement != for drill down and pi values
 * - NOT on whole condition with !at start before path
 * This class represents an ActOnCO2 action in AMEE.
 * It looks at the returned xml from AMEE (via the profile object)
 * and determines whether the action is triggered or not.
 * @author nalu
 */
public class ActOnCO2Action {

    /** An object that wraps the returned profile xml from AMEE */
    private TestProfiles profile;
    /** The drill down name of this action in the actions category in AMEE. */
    private String drillName;
    /** A list of conditions that must all be met for this action to be triggered. */
    private ArrayList<Condition> conditions = new ArrayList();
    /** All possible actions for ActOnCO2. */
    public static HashMap<String, ActOnCO2Action> actionMap = new HashMap();
    /** OR the conditions? Default is AND. */
    private boolean useOr = false;

    public ActOnCO2Action(String drillName) {
        this.drillName = drillName;
    }

    public void setProfile(TestProfiles profile) {
        this.profile = profile;
    }

    /**
     * Adds a condition for this action.
     * @param ameeCondition A string obtained from the actions category in amee.
     */
    public void addCondition(String ameeCondition) {
        Condition c = new Condition(ameeCondition);
        conditions.add(c);
    }

    /** Determines whether multiple conditions should be ANDed or ORed.
     * @param useOr If true (OR) , one or more conditions need to be met for isTriggered()
     * to return true. If false (AND), all conditions must be met.
     */
    public void setOr(boolean useOr) {
        this.useOr = useOr;
    }

    /**
     * profile must be set before this method is called.
     * @return true if all conditions are met.
     */
    public boolean isTriggered() {
        boolean isTriggered = !useOr;
        Iterator<Condition> iter = conditions.iterator();
        while (iter.hasNext()) {
            Condition c = iter.next();
            boolean isMet = c.isMet();
            if (isMet) {
                if (useOr) {
                    return true;
                }
            } else {
                if (!useOr) {
                    return false;
                }
            }
        }
        return isTriggered;
    }

    /** Returns the data item uid for a given path and drill down.
     *  At the moment it's just a dummy implementation. In practice this
     *  should use a drill down cache.
     * @param path e.g. /transport/car/generic
     * @param drill e.g. fuel=petrol&size=large
     * @return the data item uid
     */
    public static String getDataItemUid(String path, String drill) {
        String dataItemUid = null;
        if (path.equals("/home/energy/insulation")) {
            if (drill.equals("type=double glazing")) {
                dataItemUid = "25A63A9B6205";
            } else if (drill.equals("type=loft")) {
                dataItemUid = "E1DBC8B559DB";
            }
        } else if (path.equals("/home/appliances/computers/generic")) {
            if (drill.equals("device=Personal Computers&rating=Laptop")) {
                dataItemUid = "6F5DFCA1CA50";
            }
        } else if (path.equals("/home/lighting")) {
            if (drill.equals("type=normal")) {
                dataItemUid = "2ACB6277A603";
            } else {//lel
                dataItemUid = "C3220A6DE793";
            }
        } else if (path.equals("/home/appliances/entertainment/generic")) {
            if (drill.equals("device=PSU&rating=typical")) {
                dataItemUid = "FD0A14443137";
            }
        } else if (path.equals("/metadata")) {
            dataItemUid = "86D02FBD95AE";
        } else {
            System.err.println("Couldn't find dataItemUid for: " + path + ":" + drill);
        }

        return dataItemUid;
    }

    /** Returns the drill down for a given path and dataItemUid.
     *  At the moment it's just a dummy implementation. In practice this
     *  should use a drill down cache.
     * @param path e.g. /transport/car/generic
     * @param dataItemUid The AMEE dataItemUid
     * @return a string representing the drill down, e.g. e.g. fuel=petrol&size=large
     */
    public static String getDrillDown(String path, String dataItemUid) {
        String drill = null;
        if (path.equals("/home/heating")) {
            if (dataItemUid.equals("EFDCB00772C4")) {
                drill = "homeDwellingType=detached house&homeNoOfBedrooms=4"; //incomplete, but just for testing
            }
        } else if (path.equals("/home/energy/insulation")) {
            if (dataItemUid.equals("25A63A9B6205")) {
                drill = "type=double glazing";
            }
        } else if (path.equals("/home/appliances/kitchen/generic")) {
            if (dataItemUid.equals("6353FF5052D2")) {
                drill = "rating=F&age=-&temperature=-";
            } else if (dataItemUid.equals("51B086EAF576")) {
                drill = "rating=Other&age=over 12 years&temperature=-";
            } else if (dataItemUid.equals("FFB27FED13E0")) {
                drill = "rating=A&age=-&temperature=-";
            }
        } else if (path.equals("/home/appliances/computers/generic")) {
            if (dataItemUid.equals("6F5DFCA1CA50")) {
                drill = "device=Personal Computers&rating=Laptop";
            }
        } else if (path.equals("/home/appliances/computers/generic")) {
            if (dataItemUid.equals("6F5DFCA1CA50")) {
                drill = "device=Personal Computers&rating=Laptop";
            }
        } else if (path.equals("/home/lighting")) {
            if (dataItemUid.equals("2ACB6277A603")) {
                drill = "type=normal";
            } else {//lel
                drill = "type=lel";
            }
        }
        return drill;
    }

    /** This class represents a condition that needs to be met for the action
     * to be triggered.
     */
    public class Condition {

        /** Complete drill down is specified and a profile item for it exists. */
        private static final int FULL_DRILL_EXISTS = 1;
        /** Complete drill down is specified and a profile item for it exists and has a profile item value set to a given value. */
        private static final int FULL_DRILL_PIVALUE = 2;
        /** Complete drill down is specified for two items and profile item values are compared; both items must exist. */
        private static final int FULL_DRILL_COMPARE = 3;
        /** Partial drill down is specified and at least one profile item matching it exists. */
        private static final int PD_DRILL_EXISTS = 101;
        /** No profile items are in the category. */
        private static final int CATEGORY_EMPTY = 201;
        /** The condition type. Must equal one of the constants defined in this class. */
        private int type;
        private String path;
        private String drill;
        private String dataItemUid;
        private String profileItemUid;
        private String pivName;
        private String pivValue;
        private boolean profileItemGreater = false;
        private Condition compareCondition;
        private boolean isNot = false;

        /** Sets up the condition based on the condition string as stored
         * in AMEE.
         * @param ameeCondition A condition string obtained from AMEE.
         */
        public Condition(String ameeCondition) {
            String[] ss = ameeCondition.split(":");
            if (ss[0].charAt(0) == '!') {
                isNot = true;
                path = ss[0].substring(1);
            } else {
                path = ss[0];
            }
            drill = ss[1];
            if (drill.equals("EMPTY")) {
                type = CATEGORY_EMPTY;
            } else if (ss.length >= 3) {//conditions which test profile item values
                String[] pp;
                if (ss[2].indexOf('>') >= 0) {
                    pp = ss[2].split(">");
                    profileItemGreater = true;
                } else {
                    pp = ss[2].split("=");
                }
                pivName = pp[0];
                if (pp.length == 2) {
                    pivValue = pp[1];
                }

                if (ss.length == 3) {
                    type = FULL_DRILL_PIVALUE;
                } else {
                    type = FULL_DRILL_COMPARE;
                    compareCondition = new Condition(ss[4] + ":" + ss[5] + ":" + ss[6]);
                }
            } else if (ss.length == 2) {//condition only on drill, not pi values
                if (ss[1].indexOf('*') < 0 && ss[1].indexOf('|') < 0) {//complete drill
                    type = FULL_DRILL_EXISTS;
                } else {//partial drill
                    type = PD_DRILL_EXISTS;
                }
            } else {
                System.err.println("Should never get here!");
            }
        }

        /** Fetches the dataItemUid and the profileItemUid of the first profile
         *  item based on that data item.
         */
        private void getUids() {
            dataItemUid = getDataItemUid(path, drill);
            profileItemUid = profile.getProfileItemUid(path, dataItemUid);
        //System.err.println("dataItemUid=" + dataItemUid);
        //System.err.println("profileItemUid=" + profileItemUid);
        }

        /** Determines whether this condition is met.
         * @return true if condition is met. false if condition isn't met or one
         *  of the profile items involved in the condition doesn't exist.
         */
        public boolean isMet() {
            boolean result = isMetPrivate();
            if (isNot) {
                return !result;
            } else {
                return result;
            }
        }

        private boolean isMetPrivate() {
            if (type == CATEGORY_EMPTY) {
                //System.err.println("count = "+profile.getProfileItemCount(path));
                if (profile.getProfileItemCount(path) == 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (type < 100) {//full drill down is given, so get dataItemUid
                dataItemUid = getDataItemUid(path, drill);
                if (type == FULL_DRILL_EXISTS) {
                    Map<String, String> map = profile.getProfileItems(path);
                    return map.containsValue(dataItemUid);
                } else if (type == FULL_DRILL_PIVALUE) {
                    Map<String, String> map = profile.getProfileItems(path);
                    Iterator<String> iter = map.keySet().iterator();
                    while (iter.hasNext()) {
                        String puid = iter.next();
                        if (drill.equals("ANY") || map.get(puid).equals(dataItemUid)) {
                            String value = profile.getItemValue(path, puid, pivName);
                            if (value != null && profileItemGreater) {
                                try {
                                    double dAMEE = Double.parseDouble(value);
                                    double d = Double.parseDouble(pivValue);
                                    if(dAMEE>d){
                                        return true;
                                    }
                                } catch (NumberFormatException numberFormatException) {
                                    System.err.println("Can't parse " + pivName + " from " + path + "/" + puid);
                                }
                            }
                            else if (value != null && pivValue.equalsIgnoreCase(value)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (type == FULL_DRILL_COMPARE) {
                    profileItemUid = profile.getProfileItemUid(path, dataItemUid);
                    if (profileItemUid == null) {//doesn't exist
                        return false;
                    }
                    //only supports GREATER comparison
                    double d1 = getNumericProfileItemValue();
                    compareCondition.getUids();
                    if (compareCondition.profileItemUid == null) {//doesn't exist
                        return false;
                    }
                    double d2 = compareCondition.getNumericProfileItemValue();
                    return d1 > d2;
                }
            } else {//PD_DRILL_EXISTS
                Map<String, String> map = profile.getProfileItems(path);
                Iterator<String> iter = map.values().iterator();
                while (iter.hasNext()) {
                    String diuid = iter.next();
                    String drill1 = getDrillDown(path, diuid);
                    if (doesDrillMatch(drill1)) {
                        return true;
                    }
                }
                return false;
            }
            return false; //shouldn't get here
        }

        /**
         * Sees if the supplied drill matches the pattern stored in drill.
         * @param drill1
         * @return true if the drill pattern matches fullDrill
         */
        private boolean doesDrillMatch(String drill1) {
            Map<String, String> map = getDrillMap(drill);
            Map<String, String> map1 = getDrillMap(drill1);

            Iterator<String> iter1 = map1.keySet().iterator();
            while (iter1.hasNext()) {
                String drillName = iter1.next();
                String value1 = map1.get(drillName);
                String pattern = map.get(drillName);

                if (pattern != null) {//not in map, so matches any value in map1
                    if (pattern.charAt(0) == '*' && pattern.indexOf('|') < 0) {
                        if (doesPatternMatch(pattern, value1) == false) {
                            return false;
                        }
                    } else {//foo|bar|waa value1 must be foo or bar or waa
                        String[] ss = pattern.split("\\|");
                        boolean found = false;
                        for (int i = 0; i < ss.length; i++) {
                            if (ss[i].equals(value1) ||
                                    (ss[i].charAt(0) == '*' && doesPatternMatch(ss[i], value1))) {
                                found = true;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        /* @param pattern of the form *foo*
         * @param any string to search
         * @return True if the text between ** in pattern is found in s.
         */
        private boolean doesPatternMatch(String pattern, String s) {
            String search = pattern.substring(1, pattern.length() - 1);
            if (s.indexOf(search) < 0) {
                return false;
            } else {
                return true;
            }
        }

        private Map getDrillMap(String drill1) {
            Map map1 = new LinkedHashMap();
            String[] ss = drill1.split("&");
            for (int i = 0; i < ss.length; i++) {
                String[] pair = ss[i].split("=");
                map1.put(pair[0], pair[1]);
            }
            return map1;
        }

        /** Turns a string profile item value into a double. If it is of the form
         *  name*5.2 then it will be multiplied by 5.2.
         * @return
         */
        private double getNumericProfileItemValue() {
            double d = -1., fac = 1.;

            int pos = pivName.indexOf('*');
            if (pos >= 0) {
                fac = Double.parseDouble(pivName.substring(pos + 1));
                pivName = pivName.substring(0, pos);
            }
            String value = profile.getItemValue(path, profileItemUid, pivName);

            try {
                d = Double.parseDouble(value);
            } catch (NumberFormatException numberFormatException) {
                System.err.println("Can't parse " + pivName + " from " + path + "/" + profileItemUid);
            }

            return d * fac;
        }
    }

    /** For testing  -sets up some actions locally. In practice, these actions
     *  will be obtained from AMEE.
     */
    public static void initActions() {

        //tests that a profile item exists and has a particular value set
        ActOnCO2Action action = new ActOnCO2Action("no double glazing");
        action.addCondition("/home/energy/insulation:type=double glazing:description=no");
        actionMap.put(action.drillName, action);

        //a profile item based on a particular drill down exists
        action = new ActOnCO2Action("has laptop");
        action.addCondition("/home/appliances/computers/generic:device=Personal Computers&rating=Laptop");
        actionMap.put(action.drillName, action);

        //a profile item exists with a particular value set
        //AND another profile item exists that's based on one of several drill down choices
        action = new ActOnCO2Action("no loft insulation");
        action.addCondition("/home/energy/insulation:type=loft:description=no");
        action.addCondition("/home/heating:homeDwellingType=*house*|*bungalow*|*maisonette*");
        actionMap.put(action.drillName, action);

        //Check that one profile item exists
        //AND has a value that is larger than a value in some other profile item
        action = new ActOnCO2Action("majority lel bulbs");
        action.addCondition("/home/lighting:type=lel:noOfLightBulbs:GREATER:/home/lighting:type=normal:noOfLightBulbs");
        actionMap.put(action.drillName, action);

        //Check that one profile item exists
        //AND has a value that is larger than a value in some other profile item multiplied by a factor
        action = new ActOnCO2Action("few lel bulbs");
        action.addCondition("/home/lighting:type=normal:noOfLightBulbs:GREATER:/home/lighting:type=lel:noOfLightBulbs*9");
        actionMap.put(action.drillName, action);

        action = new ActOnCO2Action("drill pattern test");
        action.addCondition("/home/lighting:type=bar|*orm*|foo");
        actionMap.put(action.drillName, action);

        //Checks for a category that contains no profile items.
        action = new ActOnCO2Action("no insulation");
        action.addCondition("/home/energy/insulation:EMPTY");
        actionMap.put(action.drillName, action);

        //Checks for a profile item for a range of drill down options on rating
        // OR for a drill match for age
        action = new ActOnCO2Action("bad fridge");
        action.setOr(true);
        action.addCondition("/home/appliances/kitchen/generic:device=*fridge*&rating=C|D|E|F|G|Other");
        action.addCondition("/home/appliances/kitchen/generic:device=*fridge*&age=*over*");
        actionMap.put(action.drillName, action);

        //Checks for value set in the metadata item:
        action = new ActOnCO2Action("green tariff");
        action.addCondition("!/metadata::greenTariff=no");
        actionMap.put(action.drillName, action);

        //Checks for value set in the metadata item:
        action = new ActOnCO2Action("high car mileage");
        action.addCondition("/transport/car/generic:ANY:distanceKmPerMonth>402");
        actionMap.put(action.drillName, action);

        //Checks for value set in the metadata item:
        action = new ActOnCO2Action("any PSUs");
        action.addCondition("/home/appliances/entertainment/generic:device=PSU&rating=typical:numberOwned>0");
        actionMap.put(action.drillName, action);

    //Other tip needs to be triggered in override - see line 64 of tblTip_dump sheet
    }

    public static void testAction(ActOnCO2Action action) {
        boolean isTriggered = action.isTriggered();
        System.err.println(action.drillName + " isTriggered=" + isTriggered);
        if (isTriggered) {
            //String dataItemUid = getDataItemUid(actionCategoryPath, "name=" + action.drillName);
            //String request = "POST "+actionCategoryPath + dataItemUid;
            //Now send this request to AMEE to create the action item, not implemented.
        }
    }

    public static void main(String args[]) {
        //create the actions
        initActions();

        Main.login = args[0];
        Main.password = args[1];

        String profileUid = "949B92E176C8";
        TestProfiles profile = new TestProfiles(profileUid);

        /*ActOnCO2Action action = actionMap.get("any PSUs");
        action.setProfile(profile);
        testAction(action);*/

        Iterator<ActOnCO2Action> iter = actionMap.values().iterator();
        while (iter.hasNext()) {
            System.err.println("\n =========================");
            ActOnCO2Action action = iter.next();
            action.setProfile(profile);
            testAction(action);
        }
    }
}
