#!/usr/bin/env python3
"""Synthetic equivalence check for bounded archive reads; no user database."""
import sqlite3, re, json
from pathlib import Path
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java').read_text()
db=sqlite3.connect(':memory:')
for ddl in re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source):db.execute(ddl)
rows=[(scope,f'{ch}-{day}-{i}',str(ch),str(day),day*86400+i*1800,day*86400+i*1800+1800,'genre','title',json.dumps({'id':f'{scope}-{ch}-{day}-{i}','day':str(day)})) for scope in ('a','b') for ch in range(3) for day in range(8) for i in range(48)]
db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',rows)
cases=0
for scope in ('a','b','missing'):
 for channel in ('0','2','missing'):
  for days in ([],['7'],['7','6'],['7','6','2'],['9']):
   all_rows=list(db.execute('SELECT payload FROM programs WHERE scope=? AND channel=? ORDER BY start',(scope,channel)))
   expected=[r for r in all_rows if json.loads(r[0])['day'] in days]
   sql='SELECT payload FROM programs WHERE scope=? AND channel=? AND day IN ('+','.join('?' for _ in days)+') ORDER BY start'
   actual=list(db.execute(sql,[scope,channel]+days)) if days else []
   assert actual==expected
   cases+=1
plan=list(db.execute('EXPLAIN QUERY PLAN SELECT payload FROM programs WHERE scope=? AND channel=? AND day IN (?,?,?) ORDER BY start',['a','0','7','6','2']))
assert any('INDEX' in p[-1] for p in plan)
print(json.dumps({'cases':cases,'preserved':'scope, channel, day membership, ordering, empty windows','rows_per_channel_before':384,'rows_for_three_days':144,'plan':plan},indent=2))
