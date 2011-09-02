/*
 * TestProfiles.java
 *
 * Created on April 28, 2008, 2:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.dgen.apiexamples;

import com.twicom.qdparser.TaggedElement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author nalu
 */
public class TestProfiles extends Main {

    private String profileUid;
    private Map<String, TaggedElement> profileMap;

    /** Creates a new instance of TestProfiles */
    public TestProfiles() {
    }

    public TestProfiles(String profileUid) {
        this.profileUid = profileUid;
        profileMap = getProfileMap(profileUid);
    }

    public String createTestProfile() {
        String response;
        double kgCO2perMonth, total = 0.;

        profileUid = createProfile();
        System.err.println("profile_uid = " + profileUid);

        //metadata
        createProfileItem(profileUid, "/metadata", "", "country=United Kingdom", true);

        //bills
        response = createProfileItem(profileUid, "/home/energy/quantity", "type=electricity", "kWhPerMonth=1000", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("elec kgCO2 per month," + kgCO2perMonth);
        total += kgCO2perMonth;

        response = createProfileItem(profileUid, "/home/energy/quantity", "type=gas", "kWhPerMonth=1000", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("gas kgCO2 per month," + kgCO2perMonth);
        total += kgCO2perMonth;

        //home
        response = createProfileItem(profileUid, "/home/heating", "homeDwellingType=detached house&homeNoOfBedrooms=4&homeFuel=gas" + "&homeAge=post 1995&type=boiler (condensing)", "", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("detached house 4 gas post1995 boiler (condensing) " + kgCO2perMonth);
        total += kgCO2perMonth;

        response = createProfileItem(profileUid, "/home/lighting", "type=lel", "noOfLowEnergyLightBulbs=10", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("low energy lightbulbs kgCO2 per month," + kgCO2perMonth);
        total += kgCO2perMonth;

        //appliances
        response = createProfileItem(profileUid, "/home/appliances/kitchen/generic", "device=microwave", "", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("microwave kgCO2 per month = " + kgCO2perMonth);
        total += kgCO2perMonth;

        response = createProfileItem(profileUid, "/home/appliances/kitchen/generic", "device=Washing machine&rating=A&age=up to 12 years&temperature=30", "cyclesPerMonth=10", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("Washing machine A <12 yrs 30 kgCO2 per month," + kgCO2perMonth);
        total += kgCO2perMonth;

        //travel
        response = createProfileItem(profileUid, "/transport/car/generic", "fuel=petrol&size=large", "distanceKmPerMonth=1000", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("large petrol car kgCO2 per month," + kgCO2perMonth);

        response = createProfileItem(profileUid, "/transport/motorcycle/generic", "fuel=petrol&size=large", "distanceKmPerMonth=1000", true);
        kgCO2perMonth = extractAmountPerMonth(response);
        System.err.println("large petrol motorcycle kgCO2 per month " + kgCO2perMonth);

        System.err.println("total," + total);

        return profileUid;
    }

    /** At the moment it assumes the item is to be found in a path, e.g. /home/energy/quantity.
     * @param type corresponds to type drill down in that category, e.g. electricty or gas.
     * @return
     */
    public double extractBillData(String path, String type) {
        double value = 0.;

        try {
            TaggedElement te = profileMap.get(path);
            te = te.find("Children");
            TaggedElement par = te.find("ProfileItems");
            for (int i = 0; i < par.elements(); i++) {
                TaggedElement pi = (TaggedElement) par.getChild(i);
                String label = pi.find("dataItemLabel").toString();
                if (label.indexOf(type) >= 0) {
                    String s = pi.find("amountPerMonth").getChild(0).toString();
                    value = Double.parseDouble(s);
                }
            }
        } catch (Exception e) {//do nothing
        }
        return value;
    }

    public int getProfileItemCount(String path) {
        int count = 0;
        try {
            TaggedElement te = profileMap.get(path);
            te = te.find("Children");
            TaggedElement par = te.find("ProfileItems");
            count = par.elements();
        } catch (Exception e) {//do nothing, just means zero
            //e.printStackTrace();
        }
        return count;
    }

    public Map getProfileItems(String path) {
        Map map = new LinkedHashMap();
        try {
            TaggedElement te = profileMap.get(path);
            te = te.find("Children");
            TaggedElement par = te.find("ProfileItems");
            for (int i = 0; i < par.elements(); i++) {
                TaggedElement pi = (TaggedElement) par.getChild(i);
                String duid = pi.find("dataItemUid").getChild(0).toString();
                String puid = pi.getAttribute("uid");
                map.put(puid,duid);
            }
        } catch (Exception e) {//do nothing, just means it wasn't found
        }
        return map;
    }

    /** Returns the profile item uid for the first profile item found
     *  that is based on the data item for the given dataItemUid.
     * @param path
     * @param dataItemUid
     * @return
     */
    public String getProfileItemUid(String path, String dataItemUid) {
        String puid = null;

        try {
            TaggedElement te = profileMap.get(path);
            te = te.find("Children");
            TaggedElement par = te.find("ProfileItems");
            for (int i = 0; i < par.elements(); i++) {
                TaggedElement pi = (TaggedElement) par.getChild(i);
                String duid = pi.find("dataItemUid").getChild(0).toString();
                //System.err.println("duid=" + duid);
                if (duid.equals(dataItemUid)) {
                    puid = pi.getAttribute("uid");
                    break;//find only first match
                }
            }
        } catch (Exception e) {//do nothing, just means it wasn't found
        }
        return puid;
    }

    public String getItemValue(String path, String profileItemUid, String name) {
        String value = null;

        try {
            TaggedElement te = profileMap.get(path);
            te = te.find("Children");
            TaggedElement par = te.find("ProfileItems");
            for (int i = 0; i < par.elements(); i++) {
                TaggedElement pi = (TaggedElement) par.getChild(i);
                String puid = pi.getAttribute("uid");
                if (puid.equals(profileItemUid)) {
                    TaggedElement piv = (TaggedElement) pi.find(name);
                    value = "";
                    if (piv.hasElements()) {
                        value = piv.getChild(0).toString();
                    }
                }
            }
        } catch (Exception e) {//do nothing, just means it wasn't found
        }
        return value;
    }

    public double extractTotal(String path) {
        double value = 0.;
        TaggedElement te = profileMap.get(path);
        if (te != null) {
            value = extractTotal(te);
        }
        return value;
    }

    public double extractTotal(TaggedElement te) {
        double value = 0.;
        te = te.find("TotalAmountPerMonth");
        if (te != null) {
            value = Double.parseDouble(te.getChild(0).toString());
        }

        return value;
    }

    public static void main(String[] args) {
        //IMPORTANT: Remember to change these to your login details
        Main.login = "***USERNAME***";
        Main.password = "***PASSWORD***";
        //Main.debug=true;

        //Fetch existing
        String nonBillProfileUid = "949B92E176C8";
        String billProfileUid = "6A51FBF3EFD0";
        TestProfiles tp = new TestProfiles(billProfileUid);

        //Create new
        //TestProfiles tp = new TestProfiles();
        //tp.createTestProfile();

        System.err.println("total = " + tp.extractTotal("/home/heating"));

        Overrides ov = new Overrides();
        //extractTotal pulls TotalAmountPerMonth out of the specified category
        //ov.setHomeSubtotals(tp.extractTotal("/home/heating"), tp.extractTotal("/home/lighting"));
        double kitchen = tp.extractTotal("/home/appliances/kitchen/generic");
        kitchen += tp.extractTotal("/home/appliances/cooking");
        double entertainment = tp.extractTotal("/home/appliances/entertainment/generic");
        entertainment += tp.extractTotal("/home/appliances/televisions/generic");
        double study = tp.extractTotal("/home/appliances/computers/generic");
        //note, in v1 of ActOnCO2 PSUs should be in study, but in AMEE it's in entertainment

        //extractBillData pulls the AmountPerMonth out of the relevant profile item in the
        //specified category.
        double electricity = tp.extractBillData("/home/energy/quantity", "electricity");
        double gas = tp.extractBillData("/home/energy/quantity", "gas");

        double tpy = 12 / 1000.; //convert to tonnes per year from kg per month

        //ov.setAppliancesSubtotals(kitchen * tpy, entertainment * tpy, study * tpy);
        ov.setElectricity(electricity * tpy);
        ov.setFuel(gas * tpy);
        ov.setBillDataGiven(true);
        ov.setElectricityUsedForHeating(false);
        ov.performOverrides();
        System.err.println("Result=\n" + ov);

    /*int res = JOptionPane.showConfirmDialog(null, "Delete the profile?");
    if (res == JOptionPane.YES_OPTION) {
    boolean success = deleteProfile(profile_uid);
    System.err.println("Deleted profile " + profile_uid + " = " + success);
    }*/

    }
}
