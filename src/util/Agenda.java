package util;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import core.DTNSim;
import core.SimClock;

/**
 * This class is a helper for the UniMovement model. It is a variation of the Working Day Model and used to
 * create an agenda for a specific number of timeslots. This agenda is used by nodes to decide the next destination.
 */

public class Agenda {

    static int nHosts, dayStart, dayEnd, meanArrivalTime, meanStayDuration;
    static double stdArrivalTime, stdStayDuration, mensaProbability, shareLeisure, shareLecture, shareTutorial;
    static Random rand = new Random();
    static List<Location> locations;
    static String[][] agendaTable;
    static List<Integer> timeslotBase = new ArrayList<>();
    static int timeslotLength;
    static int scenarioEndTime;
    static int agendaAssignmentCounter = 0;

    private final String[] agenda;
    private final int[] timeSlots = new int[dayEnd-dayStart];

    public Agenda() {
        this.agenda = agendaTable[agendaAssignmentCounter++];
        // used to keep track which row of the agenda table a specific node will receive.
        // Increased at each instantiation so that no node receives the same agenda.
        Collections.shuffle(timeslotBase);
        for(int i = 0; i < timeSlots.length-1; i++) {
            timeSlots[i] = timeslotBase.get(i) + timeslotLength*i;
        }
        timeSlots[timeSlots.length-1] = timeSlots.length * timeslotLength;
    }

    private enum LocationTypes {ENTRANCE, MENSA, TUTORIAL, LECTURE, STUDY, LEISURE, ROUTING, DEFAULT}

    /**
     * Helper class used to parse a json object into a location object.
     */
    static class Location {
        // Each location is assigned to a type from LocationTypes
        private LocationTypes locationType;
        // Parsed limit denoting the maximum number of nodes that can be assigned to the location at a specific timeslot.
        private int limit;
        private String name, polygon;
        // List to keep track of how much capacity the location has left for each timeslot.
        private List<Integer> limits = new ArrayList<>();

        public void setLimits() {this.limits.addAll(Collections.nCopies(dayEnd-dayStart, this.limit));}

        /**
         * Checks whether a location is already full at a given timeslot
         * @param index Corresponds to the timeslot in question
         * @return True if the location is full
         */
        public Boolean isFull(int index) {return limits.get(index) == 0;}

        /**
         * Assigns a place in a location to a specific node for a given timeslot.
         * @param timeslot The timeslot in question to assign to a node.
         * @return The name of the location that is assigned.
         */
        public String assignPlace(int timeslot){
            this.limits.set(timeslot, this.limits.get(timeslot)-1);
            return this.name;
        }

        @Override
        public String toString() {return "Name: " + name + " Location type: " + locationType + " Limit: " + limit;}
    }

    /**
     * This is a helper class used to choose a specific location based on a location type. The specific location is
     * taken from a list containing all locations of the respective type.
     */
    private static class LocationDrawer {
        private final List<List<Location>> mensaList = new ArrayList<>();
        private final List<List<Location>> tutorialList = new ArrayList<>();
        private final List<List<Location>> lectureList = new ArrayList<>();
        private final List<List<Location>> studyList = new ArrayList<>();
        private final List<List<Location>> leisureList = new ArrayList<>();
        private final List<List<Location>> entranceList = new ArrayList<>();
        private final List<List<Location>> defaultList = new ArrayList<>();
        private int default_counter = 0;


        public LocationDrawer() {
            List<Location> tempEntrance = new ArrayList<>();
            List<Location> tempMensa = new ArrayList<>();
            List<Location> tempTutorial = new ArrayList<>();
            List<Location> tempLecture = new ArrayList<>();
            List<Location> tempStudy = new ArrayList<>();
            List<Location> tempLeisure = new ArrayList<>();
            List<Location> tempDefault = new ArrayList<>();
            // initialize the location drawer by creating a list for all possible location types and filling them with the
            // respective locations that got parsed from the json file
            for(Location location : locations) {
                switch (location.locationType) {
                    case ENTRANCE -> tempEntrance.add(location);
                    case MENSA -> tempMensa.add(location);
                    case TUTORIAL -> tempTutorial.add(location);
                    case LECTURE -> tempLecture.add(location);
                    case STUDY -> tempStudy.add(location);
                    case LEISURE -> tempLeisure.add(location);
                    case DEFAULT -> tempDefault.add(location);
                }
            }
            Collections.shuffle(entranceList);
            Collections.shuffle(mensaList);
            Collections.shuffle(tutorialList);
            Collections.shuffle(lectureList);
            Collections.shuffle(studyList);
            // Add a reference to the list to a list with the length of timeslots
            for(int i=0; i<dayEnd-dayStart; i++) {
                entranceList.add(tempEntrance);
                mensaList.add(tempMensa);
                tutorialList.add(tempTutorial);
                lectureList.add(tempLecture);
                studyList.add(tempStudy);
                leisureList.add(tempLeisure);
                defaultList.add(tempDefault);
            }
        }

