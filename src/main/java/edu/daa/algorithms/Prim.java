package edu.daa.algorithms;

import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.util.Arrays;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() { }

    private record Candidate(int from, int to, int weight, int edge) { }

    public static MstResult minimumSpanningForest(Graph graph) {
        if (graph.directed()) throw new IllegalArgumentException("Prim requires an undirected graph");
        int n = graph.vertexCount();
        boolean[] inTree = new boolean[n];
        int[] selected = new int[Math.max(0, n - 1)];
        int selectedCount = 0;
        int components = 0;
        long totalWeight = 0;
        AlgorithmMetrics metrics = new AlgorithmMetrics();
        PriorityQueue<Candidate> queue = new PriorityQueue<>((a, b) -> {
            metrics.compare();
            int byWeight = Integer.compare(a.weight, b.weight);
            if (byWeight != 0) return byWeight;
            return Integer.compare(a.edge, b.edge);
        });

        for (int start = 0; start < n; start++) {
            if (inTree[start]) continue;
            components++;
            inTree[start] = true;
            addOutgoing(graph, start, inTree, queue, metrics);
            while (!queue.isEmpty()) {
                Candidate candidate = queue.remove();
                if (inTree[candidate.to]) continue;
                inTree[candidate.to] = true;
                selected[selectedCount++] = candidate.edge;
                totalWeight += candidate.weight;
                addOutgoing(graph, candidate.to, inTree, queue, metrics);
            }
        }
        return new MstResult(totalWeight, selectedCount, components,
                Arrays.copyOf(selected, selectedCount), metrics);
    }

    private static void addOutgoing(Graph graph, int vertex, boolean[] inTree,
                                    PriorityQueue<Candidate> queue, AlgorithmMetrics metrics) {
        for (int arc = graph.firstArc(vertex); arc != -1; arc = graph.nextArc(arc)) {
            metrics.scanEdge();
            int destination = graph.arcTo(arc);
            if (!inTree[destination]) {
                queue.add(new Candidate(vertex, destination, graph.arcWeight(arc), graph.arcEdge(arc)));
            }
        }
    }
}
