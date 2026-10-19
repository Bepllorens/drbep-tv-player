#!/usr/bin/env python3
"""Synthetic, reproducible SQL check; no production/account fixtures."""
import sqlite3, time, statistics, json
c=sqlite3.connect(':memory:')
c.executescript("""
CREATE TABLE programs(scope TEXT,id TEXT,channel TEXT,day TEXT,start INTEGER,end INTEGER,genre TEXT,payload TEXT,PRIMARY KEY(scope,id));
CREATE INDEX programs_channel ON programs(scope,channel,day,start DESC);
""")
c.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?)',
 (('scope',str(i),str(i%180),str((i//180)%8),i,i+60,'genre','item:'+str(i)) for i in range(80000)))
sql='SELECT payload FROM programs WHERE scope=? AND channel=? AND start<=? AND end>=? ORDER BY start DESC,id LIMIT ?'
args=('scope','12',80000,0,40)
def measure():
 samples=[]
 for _ in range(100):
  start=time.perf_counter();rows=list(c.execute(sql,args));samples.append((time.perf_counter()-start)*1000)
 return {'plan':list(c.execute('EXPLAIN QUERY PLAN '+sql,args)),
         'median_ms':statistics.median(samples),'rows':rows}
before=measure()
c.execute('CREATE INDEX programs_strip ON programs(scope,channel,start DESC,id)')
after=measure()
assert before.pop('rows')==after.pop('rows'), 'Index must not change the selected programmes'
assert not any('TEMP B-TREE' in str(row) for row in after['plan'])
print(json.dumps({'fixture_rows':80000,'before':before,'after':after},indent=2))
