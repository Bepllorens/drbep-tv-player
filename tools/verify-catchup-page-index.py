#!/usr/bin/env python3
"""Synthetic SQLite regression/plan check. Never opens a user's database."""
from pathlib import Path
import sqlite3, re, json, time, statistics
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java').read_text()
ddl=re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source)
index=next(s for s in ddl if 'programs_page ON' in s)
db=sqlite3.connect(':memory:')
for sql in ddl:
 if sql!=index:db.execute(sql)
rows=[('scope',f'{ch}-{day}-{n}',str(ch),str(day),day*86400+n*1800,day*86400+n*1800+1800,
       'genre'+str(ch%8),'title '+str(n),json.dumps({'id':f'{ch}-{day}-{n}','description':'x'*1000}))
      for ch in range(180) for day in range(8) for n in range(48)]
db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',rows)
db.executemany('INSERT INTO saved VALUES(?,?,?,?,?)',(('scope',r[1],r[2],999999,r[8]) for r in rows[::401]))
db.execute("INSERT INTO catalog VALUES('scope','0',123,'[]')")
db.commit()
saved_before=list(db.execute('SELECT * FROM saved ORDER BY id'))
allowed=[str(ch) for ch in range(175)]
base='p.scope=? AND p.channel IN ('+','.join('?'*len(allowed))+')'
base_args=['scope']+allowed
cases=[]
for saved in (False,True):
 for channel in ('','12','179'):
  for genre in ('','genre4'):
   for search in ('','title 2'):
    where=base;args=list(base_args)
    if saved:where+=' AND EXISTS (SELECT 1 FROM saved s WHERE s.scope=p.scope AND s.id=p.id)'
    else:where+=' AND p.day=? AND p.start<=? AND p.end>?';args+=['5',8*86400,0]
    if channel:where+=' AND p.channel=?';args.append(channel)
    if genre:where+=' AND p.genre=?';args.append(genre)
    if search:where+=' AND instr(p.search,?)>0';args.append(search)
    cases.append((where,args))
def results():
 out=[]
 for where,args in cases:
  count=db.execute('SELECT count(*) FROM programs p WHERE '+where,args).fetchone()[0]
  pages=[list(db.execute('SELECT p.id FROM programs p WHERE '+where+' ORDER BY p.start DESC,p.id LIMIT 60 OFFSET '+str(offset),args)) for offset in (0,60,240)]
  out.append((count,pages))
 return out
def measure():
 where,args=cases[0];sql='SELECT count(*) FROM programs p WHERE '+where
 plan=list(db.execute('EXPLAIN QUERY PLAN '+sql,args));times=[]
 for _ in range(10):
  start=time.perf_counter();db.execute(sql,args).fetchone();times.append((time.perf_counter()-start)*1000)
 return {'plan':plan,'median_ms':round(statistics.median(times),3)}
before=results();timing_before=measure()
size_before=db.execute('PRAGMA page_count').fetchone()[0]*db.execute('PRAGMA page_size').fetchone()[0]
db.execute(index);db.execute(index) # repeat migration is idempotent
assert before==results(), 'Changed count, ordering, pagination or permissions'
assert saved_before==list(db.execute('SELECT * FROM saved ORDER BY id')), 'Saved programmes changed'
assert db.execute('SELECT count(*) FROM programs').fetchone()[0]==len(rows)
assert list(db.execute('SELECT * FROM catalog'))==[('scope','0',123,'[]')]
after=measure()
assert any('COVERING INDEX programs_page' in row[-1] for row in after['plan'])
size_after=db.execute('PRAGMA page_count').fetchone()[0]*db.execute('PRAGMA page_size').fetchone()[0]
print(json.dumps({'rows':len(rows),'cases':len(cases),'before':timing_before,'after':after,'added_index_bytes':size_after-size_before,'preserved':'programmes, saved, catalog, count/order/pages/permissions'},indent=2))
