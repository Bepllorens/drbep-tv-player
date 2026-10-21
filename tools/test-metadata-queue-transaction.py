#!/usr/bin/env python3
"""SQLite WAL transaction checks with synthetic data and the app's queue SQL."""
import pathlib,re,sqlite3,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
TEXT=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexQueue.java").read_text()
SQL=dict(re.findall(r'static final String (\w+) = "([^"]*)";',TEXT))
class QueueTransaction(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory()
  path=str(pathlib.Path(self.tmp.name)/"test.db")
  self.db=sqlite3.connect(path)
  self.db.execute("PRAGMA journal_mode=WAL")
  self.db.executescript("""
  CREATE TABLE sources(id TEXT PRIMARY KEY,active TEXT,preview TEXT);
  CREATE TABLE entries(source TEXT,generation TEXT,kind TEXT,identity TEXT,occurrence INTEGER,series TEXT);
  CREATE TABLE preferences(source TEXT,identity TEXT,occurrence INTEGER,hidden INTEGER);
  CREATE TABLE metadata_index(kind TEXT,source TEXT,generation TEXT,identity TEXT,occurrence INTEGER,series TEXT,state TEXT,
   PRIMARY KEY(kind,source,generation,identity,occurrence));
  INSERT INTO sources VALUES('a','current',NULL);
  INSERT INTO entries VALUES('a','current','movie','film',0,''),('a','current','movie','new',0,''),
   ('a','current','series','ep1',0,'Show'),('a','current','series','ep2',0,'Show');
  INSERT INTO preferences VALUES('a','ep1',0,1);
  INSERT INTO metadata_index VALUES('movie','a','old','stale',0,'','pending'),
   ('movie','a','current','film',0,'','done');
  """)
  self.db.commit()
  self.reader=sqlite3.connect(path)
  self.before=self.rows()
 def tearDown(self):
  self.reader.close();self.db.close();self.tmp.cleanup()
 def rows(self):
  return self.reader.execute("SELECT * FROM metadata_index ORDER BY kind,identity").fetchall()
 def rebuild(self,fail=None,observe=False):
  self.db.execute("BEGIN IMMEDIATE")
  try:
   self.db.execute(SQL["CLEAN_STALE"])
   if observe:self.assertEqual(self.before,self.rows())
   if fail=="cleanup":raise RuntimeError()
   for kind in ("movie","series"):
    origin=SQL["BASE_FROM"] if kind=="movie" else SQL["SERIES_PREFIX"]+SQL["BASE_FROM"]+SQL["SERIES_SUFFIX"]
    self.db.execute(SQL["INSERT"]+origin+SQL["FILTER"],(kind,kind,"",""))
    if observe:self.assertEqual(self.before,self.rows())
    if fail==kind:raise RuntimeError()
   self.db.commit()
  except:
   self.db.rollback()
   raise
 def test_failure_at_each_step_restores_queue(self):
  for stage in ("cleanup","movie","series"):
   with self.assertRaises(RuntimeError):self.rebuild(stage)
   self.assertEqual(self.before,self.rows())
 def test_reader_sees_only_complete_queue(self):
  self.rebuild(observe=True)
  result=self.rows()
  self.assertEqual(["film","new","ep2"],[r[3] for r in result])
  self.assertEqual("done",result[0][-1])
 def test_retry_after_commit_is_idempotent(self):
  self.rebuild();expected=self.rows()
  self.rebuild()
  self.assertEqual(expected,self.rows())
if __name__=="__main__":unittest.main()
