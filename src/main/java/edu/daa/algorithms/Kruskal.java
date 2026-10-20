package edu.daa.algorithms;

import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.util.Arrays;

public final class Kruskal {
    private Kruskal() { }

    public static MstResult minimumSpanningForest(Graph graph) {
        if (graph.directed()) throw new IllegalArgumentException("Kruskal requires an undirected graph");
        int m = graph.edgeCount();
        int[] order = new int[m];
        int[] auxiliary = new int[m];
        for (int i = 0; i < m; i++) order[i] = i;
        AlgorithmMetrics metrics = new AlgorithmMetrics();
        mergeSort(graph, order, auxiliary, metrics);

        DisjointSetUnion dsu = DisjointSetUnion.makeSet(graph.vertexCount());
        int[] selected = new int[Math.max(0, graph.vertexCount() - 1)];
        int selectedCount = 0;
        long totalWeight = 0;

        for (int edge : order) {
            metrics.scanEdge();
            int u = graph.edgeFrom(edge);
            int v = graph.edgeTo(edge);
            int rootU = dsu.find(u);
            int rootV = dsu.find(v);
            if (rootU != rootV && dsu.union(u, v)) {
                if (selectedCount == selected.length) {
                    selected = Arrays.copyOf(selected, Math.max(1, selected.length * 2));
                }
                selected[selectedCount++] = edge;
                totalWeight += graph.edgeWeight(edge);
                if (dsu.components() == 1) break;
            }
        }
        metrics.setDsuOps(dsu.operations());
        return new MstResult(totalWeight, selectedCount, dsu.components(),
                Arrays.copyOf(selected, selectedCount), metrics);
    }

    private static void mergeSort(Graph graph, int[] order, int[] auxiliary, AlgorithmMetrics metrics) {
        int n = order.length;
        int[] source = order;
        int[] destination = auxiliary;
        for (int width = 1; width < n; width = width > n / 2 ? n : width * 2) {
            for (int left = 0; left < n; left += 2 * width) {
                int middle = Math.min(left + width, n);
                int right = Math.min(left + 2 * width, n);
                int i = left, j = middle, out = left;
                while (i < middle && j < right) {
                    metrics.compare();
                    int a = source[i], b = source[j];
                    int byWeight = Integer.compare(graph.edgeWeight(a), graph.edgeWeight(b));
                    if (byWeight < 0 || (byWeight == 0 && a <= b)) destination[out++] = source[i++];
                    else destination[out++] = source[j++];
                }
                while (i < middle) destination[out++] = source[i++];
                while (j < right) destination[out++] = source[j++];
            }
            int[] swap = source;
            source = destination;
            destination = swap;
        }
        if (source != order) System.arraycopy(source, 0, order, 0, n);
    }
}
