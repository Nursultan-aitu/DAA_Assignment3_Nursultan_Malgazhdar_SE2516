package edu.daa.metrics;

public final class AlgorithmMetrics {
    private long dsuOps;
    private long edgeScans;
    private long comparisons;
    private int maxDepth;

    public long dsuOps() { return dsuOps; }
    public long edgeScans() { return edgeScans; }
    public long comparisons() { return comparisons; }
    public int maxDepth() { return maxDepth; }

    public void setDsuOps(long value) { dsuOps = value; }
    public void scanEdge() { edgeScans++; }
    public void compare() { comparisons++; }
    public void observeDepth(int depth) { if (depth > maxDepth) maxDepth = depth; }

    public AlgorithmMetrics copy() {
        AlgorithmMetrics copy = new AlgorithmMetrics();
        copy.dsuOps = dsuOps;
        copy.edgeScans = edgeScans;
        copy.comparisons = comparisons;
        copy.maxDepth = maxDepth;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AlgorithmMetrics that)) return false;
        return dsuOps == that.dsuOps && edgeScans == that.edgeScans
                && comparisons == that.comparisons && maxDepth == that.maxDepth;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dsuOps, edgeScans, comparisons, maxDepth);
    }
}
