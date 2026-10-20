package edu.daa.graph;

import java.util.Arrays;

/**
 * Immutable weighted graph backed by primitive arrays. Vertices are numbered 0..n-1.
 * The same representation supports directed and undirected graphs and remains compact
 * enough for the assignment's two-million-edge cases.
 */
public final class Graph {
    private final int vertexCount;
    private final boolean directed;
    private final int[] from;
    private final int[] to;
    private final int[] weight;
    private final int[] head;
    private final int[] arcTo;
    private final int[] arcWeight;
    private final int[] arcEdge;
    private final int[] nextArc;

    private Graph(int vertexCount, boolean directed, int[] from, int[] to, int[] weight) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount must be non-negative");
        }
        if (from.length != to.length || from.length != weight.length) {
            throw new IllegalArgumentException("edge arrays must have equal lengths");
        }
        this.vertexCount = vertexCount;
        this.directed = directed;
        this.from = from;
        this.to = to;
        this.weight = weight;

        int arcCount = directed ? from.length : Math.multiplyExact(from.length, 2);
        head = new int[vertexCount];
        Arrays.fill(head, -1);
        arcTo = new int[arcCount];
        arcWeight = new int[arcCount];
        arcEdge = new int[arcCount];
        nextArc = new int[arcCount];

        int arc = 0;
        for (int edge = 0; edge < from.length; edge++) {
            checkVertex(from[edge]);
            checkVertex(to[edge]);
            arc = addArc(arc, edge, from[edge], to[edge], weight[edge]);
            if (!directed) {
                arc = addArc(arc, edge, to[edge], from[edge], weight[edge]);
            }
        }
    }

    /** Takes ownership of the supplied arrays; callers must not modify them afterwards. */
    public static Graph takeOwnership(int vertexCount, boolean directed,
                                      int[] from, int[] to, int[] weight) {
        return new Graph(vertexCount, directed, from, to, weight);
    }

    /** Creates a defensive copy of the supplied edge arrays. */
    public static Graph copyOf(int vertexCount, boolean directed,
                               int[] from, int[] to, int[] weight) {
        return new Graph(vertexCount, directed, from.clone(), to.clone(), weight.clone());
    }

    private int addArc(int arc, int edge, int source, int destination, int w) {
        arcTo[arc] = destination;
        arcWeight[arc] = w;
        arcEdge[arc] = edge;
        nextArc[arc] = head[source];
        head[source] = arc;
        return arc + 1;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException("vertex outside 0..n-1: " + vertex);
        }
    }

    public int vertexCount() { return vertexCount; }
    public int edgeCount() { return from.length; }
    public boolean directed() { return directed; }
    public int edgeFrom(int edge) { return from[edge]; }
    public int edgeTo(int edge) { return to[edge]; }
    public int edgeWeight(int edge) { return weight[edge]; }
    public int firstArc(int vertex) { return head[vertex]; }
    public int nextArc(int arc) { return nextArc[arc]; }
    public int arcTo(int arc) { return arcTo[arc]; }
    public int arcWeight(int arc) { return arcWeight[arc]; }
    public int arcEdge(int arc) { return arcEdge[arc]; }

    public Graph reversed() {
        if (!directed) {
            return this;
        }
        return new Graph(vertexCount, true, to.clone(), from.clone(), weight.clone());
    }

    public static Builder builder(int vertexCount, boolean directed) {
        return new Builder(vertexCount, directed);
    }

    public static final class Builder {
        private final int vertexCount;
        private final boolean directed;
        private int[] from = new int[16];
        private int[] to = new int[16];
        private int[] weight = new int[16];
        private int size;

        private Builder(int vertexCount, boolean directed) {
            if (vertexCount < 0) {
                throw new IllegalArgumentException("vertexCount must be non-negative");
            }
            this.vertexCount = vertexCount;
            this.directed = directed;
        }

        public Builder addEdge(int source, int destination, int w) {
            if (source < 0 || source >= vertexCount || destination < 0 || destination >= vertexCount) {
                throw new IllegalArgumentException("edge endpoint outside 0..n-1");
            }
            ensureCapacity(size + 1);
            from[size] = source;
            to[size] = destination;
            weight[size] = w;
            size++;
            return this;
        }

        private void ensureCapacity(int required) {
            if (required <= from.length) return;
            int capacity = Math.max(required, from.length * 2);
            from = Arrays.copyOf(from, capacity);
            to = Arrays.copyOf(to, capacity);
            weight = Arrays.copyOf(weight, capacity);
        }

        public Graph build() {
            return Graph.takeOwnership(vertexCount, directed,
                    Arrays.copyOf(from, size), Arrays.copyOf(to, size), Arrays.copyOf(weight, size));
        }
    }
}
