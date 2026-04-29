#!/usr/bin/env python3
"""
**Deprecated for GraphWalker 4.3 + guarded models:** `WeightedRandomPath` applies `weight` only to
**guard-available** out-edges at the current step. Distributing weights on the full static graph
(especially “sum to 1.0 per vertex”) can leave the *available* subset with probability mass &lt; 1
and no zero-weight edge to absorb the remainder, causing:
`Could not calculate which weighted edge to choose from vertex: ...`

Use `random(edge_coverage(100) || length(N))` and **no** `weight` on edges; see
`strip_gw_weights_use_random.py` and `GraphWalkerExecutionPolicy`.

This script remains only for experiments with **unguarded** fully-connected subgraphs.
"""
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict
from pathlib import Path


def has_any_weight(edges: list) -> bool:
    return any("weight" in e for e in edges)


def generator_to_random(gen: str) -> str:
    return re.sub(r"weighted_random\s*\(", "random(", gen, count=1)


def fix_vertex_weights(edge_indices: list[int], weights: list[float], n: int) -> list[float]:
    s = sum(weights)
    if n == 0:
        return []
    if s == 0:
        return [1.0 / n] * n
    if s > 1.0 + 1e-12:
        return [w / s for w in weights]
    if s < 1.0 - 1e-12:
        add = (1.0 - s) / n
        return [w + add for w in weights]
    return weights


def round_weights(ws: list[float]) -> list[float]:
    """Round to 6 decimals, then nudge last weight so sum is exactly 1.0."""
    if not ws:
        return []
    out = [round(w, 6) for w in ws]
    diff = 1.0 - sum(out)
    if abs(diff) > 1e-9 and out:
        out[-1] = round(out[-1] + diff, 6)
    return out


def process_model(data: dict) -> dict:
    for model in data.get("models", []):
        edges = model.get("edges", [])
        if not edges:
            continue
        if not has_any_weight(edges):
            g = model.get("generator", "")
            if "weighted_random" in g:
                model["generator"] = generator_to_random(g)
            for e in edges:
                e.pop("weight", None)
            continue

        by_source: dict[str, list[int]] = defaultdict(list)
        for i, e in enumerate(edges):
            by_source[e["sourceVertexId"]].append(i)

        for src, idxs in by_source.items():
            n = len(idxs)
            ws = [float(edges[i].get("weight", 0) or 0) for i in idxs]
            new_ws = fix_vertex_weights(idxs, ws, n)
            new_ws = round_weights(new_ws)
            for i, w in zip(idxs, new_ws):
                edges[i]["weight"] = w

    return data


def main() -> int:
    if len(sys.argv) > 1:
        paths = [Path(p) for p in sys.argv[1:]]
    else:
        root = Path(__file__).resolve().parent.parent / "src" / "test" / "resources" / "com" / "eduplatform" / "mbt" / "models"
        paths = sorted(root.glob("*.json"))
    for path in paths:
        if not path.is_file():
            print(f"skip (not a file): {path}", file=sys.stderr)
            continue
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        process_model(data)
        with path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"OK {path.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
