#!/usr/bin/env python3
"""Real SQL constants, synthetic SQLite only. Never opens app databases."""
import json,re,sqlite3,time,statistics,math
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java').read_text()
constants=(root/'app/src/main/java/com/drbep/tvplayer/ArchiveQuerySql.java').read_text()
def const(name):return re.search(r'String '+name+r' = "([^"]+)";',constants).group(1)
db=sqlite3.connect(':memory:')
for sql in re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source):db.execute(sql)
def genres(scope):
 result=db.execute(const('GENRES_SEEK'),[scope,scope]).fetchall()
 return db.execute(const('GENRES_ALL'),[scope]).fetchall() if len(result)>256 else result
genre_cases=0
for count in (0,1,8,255,256,257,500):
 db.execute('DELETE FROM programs')
 values=['',' ','Cine','cine','Café','Cafe','NIÑOS','儿童','🎬']+[f'g{i:04}' for i in range(count)]
 db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',
 [('s',str(i),'0','0',i,i+1,g,'x','{}') for i,g in enumerate(values[:count])])
 db.execute("INSERT INTO programs VALUES('other','0','0','0',0,1,'otro','x','{}')")
 for scope in ('s','other','missing'):
  assert genres(scope)==db.execute(const('GENRES_ALL'),[scope]).fetchall()
  genre_cases+=1
db.execute('DELETE FROM programs')
rows=[('s',f'{ch}-{d}-{n}',str(ch),str(d),d*86400000+n*1800000,d*86400000+(n+1)*1800000,
 'genre'+str(ch%8),f'title {n} channel {ch} café 100% _','x'*1000) for ch in range(180) for d in range(8) for n in range(48)]
db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',rows)
db.executemany('INSERT INTO saved VALUES(?,?,?,?,?)',[('s',r[1],r[2],0,r[8]) for r in rows[::401]])
db.commit()
cases=[]
for allowed in (['0'],['179'],[str(x) for x in range(179)]):
 for search in ('title 2','absent','café','%','_'):
  for genre in ('','genre0'):
   where='p.scope=? AND p.day=? AND p.channel IN ('+','.join('?' for _ in allowed)+') AND p.start<=? AND p.end>? AND instr(p.search,?)>0'
   args=['s','5']+allowed+[8*86400000,86400000,search]
   if genre:where+=' AND p.genre=?';args+=[genre]
   cases.append(('SELECT count(*) FROM programs p WHERE '+where,args))
   for offset in (0,60,240):
    cases.append(('SELECT p.id,p.payload FROM programs p WHERE '+where+' ORDER BY p.start DESC,p.id LIMIT 60 OFFSET '+str(offset),args))
def results():return [db.execute(s,a).fetchall() for s,a in cases]
def measure(sql,args):
 times=[]
 for _ in range(31):
  t=time.perf_counter_ns();r=db.execute(sql,args).fetchall();times.append((time.perf_counter_ns()-t)/1e6)
 times=sorted(times[1:])
 return {'median_ms':round(statistics.median(times),3),'p95_ms':round(times[28],3),'rows':len(r),
 'plan':[r[3] for r in db.execute('EXPLAIN QUERY PLAN '+sql,args)]}
# Broad permission list, literal title search, no category filter.
index=next(i for i,(s,a) in enumerate(cases) if len(a)>180 and a[-1]=='title 2' and s.startswith('SELECT count'))
def timings():return {'count':measure(*cases[index]),'page':measure(*cases[index+1])}
def writes():
 times=[]
 for _ in range(5):
  t=time.perf_counter_ns();db.execute('BEGIN')
  db.executemany('UPDATE programs SET search=? WHERE scope=? AND id=?',[(r[7]+' changed','s',r[1]) for r in rows[:1000]])
  db.rollback();times.append((time.perf_counter_ns()-t)/1e6)
 return round(statistics.median(times),3)
old=results();saved=db.execute('SELECT * FROM saved ORDER BY id').fetchall()
report={'genre_cases':genre_cases,'search_cases':len(cases),'rows':len(rows),'sqlite':sqlite3.sqlite_version,
 'genres':{'before':measure(const('GENRES_ALL'),['s']),'after':measure(const('GENRES_SEEK'),['s','s'])},
 'search':{'before':timings()},'update_1000_rollback_ms':{'before':writes()}}
size=db.execute('PRAGMA page_count').fetchone()[0]*db.execute('PRAGMA page_size').fetchone()[0]
t=time.perf_counter_ns();db.execute(const('SEARCH_INDEX'));report['index_create_ms']=round((time.perf_counter_ns()-t)/1e6,3)
db.execute(const('SEARCH_INDEX')) # idempotent
report['added_bytes']=db.execute('PRAGMA page_count').fetchone()[0]*db.execute('PRAGMA page_size').fetchone()[0]-size
assert old==results(),'Search results changed'
assert saved==db.execute('SELECT * FROM saved ORDER BY id').fetchall(),'Saved rows changed'
assert len(rows)==db.execute('SELECT count(*) FROM programs').fetchone()[0]
report['search']['after']=timings();report['update_1000_rollback_ms']['after']=writes()
print(json.dumps(report,indent=2))
