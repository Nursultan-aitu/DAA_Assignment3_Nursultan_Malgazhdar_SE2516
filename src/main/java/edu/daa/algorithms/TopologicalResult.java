package edu.daa.algorithms;

import edu.daa.metrics.AlgorithmMetrics;

public record TopologicalResult(int[] order, boolean hasCycle, AlgorithmMetrics metrics) { }
