#!/usr/bin/env python3
"""Genera un informe JSON compacto y sin URLs a partir del smoke ADB."""

from __future__ import annotations

import argparse
import json
import re
import statistics
from datetime import datetime, timezone
from pathlib import Path


FIRST_FRAME_RE = re.compile(r"firstFrame .*?firstFrameElapsedMs=(\d+)")
ZAP_PREPARE_RE = re.compile(r"zapPrepare .*?generation=(\d+)")
BUFFER_END_RE = re.compile(r"playbackBufferingEnd .*?lastBufferMs=(\d+).*?totalBufferMs=(\d+).*?count=(\d+)")
CATALOG_RE = re.compile(r"startup catalog metrics .*?channels=(\d+)")
NETWORK_RE = re.compile(
    r"NetworkMonitor: network state available=(true|false) validated=(true|false) "
    r"metered=(true|false) transport=([^\s]+)"
)


def parse_log(text: str) -> dict:
    first_frames = []
    first_frames_by_generation = []
    pending_generation = None
    for line in text.splitlines():
        zap = ZAP_PREPARE_RE.search(line)
        if zap:
            pending_generation = int(zap.group(1))
            continue
        frame = FIRST_FRAME_RE.search(line)
        if frame and pending_generation is not None:
            elapsed_ms = int(frame.group(1))
            first_frames.append(elapsed_ms)
            first_frames_by_generation.append((pending_generation, elapsed_ms))
            pending_generation = None
    if not first_frames:
        first_frames = [int(value) for value in FIRST_FRAME_RE.findall(text)]
    buffer_samples = [tuple(map(int, values)) for values in BUFFER_END_RE.findall(text)]
    catalogs = [int(value) for value in CATALOG_RE.findall(text)]
    networks = NETWORK_RE.findall(text)
    last_network = networks[-1] if networks else None
    return {
        "catalog_channels": catalogs[-1] if catalogs else 0,
        "first_frame_samples_ms": first_frames,
        "startup_first_frame_ms": next((elapsed for generation, elapsed in first_frames_by_generation if generation == 1), 0),
        "zap_first_frame_samples_ms": [elapsed for generation, elapsed in first_frames_by_generation if generation > 1],
        "first_frame_median_ms": int(statistics.median(first_frames)) if first_frames else 0,
        "first_frame_max_ms": max(first_frames, default=0),
        "buffering_events": len(buffer_samples),
        "buffering_total_ms": sum(sample[0] for sample in buffer_samples),
        "playback_errors": len(re.findall(r"ExoPlaybackException|Playback error|Source error", text, re.IGNORECASE)),
        "fatal_errors": len(re.findall(r"FATAL EXCEPTION|ANR in com\.drbep\.tvplayer\.offline", text, re.IGNORECASE)),
        "network": {
            "available": last_network[0] == "true" if last_network else None,
            "validated": last_network[1] == "true" if last_network else None,
            "metered": last_network[2] == "true" if last_network else None,
            "transport": last_network[3] if last_network else "desconocida",
        },
    }


def boolean_value(value: str):
    clean = str(value).strip().lower()
    if clean == "true":
        return True
    if clean == "false":
        return False
    return None


def build_report(args: argparse.Namespace, log_text: str) -> dict:
    metrics = parse_log(log_text)
    return {
        "schema_version": 1,
        "recorded_at": datetime.now(timezone.utc).isoformat(),
        "smoke_ok": str(args.smoke_ok) == "1",
        "device": {"serial": args.device, "model": args.model},
        "app": {
            "version_code": int(args.version_code),
            "version_name": args.version_name,
            "pid": int(args.pid),
            "process_detected_seconds": int(args.process_detected_seconds),
            "total_pss_kb": int(args.total_pss_kb),
            "background_process_kept": boolean_value(args.background_process_kept),
        },
        "catalog": {"channels": metrics.pop("catalog_channels")},
        "playback": metrics,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--log", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--device", required=True)
    parser.add_argument("--model", default="")
    parser.add_argument("--version-code", default="0")
    parser.add_argument("--version-name", default="")
    parser.add_argument("--pid", default="0")
    parser.add_argument("--process-detected-seconds", default="0")
    parser.add_argument("--total-pss-kb", default="0")
    parser.add_argument("--background-process-kept", default="unknown")
    parser.add_argument("--smoke-ok", default="0")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    report = build_report(args, Path(args.log).read_text(encoding="utf-8", errors="replace"))
    Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
