#!/usr/bin/env python3
"""Validate the prepared series source pushdown, not a new runtime change."""
import pathlib,tempfile,subprocess,base64,importlib.util,json,time,statistics,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/drbep/tvplayer'
spec=importlib.util.spec_from_file_location('fixture',ROOT/'tools/benchmark-metadata-progress.py');fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
SCOPES=[(kind,source,group) for kind in ['series','movie'] for source in ['', 's0','s19','absent',"x' OR 1=1 --"] for group in (['','Drama','Missing',"Drama's"] if source else [''])]
def pairs():
 with tempfile.TemporaryDirectory() as tmp:
  d=pathlib.Path(tmp)
  (d/'SeriesSourceProbe.java').write_text("""package com.drbep.tvplayer;
import java.util.*;import java.nio.charset.StandardCharsets;
class SeriesSourceProbe {
 static String b(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static String a(String[] args){return Arrays.stream(args).map(SeriesSourceProbe::b).collect(java.util.stream.Collectors.joining(","));}
 public static void main(String[] args){for(int i=0;i<args.length;i+=3){String k=args[i],s=args[i+1],g=args[i+2];boolean series=k.equals("series");String sql=MetadataIndexQueue.sql(series,!g.isEmpty(),!s.isEmpty());String[] params=MetadataIndexQueue.args(series,k,s,g);String before=sql;String[] old=params;
 if(series&&!s.isEmpty()){before=MetadataIndexQueue.sql(true,!g.isEmpty());List<String> list=new ArrayList<>(Arrays.asList(params));list.remove(1);old=list.toArray(new String[0]);}
 System.out.println(b(before)+";"+a(old)+";"+b(sql)+";"+a(params));}}
}""")
  subprocess.run(['javac','-d',str(d),str(JAVA/'MetadataIndexSelection.java'),str(JAVA/'MetadataIndexQueue.java'),str(d/'SeriesSourceProbe.java')],check=True)
  out=subprocess.check_output(['java','-cp',str(d),'com.drbep.tvplayer.SeriesSourceProbe',*[x for row in SCOPES for x in row]],text=True)
  dec=lambda s:base64.b64decode(s).decode()
  return [(dec(v[0]),[dec(x) for x in v[1].split(',')],dec(v[2]),[dec(x) for x in v[3].split(',')]) for v in (line.split(';') for line in out.splitlines())]

def run(db,sql,args,saved=()):
 db.execute('DELETE FROM metadata_index');db.executemany('INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)',saved);db.execute(sql,args)
 return db.execute('SELECT kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY rowid').fetchall()

def main():
 compiled=pairs();report={'equivalences':0,'unchanged_sql':0,'order_changes':[],'timings':[],'candidates':[]}
 for n in [12000,120000]:
  db=fixture.fixture(n)
  for phase in ['visible','hidden','old_generation']:
   db.execute('SAVEPOINT phase')
   if phase=='hidden':db.execute("INSERT OR REPLACE INTO preferences(source,identity,occurrence,hidden) SELECT source,identity,occurrence,1 FROM entries WHERE source='s0'")
   if phase=='old_generation':db.execute("UPDATE sources SET active='g0' WHERE id='s0'")
   for scope,(old,oa,new,na) in zip(SCOPES,compiled):
    kind,source,group=scope
    if kind=='movie' or not source:assert old==new and oa==na;report['unchanged_sql']+=1
    a=run(db,old,oa);b=run(db,new,na);assert a==b,(n,phase,scope,'membership or order');report['equivalences']+=1
    saved=[tuple(list(row[:-1])+[state]) for row,state in zip(a,['done','error','unmatched','skipped'])]
    # Seed another source too; preparing one list must preserve all its states.
    other=db.execute("SELECT kind,source,generation,identity,occurrence,series,'done' FROM entries WHERE source<>? AND generation='g1' LIMIT 1",[source]).fetchone()
    if other and all(other[:6]!=row[:6] for row in saved):saved.append(other)
    a=run(db,old,oa,saved);b=run(db,new,na,saved);assert a==b,(n,phase,scope,'saved');report['equivalences']+=1
    db.execute(new,na);assert b==db.execute('SELECT kind,source,generation,identity,occurrence,series,state FROM metadata_index ORDER BY rowid').fetchall();report['equivalences']+=1
    if n==120000 and phase=='visible' and kind=='series' and source in ['s0','s19'] and group in ['','Drama']:
     values=[[],[]]
     for i in range(9):
      for v in ([0,1] if i%2==0 else [1,0]):
       sql,args=(old,oa) if v==0 else (new,na);db.execute('DELETE FROM metadata_index');t=time.perf_counter_ns();db.execute(sql,args)
       if i>=2:values[v].append((time.perf_counter_ns()-t)/1e6)
     report['timings'].append({'source':source,'group':group,'before_ms':statistics.median(values[0]),'after_ms':statistics.median(values[1])})
    if n==120000 and phase=='visible' and kind=='series' and source in ['s0','s19'] and group in ['','Drama']:
     report['candidates'].append({'source':source,'group':group,'count':len(b),'sha256':hashlib.sha256(repr(b).encode()).hexdigest()})
   db.execute('ROLLBACK TO phase');db.execute('RELEASE phase')
  db.close()
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
