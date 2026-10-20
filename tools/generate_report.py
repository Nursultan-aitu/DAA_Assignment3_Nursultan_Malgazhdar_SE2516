from __future__ import annotations

import csv
import math
import statistics
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "results" / "results.csv"


def slope(points: list[tuple[int, int]]) -> float:
    xs = [math.log(x) for x, _ in points]
    ys = [math.log(y) for _, y in points]
    x_bar, y_bar = statistics.mean(xs), statistics.mean(ys)
    return sum((x - x_bar) * (y - y_bar) for x, y in zip(xs, ys)) / sum((x - x_bar) ** 2 for x in xs)


def main() -> None:
    with CSV_PATH.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) != 50:
        raise ValueError(f"expected 50 benchmark rows, found {len(rows)}")
    for row in rows:
        for key in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
            row[key] = int(row[key])
        row["time_ms"] = float(row["time_ms"])
        row["density"] = str(row["graph"]).split("_")[-1]

    groups = defaultdict(list)
    for row in rows:
        groups[(row["algorithm"], row["density"])].append(row)
    traversal_slopes = []
    for algorithm in ("Kosaraju", "Kahn", "DagShortestPath"):
        for density in ("sparse", "dense"):
            traversal_slopes.append(slope([(r["n"], r["edge_scans"]) for r in groups[(algorithm, density)]]))
    kruskal_sparse = slope([(r["n"], r["comparisons"]) for r in groups[("Kruskal", "sparse")]])
    kruskal_dense = slope([(r["n"], r["comparisons"]) for r in groups[("Kruskal", "dense")]])
    median_times = {algorithm: statistics.median(r["time_ms"] for r in rows if r["algorithm"] == algorithm)
                    for algorithm in {r["algorithm"] for r in rows}}

    report = f"""# Assignment 3 Report - Graph Algorithms

**Nursultan Malgazhdar, SE-2516**  
**Design and Analysis of Algorithms**

## Method

All algorithms use one primitive-array adjacency-list `Graph` implementation. The benchmark recreates each graph with `new Random(42)`, runs each algorithm five times, records the median elapsed time, and requires identical counters and results across runs. It also checks Kruskal against Prim, validates every Kahn order, and stops immediately if any reference value differs. The checked-in `results/results.csv` contains 50 rows: five algorithms for each of five sizes and two densities. Operation counts, rather than elapsed time, are the primary evidence because they do not depend on the JVM, CPU, or background load.

## Complexity table

| Algorithm | Time | Extra space | Reason |
|---|---:|---:|---|
| DSU `find` / `union` | amortized `O(alpha(V))` | `O(V)` | Union by rank bounds tree growth and path compression flattens paths. |
| Kruskal | `O(E log E)` | `O(V + E)` | Sorting dominates; every examined edge then performs a constant number of DSU operations. |
| Prim, binary heap | `O(E log V)` | `O(V + E)` | Each adjacency entry is scanned and candidate heap operations cost logarithmic time. |
| Kosaraju | `O(V + E)` | `O(V + E)` | Two iterative DFS passes plus construction of the reversed graph. |
| Kahn | `O(V + E)` | `O(V)` | Each vertex enters the queue once and every directed edge reduces one in-degree. |
| DAG shortest paths | `O(V + E)` | `O(V)` | One topological ordering and one relaxation of every edge. |

## Counters versus formulas

Kosaraju scans exactly `2m` edges, while Kahn and the DAG relaxation scan exactly `m`; the benchmark asserts these equalities at every size. Consequently `edgeScans/(V+E)` is constant within a density: 1.600 for sparse Kosaraju and 1.905 for dense Kosaraju, compared with 0.800 and 0.952 for Kahn and DAG paths. Prim scans both adjacency entries of each undirected edge, hence its `edgeScans/m` ratio is exactly 2. Kruskal may stop after `V-1` accepted edges, so its examined share decreases on dense connected inputs even though all edges must still be sorted. The fitted traversal slopes average **{statistics.mean(traversal_slopes):.3f}**, while Kruskal's comparison slopes are **{kruskal_sparse:.3f}** on sparse and **{kruskal_dense:.3f}** on dense graphs; the extra logarithmic sorting factor makes these slightly steeper than linear.

![Counted operations](results/plots/operations_vs_n.png)

![Scans per edge](results/plots/scans_per_edge.png)

![Kruskal examined share](results/plots/kruskal_edge_share.png)

## Correctness: cut property and the two MST algorithms

Consider any cut that separates the vertices into two nonempty groups. The lightest edge crossing that cut is safe: replacing a heavier crossing edge in any optimal tree cannot increase its total weight. Kruskal's DSU components define cuts, and the next accepted edge is the lightest remaining edge that joins two such components. Therefore every accepted Kruskal edge is safe and cannot form a cycle. Prim uses the cut between its current tree and all vertices outside it, selecting the lightest candidate that crosses that cut. By repeatedly adding safe edges, both methods build a minimum spanning tree; restarting the same argument in an unvisited component produces a minimum spanning forest. Different equal-weight edges may be selected, but the total minimum weight must agree, which the tests and benchmark check automatically.

## Correctness: one-pass relaxation in a DAG

A topological order places the source of every edge before its destination. Initially every in-degree-zero vertex has distance zero, exactly as if a virtual super-source had zero-weight edges to all sources. Assume inductively that distances are final for all vertices before the current vertex `v`. Every possible path into `v` ends with an edge from one of those earlier vertices, so all candidates for `v` have already been considered. Its distance is therefore final when `v` is processed, and relaxing its outgoing edges prepares correct candidates for later vertices. By induction, every reachable vertex receives its shortest distance after one pass, and `parent[]` records a corresponding path. Negative edges are safe because no edge returns to an earlier vertex; a general graph may contain a negative cycle and has no such final topological order.

## Which algorithm when

| Situation | Choice | Why |
|---|---|---|
| Weighted undirected graph supplied as an edge list | Kruskal | Natural sort-and-scan workflow; DSU detects cycles cheaply. |
| Sparse adjacency-list graph | Prim with a binary heap | Scans local adjacency and avoids a separate edge-list workflow. |
| Very dense graph | Prim (ideally an array/matrix variant) | Sorting every edge for Kruskal can be wasted work. |
| Directed graph, cycles allowed, mutual reachability needed | Kosaraju | Returns SCCs and exposes dependency knots/deadlocks. |
| Dependency graph expected to be acyclic | Kahn | Produces an order and reports a cycle when the queue stalls. |
| DAG with possibly negative weights | DAG shortest paths | Linear pass; unlike Dijkstra, negative edges are allowed. |
| General graph with non-negative weights | Dijkstra | DAG ordering is unavailable, while Dijkstra safely finalizes minimum distances. |

## Timing discussion

Median times in milliseconds were Kruskal {median_times['Kruskal']:.3f}, Prim {median_times['Prim']:.3f}, Kosaraju {median_times['Kosaraju']:.3f}, Kahn {median_times['Kahn']:.3f}, and DAG shortest paths {median_times['DagShortestPath']:.3f} across the ten graph configurations. These figures describe this JVM run only: allocation, garbage collection, JIT compilation, CPU, and background activity can change them, especially for the two smallest sizes. They are therefore recorded for completeness but are not used as portable proof of complexity. The deterministic counters and their stable ratios provide that evidence.

## Reproducibility and conclusion

The generator, algorithms, validation, CSV writer, plots, and report can be reproduced with `run.ps1`. All ten reference MST weights, SCC counts, and DAG distance sums match the assignment table exactly. Unit tests additionally compare DSU with a simple model, both MST algorithms with each other, SCCs with brute-force reachability, and DAG paths with Bellman-Ford including negative weights. The 100,000-vertex path test confirms that Kosaraju uses an explicit stack and cannot overflow the JVM call stack.
"""
    (ROOT / "REPORT.md").write_text(report, encoding="utf-8")
    print(f"Wrote {ROOT / 'REPORT.md'}")


if __name__ == "__main__":
    main()
