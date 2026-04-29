#!/usr/bin/env python3
"""Remove edge weights and use random() — GraphWalker 4.3 WeightedRandomPath + guards is unsafe (see GraphWalkerExecutionPolicy)."""
import json
from pathlib import Path

def main() -> None:
    root = Path(__file__).resolve().parent.parent / "src" / "test" / "resources" / "com" / "eduplatform" / "mbt" / "models"
    for path in sorted(root.glob("*.json")):
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        for m in data.get("models", []):
            g = m.get("generator", "")
            m["generator"] = g.replace("weighted_random(", "random(")
            for e in m.get("edges", []):
                e.pop("weight", None)
        with path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print("OK", path.name)

if __name__ == "__main__":
    main()
