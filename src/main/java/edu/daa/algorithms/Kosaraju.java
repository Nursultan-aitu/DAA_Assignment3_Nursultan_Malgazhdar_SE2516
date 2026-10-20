package edu.daa.algorithms;

import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.util.Arrays;

public final class Kosaraju {
    private Kosaraju() { }

    public static SccResult stronglyConnectedComponents(Graph graph) {
        if (!graph.directed()) throw new IllegalArgumentException("Kosaraju requires a directed graph");
        int n = graph.vertexCount();
        AlgorithmMetrics metrics = new AlgorithmMetrics();
        boolean[] visited = new boolean[n];
        int[] finishOrder = new int[n];
        int finishSize = 0;
        int[] stackVertex = new int[n];
        int[] stackArc = new int[n];

        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            int depth = 1;
            stackVertex[0] = start;
            stackArc[0] = graph.firstArc(start);
            visited[start] = true;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int top = depth - 1;
                int arc = stackArc[top];
                if (arc == -1) {
                    finishOrder[finishSize++] = stackVertex[top];
                    depth--;
                    continue;
                }
                stackArc[top] = graph.nextArc(arc);
                metrics.scanEdge();
                int next = graph.arcTo(arc);
                if (!visited[next]) {
                    visited[next] = true;
                    stackVertex[depth] = next;
                    stackArc[depth] = graph.firstArc(next);
                    depth++;
                    metrics.observeDepth(depth);
                }
            }
        }

        Graph reversed = graph.reversed();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int componentCount = 0;
        for (int position = finishSize - 1; position >= 0; position--) {
            int start = finishOrder[position];
            if (component[start] != -1) continue;
            int depth = 1;
            stackVertex[0] = start;
            component[start] = componentCount;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int vertex = stackVertex[--depth];
                for (int arc = reversed.firstArc(vertex); arc != -1; arc = reversed.nextArc(arc)) {
                    metrics.scanEdge();
                    int next = reversed.arcTo(arc);
                    if (component[next] == -1) {
                        component[next] = componentCount;
                        stackVertex[depth++] = next;
                        metrics.observeDepth(depth);
                    }
                }
            }
            componentCount++;
        }
        return new SccResult(componentCount, component, metrics);
    }
}
