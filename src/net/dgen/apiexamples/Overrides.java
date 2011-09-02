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

/**
 * This class implements the ActOnCO2 v1 overrides and breakdown
 * of results. 
 * 
 * To use this class, the following methods must always be called: setHomeSubTotals(),
 * setAppliancesSubTotals() and setTransportSubtotals(). If bills are given
 * then call setBillDataGiven(true) and then setElectricity(). If electricity is
 * not used for heating then call setElectricityUsedForHeating(true) and call
 * setFuel().
 * 
 * IMPORTANT: All values given to the set methods must be in tonnes CO2 per year.
 * 
 * The subtotals that need to be set are closely related to values returned
 * in XML from AMEE. For full details, see http://wiki.amee.com/index.php/ActOnCO2#Overrides.
 * The subtotals that are given in the set methods of this class are sometimes called
 * inferred values (i.e. not from bill data).
 * 
 * In all cases performOverrides should then be called. The totals and subtotals
 * for displaying to the user can then be retrieved via the getXXX() methods, e.g.
 *  getHome() or getLighting().
 * @author Andrew Conway
 */
public class Overrides {

    /** Top level totals for tonnes CO2 per year. */
    private double home,  appliances,  transport;
    /** home subtotals */
    private double spaceHeating,  waterHeating,  lighting;// *** 30mar09
    /** appliances subtotals - renewableSaving must be negative */
    private double kitchen,  entertainment,  study,  other,  renewableSaving;
    /** transport subtotals */
    private double carsMotorbikes,  flights;
    /** tonnes CO2 per year from bill data. */
    private double electricity = Double.NaN,  fuel = Double.NaN;
    private boolean isElectricityUsedForHeating = true;
    private boolean isElectricityUsedForWaterHeating = true;// *** 30mar09
    private boolean isBillDataGiven = false;

    public void setHomeSubtotals(double spaceHeating, double waterHeating, double lighting) {// *** 30mar09
        this.spaceHeating = spaceHeating;// *** 30mar09
        this.waterHeating = waterHeating;// *** 30mar09
        this.lighting = lighting;
        this.home = spaceHeating + waterHeating + lighting;// *** 30mar09
    }

    /** If bill data is given, pass in a zero value for renewableSaving
     * other doesn't get set explicitly but is calculated later on */
    public void setAppliancesSubtotals(double kitchen, double entertainment, double study, double renewableSaving) {// *** 6apr09
        this.kitchen = kitchen;
        this.entertainment = entertainment;
        this.study = study;
        this.renewableSaving = renewableSaving;// *** 6apr09
        this.appliances = kitchen + entertainment + study + renewableSaving;// *** 6apr09
    }

    /** transport isn't involved in overrides */
    public void setTransportSubtotals(double carsMotorbikes, double flights) {
        this.carsMotorbikes = carsMotorbikes;
        this.flights = flights;
        this.transport = carsMotorbikes + flights;
    }

    /** If this is set to true then setElectricity and setElectricityUsedForHeating
     *  must be called too.
     * @param isBillDataGiven
     */
    public void setBillDataGiven(boolean isBillDataGiven) {
        this.isBillDataGiven = isBillDataGiven;
    }

    public void setElectricity(double electricity) {
        this.electricity = electricity;
    }

    /** If this is set to false then setFuel must be called. */
    public void setElectricityUsedForHeating(boolean isElectricityUsedForHeating) {
        this.isElectricityUsedForHeating = isElectricityUsedForHeating;
        setElectricityUsedForWaterHeating(isElectricityUsedForHeating);// *** 30mar09
    }

    /** If this is set to false then setFuel must be called. */
    public void setElectricityUsedForWaterHeating(boolean isElectricityUsedForWaterHeating) {// *** 30mar09
        this.isElectricityUsedForWaterHeating = isElectricityUsedForWaterHeating;// *** 30mar09
    }// *** 30mar09

    /** This only needs to be set if electricityUsedForHeating=false */
    public void setFuel(double fuel) {
        this.fuel = fuel;
    }

