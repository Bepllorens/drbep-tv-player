#!/usr/bin/env python3
"""Saved query equivalence and timings using synthetic data only."""
import json,re,sqlite3,time,statistics,math
from pathlib import Path
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java').read_text()
assert 'SELECT id FROM saved WHERE scope=? LIMIT 513' in source
assert 'if(savedCount<=512)' in source
assert 'saved s CROSS JOIN programs p' in source
old="EXISTS (SELECT 1 FROM saved s WHERE s.scope=p.scope AND s.id=p.id)"
new="s.scope=p.scope AND s.id=p.id"
db=sqlite3.connect(':memory:')
for sql in re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source):db.execute(sql)
rows=[('scope',f'{ch}-{day}-{n}',str(ch),str(day),day*86400000+n*1800000,
 day*86400000+(n+1)*1800000,'genre'+str(ch%8),f'title {n}',
 json.dumps({'id':f'{ch}-{day}-{n}','description':'x'*1000}))
 for ch in range(180) for day in range(8) for n in range(48)]
db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',rows)
db.execute("INSERT INTO programs SELECT 'other',id,channel,day,start,end,genre,search,payload FROM programs WHERE channel='0'")
def seed(stride):
 db.execute('DELETE FROM saved')
 db.executemany('INSERT INTO saved VALUES(?,?,?,?,?)',
 [('scope',r[1],r[2],0,r[8]) for r in rows[::stride]] if stride else [])
 # Other account, orphan and misleading channel: membership must remain scope+id only.
 db.execute("INSERT OR IGNORE INTO saved VALUES('other','0-0-1','0',0,'other')")
 db.execute("INSERT OR IGNORE INTO saved VALUES('scope','orphan','0',0,'orphan')")
 if stride:db.execute("UPDATE saved SET channel='wrong' WHERE scope='scope' AND id='0-0-0'")
def query(scope,allowed,extra='',extraargs=(),optimized=False,offset=0,count=False):
 optimized=optimized and len(db.execute('SELECT id FROM saved WHERE scope=? LIMIT 513',(scope,)).fetchall())<=512
 where='p.scope=? AND p.channel IN ('+','.join('?' for _ in allowed)+') AND '
 args=[scope]+allowed
 where+=new if optimized else old

 where+=extra;args.extend(extraargs)
 sql=('SELECT count(*)' if count else 'SELECT p.id,p.payload')+(' FROM saved s CROSS JOIN programs p WHERE ' if optimized else ' FROM programs p WHERE ')+where
 if not count:sql+=' ORDER BY p.start DESC,p.id LIMIT 60 OFFSET '+str(offset)
 return sql,args
cases=0
for stride in (0,1440,135,134,7):
 seed(stride)
 for scope in ('scope','other','missing'):
  for allowed in ([],['0'],['179'],[str(x) for x in range(179)]):
   for extra,args in (('',()),(' AND p.genre=?',('genre0',)),(' AND instr(p.search,?)>0',('title 2',)),(' AND p.channel=?',('12',))):
    for offset in (0,60,240):
     for count in (False,True):
      a=query(scope,allowed,extra,args,False,offset,count);b=query(scope,allowed,extra,args,True,offset,count)
      assert db.execute(*a).fetchall()==db.execute(*b).fetchall(),(stride,scope,allowed,extra,offset,count)
      cases+=1
def measure(opt,count):
 sql,args=query('scope',[str(x) for x in range(179)],optimized=opt,count=count)
 times=[]
 for _ in range(31):
  t=time.perf_counter_ns();sql,args=query('scope',[str(x) for x in range(179)],optimized=opt,count=count);r=db.execute(sql,args).fetchall();times.append((time.perf_counter_ns()-t)/1e6)
 times=sorted(times[1:])
 return {'median_ms':round(statistics.median(times),3),'p95_ms':round(times[math.ceil(.95*len(times))-1],3),
  'rows':len(r),'plan':[r[3] for r in db.execute('EXPLAIN QUERY PLAN '+sql,args)]}
timings={}
for stride in (1440,7):
 seed(stride);timings[str(stride)]={k:{'before':measure(False,c),'after':measure(True,c)} for k,c in [('page',False),('count',True)]}
print(json.dumps({'equivalence_cases':cases,'programs':len(rows),'sqlite':sqlite3.sqlite_version,'timings_by_saved_stride':timings},indent=2))
