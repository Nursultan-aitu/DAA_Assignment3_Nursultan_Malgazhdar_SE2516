package edu.daa.algorithms;

import edu.daa.metrics.AlgorithmMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record DagShortestPathResult(long[] distance, int[] parent, AlgorithmMetrics metrics) {
    public static final long INF = Long.MAX_VALUE / 4;

    public List<Integer> pathTo(int vertex) {
        if (vertex < 0 || vertex >= distance.length) throw new IndexOutOfBoundsException("vertex: " + vertex);
        if (distance[vertex] == INF) return List.of();
        ArrayList<Integer> path = new ArrayList<>();
        for (int current = vertex; current != -1; current = parent[current]) path.add(current);
        Collections.reverse(path);
        return List.copyOf(path);
    }

    public long sumOfDistances() {
        long sum = 0;
        for (long value : distance) if (value != INF) sum = Math.addExact(sum, value);
        return sum;
    }
}
