#!/usr/bin/env python3
"""Compare repeated vs end-of-batch retention using synthetic SQLite only."""
import json,random,re,sqlite3,time,statistics
from pathlib import Path
SOURCE=Path(__file__).resolve().parents[1]/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java'
source=SOURCE.read_text()
sql=re.search(r'db.execSQL\("(DELETE FROM programs WHERE scope=\? AND end<\?[^"]+)"',source).group(1)
def setup(rows,saved):
 db=sqlite3.connect(':memory:')
 db.executescript("CREATE TABLE programs(scope TEXT,id TEXT,end INTEGER,payload TEXT,PRIMARY KEY(scope,id));CREATE TABLE saved(scope TEXT,id TEXT,PRIMARY KEY(scope,id));CREATE INDEX ends ON programs(scope,end);")
 db.executemany('INSERT INTO programs VALUES(?,?,?,?)',rows)
 db.executemany('INSERT INTO saved VALUES(?,?)',saved);db.commit();return db
def apply(db,batches,once):
 start=time.perf_counter_ns()
 with db:
  for batch in batches:
   db.executemany('INSERT OR REPLACE INTO programs VALUES(?,?,?,?)',batch)
   if not once:db.execute(sql,('a',100))
  if once and batches:db.execute(sql,('a',100))
 elapsed=(time.perf_counter_ns()-start)/1e6
 return list(db.execute('SELECT * FROM programs ORDER BY scope,id')),list(db.execute('SELECT * FROM saved ORDER BY scope,id')),elapsed
def run():
 rng=random.Random(20261020)
 for case in range(150):
  rows=[(scope,str(i),rng.choice([99,100,101,200]),'old') for scope in ('a','b') for i in range(80)]
  saved=[(scope,str(i)) for scope in ('a','b') for i in range(0,80,7)]
  batches=[[(rng.choice(['a','b']),str(rng.randrange(100)),rng.choice([99,100,101,200]),'new') for _ in range(rng.randrange(12))] for _ in range(case%15)]
  old=setup(rows,saved);new=setup(rows,saved)
  a=apply(old,batches,False);b=apply(new,batches,True)
  assert a[:2]==b[:2],case
  old.close();new.close()
 for once in (False,True):
  db=setup([('a','old',99,'old')],[])
  try: apply(db,[[('a','new',200,'new')],[('malformed',)]],once);raise AssertionError('Expected write failure')
  except sqlite3.ProgrammingError: pass
  assert list(db.execute('SELECT * FROM programs'))==[('a','old',99,'old')]
  db.close()
 rows=[('a',str(i),99 if i%3==0 else 200,'x'*100) for i in range(20000)]
 saved=[('a',str(i)) for i in range(0,20000,11)]
 batches=[[('a',str(20000+c*48+n),200,'new') for n in range(48)] for c in range(100)]
 timings=[[],[]]
 for repeat in range(5):
  for mode in (0,1):
   db=setup(rows,saved);result=apply(db,batches,bool(mode));timings[mode].append(result[2]);db.close()
 report={'equivalence_cases':150,'rollback_cases':2,'channels_per_batch':100,'initial_rows':20000,'repeats':5,
         'repeated_cleanup_median_ms':round(statistics.median(timings[0]),3),
         'single_cleanup_median_ms':round(statistics.median(timings[1]),3),
         'limits':'Synthetic SQLite subset schema, fixed cutoff, no Android transactions/UI. Same SQL retention predicate extracted from source.'}
 print(json.dumps(report,indent=2))
if __name__=='__main__':run()
