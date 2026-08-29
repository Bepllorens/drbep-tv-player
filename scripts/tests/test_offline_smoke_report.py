import argparse
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "offline_smoke_report.py"
SPEC = importlib.util.spec_from_file_location("offline_smoke_report", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class OfflineSmokeReportTest(unittest.TestCase):
    LOG = """
08-29 W NetworkMonitor: network state available=true validated=true metered=false transport=wifi
08-29 W DRBEP-TV-Native: startup catalog metrics stage=startup-load channels=2942 durationMs=194
08-29 W PlayerController: zapPrepare channel={1,LA 1} generation=1 autoPlay=true
08-29 W PlayerController: firstFrame channel={1,LA 1} firstFrameElapsedMs=646 bufferCount=0
08-29 W PlayerController: firstFrame channel={1,LA 1} firstFrameElapsedMs=2646 bufferCount=0
08-29 W PlayerController: playbackBufferingEnd channel={1,LA 1} lastBufferMs=145 totalBufferMs=145 count=1
08-29 W PlayerController: zapPrepare channel={2,LA 2} generation=2 autoPlay=true
08-29 W PlayerController: firstFrame channel={2,LA 2} firstFrameElapsedMs=538 bufferCount=0
"""

    def test_parse_log_extracts_safe_metrics(self):
        parsed = MODULE.parse_log(self.LOG)
        self.assertEqual(2942, parsed["catalog_channels"])
        self.assertEqual([646, 538], parsed["first_frame_samples_ms"])
        self.assertEqual(646, parsed["startup_first_frame_ms"])
        self.assertEqual([538], parsed["zap_first_frame_samples_ms"])
        self.assertEqual(592, parsed["first_frame_median_ms"])
        self.assertEqual(646, parsed["first_frame_max_ms"])
        self.assertEqual(1, parsed["buffering_events"])
        self.assertEqual(145, parsed["buffering_total_ms"])
        self.assertEqual("wifi", parsed["network"]["transport"])
        self.assertTrue(parsed["network"]["validated"])

    def test_report_contains_device_lifecycle_and_no_stream_urls(self):
        args = argparse.Namespace(
            smoke_ok="1",
            device="test-device",
            model="Fire TV",
            version_code="451",
            version_name="beta",
            pid="123",
            process_detected_seconds="1",
            total_pss_kb="45678",
            background_process_kept="true",
        )
        report = MODULE.build_report(args, self.LOG + "\nsource=https://private.example/token")
        encoded = json.dumps(report)
        self.assertTrue(report["smoke_ok"])
        self.assertTrue(report["app"]["background_process_kept"])
        self.assertEqual(45678, report["app"]["total_pss_kb"])
        self.assertNotIn("private.example", encoded)
        self.assertNotIn("token", encoded)


if __name__ == "__main__":
    unittest.main()
