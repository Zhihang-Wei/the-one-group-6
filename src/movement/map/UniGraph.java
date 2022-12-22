package movement.map;

import movement.UniMovement.UniHub;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;

/**
 * This class represents the graph model that is used by UniMovement.
 * Nodes use this functionality to find the route they need to take to get to their destination.
 */
public class UniGraph {
    //This class uses DijkstraShortestPath to find the route to the destination on the Graph
    private final DijkstraShortestPath<UniHub, DefaultEdge> dijkstraShortestPath;

    public UniGraph(List<UniHub> vertices, List<Pair<UniHub, UniHub>> edges) {
        Graph<UniHub, DefaultEdge> graph = createUniGraph(vertices, edges);
        dijkstraShortestPath = new DijkstraShortestPath<>(graph);
    }

    /**
     * This function uses dijkstraShortestPath to get the shortest path on a graph to the destination.
     * @param source start UniHub of a node
     * @param sink goal UniHub of a node
     * @return List of UniHubs that represent the route the node needs to take to get from start to goal
     */
    public List<UniHub> getPath(UniHub source, UniHub sink) {
        return dijkstraShortestPath.getPath(source, sink).getVertexList();
    }


    /**
     * Helper function to create the jgrapht Graph of a UniGraph from the deserialized vertices and edges in UniMovement
     * @param uniHubs deserialized UniHubs from vertices in the JSON file
     * @param edges deserialized Pair of UniHubs from the edges in the JSON file
     * @return jgrapht Graph that represents the building provided by UniHub vertices and Pair<UniHub,UniHub> edges
     */
    private Graph<UniHub, DefaultEdge> createUniGraph(List<UniHub> uniHubs, List<Pair<UniHub, UniHub>> edges) {
        Graph<UniHub, DefaultEdge> uni = new SimpleGraph<>(DefaultEdge.class);

        for (UniHub uniHub : uniHubs) {
            uni.addVertex(uniHub);
        }

        for (Pair<UniHub, UniHub> edge : edges) {
            UniHub firstVertex = edge.getFirst();
            UniHub secondVertex = edge.getSecond();
            uni.addEdge(firstVertex, secondVertex);
        }

        return uni;
    }
}
