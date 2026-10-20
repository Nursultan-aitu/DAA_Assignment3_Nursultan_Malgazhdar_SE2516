package edu.daa.algorithms;

import edu.daa.metrics.AlgorithmMetrics;

public record MstResult(long totalWeight, int edgeCount, int components,
                        int[] selectedEdges, AlgorithmMetrics metrics) { }
