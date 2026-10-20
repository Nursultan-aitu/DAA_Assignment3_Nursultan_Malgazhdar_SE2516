package edu.daa.benchmark;

import edu.daa.graph.Graph;

import java.util.Random;

/** Implements the assignment generator literally; each public method starts with a fresh Random(seed). */
public final class GraphGenerator {
    private GraphGenerator() { }

    public static Graph undirected(int n, int m, long seed) {
        if (m < Math.max(0, n - 1)) throw new IllegalArgumentException("connected graph needs at least n-1 edges");
        Random random = new Random(seed);
        int[] permutation = shuffledIdentity(n, random);
        int[] from = new int[m];
        int[] to = new int[m];
        int[] weight = new int[m];
        int edge = 0;
        for (int i = 1; i < n; i++) {
            from[edge] = permutation[random.nextInt(i)];
            to[edge] = permutation[i];
            weight[edge] = 1 + random.nextInt(1_000);
            edge++;
        }
        while (edge < m) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (b == a) b = random.nextInt(n);
            from[edge] = a;
            to[edge] = b;
            weight[edge] = 1 + random.nextInt(1_000);
            edge++;
        }
        return Graph.takeOwnership(n, false, from, to, weight);
    }

    public static Graph directed(int n, int m, long seed) {
        Random random = new Random(seed);
        int[] from = new int[m];
        int[] to = new int[m];
        int[] weight = new int[m];
        for (int edge = 0; edge < m; edge++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (b == a) b = random.nextInt(n);
            from[edge] = a;
            to[edge] = b;
            weight[edge] = 1 + random.nextInt(1_000);
        }
        return Graph.takeOwnership(n, true, from, to, weight);
    }

    public static Graph dag(int n, int m, long seed) {
        Random random = new Random(seed);
        int[] rank = shuffledIdentity(n, random);
        int[] from = new int[m];
        int[] to = new int[m];
        int[] weight = new int[m];
        for (int edge = 0; edge < m; edge++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            while (b == a) b = random.nextInt(n);
            if (rank[a] > rank[b]) {
                int swap = a;
                a = b;
                b = swap;
            }
            from[edge] = a;
            to[edge] = b;
            weight[edge] = 1 + random.nextInt(1_000);
        }
        return Graph.takeOwnership(n, true, from, to, weight);
    }

    private static int[] shuffledIdentity(int n, Random random) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = values[i];
            values[i] = values[j];
            values[j] = swap;
        }
        return values;
    }
}
