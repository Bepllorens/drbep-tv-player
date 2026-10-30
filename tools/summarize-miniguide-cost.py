#!/usr/bin/env python3
"""Summarize numeric GuideBindCost lines; never emit raw logs or channel data."""
import argparse
import json
import math
import re
import statistics
import sys
import unittest
from collections import defaultdict

PATTERN = re.compile(
    r"GuideBindCost rows=(\d+)"
    r"(?: cache_hit=(true|false) prepare_us=(\d+) format_us=(\d+)"
    r" selection_us=(\d+) bind_us=(\d+))? micros=(\d+)(?:\s|$)"
)
PHASES = ("prepare_us", "format_us", "selection_us", "bind_us", "total_us")


def summarize(lines):
    groups = defaultdict(list)
    for line in lines:
        match = PATTERN.search(line)
        if not match:
            continue
        rows, hit, prepare, formatting, selection, binding, total = match.groups()
        row_count = int(rows)
        if row_count > 240:
            continue
        values = [int(v) if v is not None else None
                  for v in (prepare, formatting, selection, binding, total)]
        if hit is not None and not 0 <= values[-1] - sum(values[:-1]) <= 3:
            continue
        groups[(row_count, hit or "legacy")].append(values)
    output = []
    for (rows, hit), samples in sorted(groups.items()):
        result = {"rows": rows, "cache_hit": hit, "samples": len(samples)}
        for index, name in enumerate(PHASES):
            values = sorted(s[index] for s in samples if s[index] is not None)
            if values:
                result[name] = {
                    "median": statistics.median(values),
                    "p95": values[max(0, math.ceil(.95 * len(values)) - 1)],
                    "max": values[-1],
                }
        output.append(result)
    return output


class SummaryTests(unittest.TestCase):
    def test_groups_legacy_cold_warm_separately(self):
        data = summarize([
            "GuideBindCost rows=24 micros=80",
            "GuideBindCost rows=24 cache_hit=false prepare_us=1 format_us=60 selection_us=8 bind_us=9 micros=80",
            "GuideBindCost rows=24 cache_hit=true prepare_us=1 format_us=0 selection_us=8 bind_us=9 micros=20",
            "GuideBindCost rows=12 micros=10",
        ])
        self.assertEqual(4, len(data))
        self.assertEqual({"false", "true", "legacy"}, {x["cache_hit"] for x in data})
        self.assertEqual(20, next(x for x in data if x["cache_hit"] == "true")["total_us"]["median"])

    def test_rejects_inconsistent_or_out_of_range(self):
        self.assertEqual([], summarize([
            "GuideBindCost rows=241 micros=90",
            "GuideBindCost rows=24 cache_hit=true prepare_us=10 format_us=5 selection_us=8 bind_us=9 micros=20",
            "GuideBindCost rows=-1 micros=3",
        ]))

    def test_never_outputs_raw_text(self):
        result = json.dumps(summarize(["private-fixture GuideBindCost rows=1 micros=2"]))
        self.assertNotIn("private-fixture", result)

    def test_percentiles(self):
        values = summarize(f"GuideBindCost rows=10 micros={i}" for i in range(1, 101))
        self.assertEqual({"median": 50.5, "p95": 95, "max": 100}, values[0]["total_us"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", nargs="?", help="Local log file; omit to read stdin")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(SummaryTests)
        return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1
    if args.input:
        with open(args.input, encoding="utf-8", errors="replace") as stream:
            output = summarize(stream)
    else:
        output = summarize(sys.stdin)
    print(json.dumps(output, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
