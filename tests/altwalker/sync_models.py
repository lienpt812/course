"""Copy GraphWalker JSON models into tests/altwalker/models (PowerShell-safe; no multiline -c)."""
from __future__ import annotations

import glob
import json
import os
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(REPO, "tests", "graphwalker", "model")
DST = os.path.join(HERE, "models")


def _dedupe_edge_names(data: dict) -> int:
    """GraphWalker rejects models with duplicate edge `name` (HTTP 500). Rename extras to `id`."""
    nfixed = 0
    for m in data.get("models", []):
        counts: dict[str, int] = {}
        for e in m.get("edges", []):
            n = e.get("name") or ""
            eid = e.get("id") or n
            counts[n] = counts.get(n, 0) + 1
            if counts[n] > 1:
                e["name"] = eid
                nfixed += 1
    return nfixed


def _apply(data: dict) -> tuple[int, int]:
    gen_fixed = 0
    for m in data.get("models", []):
        gen = m.get("generator", "")
        if gen and not str(gen).startswith("random("):
            m["generator"] = "random(edge_coverage(100))"
            gen_fixed += 1
    dedupe = _dedupe_edge_names(data)
    return gen_fixed, dedupe


def main() -> None:
    os.makedirs(DST, exist_ok=True)
    if os.path.isdir(SRC):
        for src in glob.glob(os.path.join(SRC, "*.json")):
            shutil.copy2(src, os.path.join(DST, os.path.basename(src)))

    gen_total = dedupe_total = 0
    for path in glob.glob(os.path.join(DST, "*.json")):
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
            g, d = _apply(data)
            gen_total += g
            dedupe_total += d
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f)
        except (OSError, json.JSONDecodeError):
            continue

    n_src = len(glob.glob(os.path.join(SRC, "*.json"))) if os.path.isdir(SRC) else 0
    n_dst = len(glob.glob(os.path.join(DST, "*.json")))
    print(
        f"[sync_models] src={n_src} dst={n_dst} fixed_generators={gen_total} fixed_duplicate_edge_names={dedupe_total}"
    )


if __name__ == "__main__":
    main()
