package edu.daa.algorithms;

public final class DisjointSetUnion {
    private final int[] parent;
    private final byte[] rank;
    private int components;
    private long findCalls;
    private long successfulUnions;

    public DisjointSetUnion(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        parent = new int[n];
        rank = new byte[n];
        components = n;
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    public static DisjointSetUnion makeSet(int n) {
        return new DisjointSetUnion(n);
    }

    public int find(int x) {
        check(x);
        findCalls++;
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != x) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) return false;
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        components--;
        successfulUnions++;
        return true;
    }

    public int components() { return components; }
    public long findCalls() { return findCalls; }
    public long successfulUnions() { return successfulUnions; }
    public long operations() { return findCalls + successfulUnions; }

    private void check(int x) {
        if (x < 0 || x >= parent.length) throw new IndexOutOfBoundsException("vertex: " + x);
    }
}
