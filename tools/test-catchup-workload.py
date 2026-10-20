
import copy,importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('bench',Path(__file__).with_name('benchmark-catchup-workload.py'))
bench=importlib.util.module_from_spec(spec);spec.loader.exec_module(bench)
class WorkloadTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):cls.report=bench.run(channels=4,samples=5)
 def test_fixed_workload_results(self):
  cases=self.report['cases']
  self.assertEqual(cases['page_first']['rows'],60)
  self.assertEqual(cases['page_next']['rows'],60)
  self.assertEqual(cases['saved']['rows'],48)
  self.assertEqual(cases['strip']['rows'],40)
  self.assertEqual(cases['genres']['rows'],4)
 def test_same_report_compares(self):
  self.assertTrue(all(v['p95_delta_ms']==0 for v in bench.compare(self.report,self.report).values()))
 def test_changed_result_refused(self):
  other=copy.deepcopy(self.report);other['cases']['saved']['result_sha256']='changed'
  with self.assertRaises(ValueError):bench.compare(self.report,other)
 def test_changed_query_refused(self):
  other=copy.deepcopy(self.report);other['cases']['strip']['query_sha256']='changed'
  with self.assertRaises(ValueError):bench.compare(self.report,other)
 def test_different_dataset_or_engine_refused(self):
  for field in ('fixture_sha256','sqlite','machine'):
   other=copy.deepcopy(self.report);other[field]='different'
   with self.assertRaises(ValueError):bench.compare(self.report,other)
if __name__=='__main__':unittest.main()
