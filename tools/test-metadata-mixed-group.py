#!/usr/bin/env python3
"""Edge cases for production Java mixed-group SQL; synthetic SQLite only."""
import pathlib,importlib.util,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('mixed',ROOT/'tools/benchmark-metadata-mixed-group.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class MixedGroup(unittest.TestCase):
 @classmethod
 def setUpClass(cls):cls.scopes=dict(zip(m.SCOPES,m.scopes()))
 def setUp(self):
  self.db=m.progress.fixture(120)
  for table in ('entries','preferences','metadata_index'):self.db.execute('DELETE FROM '+table)
  self.entry('s0','g1','film',0,'movie','','Drama');self.entry('s0','g1','film',1,'movie','','Other')
  self.entry('s0','g1','representative',0,'series','Show','Other');self.entry('s0','g1','episode',0,'series','Show','Drama',queue=False)
  self.entry('s0','g0','old',0,'series','Old','Drama');self.entry('s19','g1','preview',0,'series','Preview','Drama')
  self.entry('s0','g1','unknown',0,'live','','Drama')
 def tearDown(self):self.db.close()
 def entry(self,source,generation,identity,occurrence,kind,series,group,queue=True):
  self.db.execute('INSERT INTO entries VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',(source,generation,identity,occurrence,0,kind,identity,series,1,1,group,'','https://example.invalid/fixture','{}',''))
  if queue:self.db.execute('INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)',(kind,source,generation,identity,occurrence,series,'error'))
 def compare(self,source='s0',group='Drama'):
  data=self.scopes[(source,'',group)];o,n=data[:4],data[4:]
  for (_,oq,oa),(_,nq,na) in zip(m.queries(o),m.queries(n)):self.assertEqual(self.db.execute(oq,oa).fetchall(),self.db.execute(nq,na).fetchall())
  return self.db.execute('SELECT identity,occurrence FROM metadata_index WHERE '+n[0]+' ORDER BY rowid',n[1]).fetchall()
 def test_occurrence_series_other_group_and_unknown_kind(self):
  for version in (8,9):
   if version==9:self.db.execute('CREATE INDEX entries_series_group ON entries(source,generation,kind,series,group_name)')
   self.assertEqual([('film',0),('representative',0)],self.compare())
 def test_hiding_only_group_episode_removes_series(self):
  self.db.execute("INSERT INTO preferences(source,identity,occurrence,hidden) VALUES('s0','episode',0,1)")
  self.assertEqual([('film',0)],self.compare())
 def test_active_generation_preview_and_literal_parameters(self):
  self.assertEqual([('preview',0)],self.compare('s19'))
  self.db.execute("UPDATE sources SET active='g0' WHERE id='s0'");self.assertEqual([('old',0)],self.compare())
  self.assertEqual([],self.compare(group="Drama' OR 1=1 --"))
 def test_retry_preserves_other_kinds_sources_and_states(self):
  data=self.scopes[('s0','','Drama')];states=[]
  for where,args,*_ in (data[:4],data[4:]):
   self.db.execute('SAVEPOINT retry');self.db.execute("UPDATE metadata_index SET state='pending' WHERE state='error' AND "+where,args)
   states.append(self.db.execute('SELECT identity,occurrence,state FROM metadata_index ORDER BY rowid').fetchall());self.db.execute('ROLLBACK TO retry');self.db.execute('RELEASE retry')
  self.assertEqual(states[0],states[1]);self.assertEqual(2,sum(row[2]=='pending' for row in states[1]))
if __name__=='__main__':unittest.main()
