#!/usr/bin/env python3
"""SQLite migration checks with isolated fixtures, no app/production data."""
import importlib.util,pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('groupbench',ROOT/'tools/benchmark-metadata-series-group.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Migration(unittest.TestCase):
 def setUp(self):
  self.db=m.progress.fixture(1200);self.db.execute('PRAGMA user_version=8');self.db.commit()
 def tearDown(self):self.db.close()
 def upgrade(self):
  with self.db:
   self.db.execute(m.INDEX);self.db.execute('PRAGMA user_version=9')
 def test_upgrade_preserves_every_row_state_and_integrity(self):
  before=m.fingerprint(self.db);self.upgrade();self.assertEqual(before,m.fingerprint(self.db))
  self.assertEqual((9,),self.db.execute('PRAGMA user_version').fetchone());self.assertEqual(('ok',),self.db.execute('PRAGMA integrity_check').fetchone())
  self.assertEqual(['source','generation','kind','series','group_name'],[r[2] for r in self.db.execute('PRAGMA index_info(entries_series_group)')])
 def test_failed_upgrade_rolls_back_index_version_and_rows(self):
  before=m.fingerprint(self.db)
  try:
   self.db.execute('BEGIN');self.db.execute(m.INDEX);self.db.execute('PRAGMA user_version=9');raise RuntimeError('simulated failure')
  except RuntimeError:self.db.rollback()
  self.assertEqual(before,m.fingerprint(self.db));self.assertEqual((8,),self.db.execute('PRAGMA user_version').fetchone());self.assertEqual([],self.db.execute("SELECT name FROM sqlite_master WHERE name='entries_series_group'").fetchall())
 def test_index_handles_long_series_and_rare_or_missing_group(self):
  self.db.execute("UPDATE entries SET kind='series',series='Long series',group_name='Other' WHERE source='s0'")
  self.db.execute("UPDATE entries SET group_name='Rare' WHERE source='s0' AND rowid=(SELECT MAX(rowid) FROM entries WHERE source='s0')")
  query="SELECT identity,occurrence FROM entries WHERE source='s0' AND generation='g1' AND kind='series' AND series='Long series' AND group_name=? ORDER BY rowid"
  before=[self.db.execute(query,(g,)).fetchall() for g in ('Rare','Missing','Other')];self.db.commit();self.upgrade()
  self.assertEqual(before,[self.db.execute(query,(g,)).fetchall() for g in ('Rare','Missing','Other')])
 def test_upgrade_after_adoption_is_idempotent_and_preserves_rowids(self):
  before=self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall()
  self.db.execute(m.INDEX);self.db.commit();self.assertEqual((8,),self.db.execute('PRAGMA user_version').fetchone())
  self.upgrade();self.upgrade()
  self.assertEqual(before,self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall())
  self.assertEqual((1,),self.db.execute("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='entries_series_group'").fetchone())
 def test_live_additive_return_keeps_schema8_and_progress(self):
  before=m.fingerprint(self.db);rows=self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall()
  self.db.execute(m.INDEX);self.db.commit();self.db.execute('DROP INDEX entries_series_group');self.db.commit()
  self.assertEqual((8,),self.db.execute('PRAGMA user_version').fetchone());self.assertEqual(before,m.fingerprint(self.db))
  self.assertEqual(rows,self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall())
 def test_failed_future_upgrade_retains_preexisting_adopted_index(self):
  self.db.execute(m.INDEX);self.db.commit();before=m.fingerprint(self.db)
  self.db.execute('BEGIN');self.db.execute(m.INDEX);self.db.execute('PRAGMA user_version=9');self.db.rollback()
  self.assertEqual((8,),self.db.execute('PRAGMA user_version').fetchone());self.assertEqual(before,m.fingerprint(self.db))
  self.assertEqual((1,),self.db.execute("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='entries_series_group'").fetchone())
 def test_version9_return_preserves_progress_added_after_upgrade(self):
  self.upgrade();self.db.execute("UPDATE metadata_index SET state='done' WHERE rowid<=5");self.db.commit()
  before=m.fingerprint(self.db);rows=self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall()
  with self.db:self.db.execute('PRAGMA user_version=8')
  self.assertEqual(before,m.fingerprint(self.db));self.assertEqual(rows,self.db.execute("SELECT rowid,* FROM metadata_index ORDER BY rowid").fetchall())
  self.assertEqual((8,),self.db.execute('PRAGMA user_version').fetchone());self.assertEqual(('ok',),self.db.execute('PRAGMA quick_check').fetchone())
 def test_new_install_and_upgrade_are_both_wired(self):
  source=m.SCHEMA
  self.assertIn('name, null, 9)',source)
  create=source.split('public void onCreate')[1].split('public void onUpgrade')[0]
  upgrade=source.split('public void onUpgrade')[1].split('private void migrateV9')[0]
  self.assertIn('migrateV9(db);',create);self.assertIn('if (oldVersion < 9) migrateV9(db);',upgrade)
if __name__=='__main__':unittest.main()
