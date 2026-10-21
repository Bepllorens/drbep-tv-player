#!/usr/bin/env python3
"""Synthetic SQLite checks for the shared metadata scope; no production data."""
import pathlib,re,sqlite3,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
def scope(source,kind,counts=False):
 text=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()
 if counts and not source and kind:
  return re.search(r'COUNT_KIND_SQL="([^"]+)"',text).group(1),[kind,kind]
 name=("SOURCE_KIND_SQL" if kind else "SOURCE_SQL") if source else ("KIND_SQL" if kind else "ALL_SQL")
 sql=re.search(name+r'="([^"]+)"',text).group(1)
 return sql,[v for v in (source,kind) if v]

class ScopeSQL(unittest.TestCase):
 def setUp(self):
  source=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()
  self.where="1=1"
  self.db=sqlite3.connect(":memory:")
  self.db.execute("CREATE TABLE metadata_index(source,kind,state)")
  self.db.executemany("INSERT INTO metadata_index VALUES(?,?,?)",[
   ("a","movie","pending"),("a","series","pending"),("b","series","pending"),
   ("a","series","error"),("b","series","error"),("a","movie","done")])
 def tearDown(self):self.db.close()
 def args(self,s,k):
  self.where,args=scope(s,k);return args
 def test_selection_count_and_retry_share_scope(self):
  a=self.args("a","series")
  rows=self.db.execute("SELECT source,kind FROM metadata_index WHERE state='pending' AND "+self.where,a).fetchall()
  self.assertEqual([("a","series")],rows)
  self.db.execute("UPDATE metadata_index SET state='pending' WHERE state='error' AND "+self.where,a)
  counts=dict(self.db.execute("SELECT state,COUNT(*) FROM metadata_index WHERE "+self.where+" GROUP BY state",a))
  self.assertEqual({"pending":2},counts)
  self.assertEqual(1,self.db.execute("SELECT COUNT(*) FROM metadata_index WHERE source='b' AND state='error'").fetchone()[0])
  self.assertEqual(1,self.db.execute("SELECT COUNT(*) FROM metadata_index WHERE state='done'").fetchone()[0])
 def test_all_deleted_and_literal_identifiers(self):
  for source,kind,count in [("","",6),("","series",4),("a","",4),("missing","",0),("a' OR 1=1 --","",0)]:
   args=self.args(source,kind)
   self.assertEqual(count,self.db.execute("SELECT COUNT(*) FROM metadata_index WHERE "+self.where,args).fetchone()[0])

class GroupSQL(unittest.TestCase):
 def setUp(self):
  text=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()
  self.predicate=re.search(r'GROUP_SQL="([^"]+)"',text).group(1)
  self.db=sqlite3.connect(":memory:")
  self.db.executescript("""
   CREATE TABLE sources(id,active,preview);
   CREATE TABLE entries(source,generation,kind,identity,occurrence,series,group_name);
   CREATE TABLE preferences(source,identity,occurrence,hidden);
   CREATE TABLE metadata_index(source,generation,kind,identity,occurrence,series,state);
   INSERT INTO sources VALUES('a','v2',NULL),('b','v2',NULL);
   INSERT INTO entries VALUES
    ('a','v2','series','rep',0,'Show','Other'),
    ('a','v2','series','ep2',0,'Show','Drama'),
    ('a','v2','series','ep3',0,'Show','Drama'),
    ('a','v2','movie','film',0,'','Drama'),
    ('a','v2','movie','film',1,'','Other'),
    ('a','v1','series','old',0,'Old','Drama'),
    ('b','v2','series','rep',0,'Show','Other');
   INSERT INTO metadata_index VALUES
    ('a','v2','series','rep',0,'Show','error'),
    ('a','v2','movie','film',0,'','error'),
    ('a','v2','movie','film',1,'','error'),
    ('a','v1','series','old',0,'Old','error'),
    ('b','v2','series','rep',0,'Show','error');
  """)
 def tearDown(self):self.db.close()
 def rows(self,group):
  return self.db.execute("SELECT kind,identity,occurrence FROM metadata_index WHERE "+self.predicate.replace("@","metadata_index"),(group,)).fetchall()
 def test_shared_series_once_and_movie_occurrence_is_exact(self):
  self.assertEqual([("series","rep",0),("movie","film",0)],self.rows("Drama"))
  self.assertEqual([],self.rows("Missing"));self.assertEqual([],self.rows("Drama' OR 1=1 --"))
  # The representative itself need not belong to the group.
  self.assertEqual([("rep",)],self.db.execute("SELECT e.identity FROM entries e WHERE e.source='a' AND e.identity='rep' AND "+self.predicate.replace("@","e"),("Drama",)).fetchall())
 def test_hidden_episodes_and_old_generation_do_not_match(self):
  self.db.executemany("INSERT INTO preferences VALUES('a',?,0,1)",[("ep2",),("ep3",)])
  self.assertEqual([("movie","film",0)],self.rows("Drama"))
 def test_retry_only_changes_group_members(self):
  self.db.execute("UPDATE metadata_index SET state='pending' WHERE state='error' AND "+self.predicate.replace("@","metadata_index"),("Drama",))
  self.assertEqual(2,self.db.execute("SELECT COUNT(*) FROM metadata_index WHERE state='pending'").fetchone()[0])
  self.assertEqual(3,self.db.execute("SELECT COUNT(*) FROM metadata_index WHERE state='error'").fetchone()[0])