        /**
         * Returns a location based on a location type and an index.
         * The main functionality is to check whether a specific location is already full and then choose another
         * one of the same location type or default to a default location.
         * @param locationType The location type one wants to return
         * @param index The index corresponding to the timeslot in question
         * @return A specific location
         */
        public String getLocation(LocationTypes locationType, int index) {

            List<List<Location>> chosenList;
            // Choose what list to draw the location from based on the provided locationType
            switch (locationType) {
                case ENTRANCE -> {
                    return "entrance";
                }
                case MENSA -> chosenList = mensaList;
                case TUTORIAL -> chosenList = tutorialList;
                case LECTURE -> chosenList = lectureList;
                case STUDY -> chosenList = studyList;
                case DEFAULT -> chosenList = defaultList;
                default -> chosenList = leisureList;
            }
            // If the chosen list is empty, get a location from the default list
            if (chosenList.get(index).isEmpty()) {
                return getLocation(LocationTypes.DEFAULT, index);
            }
            // Choose a random location from the location list
            int locationIndex = new Random().nextInt(chosenList.get(index).size());
            Location location = chosenList.get(index).get(locationIndex);
            // If there is still space, return this location
            if (!location.isFull(index)) {return location.assignPlace(index);}
            // If the location is full, remove it from the
            else {
                chosenList.get(index).remove(locationIndex);
                return(getLocation(locationType, index));
            }
        }
        /**
         * Chooses an entrance from the entrance list
         */
        public String getEntrance() {
            for (Location location : entranceList.get(0)){
                if (location.limit != 0) {
                    location.limit -= 1;
                    return location.name;
                } else {
                    String ret_val = entranceList.get(0).get(default_counter).name;
                    default_counter = (default_counter + 1) % entranceList.get(0).size();
                    return ret_val;
                }
            }
            return "None";
        }
    }

