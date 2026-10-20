import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("report", Path(__file__).with_name("report-zap-timing.py"))
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)

def row(session, kind, op, a=0, b=0, c=0):
    return json.dumps(dict(session=session, version=771, event=kind, op=op, a=a, b=b, c=c))

class ReportTests(unittest.TestCase):
    def test_same_operation_in_different_sessions_and_duplicate_exports(self):
        lines=[]
        for session, elapsed in [("one", 1000), ("two", 3000)]:
            lines.extend([row(session,"ZAP_PRESS",1,1),row(session,"ZAP_TUNE",1,250),
                          row(session,"ZAP_FRAME",1,elapsed,elapsed-250,7)])
        result=report.summarize(lines+lines)
        self.assertEqual(2,len(result["groups"]))
        self.assertEqual([1000,3000],[g["press_to_frame"]["p50_ms"] for g in result["groups"]])
        self.assertEqual([1,1],[g["measured_frames"] for g in result["groups"]])
        self.assertNotIn('"one"',json.dumps(result))
    def test_orphans_and_inconsistent_timing_not_counted(self):
        lines=[row("one","ZAP_FRAME",1,100,90),row("one","ZAP_PRESS",2),
               row("one","ZAP_TUNE",2,250),row("one","ZAP_FRAME",2,1000,900),"{bad"]
        result=report.summarize(lines)
        self.assertEqual(0,result["groups"][0]["measured_frames"])
        self.assertEqual(1,result["groups"][0]["unmatched_frames"])
        self.assertEqual(2,result["rejected"])
    def test_p95_uses_nearest_rank(self):
        self.assertEqual(19,report.percentile(list(range(1,21)),.95))
        self.assertIsNone(report.percentile([], .95))

if __name__ == "__main__":
    unittest.main()
