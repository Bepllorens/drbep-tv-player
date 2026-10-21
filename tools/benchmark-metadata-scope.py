#!/usr/bin/env python3
"""Synthetic SQLite equivalence and timing. No production DB or network."""
import importlib.util,json,pathlib,sqlite3,statistics,time
root=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("scopes",root/"tools/test-metadata-index-scope.py")
scopes=importlib.util.module_from_spec(spec);spec.loader.exec_module(scopes)
db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE metadata_index(kind TEXT NOT NULL,source TEXT NOT NULL,generation TEXT NOT NULL,
identity TEXT NOT NULL,occurrence INTEGER NOT NULL,series TEXT NOT NULL,state TEXT NOT NULL,
PRIMARY KEY(kind,source,generation,identity,occurrence));
CREATE INDEX metadata_index_state ON metadata_index(state,kind);
""")
states=("pending","done","error","unmatched","skipped")
db.executemany("INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)",(
 ("movie" if (i//20)%2==0 else "series","s"+str(i%20),"g"+str((i//40)%2),
  "item"+str(i),0,"series"+str(i//4),states[(i//800)%5]) for i in range(120000)))
db.commit()
old="(?='' OR source=?) AND (?='' OR kind=?)"
comparisons=0
queries=(
 ("counts","SELECT state,COUNT(*) FROM metadata_index WHERE {} GROUP BY state"),
 ("next","SELECT rowid FROM metadata_index WHERE state='pending' AND {} ORDER BY rowid LIMIT 1"),
 ("retry_members","SELECT rowid FROM metadata_index WHERE state='error' AND {} ORDER BY rowid"))
for source in ("","s0","s19","missing","x' OR 1=1 --"):
 for kind in ("","movie","series"):
  new,args=scopes.scope(source,kind)
  for name,query in queries:
   new,args=scopes.scope(source,kind,counts=name=="counts")
   a=db.execute(query.format(old),(source,source,kind,kind)).fetchall()
   b=db.execute(query.format(new),args).fetchall()
   assert a==b,(source,kind,query)
   comparisons+=1
def measure(query,args):
 for _ in range(3):db.execute(query,args).fetchall()
 values=[]
 for _ in range(15):
  start=time.perf_counter_ns();db.execute(query,args).fetchall()
  values.append((time.perf_counter_ns()-start)/1e6)
 return round(statistics.median(values),4)
timings=[]
for source,kind in (("",""),("s0",""),("","movie"),("s0","movie"),("s19","series")):
 new,args=scopes.scope(source,kind)
 for name,query in queries[:2]:
  new,args=scopes.scope(source,kind,counts=name=="counts")
  before=query.format(old);after=query.format(new)
  timings.append(dict(source=source,kind=kind,query=name,
   before_ms=measure(before,(source,source,kind,kind)),after_ms=measure(after,args),
   before_plan=[r[3] for r in db.execute("EXPLAIN QUERY PLAN "+before,(source,source,kind,kind))],
   after_plan=[r[3] for r in db.execute("EXPLAIN QUERY PLAN "+after,args)]))
print(json.dumps(dict(rows=120000,equivalences=comparisons,sqlite=sqlite3.sqlite_version,timings=timings),indent=2))
