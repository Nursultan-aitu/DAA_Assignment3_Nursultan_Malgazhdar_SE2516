package edu.daa.algorithms;

import edu.daa.benchmark.GraphGenerator;
import edu.daa.graph.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void kruskalAndPrimAgreeOnOneHundredConnectedGraphs() {
        for (int seed = 0; seed < 100; seed++) {
            int n = 2 + seed % 39;
            int m = n - 1 + (seed * 17) % (4 * n);
            Graph graph = GraphGenerator.undirected(n, m, seed);
            MstResult kruskal = Kruskal.minimumSpanningForest(graph);
            MstResult prim = Prim.minimumSpanningForest(graph);
            assertEquals(kruskal.totalWeight(), prim.totalWeight(), "seed=" + seed);
            assertEquals(n - 1, kruskal.edgeCount());
            assertEquals(n - 1, prim.edgeCount());
            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());
        }
    }

    @Test
    void disconnectedGraphProducesMinimumSpanningForest() {
        Graph graph = Graph.builder(6, false)
                .addEdge(0, 1, 5).addEdge(1, 2, 2).addEdge(0, 2, 20)
                .addEdge(3, 4, 7).addEdge(3, 4, 9)
                .addEdge(5, 5, -100)
                .build();
        MstResult kruskal = Kruskal.minimumSpanningForest(graph);
        MstResult prim = Prim.minimumSpanningForest(graph);
        assertAll(
                () -> assertEquals(14, kruskal.totalWeight()),
                () -> assertEquals(14, prim.totalWeight()),
                () -> assertEquals(3, kruskal.components()),
                () -> assertEquals(3, prim.components()),
                () -> assertEquals(3, kruskal.edgeCount()),
                () -> assertEquals(3, prim.edgeCount())
        );
    }
}
