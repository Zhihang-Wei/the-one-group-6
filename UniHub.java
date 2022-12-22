package movement;

import core.Settings;
import org.locationtech.jts.geom.*;

public class UniHub {

    private String name;
    private Geometry polygon;
    //List of neighbors of this hub
    //private List<UniHub> neighbors = new ArrayList<>();

    public UniHub(Geometry polygon, String name) {
        this.polygon = polygon;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format(
                "[UniHub: name=%1$s, polygon=%2$s]",
                name, polygon);
    }

    public String getName() {
        return name;
    }

    public Geometry getPolygon() {
        return polygon;
    }


    /*public void addNeighbor(UniHub hub) {
        this.neighbors.add(hub);
    }*/
}