package movement;

import com.google.gson.*;
import core.Coord;
import core.Settings;
import core.SimClock;
import movement.map.UniGraph;
import org.jgrapht.alg.util.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import util.Agenda;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class UniMovement extends MovementModel{

    private final static GeometryFactory gf = new GeometryFactory();
    //UniGraph contains the graph we use for routing and dijkstra
    private static UniGraph graph;
    //Hub the node currently resides in
    private UniHub current;
    //Hub that is the node's destination; set during timeslot
    private UniHub goal;
    //Route the node needs to take to get to goal
    private List<UniHub> route = null;
    private int route_index = 0;
    private int current_index = 0;
    private Coord lastWaypoint;
    private String[] agenda;
    //Just a helper Map to get the UniHub corresponding the name
    private final static HashMap<String, UniHub> locationMap = new HashMap<>();
    private int[] timeSlots;
    //Just helps to only initialize static stuff
    private static boolean parsed = false;

    /**
     * record that saves  the JTS Geometry of a Hub for ProhibitedPolygonRWP and a name for identification
     * @param polygon the Geometry of the current UniHub
     * @param name the name for identification
     */
    public record UniHub(Geometry polygon, String name) {
        @Override
            public String toString() {
                return String.format(
                        "[UniHub: name=%1$s, polygon=%2$s]",
                        name, polygon);
            }
    }

    @Override
    public Path getPath() {
        /*
        * We get a new UniHub goal according to the agenda and timeslot.
        * Then we get create a new path using our UniGraph
        * */
        if((int) SimClock.getTime() > timeSlots[current_index]){
            current = locationMap.get(agenda[current_index]);
            current_index ++;
            goal = locationMap.get(agenda[current_index]);
            route = graph.getPath(current, goal);
        }

        //If we reached the route UniHub we get to the next one in the list, until we reach teh goal
        if(route != null){
            if (route_index >= route.size()) {
                route_index = route.indexOf(current);
            }
            current = route.get(route_index);
            route_index++;
            if(current == goal) {
                route = null;
                route_index = 0;
            }
        }

        //ProhibitedPolygonRWP inside UniHubs
        Point pt;
        Coordinate c;

        do {
            c = randomCoordinate();
            pt = gf.createPoint(c);
        }while (!current.polygon().contains(pt));

        final Path p;
        p = new Path( super.generateSpeed() );
        p.addWaypoint( this.lastWaypoint.clone() );

        p.addWaypoint(convert(c));
        this.lastWaypoint = convert(c);
        return p;
    }

    @Override
    public Coord getInitialLocation() {
        /*
        * Spawn node in a random UniHub at a random position
        * */
        Point pt;
        Coordinate c;

        do {
            c = randomCoordinate();
            pt = gf.createPoint(c);
        }while (!current.polygon().contains(pt));

        this.lastWaypoint = convert(c);
        return convert(c);
    }

    public UniMovement(Settings settings){
        super(settings);

        //Parsing, Definitions etc. that only need to happen once (there is probably a cleaner way, but it works)
        if(!parsed) {
            String jsonPath = settings.getSetting("wktPath", null);
            if (jsonPath == null) {
                System.err.println("wktPath is null.");
                return;
            }

            //Read JSON file contents and save them into a String
            String content;
            try {
                content = new Scanner(new File(jsonPath)).useDelimiter("\\Z").next();
            } catch (IOException e) {
                //System.err.println(e);
                e.printStackTrace();
                return;
            }

            //Deserialize the JSON into UniHubs and UniGraph so that the ONE can use them
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            List<UniHub> vertices = deserializeVertices(jsonObject.get("vertices"));
            for(UniHub hub : vertices) {
                locationMap.put(hub.name(), hub);
            }

            List<Pair<UniHub, UniHub>> edges = deserializeEdges(jsonObject.get("edges"), vertices);

            //Now we can create a new UniGraph from the parsed Edges and Vertices
            graph = new UniGraph(vertices, edges);
            parsed = true;
        }
        Agenda a = new Agenda();
        this.agenda = a.getAgenda();
    }
    public UniMovement(UniMovement other){
        super(other);
        Agenda a = new Agenda();
        this.agenda = a.getAgenda();
        this.current = locationMap.get(agenda[0]);
        this.timeSlots = a.getTimeSlots();
    }

    @Override
    public MovementModel replicate() {
        return new UniMovement(this);
    }

    /**
     * Helper function to convert JTS Coordinates to ONE Simulator Coords
     * @param c JTS Coordinate
     * @return ONE Simulator Coord
     */
    private Coord convert(Coordinate c){
        return new Coord(c.getX(), c.getY());
    }

    /**
     * Helper to get a random Coordinate for RWP
     * @return random JTS Coordinate
     */
    private Coordinate randomCoordinate() {
        return new Coordinate(
            rng.nextDouble() * super.getMaxX(),
            rng.nextDouble() * super.getMaxY()
        );
    }

    /**
     * Since we define our UniGraph by using a JSON file, we need to deserialize the vertices.
     * This functions takes the JsonElement that lists all vertices
     * and then deserializes them directly into our UniHub record.
     * @param jsonInput vertices in JSON format
     * @return List of deserialized UniHubs
     */
    private List<UniHub> deserializeVertices(JsonElement jsonInput){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        gsonBuilder.registerTypeAdapter(Geometry.class, new GeometryDeserializer());

        Gson gson = gsonBuilder.create();
        return Arrays.asList(gson.fromJson(jsonInput, UniHub[].class));
    }

    /**
     * Since we define our UniGraph by using a JSON file, we need to deserialize the edges.
     * This function takes the previously deserialized vertices and matches the name of the UniHub to
     * the defined names in the JSON file and then creates a Pair of UniHubs.
     * This Pair represents an edge between two vertices on the graph.
     * @param jsonInput edges in JSON format
     * @param hubs List of UniHubs that are used to create the Pairs
     * @return List of Pairs representing edges
     */
    private List<Pair<UniHub, UniHub>> deserializeEdges(JsonElement jsonInput, List<UniHub> hubs){

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

        Gson gson = gsonBuilder.create();
        edge[] edges = gson.fromJson(jsonInput, edge[].class);

        List<Pair<UniHub, UniHub>> ret = new ArrayList<>();
        for(edge e : edges){
            //This just uses hubs and filters fo the name to be equal; ugly but it works
            UniHub first = hubs.stream().filter(hub -> e.first.equals(hub.name())).findFirst().orElse(null);
            UniHub second = hubs.stream().filter(hub -> e.second.equals(hub.name())).findFirst().orElse(null);
            ret.add(new Pair<>(first, second));
        }
        return ret;
    }
}