    static {

        List<Location> jsonLocations = null;
        Properties prop = new Properties();
        try {
            String propFile = "example_settings/spy_settings.txt";
            String locationFile = "data/fmi_locations.json";
            prop.load(Files.newInputStream(Paths.get(propFile)));
            Reader reader = Files.newBufferedReader(Paths.get(locationFile));


            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            jsonLocations = new Gson().fromJson(jsonObject.get("vertices"), new TypeToken<List<Location>>() {
            }.getType());
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        nHosts = Integer.parseInt(prop.getProperty("nHosts"));
        dayStart = Integer.parseInt(prop.getProperty("dayStart"));
        dayEnd = Integer.parseInt(prop.getProperty("dayEnd"));
        meanArrivalTime = Integer.parseInt(prop.getProperty("meanArrivalTime"));
        meanStayDuration = Integer.parseInt(prop.getProperty("meanStayDuration"));
        stdArrivalTime = Double.parseDouble(prop.getProperty("stdArrivalTime"));
        stdStayDuration = Double.parseDouble(prop.getProperty("stdStayDuration"));
        mensaProbability = Double.parseDouble(prop.getProperty("mensaProbability"));
        shareLeisure = Double.parseDouble(prop.getProperty("shareLeisure"));
        shareLecture = Double.parseDouble(prop.getProperty("shareLecture"));
        shareTutorial = Double.parseDouble(prop.getProperty("shareTutorial"));
        scenarioEndTime = Integer.parseInt(prop.getProperty("Scenario.endTime"));
        timeslotLength = scenarioEndTime / (dayEnd-dayStart);

        locations = jsonLocations;
        assert locations != null;

        DTNSim.registerForReset(Agenda.class.getCanonicalName());
        reset();
    }

    public String[] getAgenda() {return this.agenda;}
    public int[] getTimeSlots(){return this.timeSlots;}

    /**
     * First step of the Agenda table creation. This method creates a table with dimension (number of hosts, number of timeslots).
     * This table is filled with individual agendas but only with a location type for each timeslot, no actual locations.
     * These location types can then be used later to choose an actual location from the ones provided in the json file.
     */
    private static List<List<LocationTypes>> assignLocationTypes() {
        List<List<LocationTypes>> agendaTable = new ArrayList<>();
        // Each iteration creates an individual agenda for each node.
        for(int i = 0; i < nHosts; i++) {
            // Choose timeslot at which the nodes agenda starts
            int arrivalTime = (int) Math.round(Math.max(dayStart, Math.min(dayEnd-3, rand.nextGaussian()*stdArrivalTime + meanArrivalTime)));
            // Choose the number of timeslots a node stays at university
            int stayDuration = (int) Math.round(Math.min(dayEnd- arrivalTime, Math.max(1, rand.nextGaussian()*stdStayDuration + meanStayDuration)));
            // Value to keep track of unassigned, still available timeslots
            int freeTimeSlots = stayDuration;
            int departureTime = arrivalTime + stayDuration;
            int mensaTimeslot = -1;
            // Assign a visit to mensa with a specific probability (mensa probability)
            if (Math.random() > 1-mensaProbability && arrivalTime < 15 && departureTime > 11){
                mensaTimeslot = (int) Math.max(11, Math.min(14, rand.nextGaussian() + 12.5));
                freeTimeSlots -= 1;
            }
            // Calculate the composition of the agenda based on values provided by the settings file
            int nLecture = (int) Math.round(freeTimeSlots*shareLecture);
            int nTutorial = (int) Math.round(freeTimeSlots*shareTutorial);
            int nLeisure = (int) Math.round(freeTimeSlots*shareLeisure);
            int nStudy = freeTimeSlots - nLecture - nTutorial - nLeisure;

            List<LocationTypes> activityList= new ArrayList<>();

            // Create a list containing all location (types) a node will visit, disregarding the mensa
            activityList.addAll(Collections.nCopies(nLecture, LocationTypes.LECTURE));
            activityList.addAll(Collections.nCopies(nTutorial, LocationTypes.TUTORIAL));
            activityList.addAll(Collections.nCopies(nLecture, LocationTypes.LEISURE));
            activityList.addAll(Collections.nCopies(nStudy, LocationTypes.STUDY));

            // Randomize the list
            Collections.shuffle(activityList);
            // Fill the unassigned locations with entrance. This value is a placeholder for "absence".
            int nPrepend = arrivalTime - dayStart;
            int nAppend = dayEnd - arrivalTime - stayDuration;
            activityList.addAll(0, Collections.nCopies(nPrepend, LocationTypes.ENTRANCE));
            activityList.addAll(Collections.nCopies(nAppend, LocationTypes.ENTRANCE));
            // Add the mensa timeslot between 11:00 and 14:00
            if (mensaTimeslot != -1) {activityList.add(mensaTimeslot-dayStart, LocationTypes.MENSA);}
            agendaTable.add(activityList);
        }
        return agendaTable;
    }

    /**
     * This is the second step of the agenda creation. Here, the location types in the agenda table are replaced with
     * specific locations.
     * @param agendaTable Table containing location types
     */
    private static void assignLocations(List<List<LocationTypes>> agendaTable){
        int nTimeslots = dayEnd-dayStart;
        String[][] agenda = new String[nHosts][nTimeslots];
        LocationDrawer locationDrawer = new LocationDrawer();
        for(int i = 0; i < nHosts; i++){
            // Choose a single entrance and exit for individual agendas
            String entranceName = locationDrawer.getEntrance();
            for(int j = 0; j < nTimeslots; j++){
                // Replace the entrance types with the chosen entrance
                if (agendaTable.get(i).get(j) == LocationTypes.ENTRANCE) {
                    agenda[i][j] = entranceName;
                } else {
                    // Replace all other location types with a specific location
                    agenda[i][j] = locationDrawer.getLocation(agendaTable.get(i).get(j), j);
                }
            }
        }
        Agenda.agendaTable = agenda;
    }

    public static void reset() {

        for(Location location : locations) {
            location.limits = new ArrayList<>();
            location.setLimits();
        }

        for(Location location: locations){
            location.setLimits();
        }

        List<List<LocationTypes>> tmp = assignLocationTypes();
        assignLocations(tmp);
        Random rand = new Random();
        for(int i = 0; i < dayEnd-dayStart-1; i++){
            timeslotBase.add((int) ((rand.nextDouble()*(1.2-0.8) + 0.8) * timeslotLength));
        }

        agendaAssignmentCounter = 0;
        System.out.println("Reset");
    }
}