    /** Once heating, lighting and (optionally) bill data is set, this
     *  method can be called to give the interim home result, that is
     *  the result displayed at the end of the "Home" section of ActOnCO2.
     *  IMPORTANT: All values must be in tonnes CO2 per year.
     * @param numberOfPeople
     */
    public double getInterimHome(int numberOfPeople) {
        double result = 0.;
        if (isBillDataGiven == false) {
            result = getHeating() + getLighting();
        } else {//bill data given
            if (isElectricityUsedForHeating == false) {
                if (isElectricityUsedForWaterHeating == false) {// *** 30mar09
                    result = fuel + getLighting();
                } else {// *** 30mar09
                    double total = waterHeating + getLighting() + 0.683 * numberOfPeople;// *** 30mar09
                    result = fuel + (waterHeating + getLighting()) * electricity / total;// *** 30mar09
                }// *** 30mar09
            } else {//electric heating
                double total = getHeating() + getLighting() + 0.683 * numberOfPeople;
                result = (getHeating() + getLighting()) * electricity / total;
            }
        }
        return result;
    }

    /** Performs the overrides - calculates "other" and, if necessary, scales totals
     *  to accord with bill data.
     */
    public void performOverrides() {
        if (isBillDataGiven == false) {//case 1, on bill data
            other = 0.0526 * (getLighting() + getAppliances());
        } else {
            if (isElectricityUsedForHeating == false) {//case 2, non electric heating
                if (isElectricityUsedForWaterHeating == false) {// *** 30mar09
                    double heatingFac = fuel / (spaceHeating + waterHeating);// *** 30mar09
                    spaceHeating *= heatingFac;// *** 30mar09
                    waterHeating *= heatingFac;// *** 30mar09
                    if (electricity <= (getLighting() + getAppliances())) {//case 2a
                        other = 0.;
                        double fac = electricity / (getLighting() + getAppliances());
                        lighting *= fac;
                        kitchen *= fac;
                        entertainment *= fac;
                        study *= fac;
                        renewableSaving *= fac;// *** 6apr09
                    } else {//case 2b, elec bill too big
                        other = electricity - (getLighting() + getAppliances());
                    }
                } else { //water heating is electric, but space heating isn't // *** 30mar09
                    spaceHeating = fuel;// *** 30mar09
                    if (electricity <= (getLighting() + getAppliances() + waterHeating)) {//case 2a // *** 30mar09
                        other = 0.;// *** 30mar09
                        double fac = electricity / (getLighting() + getAppliances() + waterHeating);// *** 30mar09
                        waterHeating *= fac;// *** 30mar09
                        lighting *= fac;// *** 30mar09
                        kitchen *= fac;// *** 30mar09
                        entertainment *= fac;// *** 30mar09
                        study *= fac;// *** 30mar09
                        renewableSaving *= fac;// *** 6apr09
                    } else {//case 2b, elec bill too big// *** 30mar09
                        other = electricity - (getLighting() + getAppliances() + waterHeating);// *** 30mar09
                    }// *** 30mar09
                }// *** 30mar09
            } else {//case 3, electric heating
                if (electricity <= (getHeating() + getLighting() + getAppliances())) {//case 3a
                    other = 0.;
                    double fac = electricity / (getHeating() + getLighting() + getAppliances());
                    spaceHeating *= fac;// *** 30mar09
                    waterHeating *= fac;// *** 30mar09
                    lighting *= fac;
                    kitchen *= fac;
                    entertainment *= fac;
                    study *= fac;
                    renewableSaving *= fac;// *** 6apr09
                } else {//case 3b, elec bill too big
                    other = electricity - (getHeating() + getLighting() + getAppliances());
                }
            }
        }
        //Now recalculate the totals
        home = getHeating() + getLighting();
        appliances = getKitchen() + getEntertainment() + getStudy() + getOther() + renewableSaving; // *** 6apr09
    }

    public double getHome() {
        return home;
    }

    public double getAppliances() {
        return appliances;
    }

    public double getTransport() {
        return transport;
    }

    public double getHeating() {
        return spaceHeating + waterHeating;// *** 30mar09
    }

    public double getLighting() {
        return lighting;
    }

    public double getKitchen() {
        return kitchen;
    }