class ReviewSQL(GroupSQL):
 def query(self,source="a",kind="",group=""):
  text=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexReview.java").read_text()
  prefix=re.search(r'PREFIX="([^"]+)"',text).group(1)
  suffix=re.search(r'SUFFIX="([^"]+)"',text).group(1)
  where,args=scope(source,kind)
  if group:
   name="GROUP_MOVIE_SQL" if kind=="movie" else "GROUP_SERIES_SQL" if kind=="series" else "GROUP_SQL"
   predicate=re.search(name+r'="([^"]+)"',(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()).group(1)
   where+=" AND "+predicate.replace("@","metadata_index");args.append(group)
  return prefix+where+suffix,args
 def prepare(self):
  self.db.execute("ALTER TABLE sources ADD COLUMN name")
  self.db.execute("UPDATE sources SET name=id")
 def test_review_keeps_only_active_visible_selected_rows(self):
  self.prepare();sql,args=self.query(group="Drama")
  rows=self.db.execute(sql,args+["error",0]).fetchall()
  self.assertEqual(2,len(rows))
  self.db.execute("INSERT INTO preferences VALUES('a','film',0,1)")
  self.assertEqual(1,len(self.db.execute(sql,args+["error",0]).fetchall()))
  self.assertEqual([],self.db.execute(sql,args+["unmatched",0]).fetchall())
 def test_review_pages_by_identity_when_prior_results_change(self):
  self.prepare()
  for n in range(120):
   identity="new"+str(n)
   self.db.execute("INSERT INTO entries VALUES('a','v2','movie',?,0,'','Drama')",(identity,))
   self.db.execute("INSERT INTO metadata_index VALUES('a','v2','movie',?,0,'','unmatched')",(identity,))
  sql,args=self.query(kind="movie")
  first=self.db.execute(sql,args+["unmatched",0]).fetchall()
  self.assertEqual(51,len(first))
  cursor=first[49][-1]
  self.db.execute("UPDATE metadata_index SET state='done' WHERE rowid<=?",(cursor,))
  second=self.db.execute(sql,args+["unmatched",cursor]).fetchall()
  self.assertEqual(first[50][-1],second[0][-1])
  self.assertEqual(51,len(second))


class SpecializedGroupSQL(GroupSQL):
 def check_specialized(self):
  text=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()
  for kind,name in (("movie","GROUP_MOVIE_SQL"),("series","GROUP_SERIES_SQL")):
   predicate=re.search(name+r'="([^"]+)"',text).group(1).replace("@","metadata_index")
   for group in ("Drama","Other","Missing","Drama' OR 1=1 --"):
    prefix="SELECT source,generation,identity,occurrence FROM metadata_index WHERE kind=? AND "
    expected=self.db.execute(prefix+self.predicate.replace("@","metadata_index")+" ORDER BY rowid",(kind,group)).fetchall()
    actual=self.db.execute(prefix+predicate+" ORDER BY rowid",(kind,group)).fetchall()
    self.assertEqual(expected,actual)
 def test_same_rows_after_hiding_and_generation_changes(self):
  self.check_specialized()
  self.db.execute("INSERT INTO preferences VALUES('a','ep2',0,1)")
  self.check_specialized()
  self.db.execute("INSERT INTO preferences VALUES('a','ep3',0,1)")
  self.check_specialized()
  self.db.execute("UPDATE sources SET active='v1' WHERE id='a'")
  self.check_specialized()

if __name__=="__main__":unittest.main()
