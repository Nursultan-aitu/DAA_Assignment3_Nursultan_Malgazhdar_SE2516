package edu.daa.algorithms;

import edu.daa.graph.Graph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopologicalSortTest {
    @Test
    void returnsValidOrderForDag() {
        Graph graph = Graph.builder(6, true)
                .addEdge(0, 2, 1).addEdge(1, 2, 1).addEdge(1, 3, 1)
                .addEdge(2, 4, 1).addEdge(3, 4, 1).addEdge(4, 5, 1)
                .build();
        TopologicalResult result = KahnTopologicalSort.sort(graph);
        assertFalse(result.hasCycle());
        assertTrue(KahnTopologicalSort.isValid(graph, result.order()));
        assertEquals(graph.edgeCount(), result.metrics().edgeScans());
    }

    @Test
    void reportsCycleInsteadOfReturningWrongOrder() {
        Graph graph = Graph.builder(4, true)
                .addEdge(0, 1, 1).addEdge(1, 2, 1).addEdge(2, 0, 1).addEdge(2, 3, 1)
                .build();
        TopologicalResult result = KahnTopologicalSort.sort(graph);
        assertTrue(result.hasCycle());
        assertTrue(result.order().length < graph.vertexCount());
        assertFalse(KahnTopologicalSort.isValid(graph, result.order()));
    }
}
