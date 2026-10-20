# Assignment 3 - Graphs: DSU, MST, SCC and Shortest Paths

**Nursultan Malgazhdar | SE-2516**  
Design and Analysis of Algorithms

This Maven project implements a shared adjacency-list graph, DSU with both required heuristics, Kruskal and Prim minimum spanning forests, iterative Kosaraju SCC, Kahn topological ordering, and shortest paths in a DAG. The benchmark reproduces the assignment generator and fails immediately if an algorithm disagrees with another implementation or with the provided reference table.

## Requirements and commands

- JDK 17 or newer (JDK 21 is preferred).
- PowerShell on Windows for the convenience script.
- Python 3 with Pillow only when regenerating plots and `REPORT.md`
  (`python -m pip install -r tools/requirements.txt`).

```powershell
.\run.ps1 -Task test       # clean build and all JUnit 5 tests
.\run.ps1 -Task benchmark  # five-run medians and exact-reference validation
.\run.ps1 -Task plots      # regenerate three PNG plots and REPORT.md from the CSV
.\run.ps1 -Task all        # perform all of the above
```

The Maven Wrapper may also be used directly:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd compile
java -Xmx3g -cp target/classes edu.daa.benchmark.Benchmark
```

The full benchmark includes graphs with two million edges and is intentionally launched with a 3 GB heap limit. Exact operation counts and final results are reproducible; elapsed times are machine-dependent.

## Project layout

```text
src/main/java/edu/daa/graph/       Graph and text-file reader
src/main/java/edu/daa/algorithms/  DSU, MST, SCC, topological order, DAG paths
src/main/java/edu/daa/metrics/     Deterministic operation counters
src/main/java/edu/daa/benchmark/   Exact Random(42) generator and benchmark
src/test/java/edu/daa/             Differential, edge-case, and stack-safety tests
data/                              Small connected, disconnected, cyclic, and negative DAG inputs
results/results.csv                50 verified benchmark rows
results/plots/                     Three required PNG plots
tools/                             Plot and report regeneration scripts
REPORT.md                          Complexity, correctness, and algorithm-choice analysis
```

## Input format

The first non-comment line declares the graph. It is followed by exactly `m` weighted edges:

```text
n m directed|undirected
u v w
```

Blank lines and lines beginning with `#` are ignored. Vertices must be in `0..n-1`; weights are Java `int` values. Self-loops, parallel edges, isolated vertices, negative weights, and disconnected graphs are retained by the graph core rather than silently discarded.

## Design notes

- `Graph` uses primitive edge and adjacency arrays. This avoids millions of per-edge wrapper objects in the largest cases while preserving a normal adjacency-list interface.
- `DisjointSetUnion` uses union by rank and path compression. Its `find` calls and successful unions are counted where they occur.
- Kruskal uses a counted stable merge sort and stops scanning once a connected graph has `n-1` selected edges. Prim uses `PriorityQueue`, starts at vertex 0, and restarts at every still-unvisited vertex to return a forest.
- Kosaraju stores vertices and adjacency cursors in explicit arrays for both passes. Its `maxDepth` is the greatest explicit-stack depth, and it scans exactly `2m` directed edges.
- Kahn returns a partial order with `hasCycle=true` if fewer than `n` vertices leave the queue. `isValid` checks `pos[u] < pos[v]` for every edge.
- DAG shortest paths initialize every in-degree-zero vertex to distance zero, relax in topological order, support negative weights, and preserve `parent[]` for path restoration.
- Benchmark counters are reset by constructing a fresh result on each run. All five runs must produce identical results and counters before their median time is written.

## Verification performed by the tests

- randomized DSU operations against a simple component-label model;
- Kruskal versus Prim on 100 connected graphs and a disconnected multigraph;
- Kosaraju versus BFS reachability and an iterative path of 100,000 vertices;
- valid Kahn ordering and explicit cycle detection;
- DAG shortest paths versus Bellman-Ford on 100 random DAGs with negative weights;
- parsing of loops, parallel edges, negative weights, and isolated vertices.

## AI-use acknowledgement

OpenAI Codex was used to help scaffold, implement, test, benchmark, and document this project at the repository owner's request. The owner is responsible for reviewing the generated work, confirming that its use follows the course policy, and being able to explain every submitted algorithm, counter, test, and conclusion during the defense.
