package edu.daa.algorithms;

import edu.daa.graph.Graph;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KosarajuTest {
    @Test
    void agreesWithBruteForceReachability() {
        Random random = new Random(42);
        for (int sample = 0; sample < 20; sample++) {
            int n = sample == 19 ? 200 : 5 + random.nextInt(50);
            Graph.Builder builder = Graph.builder(n, true);
            int m = 3 * n;
            for (int edge = 0; edge < m; edge++) {
                builder.addEdge(random.nextInt(n), random.nextInt(n), 1);
            }
            Graph graph = builder.build();
            SccResult result = Kosaraju.stronglyConnectedComponents(graph);
            boolean[][] reachable = allReachability(graph);
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    assertEquals(reachable[u][v] && reachable[v][u],
                            result.componentOf()[u] == result.componentOf()[v],
                            "sample=" + sample + ", u=" + u + ", v=" + v);
                }
            }
            assertEquals(2L * graph.edgeCount(), result.metrics().edgeScans());
        }
    }

    @Test
    void pathOfOneHundredThousandVerticesIsStackSafe() {
        int n = 100_000;
        Graph.Builder builder = Graph.builder(n, true);
        for (int vertex = 0; vertex + 1 < n; vertex++) builder.addEdge(vertex, vertex + 1, 1);
        SccResult result = assertDoesNotThrow(() -> Kosaraju.stronglyConnectedComponents(builder.build()));
        assertEquals(n, result.componentCount());
        assertEquals(n, result.metrics().maxDepth());
    }

    private static boolean[][] allReachability(Graph graph) {
        int n = graph.vertexCount();
        boolean[][] result = new boolean[n][n];
        for (int start = 0; start < n; start++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            result[start][start] = true;
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (int arc = graph.firstArc(vertex); arc != -1; arc = graph.nextArc(arc)) {
                    int next = graph.arcTo(arc);
                    if (!result[start][next]) { result[start][next] = true; queue.add(next); }
                }
            }
        }
        return result;
    }
}
