#!/usr/bin/env python3
"""Compare queue SQL against the existing catalogue query using synthetic SQLite."""
import json,pathlib,re,sqlite3,statistics,time
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/com/drbep/tvplayer"
text=(JAVA/"MetadataIndexQueue.java").read_text()
constants=dict(re.findall(r'static final String (\w+) = "([^"]*)";',text))
repo=(JAVA/"ImportedMovieRepository.kt").read_text()
episode=re.search(r'private val episodeFrom="([^"]+)"',repo).group(1)
movie=re.search(r'private val from get\(\)=if\(!series\) episodeFrom\+"([^"]+)"',repo).group(1)
series=re.search(r'else\s*\n\s*"([^"]+)"\+episodeFrom\+"([^"]+)"',repo)
assert series
group=re.search(r'GROUP_SQL="([^"]+)"',(JAVA/"MetadataIndexSelection.java").read_text()).group(1).replace("@","e")
def query(kind,grouped,new,specialized=True,scoped=False):
 if new:
  origin=constants["BASE_FROM"] if kind=="movie" else constants["SERIES_PREFIX"]+constants["BASE_FROM"]+constants["SERIES_SUFFIX"]
 else:
  origin=episode+movie if kind=="movie" else series.group(1)+episode+series.group(2)
 if new and scoped and kind=="series":
  origin=origin.replace(" GROUP BY"," AND e.source=? GROUP BY",1)
 predicate=group
 if new and specialized:
  name="GROUP_MOVIE_SQL" if kind=="movie" else "GROUP_SERIES_SQL"
  predicate=re.search(name+r'="([^"]+)"',(JAVA/"MetadataIndexSelection.java").read_text()).group(1).replace("@","e")
 return constants["INSERT"]+origin+constants["FILTER"]+(" AND "+predicate if grouped else "")
