import importlib.util,json,tempfile,unittest,zipfile
from pathlib import Path
spec=importlib.util.spec_from_file_location('report',Path(__file__).with_name('report-playback-evidence.py'))
r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)
def row(at,pos=0,buffer=4000,flags=27,kind='PLAYBACK_SAMPLE',session='secret-session',op=1):
 return json.dumps(dict(event=kind,session=session,op=op,version=771,at=at,a=pos,b=buffer,c=flags))
class EvidenceTest(unittest.TestCase):
 def report(self,lines):return r.summarize(lines)
 def test_pause_and_suppression_are_not_stationary_faults(self):
  for flags,label in [(3,'pause_requested'),(75,'suppressed')]:
   g=self.report([row(1000,flags=flags),row(6000,flags=flags)])['groups'][0]
   self.assertEqual({label:1},g['intervals']);self.assertEqual([],g['stationary_intervals'])
 def test_position_progress_rewind_and_gap(self):
  lines=[row(1000,10),row(6000,5010),row(11000,0),row(200000,0)]
  self.assertEqual({'position_advancing':1,'position_rewind':1,'gap_or_ambiguous_time':1},self.report(lines)['groups'][0]['intervals'])
 def test_stationary_counters_and_reset(self):
  lines=[row(1000),row(6000),
   row(1001,10,0,4000,'AUDIO_COUNTERS'),row(6001,10,0,4000,'AUDIO_COUNTERS'),
   row(1001,20,1,4000,'VIDEO_COUNTERS'),row(6001,3,0,4000,'VIDEO_COUNTERS')]
  x=self.report(lines)['groups'][0]['stationary_intervals'][0]
  self.assertEqual('stationary_with_buffer',x['observation'])
  self.assertEqual(0,x['audio_delta']['rendered']);self.assertIsNone(x['video_delta'])
 def test_missing_counters_are_not_zero(self):
  x=self.report([row(1000,buffer=0),row(6000,buffer=0)])['groups'][0]['stationary_intervals'][0]
  self.assertEqual('stationary_low_buffer',x['observation']);self.assertIsNone(x['audio_delta'])
 def test_duplicates_out_of_order_and_session_isolation(self):
  lines=[row(6000),row(1000)];out=self.report(lines+lines+[row(11000,session='other'),row(12000,op=2)])
  self.assertEqual(2,out['duplicates']);self.assertEqual(3,len(out['groups']))
  self.assertNotIn('secret-session',json.dumps(out))
 def test_invalid_numbers_and_flags_rejected(self):
  e=json.loads(row(1000));e['a']=True
  self.assertEqual(3,self.report(['bad',json.dumps(e),row(1000,flags=255)])['rejected'])
 def test_buffering_is_separate(self):
  self.assertEqual({'stationary_not_ready':1},self.report([row(1000,flags=42),row(6000,flags=42)])['groups'][0]['intervals'])
 def test_zip_selects_only_event_files_and_bounds_line(self):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/'log.zip'
   with zipfile.ZipFile(p,'w') as z:
    z.writestr('events-1.jsonl',row(1000)+'\n');z.writestr('private.txt','do not read')
   self.assertEqual(1,len(self.report(r.read_lines([p]))['groups']))
   text=Path(d)/'large.jsonl';text.write_bytes(b'x'*(r.MAX_LINE+1))
   with self.assertRaises(ValueError):list(r.read_lines([text]))
 def test_clock_changes_do_not_fake_gaps(self):
  first=json.loads(row(1000));first['elapsed']=1000
  second=json.loads(row(100000000));second['elapsed']=6000
  result=self.report([json.dumps(first),json.dumps(second)])['groups'][0]
  self.assertEqual('monotonic',result['clock'])
  self.assertEqual({'stationary_with_buffer':1},result['intervals'])
 def test_multiview_prevents_ambiguous_attribution(self):
  result=self.report([row(1000),row(6000),row(2000,kind='MULTI_OPEN')])['groups'][0]
  self.assertIn('unclassified',result);self.assertNotIn('intervals',result)
if __name__=='__main__':unittest.main()
