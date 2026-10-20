package edu.daa.algorithms;

import edu.daa.graph.Graph;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DagShortestPathsTest {
    @Test
    void agreesWithBellmanFordOnRandomDagsIncludingNegativeWeights() {
        Random random = new Random(42);
        for (int sample = 0; sample < 100; sample++) {
            int n = 2 + random.nextInt(39);
            int[] order = shuffled(n, random);
            int[] position = new int[n];
            for (int i = 0; i < n; i++) position[order[i]] = i;
            Graph.Builder builder = Graph.builder(n, true);
            int m = 1 + random.nextInt(5 * n);
            for (int edge = 0; edge < m; edge++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                if (a == b) continue;
                if (position[a] > position[b]) { int swap = a; a = b; b = swap; }
                builder.addEdge(a, b, random.nextInt(41) - 20);
            }
            Graph graph = builder.build();
            DagShortestPathResult actual = DagShortestPaths.shortestPaths(graph);
            assertArrayEquals(bellmanFordFromAllSources(graph), actual.distance(), "sample=" + sample);
            assertEquals(graph.edgeCount(), actual.metrics().edgeScans());
            for (int vertex = 0; vertex < n; vertex++) {
                assertRestoredPath(graph, actual, vertex);
            }
        }
    }

    @Test
    void rejectsCyclicGraph() {
        Graph cycle = Graph.builder(2, true).addEdge(0, 1, 1).addEdge(1, 0, -2).build();
        assertThrows(IllegalArgumentException.class, () -> DagShortestPaths.shortestPaths(cycle));
    }

    private static long[] bellmanFordFromAllSources(Graph graph) {
        int n = graph.vertexCount();
        int[] indegree = new int[n];
        for (int edge = 0; edge < graph.edgeCount(); edge++) indegree[graph.edgeTo(edge)]++;
        long[] distance = new long[n];
        Arrays.fill(distance, DagShortestPathResult.INF);
        for (int vertex = 0; vertex < n; vertex++) if (indegree[vertex] == 0) distance[vertex] = 0;
        for (int pass = 1; pass < n; pass++) {
            boolean changed = false;
            for (int edge = 0; edge < graph.edgeCount(); edge++) {
                int u = graph.edgeFrom(edge), v = graph.edgeTo(edge);
                if (distance[u] != DagShortestPathResult.INF
                        && distance[u] + graph.edgeWeight(edge) < distance[v]) {
                    distance[v] = distance[u] + graph.edgeWeight(edge);
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return distance;
    }

    private static void assertRestoredPath(Graph graph, DagShortestPathResult result, int vertex) {
        java.util.List<Integer> path = result.pathTo(vertex);
        if (result.distance()[vertex] == DagShortestPathResult.INF) {
            assertTrue(path.isEmpty());
            return;
        }
        assertFalse(path.isEmpty());
        assertEquals(0, result.distance()[path.get(0)]);
        assertEquals(vertex, path.get(path.size() - 1));
        for (int i = 1; i < path.size(); i++) {
            int u = path.get(i - 1), v = path.get(i);
            long delta = result.distance()[v] - result.distance()[u];
            boolean matchingEdge = false;
            for (int edge = 0; edge < graph.edgeCount(); edge++) {
                if (graph.edgeFrom(edge) == u && graph.edgeTo(edge) == v
                        && graph.edgeWeight(edge) == delta) matchingEdge = true;
            }
            assertTrue(matchingEdge, "parent link must explain the distance change");
        }
    }

    private static int[] shuffled(int n, Random random) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = values[i]; values[i] = values[j]; values[j] = swap;
        }
        return values;
    }
}
