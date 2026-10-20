package edu.daa.algorithms;

import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.util.Arrays;

public final class DagShortestPaths {
    private DagShortestPaths() { }

    public static DagShortestPathResult shortestPaths(Graph graph) {
        if (!graph.directed()) throw new IllegalArgumentException("DAG shortest paths require a directed graph");
        TopologicalResult topological = KahnTopologicalSort.sort(graph);
        if (topological.hasCycle()) throw new IllegalArgumentException("graph contains a cycle");

        int n = graph.vertexCount();
        int[] indegree = KahnTopologicalSort.indegrees(graph);
        long[] distance = new long[n];
        int[] parent = new int[n];
        Arrays.fill(distance, DagShortestPathResult.INF);
        Arrays.fill(parent, -1);
        for (int vertex = 0; vertex < n; vertex++) if (indegree[vertex] == 0) distance[vertex] = 0;

        AlgorithmMetrics metrics = new AlgorithmMetrics();
        for (int vertex : topological.order()) {
            for (int arc = graph.firstArc(vertex); arc != -1; arc = graph.nextArc(arc)) {
                metrics.scanEdge();
                if (distance[vertex] == DagShortestPathResult.INF) continue;
                int destination = graph.arcTo(arc);
                long candidate = distance[vertex] + graph.arcWeight(arc);
                if (candidate < distance[destination]) {
                    distance[destination] = candidate;
                    parent[destination] = vertex;
                }
            }
        }
        return new DagShortestPathResult(distance, parent, metrics);
    }
}
