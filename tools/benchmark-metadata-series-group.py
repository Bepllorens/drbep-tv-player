#!/usr/bin/env python3
"""Measure additive series/group index, data preservation and query equivalence."""
import pathlib,importlib.util,re,sqlite3,time,statistics,json,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
def module(name,file):
 s=importlib.util.spec_from_file_location(name,ROOT/"tools"/file);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
progress=module("progress","benchmark-metadata-progress.py")
SCHEMA=(ROOT/"app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java").read_text()
INDEX=re.search(r'"(CREATE INDEX(?: IF NOT EXISTS)? entries_series_group[^"\n]+)"',SCHEMA).group(1)
review=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexReview.java").read_text()
PREFIX=re.search(r'PREFIX="([^"]+)"',review).group(1);SUFFIX=re.search(r'SUFFIX="([^"]+)"',review).group(1)
def fingerprint(db):
 h=hashlib.sha256()
 for table in ("sources","entries","preferences","metadata_index"):
  for row in db.execute("SELECT * FROM "+table+" ORDER BY rowid"):h.update(repr((table,row)).encode())
 return h.hexdigest()
def pair(a,b,q,args):
 for db in (a,b):
  for _ in range(2):db.execute(q,args).fetchall()
 vals=[[],[]]
 for i in range(9):
  for v in ((0,1) if i%2==0 else (1,0)):
   t=time.perf_counter_ns();(a,b)[v].execute(q,args).fetchall();vals[v].append((time.perf_counter_ns()-t)/1e6)
 return [round(statistics.median(v),4) for v in vals]
def compare(a,b,actual):
 equal=0
 for (source,kind,group),(where,args,cwhere,cargs) in zip(progress.SCOPES,actual):
  if not group:continue
  queries=[("SELECT state,COUNT(*) FROM metadata_index WHERE "+cwhere+" GROUP BY state",cargs),("SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' AND "+where+" ORDER BY rowid LIMIT 1",args),("SELECT rowid FROM metadata_index WHERE state='error' AND "+where+" ORDER BY rowid",args),(PREFIX+where+SUFFIX,args+["unmatched",0])]
  for q,params in queries:
   assert a.execute(q,params).fetchall()==b.execute(q,params).fetchall(),(source,kind,group,q);equal+=1
 return equal

def main():
 actual=progress.java_scopes();report=dict(sqlite=sqlite3.sqlite_version,equivalences=0,timings=[],storage=[],imports=[])
 for n in (12000,120000):
  a=progress.fixture(n);b=sqlite3.connect(":memory:");a.backup(b);before=fingerprint(b)
  pages=b.execute('PRAGMA page_count').fetchone()[0];size=b.execute('PRAGMA page_size').fetchone()[0]
  t=time.perf_counter_ns();b.execute(INDEX);create=(time.perf_counter_ns()-t)/1e6
  assert fingerprint(b)==before;assert b.execute('PRAGMA integrity_check').fetchone()==('ok',)
  report['storage'].append(dict(rows=n,create_ms=create,added_bytes=(b.execute('PRAGMA page_count').fetchone()[0]-pages)*size))
  for phase in ('visible','hidden','old_generation'):
   if phase=='hidden':
    for db in (a,b):db.execute("UPDATE preferences SET hidden=1 WHERE source='s0'")
   if phase=='old_generation':
    for db in (a,b):db.execute("UPDATE sources SET active='g0' WHERE id='s0'")
   report['equivalences']+=compare(a,b,actual)
   if phase=='visible':
    for (source,kind,group),(where,args,cwhere,cargs) in zip(progress.SCOPES,actual):
     if source!='s0' or group not in ('Drama','Missing'):continue
     for name,q,params in [('counts','SELECT state,COUNT(*) FROM metadata_index WHERE '+cwhere+' GROUP BY state',cargs),('next',"SELECT rowid FROM metadata_index WHERE state='pending' AND "+where+' ORDER BY rowid LIMIT 1',args)]:
      bm,am=pair(a,b,q,params);report['timings'].append(dict(rows=n,kind=kind,group=group,query=name,before_ms=bm,after_ms=am,after_plan=[r[3] for r in b.execute('EXPLAIN QUERY PLAN '+q,params)]))
  # Measure added index write cost separately from query cost. Fixed synthetic batch.
  seed=list(a.execute('SELECT * FROM entries LIMIT 5000'))
  batch=[]
  for i,row in enumerate(seed):
   row=list(row);row[2]='new-import-'+str(i);row[4]=n+i;batch.append(row)
  vals=[[],[]]
  for i in range(9):
   for v in ((0,1) if i%2==0 else (1,0)):
    db=(a,b)[v];db.execute('SAVEPOINT import_test');t=time.perf_counter_ns();db.executemany('INSERT INTO entries VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',batch);ms=(time.perf_counter_ns()-t)/1e6;db.execute('ROLLBACK TO import_test');db.execute('RELEASE import_test')
    if i>=2:vals[v].append(ms)
  report['imports'].append(dict(rows=n,batch=5000,before_ms=statistics.median(vals[0]),after_ms=statistics.median(vals[1])))
  # Reference/new series preparation on empty queues, same SQL and arguments.
  prep=module('prep','benchmark-metadata-preparation.py');prep.SCOPES=[('series','s0','Drama')];_,_,q,args=prep.queries()[0]
  for db in (a,b):db.execute("UPDATE sources SET active='g1' WHERE id='s0'");db.execute('DELETE FROM metadata_index')
  vals=[[],[]];digests=[]
  for i in range(9):
   for v in ((0,1) if i%2==0 else (1,0)):
    db=(a,b)[v];db.execute('DELETE FROM metadata_index');t=time.perf_counter_ns();db.execute(q,args);ms=(time.perf_counter_ns()-t)/1e6
    if i>=2:vals[v].append(ms)
  assert a.execute('SELECT * FROM metadata_index ORDER BY rowid').fetchall()==b.execute('SELECT * FROM metadata_index ORDER BY rowid').fetchall();report['equivalences']+=1
  report['timings'].append(dict(rows=n,kind='series',group='Drama',query='prepare',before_ms=statistics.median(vals[0]),after_ms=statistics.median(vals[1])))
  a.close();b.close()
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
