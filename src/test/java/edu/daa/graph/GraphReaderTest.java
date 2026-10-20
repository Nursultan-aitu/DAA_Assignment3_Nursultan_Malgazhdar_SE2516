package edu.daa.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphReaderTest {
    @TempDir Path temporaryDirectory;

    @Test
    void readsLoopsParallelEdgesAndIsolatedVertices() throws Exception {
        Path file = temporaryDirectory.resolve("graph.txt");
        Files.writeString(file, """
                # graph with an isolated vertex 4
                5 4 undirected
                0 0 7
                0 1 5
                0 1 2
                2 3 -4
                """);
        Graph graph = GraphReader.read(file);
        assertFalse(graph.directed());
        assertEquals(5, graph.vertexCount());
        assertEquals(4, graph.edgeCount());
        assertEquals(-4, graph.edgeWeight(3));
        assertEquals(-1, graph.firstArc(4));
    }

    @Test
    void rejectsWrongDeclaredEdgeCount() throws Exception {
        Path file = temporaryDirectory.resolve("bad.txt");
        Files.writeString(file, "3 2 directed\n0 1 1\n");
        assertThrows(java.io.IOException.class, () -> GraphReader.read(file));
    }
}
