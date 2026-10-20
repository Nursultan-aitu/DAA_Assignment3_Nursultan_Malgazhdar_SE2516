package edu.daa.benchmark;

import edu.daa.algorithms.DagShortestPathResult;
import edu.daa.algorithms.DagShortestPaths;
import edu.daa.algorithms.KahnTopologicalSort;
import edu.daa.algorithms.Kosaraju;
import edu.daa.algorithms.Kruskal;
import edu.daa.algorithms.MstResult;
import edu.daa.algorithms.Prim;
import edu.daa.algorithms.SccResult;
import edu.daa.algorithms.TopologicalResult;
import edu.daa.graph.Graph;
import edu.daa.metrics.AlgorithmMetrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class Benchmark {
    private static final int[] SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    private static final int[] DENSITIES = {4, 20};
    private static final int RUNS = 5;
    private static final long SEED = 42L;
    private static final long[] EXPECTED_MST = {
            147_970, 30_363, 468_911, 97_589, 1_486_139,
            309_550, 4_715_515, 968_517, 15_012_094, 3_056_681
    };
    private static final long[] EXPECTED_SCC = {27, 1, 137, 1, 373, 1, 1_188, 1, 3_923, 1};
    private static final long[] EXPECTED_DAG_SUM = {
            396_274, 252_411, 1_271_911, 723_157, 3_941_828,
            2_381_342, 12_551_891, 7_376_612, 40_096_539, 23_512_087
    };

    private Benchmark() { }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("results", "results.csv") : Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        List<Row> rows = new ArrayList<>(50);
        int reference = 0;
        for (int n : SIZES) {
            for (int factor : DENSITIES) {
                int m = Math.multiplyExact(n, factor);
                String density = factor == 4 ? "sparse" : "dense";
                System.out.printf(Locale.ROOT, "n=%,d m=%,d (%s)%n", n, m, density);

                Graph undirected = GraphGenerator.undirected(n, m, SEED);
                Measured kruskal = measure(() -> {
                    MstResult result = Kruskal.minimumSpanningForest(undirected);
                    return new RunResult(result.totalWeight(), result.metrics());
                });
                Measured prim = measure(() -> {
                    MstResult result = Prim.minimumSpanningForest(undirected);
                    return new RunResult(result.totalWeight(), result.metrics());
                });
                require(kruskal.result == prim.result, "Kruskal and Prim disagree");
                require(kruskal.result == EXPECTED_MST[reference],
                        "MST reference mismatch: " + kruskal.result + " != " + EXPECTED_MST[reference]);
                rows.add(Row.of("Kruskal", "undirected_" + density, n, m, kruskal));
                rows.add(Row.of("Prim", "undirected_" + density, n, m, prim));
                Graph directed = GraphGenerator.directed(n, m, SEED);
                Measured kosaraju = measure(() -> {
                    SccResult result = Kosaraju.stronglyConnectedComponents(directed);
                    return new RunResult(result.componentCount(), result.metrics());
                });
                require(kosaraju.result == EXPECTED_SCC[reference],
                        "SCC reference mismatch: " + kosaraju.result + " != " + EXPECTED_SCC[reference]);
                require(kosaraju.metrics.edgeScans() == 2L * m, "Kosaraju must scan exactly 2m edges");
                rows.add(Row.of("Kosaraju", "directed_" + density, n, m, kosaraju));
                Graph dag = GraphGenerator.dag(n, m, SEED);
                TopologicalResult validationOrder = KahnTopologicalSort.sort(dag);
                require(!validationOrder.hasCycle(), "generated DAG contains a cycle");
                require(KahnTopologicalSort.isValid(dag, validationOrder.order()), "invalid topological order");
                Measured kahn = measure(() -> {
                    TopologicalResult result = KahnTopologicalSort.sort(dag);
                    return new RunResult(result.order().length, result.metrics());
                });
                Measured paths = measure(() -> {
                    DagShortestPathResult result = DagShortestPaths.shortestPaths(dag);
                    return new RunResult(result.sumOfDistances(), result.metrics());
                });
                require(paths.result == EXPECTED_DAG_SUM[reference],
                        "DAG reference mismatch: " + paths.result + " != " + EXPECTED_DAG_SUM[reference]);
                require(kahn.metrics.edgeScans() == m, "Kahn must scan exactly m edges");
                require(paths.metrics.edgeScans() == m, "DAG relaxation must scan exactly m edges");
                rows.add(Row.of("Kahn", "dag_" + density, n, m, kahn));
                rows.add(Row.of("DagShortestPath", "dag_" + density, n, m, paths));
                reference++;
            }
        }
        writeCsv(output, rows);
        System.out.println("Verified all reference values and wrote " + rows.size() + " rows to " + output);
    }

    private static Measured measure(Supplier<RunResult> task) {
        double[] milliseconds = new double[RUNS];
        RunResult reference = null;
        for (int run = 0; run < RUNS; run++) {
            long start = System.nanoTime();
            RunResult result = task.get();
            milliseconds[run] = (System.nanoTime() - start) / 1_000_000.0;
            if (reference == null) reference = result;
            else {
                require(result.result == reference.result, "non-deterministic result across runs");
                require(result.metrics.equals(reference.metrics), "non-deterministic counters across runs");
            }
        }
        Arrays.sort(milliseconds);
        return new Measured(milliseconds[RUNS / 2], reference.result, reference.metrics.copy());
    }

    private static void writeCsv(Path output, List<Row> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            writer.newLine();
            for (Row row : rows) {
                writer.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d",
                        row.algorithm, row.graph, row.n, row.m, row.timeMs,
                        row.metrics.dsuOps(), row.metrics.edgeScans(), row.metrics.comparisons(),
                        row.metrics.maxDepth(), row.result));
                writer.newLine();
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record RunResult(long result, AlgorithmMetrics metrics) { }
    private record Measured(double timeMs, long result, AlgorithmMetrics metrics) { }
    private record Row(String algorithm, String graph, int n, int m, double timeMs,
                       AlgorithmMetrics metrics, long result) {
        static Row of(String algorithm, String graph, int n, int m, Measured measured) {
            return new Row(algorithm, graph, n, m, measured.timeMs, measured.metrics, measured.result);
        }
    }
}
