package edu.daa.algorithms;

import edu.daa.metrics.AlgorithmMetrics;

public record SccResult(int componentCount, int[] componentOf, AlgorithmMetrics metrics) { }
