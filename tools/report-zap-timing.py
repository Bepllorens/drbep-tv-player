#!/usr/bin/env python3
"""Summarize numeric zap timing; no channel names, URLs or session identifiers."""
import argparse
import json
import math
from pathlib import Path
import zipfile

EVENTS = {"ZAP_PRESS", "ZAP_TUNE", "ZAP_FRAME"}
MAX_BYTES = 8 * 1024 * 1024

def read_lines(path):
    if path.suffix.lower() == ".zip":
        with zipfile.ZipFile(path) as archive:
            members = [x for x in archive.infolist()
                       if Path(x.filename).name.startswith("events-") and x.filename.endswith(".jsonl")]
            if len(members) > 64:
                raise ValueError("too many event files")
            for member in members:
                if member.file_size > MAX_BYTES:
                    raise ValueError("event file exceeds size limit")
                with archive.open(member) as source:
                    yield from source
    else:
        if path.stat().st_size > MAX_BYTES:
            raise ValueError("event file exceeds size limit")
        with path.open("rb") as source:
            yield from source

def percentile(values, fraction):
    return sorted(values)[max(0, math.ceil(len(values) * fraction) - 1)] if values else None

def summarize(lines):
    groups = {}
    rejected = 0
    for line in lines:
        try:
            event = json.loads(line)
            if not isinstance(event, dict):
                raise ValueError()
            kind = event.get("event")
            if kind not in EVENTS:
                continue
            version, operation = event["version"], event["op"]
            session = event["session"]
            if not isinstance(session, str) or not isinstance(version, int) or not isinstance(operation, int):
                raise ValueError()
            if operation <= 0:
                raise ValueError()
            numeric = [event.get(k, 0) for k in ("a", "b", "c")]
            if any(not isinstance(x, int) for x in numeric):
                raise ValueError()
            group = groups.setdefault((session, version), {})
            group.setdefault(operation, {})[kind] = numeric
        except (ValueError, KeyError, TypeError, UnicodeDecodeError):
            rejected += 1
    output = []
    for index, ((_, version), operations) in enumerate(groups.items(), 1):
        press_times, tune_times, queue_times = [], [], []
        unmatched_frames = 0
        for events in operations.values():
            if "ZAP_FRAME" not in events:
                continue
            if "ZAP_PRESS" not in events or "ZAP_TUNE" not in events:
                unmatched_frames += 1
                continue
            total, tune, _ = events["ZAP_FRAME"]
            wait = events["ZAP_TUNE"][0]
            if total < tune or min(total, tune, wait) < 0 or abs(total - tune - wait) > 1:
                rejected += 1
                continue
            press_times.append(total); tune_times.append(tune); queue_times.append(wait)
        def stats(values):
            return {"p50_ms": percentile(values, .5), "p95_ms": percentile(values, .95),
                    "max_ms": max(values) if values else None}
        output.append({"session_group": index, "version": version,
                       "presses": sum("ZAP_PRESS" in x for x in operations.values()),
                       "tunes": sum("ZAP_TUNE" in x for x in operations.values()),
                       "measured_frames": len(press_times), "unmatched_frames": unmatched_frames,
                       "press_to_frame": stats(press_times), "tune_to_frame": stats(tune_times),
                       "queue_wait": stats(queue_times)})
    return {"groups": output, "rejected": rejected}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", nargs="+", type=Path)
    args = parser.parse_args()
    result = summarize(line for path in args.files for line in read_lines(path))
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    main()
