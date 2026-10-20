from __future__ import annotations

import csv
import math
from collections import defaultdict
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "results" / "results.csv"
PLOT_DIR = ROOT / "results" / "plots"
COLORS = {
    "Kruskal": "#2478B4", "Prim": "#E27421", "Kosaraju": "#2D914B",
    "Kahn": "#C8433E", "DagShortestPath": "#7656A8",
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        Path("C:/Windows/Fonts") / ("arialbd.ttf" if bold else "arial.ttf"),
        Path("/usr/share/fonts/truetype/dejavu") / ("DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def load_rows() -> list[dict[str, object]]:
    with CSV_PATH.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) != 50:
        raise ValueError(f"expected 50 rows, found {len(rows)}")
    for row in rows:
        for name in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
            row[name] = int(row[name])
        row["time_ms"] = float(row["time_ms"])
        row["density"] = str(row["graph"]).split("_")[-1]
        row["operations"] = int(row["dsu_ops"]) + int(row["edge_scans"]) + int(row["comparisons"])
    return rows


def groups(rows: list[dict[str, object]]) -> dict[tuple[str, str], list[dict[str, object]]]:
    result: dict[tuple[str, str], list[dict[str, object]]] = defaultdict(list)
    for row in rows:
        result[(str(row["algorithm"]), str(row["density"]))].append(row)
    for values in result.values():
        values.sort(key=lambda item: int(item["n"]))
    return result


def dashed(draw: ImageDraw.ImageDraw, points: list[tuple[float, float]], fill: str, width: int) -> None:
    for start, end in zip(points, points[1:]):
        dx, dy = end[0] - start[0], end[1] - start[1]
        length = math.hypot(dx, dy)
        if length == 0:
            continue
        unit_x, unit_y = dx / length, dy / length
        position = 0.0
        while position < length:
            finish = min(position + 13, length)
            draw.line((start[0] + unit_x * position, start[1] + unit_y * position,
                       start[0] + unit_x * finish, start[1] + unit_y * finish), fill=fill, width=width)
            position += 22


def vertical_label(image: Image.Image, text: str, center_y: int) -> None:
    label = Image.new("RGBA", (500, 60), (255, 255, 255, 0))
    label_draw = ImageDraw.Draw(label)
    label_draw.text((250, 30), text, fill="#333333", font=font(24), anchor="mm")
    rotated = label.rotate(90, expand=True)
    image.paste(rotated, (22, int(center_y - rotated.height / 2)), rotated)


def chart(series: list[tuple[str, str, list[tuple[float, float]]]], title: str, ylabel: str,
          output: Path, log_y: bool) -> None:
    image = Image.new("RGB", (1600, 1000), "white")
    draw = ImageDraw.Draw(image)
    left, top, right, bottom = 150, 105, 1510, 760
    x_values = [x for _, _, values in series for x, _ in values]
    y_values = [y for _, _, values in series for _, y in values]
    min_x, max_x = min(x_values), max(x_values)
    if log_y:
        min_y = 10 ** math.floor(math.log10(min(y_values)))
        max_y = 10 ** math.ceil(math.log10(max(y_values)))
    else:
        min_y = 0.0
        max_y = max(y_values) * 1.12 if max(y_values) else 1.0

    def px(x: float) -> float:
        return left + (math.log10(x) - math.log10(min_x)) / (math.log10(max_x) - math.log10(min_x)) * (right - left)

    def py(y: float) -> float:
        value = math.log10(y) if log_y else y
        low = math.log10(min_y) if log_y else min_y
        high = math.log10(max_y) if log_y else max_y
        return bottom - (value - low) / (high - low) * (bottom - top)

    draw.text((800, 35), title, fill="#18212B", font=font(34, True), anchor="ma")
    draw.line((left, top, left, bottom), fill="#333333", width=3)
    draw.line((left, bottom, right, bottom), fill="#333333", width=3)
    for tick in sorted(set(int(x) for x in x_values)):
        x = px(tick)
        draw.line((x, top, x, bottom), fill="#E3E7EB", width=1)
        draw.line((x, bottom, x, bottom + 8), fill="#333333", width=2)
        draw.text((x, bottom + 18), f"{tick:,}", fill="#333333", font=font(20), anchor="ma")

    if log_y:
        low_power = int(math.log10(min_y))
        high_power = int(math.log10(max_y))
        y_ticks = [10 ** power for power in range(low_power, high_power + 1)]
        labels = [f"10^{power}" for power in range(low_power, high_power + 1)]
    else:
        y_ticks = [max_y * i / 5 for i in range(6)]
        labels = [f"{value:.2f}" for value in y_ticks]
    for tick, label in zip(y_ticks, labels):
        y = py(tick if tick > 0 else min_y)
        draw.line((left, y, right, y), fill="#E3E7EB", width=1)
        draw.line((left - 8, y, left, y), fill="#333333", width=2)
        draw.text((left - 18, y), label, fill="#333333", font=font(20), anchor="rm")

    draw.text(((left + right) / 2, 825), "Vertices (n), logarithmic scale", fill="#333333", font=font(24), anchor="ma")
    vertical_label(image, ylabel, int((top + bottom) / 2))

    for name, density, values in series:
        color = COLORS[name]
        points = [(px(x), py(y)) for x, y in values]
        if density == "dense": dashed(draw, points, color, 4)
        else: draw.line(points, fill=color, width=4)
        for x, y in points:
            draw.ellipse((x - 6, y - 6, x + 6, y + 6), fill="white", outline=color, width=4)

    columns = 2 if len(series) > 4 else 1
    start_x, start_y, column_width = 180, 885, 650
    for index, (name, density, _) in enumerate(series):
        column, row = index % columns, index // columns
        x, y = start_x + column * column_width, start_y + row * 23
        if density == "dense": dashed(draw, [(x, y), (x + 55, y)], COLORS[name], 4)
        else: draw.line((x, y, x + 55, y), fill=COLORS[name], width=4)
        draw.text((x + 68, y), f"{name} - {density}", fill="#333333", font=font(17), anchor="lm")
    image.save(output, optimize=True)


def main() -> None:
    rows = load_rows()
    grouped = groups(rows)
    PLOT_DIR.mkdir(parents=True, exist_ok=True)
    order = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
    operation_series = []
    scan_series = []
    for algorithm in order:
        for density in ("sparse", "dense"):
            values = grouped[(algorithm, density)]
            operation_series.append((algorithm, density, [(r["n"], r["operations"]) for r in values]))
            scan_series.append((algorithm, density, [(r["n"], r["edge_scans"] / r["m"]) for r in values]))
    chart(operation_series, "Counted operations vs graph size", "Counted operations",
          PLOT_DIR / "operations_vs_n.png", log_y=True)
    chart(scan_series, "Edge scans per input edge", "edgeScans / m",
          PLOT_DIR / "scans_per_edge.png", log_y=False)
    kruskal = [("Kruskal", density, [(r["n"], r["edge_scans"] / r["m"])
                                     for r in grouped[("Kruskal", density)]])
                for density in ("sparse", "dense")]
    chart(kruskal, "Share of sorted edges examined by Kruskal", "edgeScans / m",
          PLOT_DIR / "kruskal_edge_share.png", log_y=False)
    print(f"Wrote three plots to {PLOT_DIR}")


if __name__ == "__main__":
    main()
