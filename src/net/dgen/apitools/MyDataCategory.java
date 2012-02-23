package net.dgen.apitools;

import com.amee.client.AmeeException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import net.dgen.apiexamples.Main;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author nalu
 */
public class MyDataCategory {
    public static void testCar() throws AmeeException {
        path = "/transport/car/generic";
        String key = "fuel=petrol&size=medium";
        String values = "distanceKmPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "fuel=diesel&size=large";
        values = "distanceKmPerMonth=1000&kmPerLitreOwn=10";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testMotorcycle() throws AmeeException {
        path = "/transport/motorcycle/generic";
        String key = "fuel=petrol&size=medium";
        String values = "distanceKmPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testPlane() throws AmeeException {
        path = "/transport/plane/generic";
        String key = "type=long haul&size=one way";
        String values = "journeysPerYear=2";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=short haul&size=-";
        values = "distanceKmPerYear=10000";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=auto&size=return";
        values = "IATACode1=LHR&IATACode2=LAX";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testKitchen() throws AmeeException {
        path = "/home/appliances/kitchen/generic";
        String key = "device=kettle&rating=-&age=-&temperature=-";
        String values = "";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "device=washing machine&rating=A&age=up to 12 years&temperature=40";
        values = "cyclesPerMonth=10";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testEntertainment() throws AmeeException {
        path = "/home/appliances/entertainment/generic";
        String key = "device=Video&rating=DVD";
        String values = "numberOwned=10";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testComputers() throws AmeeException {
        path = "/home/appliances/computers/generic";
        String key = "device=Personal Computers&rating=Desktop";
        String values = "numberOwned=10";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testCooking() throws AmeeException {
        path = "/home/appliances/cooking/hob";
        String key = "type=gas";
        String values = "";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/home/appliances/cooking/oven";
        key = "type=gas";
        values = "";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testEnergyQuantity() throws AmeeException {
        path = "/home/energy/quantity";
        String key = "type=gas";
        String values = "kWhPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=electricity";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testFuelPrice() throws AmeeException {
        path = "/home/energy/uk/price";
        String key = "type=gas&payment=normal";
        String values = "currencyGBPPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=gas&payment=prepayment";
        values += "&season=summer";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testHeatingUK() throws AmeeException {
        path = "/home/heating/uk";
        String key = "homeType=flat&fuel=coal";
        String values = "numberOfBedrooms=4&heatingType=open fires&age=pre 1930";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testLighting() throws AmeeException {
        path = "/home/lighting";
        String key = "type=lel";
        String values = "noOfLightBulbs=100";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testTelevisions() throws AmeeException {
        path = "/home/appliances/televisions/generic";
        String key = "type=LCD&size=32\"";
        String values = "hoursPerMonth=100";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testPlaneExtra() throws AmeeException {
        path = "/transport/plane/generic";
        String key = "type=long haul&size=return";
        String values = "journeysPerYear=2&numberOfPassengers=10";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=long haul&size=-";
        values = "distanceKmPerYear=10000&passengerClass=first";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testKitchenExtra() throws AmeeException {
        path = "/home/appliances/kitchen/generic";
        String key = "device=tumble dryer&rating=standard&age=-&temperature=-";
        String values = "cyclesPerMonth=10";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "device=Washer/dryer&rating=Dry only&age=-&temperature=-";
        values = "cyclesPerMonth=10";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testBus() throws AmeeException {
        path = "/transport/bus/generic";
        String key = "type=local";
        String values = "distanceKmPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=coach";
        values = "numberOfJourneys=2&journeyFrequency=daily&useTypicalDistance=true";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=typical";
        values = "numberOfJourneys=7&journeyFrequency=weekly&useTypicalDistance=false&distancePerJourney=10&isReturn=true";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testTrain() throws AmeeException {
        path = "/transport/train/generic";
        String key = "type=international";
        String values = "distanceKmPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=tram";
        values = "numberOfJourneys=2&journeyFrequency=daily&useTypicalDistance=true";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=national";
        values = "numberOfJourneys=7&journeyFrequency=weekly&useTypicalDistance=false&distancePerJourney=10&isReturn=true";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testTaxi() throws AmeeException {
        path = "/transport/taxi/generic/perpassenger";
        String key = "type=typical";
        String values = "distanceKmPerMonth=1000";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        key = "type=black cab";
        values = "numberOfJourneys=7&journeyFrequency=weekly&useTypicalDistance=false&distancePerJourney=10&isReturn=true";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testNone() throws AmeeException {
        path = "/home/appliances/kitchen/generic";
        String key = "device=microwave&rating=-&age=-&temperature=-";
        String values = "name=none";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/home/appliances/cooking/hob";
        key = "type=electric";
        values = "name=none";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/home/appliances/cooking/oven";
        key = "type=electric";
        values = "name=none";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testCarExtra() throws AmeeException {
        path = "/transport/car/generic";
        String key = "fuel=petrol&size=small";
        String values = "useTypicalDistance=true";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testMotorcycleExtra() throws AmeeException {
        path = "/transport/motorcycle/generic";
        String key = "fuel=petrol&size=small";
        String values = "useTypicalDistance=true";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testPublicTransport() throws AmeeException {
        //test number of passengers
        path = "/transport/bus/generic";
        String key = "type=local";
        String values = "distanceKmPerMonth=1000&numberOfPassengers=10&name=passengers";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/transport/train/generic";
        key = "type=international";
        values = "distanceKmPerMonth=1000&numberOfPassengers=10&name=passengers";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/transport/taxi/generic/perpassenger";
        key = "type=typical";
        values = "distanceKmPerMonth=1000&numberOfPassengers=10&name=passengers";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testStandby() throws AmeeException {
        path = "/home/appliances/computers/generic";
        String key = "device=standby&rating=-";
        String values = "name=standby&onStandby=always";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/home/appliances/entertainment/generic";
        values = "name=standby&onStandby=never";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testTelevisionsRanges() throws AmeeException {
        path = "/home/appliances/televisions/generic/ranges";
        String key = "type=lcd&size=29-33";
        String values = "hoursPerMonth=100";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testStandbyWithTVs() throws AmeeException {
        path = "/home/appliances/entertainment/generic";
        String key = "device=standby&rating=-";
        String values = "name=includes tvs&onStandby=sometimes";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }

    public static void testCarLPG() throws AmeeException {
        path = "/transport/car/generic";
        String key = "fuel=lpg&size=medium";
        String values = "useTypicalDistance=true";
        String response = Main.createProfileItem(profileUid, path, key, values, true);
        double kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);

        path = "/transport/car/generic";
        key = "fuel=lpg&size=large";
        values = "distanceKmPerMonth=1000&kmPerLitreOwn=10";
        response = Main.createProfileItem(profileUid, path, key, values, true);
        kgCO2 = Main.extractAmountPerMonth(response);
        System.err.println(path + "," + key + "," + values + "," + kgCO2);
    }
    private static String profileUid,  path;

    public static void test() throws AmeeException {
        profileUid = Main.createProfile();
        String metadataUid;
        metadataUid = "86D02FBD95AE";//same on stage and live and even dev!
        String metadataValues = "peopleInHousehold=3";
        Main.createProfieItemFromDataUid(profileUid, "/metadata", metadataUid, metadataValues);
        System.err.println("profileUid," + profileUid);
        System.err.println("/metadata,," + metadataValues);

        //these test categories for ActonCO2 migration
        testCar();
        testMotorcycle();
        testPlane();
        testKitchen();
        testEntertainment();
        testComputers();
        testCooking();
        testEnergyQuantity();
        testFuelPrice();
        testHeatingUK();
        testLighting();
        testTelevisions();

        //test for actonco2 v2 features
        testPlaneExtra(); //test planes class and number of passengers
        testKitchenExtra();//test tumble dryers
        //test bus,train,taxi distance and journeys
        testBus();
        testTrain();
        testTaxi();
        testNone();//test none items
        testCarExtra();
        testMotorcycleExtra();
        //tests numberOfPassengers
        testPublicTransport();
        testStandby();

        testTelevisionsRanges();
        testStandbyWithTVs();
        testCarLPG();
    }

    public static boolean fetchItemDefFromAPIAndWriteToCSV(int site, String path) throws AmeeException {
        File csvDir = new File(ApiTools.csvDir, path);
        ItemDefinition id = ItemDefinition.fetchItemDefFromAPI(site, path);

        //System.err.println("id:\n" + id);
        boolean success = id.save(csvDir);

        System.err.println(id.name + ": save success=" + success);
        return success;
    }

    public static void main(String[] args) throws AmeeException {
        int site = DataCategory.LIVE;
        Main.setLogin(args[0]);
        Main.setPassword(args[1]);
        //Main.proxy=new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 1080));
        //Main.ip="85.133.58.72";
        //isAdmin=true;
        ApiTools.init(site);

        //Main.debug = true;
        //test();

        ApiTools.csvDir = new File("/home/nalu/amee/api_csvs");
/*        String[] paths = {/*"/home/appliances/computers/generic", "/home/appliances/cooking",
        "/home/appliances/cooking/hob", "/home/appliances/cooking/oven",
        "/home/appliances/entertainment/generic","/home/appliances/kitchen/generic",
        "/home/appliances/televisions/generic","/home/appliances/televisions/generic/ranges",
        "/home/energy/electricity", "/home/energy/electricityISO",
        "/home/energy/quantity", "/home/energy/uk/price", "/home/energy/uk/seasonal",
        "/home/lighting", "/home/heating", "/home/heating/uk", "/home/heating/uk/heatingtypes",
        "/home/heating/uk/floorareas", "/home/heating/uk/renewable",
        "/transport/bus/generic", "/transport/train/generic", "/transport/taxi/generic",
        "/transport/taxi/generic/perpassenger", "/transport/ship/generic",
        "/transport/car/generic", "/transport/motorcycle/generic", "/transport/plane/generic",
        "/transport/plane/generic/passengerclass", "/transport/plane/generic/airports/codes",
        "/transport/plane/generic/airports/countries",
        "/metadata", "/metadata/actonco2", "/metadata/actonco2/actions",
        "/planet/country/uk/average/home",
        "/planet/country/uk/average/appliances",
        "/planet/country/uk/average/travel"
        };*/
        String[] paths = {"/transport/ship/generic"};
        for (String path : paths) {
            //String path = "/home/lighting";
            //fetchItemDefFromAPIAndWriteToCSV(site, path);

            File csvDir = new File(ApiTools.csvDir, path);
            File csvFile = new File(csvDir, "itemdef.csv");
            //ItemDefinition id = ItemDefinition.fetchItemDefFromCSV(csvFile);
            //ItemDefinition id = ItemDefinition.fetchItemDefFromAPI(site, path);
            //id.algorithmName="default";
            //System.err.println("id:\n" + id);

            //System.err.println("save=" + id.save(new File(csvDir,"live")));
            //NOTE: You CAN use updateInAPI now, but bear in mind:
            // - you MUST delete itemdefs manually - it won't do it for you
            // - it can't rename an existing item def - must be done manually
/*            ItemDefinitionSync.testMode = true; //don't actually change AMEE
            ItemDefinitionSync.isToUpdateAlgorithm = true;
            ItemDefinitionSync.isToUpdateDifferingValues = true;
            boolean success = ItemDefinitionSync.updateInAPI(site, csvFile, path);
            //boolean success = ItemDefinitionSync.createInAPI(site, id);
            System.err.println(path + " ------------------- result = " + success);


  */          DataCategory cat = DataCategory.getDataCategory(path);
            //cat.setDefaultAction(DataCategory.POST);//set this if you know the data items aren't in AMEE
            //cat.setStart(123);
            //cat.setLimit(123); //Only 10 entries will be loaded from local data file

            cat.setUseCache(true);
            cat.useItemDef = true; //load/save item def? otherwise use simple old system
            cat.doBackup = false; //saves to backup in csv dir, NOTE: doesn't backup source item value from API
            cat.checkSource = true; //check the source fields? make sure there's a dummy "units" col before source!
            cat.isStrict = true; //ABORT if certain problems occur, e.g. no source is given
            cat.doBatch = true; //batch things up in xml and send in one go at the end
            //Main.charSetName="UTF-8";
            //Note, if any of the data contains double quotes, replace each one with _QUOTE_
            //in the CSVs and it will be converted to a double quotes after loading.
            cat.loadLocalData();
            //ApiTools.charSetName="ISO-8859-1";
            //cat.updateAPI();
            cat.checkAPI(); //just check, don't alter AMEE
        }

    /*for (int i = 1; i < 1009; i += 50) {
    int iStop = i + 49;
    if (iStop >= 1008) {
    iStop = 1008;
    }
    cat.setStart(i); //Start from nth entry, default is 1
    cat.setStop(iStop);//Stop at mth entry, default is max integer
    System.err.println("BATCHING from " + i + " to " + iStop);

    cat.updateAPI();
    //cat.checkAPI(); //just check, don't alter AMEE
    }*/
    }

}
