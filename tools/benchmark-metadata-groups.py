#!/usr/bin/env python3
"""Actual Java group-query helper, exact equivalence and paired SQLite timings."""
import pathlib,tempfile,subprocess,base64,importlib.util,json,time,statistics
ROOT=pathlib.Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/drbep/tvplayer'
spec=importlib.util.spec_from_file_location('fixture',ROOT/'tools/benchmark-metadata-progress.py');fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
def plans():
 with tempfile.TemporaryDirectory() as tmp:
  d=pathlib.Path(tmp)
  (d/'GroupsProbe.java').write_text("""package com.drbep.tvplayer;
import java.util.*;import java.nio.charset.StandardCharsets;
class GroupsProbe {
 static String b(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static String a(String[] args){return Arrays.stream(args).map(GroupsProbe::b).collect(java.util.stream.Collectors.joining(","));}
 public static void main(String[] args){for(int i=0;i<args.length;i+=2){String s=args[i],k=args[i+1];System.out.println(b(MetadataIndexGroups.probeSql(k))+";"+a(MetadataIndexGroups.probeArgs(s,k))+";"+b(MetadataIndexGroups.sql(k,true))+";"+a(MetadataIndexGroups.args(s,k,true))+";"+b(MetadataIndexGroups.sql(k,false))+";"+a(MetadataIndexGroups.args(s,k,false)));}}
}""")
  subprocess.run(['javac','-d',str(d),str(JAVA/'MetadataIndexSelection.java'),str(JAVA/'MetadataIndexGroups.java'),str(d/'GroupsProbe.java')],check=True)
  scopes=[(s,k) for s in ['s0','s19','absent',"x' OR 1=1 --"] for k in ['','movie','series']]
  out=subprocess.check_output(['java','-cp',str(d),'com.drbep.tvplayer.GroupsProbe',*[v for row in scopes for v in row]],text=True)
  dec=lambda s:base64.b64decode(s).decode()
  result=[]
  for scope,line in zip(scopes,out.splitlines()):
   v=line.split(';');result.append((scope,[(dec(v[i]),[dec(x) for x in v[i+1].split(',')] if v[i+1] else []) for i in range(0,6,2)]))
  return result

def query(db,plan,new):
 probe,grouped,legacy=plan
 if new:
  candidates=db.execute(*probe).fetchall();q=grouped if len(candidates)<33 else legacy
 else:q=legacy
 return db.execute(*q).fetchall()

def main():
 compiled=plans();report={'equivalences':0,'timings':[],'sqlite':fixture.sqlite3.sqlite_version}
 for n in [12000,120000]:
  db=fixture.fixture(n)
  for phase in ['few','preview','hidden','unique','all_hidden']:
   db.execute('SAVEPOINT scenario')
   if phase=='preview':db.execute("UPDATE sources SET active=NULL WHERE id='s0'")
   if phase=='hidden':db.execute("INSERT OR REPLACE INTO preferences(source,identity,occurrence,hidden) SELECT source,identity,occurrence,1 FROM entries WHERE source='s0' AND group_name='Drama'")
   if phase in ['unique','all_hidden']:db.execute("UPDATE entries SET group_name=printf('Group %09d',rowid)")
   if phase=='all_hidden':db.execute("INSERT OR REPLACE INTO preferences(source,identity,occurrence,hidden) SELECT source,identity,occurrence,1 FROM entries WHERE source='s0'")
   for (source,kind),plan in compiled:
    a=query(db,plan,False);b=query(db,plan,True);assert a==b,(n,phase,source,kind);report['equivalences']+=1
    if source=='s0' and n==120000:
     for _ in range(2):query(db,plan,False);query(db,plan,True)
     samples=[[],[]]
     for i in range(9):
      for v in ([0,1] if i%2==0 else [1,0]):
       t=time.perf_counter_ns();query(db,plan,bool(v));samples[v].append((time.perf_counter_ns()-t)/1e6)
     report['timings'].append({'phase':phase,'kind':kind or 'both','returned':len(a),'before_ms':statistics.median(samples[0]),'after_ms':statistics.median(samples[1])})
   db.execute('ROLLBACK TO scenario');db.execute('RELEASE scenario')
  # SQL edge cases: exactly the threshold, duplicates, empty/unicode/quote names,
  # a group hidden for movies but visible for series, and >1000 groups.
  for count in [1,32,33,34,1100]:
   db.execute('SAVEPOINT edge');db.execute("UPDATE entries SET group_name=CASE WHEN rowid%13=0 THEN '' WHEN rowid%17=0 THEN 'Niños — Acción' WHEN rowid%19=0 THEN 'Drama''s' ELSE printf('G%04d',rowid%?) END",[count])
   for (source,kind),plan in compiled:
    assert query(db,plan,False)==query(db,plan,True),(count,source,kind);report['equivalences']+=1
   db.execute('ROLLBACK TO edge');db.execute('RELEASE edge')
  db.close()
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