db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE sources(id TEXT PRIMARY KEY,active TEXT,preview TEXT);
CREATE TABLE entries(source TEXT,generation TEXT,kind TEXT,identity TEXT,occurrence INTEGER,series TEXT,group_name TEXT,position INTEGER DEFAULT 0,season INTEGER DEFAULT 0,episode INTEGER DEFAULT 0,PRIMARY KEY(source,generation,identity,occurrence));
CREATE INDEX entries_series ON entries(source,generation,series,season,episode);
CREATE INDEX entries_page ON entries(source,generation,kind,position);
CREATE INDEX entries_group ON entries(source,generation,group_name,kind,position);
CREATE TABLE preferences(source TEXT,identity TEXT,occurrence INTEGER,hidden INTEGER,resume_ms INTEGER,PRIMARY KEY(source,identity,occurrence));
CREATE TABLE movie_added(source TEXT,identity TEXT,occurrence INTEGER,added INTEGER,PRIMARY KEY(source,identity,occurrence));
CREATE TABLE movie_match(source TEXT,identity TEXT,occurrence INTEGER,query TEXT,PRIMARY KEY(source,identity,occurrence));
CREATE TABLE series_added(source TEXT,series TEXT,added INTEGER,PRIMARY KEY(source,series));
CREATE TABLE series_match(source TEXT,series TEXT,query TEXT,PRIMARY KEY(source,series));
CREATE TABLE movie_metadata(query TEXT PRIMARY KEY,json TEXT);
CREATE TABLE metadata_index(kind TEXT,source TEXT,generation TEXT,identity TEXT,occurrence INTEGER,series TEXT,state TEXT,
PRIMARY KEY(kind,source,generation,identity,occurrence));
""")
db.executemany("INSERT INTO sources VALUES(?,?,?)",[("a","g2",None),("b",None,"g2"),("c","g2","g1"),("d","g1",None)])
rows=[]
for i in range(24000):
 source="abcd"[i%4];kind="movie" if i%8<4 else "series"
 rows.append((source,"g1" if i%5==0 else "g2",kind,"item"+str(i),i%2,"Show"+str(i//32) if kind=="series" else "",("Drama","Other","Drama's")[i%3]))
db.executemany("INSERT INTO entries(source,generation,kind,identity,occurrence,series,group_name) VALUES(?,?,?,?,?,?,?)",rows)
db.executemany("INSERT INTO preferences VALUES(?,?,?,?,?)",[(r[0],r[3],r[4],int(i%13==0),i*100) for i,r in enumerate(rows) if i%7==0 or i%13==0])
db.executemany("INSERT INTO movie_added VALUES(?,?,?,?)",[(r[0],r[3],r[4],i) for i,r in enumerate(rows) if r[2]=="movie"])
db.executemany("INSERT INTO movie_match VALUES(?,?,?,?)",[(r[0],r[3],r[4],"meta"+str(i)) for i,r in enumerate(rows) if r[2]=="movie"])
db.executemany("INSERT OR IGNORE INTO series_added VALUES(?,?,?)",[(r[0],r[5],i) for i,r in enumerate(rows) if r[2]=="series"])
db.executemany("INSERT OR IGNORE INTO series_match VALUES(?,?,?)",[(r[0],r[5],"meta"+str(i)) for i,r in enumerate(rows) if r[2]=="series"])
db.executemany("INSERT INTO movie_metadata VALUES(?,?)",[("meta"+str(i),'{"title":"synthetic"}') for i in range(24000)])
db.commit()
def run(sql,args,existing=None):
 db.execute("DELETE FROM metadata_index")
 if existing:db.execute("INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)",existing)
 db.execute(sql,args)
 return db.execute("SELECT * FROM metadata_index ORDER BY kind,source,generation,identity,occurrence").fetchall()
comparisons=0
for kind in ("movie","series"):
 for source in ("","a","b","d","absent","x' OR 1=1 --"):
  for g in (("","Drama","Drama's","absent") if source else ("",)):
   args=[kind,kind,source,source]+([g] if g else [])
   old=run(query(kind,bool(g),False),args)
   new=run(query(kind,bool(g),True),args)
   assert old==new,(kind,source,g)
   comparisons+=1
   if old:
    saved=list(old[0]);saved[-1]="done"
    assert run(query(kind,bool(g),False),args,saved)==run(query(kind,bool(g),True),args,saved)
    assert db.execute("SELECT COUNT(*) FROM metadata_index WHERE state='done'").fetchone()[0]==1
    comparisons+=1
# Compare selected-source pushdown to the immediately preceding queue.
for source in ("a","b","d","absent","x' OR 1=1 --"):
 for g in ("","Drama","Drama's","absent"):
  args=["series","series",source,source]+([g] if g else [])
  scoped_args=["series",source]+args[1:]
  old=run(query("series",bool(g),True),args)
  new=run(query("series",bool(g),True,scoped=True),scoped_args)
  assert old==new,("source-pushdown",source,g)
  comparisons+=1
  if old:
   saved=list(old[0]);saved[-1]="done"
   assert run(query("series",bool(g),True),args,saved)==run(query("series",bool(g),True,scoped=True),scoped_args,saved)
   comparisons+=1
def timing(sql,args):
 values=[]
 for i in range(9):
  db.execute("DELETE FROM metadata_index")
  start=time.perf_counter_ns();db.execute(sql,args)
  elapsed=(time.perf_counter_ns()-start)/1e6
  if i>1:values.append(elapsed)
 return round(statistics.median(values),3)
times=[]
for kind in ("movie","series"):
 for source,g in (("",""),("a",""),("a","Drama")):
  args=[kind,kind,source,source]+([g] if g else [])
  times.append(dict(kind=kind,source=source,group=g,before_ms=timing(query(kind,bool(g),False),args),after_ms=timing(query(kind,bool(g),True),args),queue_previous_group_ms=timing(query(kind,bool(g),True,False),args)))
for source,g in (("a",""),("a","Drama"),("b","")):
 args=["series","series",source,source]+([g] if g else [])
 times.append(dict(kind="series-source-pushdown",source=source,group=g,
  before_ms=timing(query("series",bool(g),True),args),
  after_ms=timing(query("series",bool(g),True,scoped=True),["series",source]+args[1:])))
print(json.dumps(dict(rows=len(rows),equivalences=comparisons,sqlite=sqlite3.sqlite_version,timings=times),indent=2))
