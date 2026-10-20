import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('contention',Path(__file__).with_name('benchmark-catchup-contention.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class ContentionTest(unittest.TestCase):
 def test_results_stable_during_actual_metadata_writes(self):
  first=module.run(2,5);second=module.run(2,5)
  self.assertEqual(first['rows'],768)
  self.assertEqual(first['result_sha256'],second['result_sha256'])
  for result in (first,second):
   self.assertGreater(result['write_transactions'],0)
   self.assertEqual(result['journal_mode'],'wal')
   self.assertGreaterEqual(result['with_writer']['p95_ms'],result['with_writer']['median_ms'])
 def test_fingerprint_detects_different_result(self):
  self.assertNotEqual(module.fingerprint([['a']]),module.fingerprint([['b']]))
if __name__=='__main__':unittest.main()
