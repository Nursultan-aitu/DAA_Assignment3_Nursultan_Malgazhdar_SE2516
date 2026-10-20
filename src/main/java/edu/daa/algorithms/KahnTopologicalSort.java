package edu.daa.algorithms;

import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class KahnTopologicalSort {
    private KahnTopologicalSort() { }

    public static TopologicalResult sort(Graph graph) {
        if (!graph.directed()) throw new IllegalArgumentException("topological ordering requires a directed graph");
        int n = graph.vertexCount();
        int[] indegree = indegrees(graph);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < n; vertex++) if (indegree[vertex] == 0) queue.add(vertex);
        int[] order = new int[n];
        int size = 0;
        AlgorithmMetrics metrics = new AlgorithmMetrics();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order[size++] = vertex;
            for (int arc = graph.firstArc(vertex); arc != -1; arc = graph.nextArc(arc)) {
                metrics.scanEdge();
                int destination = graph.arcTo(arc);
                if (--indegree[destination] == 0) queue.add(destination);
            }
        }
        return new TopologicalResult(Arrays.copyOf(order, size), size != n, metrics);
    }

    public static boolean isValid(Graph graph, int[] order) {
        if (order.length != graph.vertexCount()) return false;
        int[] position = new int[order.length];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            int vertex = order[i];
            if (vertex < 0 || vertex >= order.length || position[vertex] != -1) return false;
            position[vertex] = i;
        }
        for (int edge = 0; edge < graph.edgeCount(); edge++) {
            if (position[graph.edgeFrom(edge)] >= position[graph.edgeTo(edge)]) return false;
        }
        return true;
    }

    static int[] indegrees(Graph graph) {
        int[] indegree = new int[graph.vertexCount()];
        for (int edge = 0; edge < graph.edgeCount(); edge++) indegree[graph.edgeTo(edge)]++;
        return indegree;
    }
}