    public double getEntertainment() {
        return entertainment;
    }

    public double getStudy() {
        return study;
    }

    public double getOther() {
        return other;
    }

    public double getCarsMotorbikes() {
        return carsMotorbikes;
    }

    public double getFlights() {
        return flights;
    }

    public String toString() {
        String s = "";
        s += "home=" + home + "\n";
        s += "  spaceHeating=" + spaceHeating + "\n";// *** 30mar09
        s += "  waterHeating=" + waterHeating + "\n";// *** 30mar09
        s += "  lighting=" + lighting + "\n";
        s += "appliances=" + appliances + "\n";
        s += "  kitchen=" + kitchen + "\n";
        s += "  entertainment=" + entertainment + "\n";
        s += "  study=" + study + "\n";
        s += "  renewableSaving=" + renewableSaving + "\n";// *** 6apr09
        s += "  other=" + other + "\n";
        if (isBillDataGiven) {
            s += "electricity=" + electricity + "\n";
            if (isElectricityUsedForHeating == false) {
                s += "fuel=" + fuel + "\n";
            }
        }
        return s;
    }

    /** This example illustrates all possible cases using some
     *  contrived data where totals come to fairly round numbers. For example,
     *  lighting+appliances=1 and electricity bills are either double the
     *  inferred totals, or half the inferred totals.
     *  There is no communication with AMEE in this example. 
     */
    private static void runSimpleExample() {
        int numberOfPeople = 3;

       //If bill data is given, pass in a zero value for renewableSaving to setAppliancesSubtotals()

        System.err.println("***** NO BILL:");
        Overrides ov = new Overrides();
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, -0.2);// *** 6apr09
        ov.setBillDataGiven(false);
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));
        ov.performOverrides();
        System.err.println(ov);

        System.err.println("***** BILL GIVEN, non-elec heating, bill too small - case 2a:");
        ov = new Overrides();
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, 0.);
        ov.setBillDataGiven(true);
        ov.setElectricityUsedForHeating(false);
        ov.setElectricity(0.5);
        ov.setFuel(4.2);
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));
        ov.performOverrides();
        System.err.println(ov);

        System.err.println("***** BILL GIVEN, non-elec heating, bill too small - WATER HEATING ELEC - case 2a:");// *** 30mar09
        ov = new Overrides();// *** 30mar09
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, 0.);// *** 30mar09
        ov.setBillDataGiven(true);// *** 30mar09
        ov.setElectricityUsedForHeating(false);// *** 30mar09
        ov.setElectricityUsedForWaterHeating(true);// *** 30mar09
        ov.setElectricity(0.5);// *** 30mar09
        ov.setFuel(4.2);// *** 30mar09
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));// *** 30mar09
        ov.performOverrides();// *** 30mar09
        System.err.println(ov);// *** 30mar09

        System.err.println("***** BILL GIVEN, non-elec heating, bill too big - case 2b:");
        ov = new Overrides();
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, 0.);
        ov.setBillDataGiven(true);
        ov.setElectricityUsedForHeating(false);
        ov.setElectricity(2.);
        ov.setFuel(4.2);
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));
        ov.performOverrides();
        System.err.println(ov);

        System.err.println("***** BILL GIVEN, elec heating, bill too small - case 3a:");
        ov = new Overrides();
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, 0.);
        ov.setBillDataGiven(true);
        ov.setElectricityUsedForHeating(true);
        ov.setElectricity(3.0);
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));
        ov.performOverrides();
        System.err.println(ov);

        System.err.println("***** BILL GIVEN, elec heating, bill too big - case 3b:");
        ov = new Overrides();
        ov.setHomeSubtotals(4.5, 0.5, 0.1);// *** 30mar09
        ov.setAppliancesSubtotals(0.6, 0.2, 0.1, 0.);
        ov.setBillDataGiven(true);
        ov.setElectricityUsedForHeating(true);
        ov.setElectricity(12.0);
        System.err.println("interimHome=" + ov.getInterimHome(numberOfPeople));
        ov.performOverrides();
        System.err.println(ov);
    }

    public static void main(String args[]) {
        runSimpleExample();

    }
}
