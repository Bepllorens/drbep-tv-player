#!/usr/bin/env python3
"""Fixed synthetic catchup workload. Temporary DB only; never opens app data."""
import argparse, hashlib, json, math, platform, re, sqlite3, statistics, tempfile, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java'
def digest(value):
    return hashlib.sha256(json.dumps(value,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
def run(channels=180,samples=30):
    source=SOURCE.read_text()
    ddl=re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source)
    assert any('programs_page ON' in s for s in ddl), 'Schema extraction changed; review benchmark'
    now=8*86400000
    fixture={'version':1,'channels':channels,'days':8,'programs_per_day':48,'payload_padding':1000}
    with tempfile.TemporaryDirectory(prefix='drbep-synthetic-catchup-') as folder:
        db=sqlite3.connect(str(Path(folder)/'fixture.sqlite'))
        for sql in ddl:db.execute(sql)
        def rows():
            for ch in range(channels):
                for day in range(8):
                    for n in range(48):
                        id=f'{ch}-{day}-{n}';start=day*86400000+n*1800000
                        yield ('scope',id,str(ch),str(day),start,start+1800000,'genre'+str(ch%8),
                               f'title {n} channel {ch}',json.dumps({'id':id,'description':'x'*1000}))
        db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',rows())
        db.execute("INSERT INTO saved SELECT scope,id,channel,?,payload FROM programs WHERE channel='0' AND day='5'",(now,))
        db.commit()
        allowed=[str(ch) for ch in range(channels-1)]
        base='p.scope=? AND p.channel IN ('+','.join('?'*len(allowed))+')'
        args=['scope']+allowed
        daywhere=base+' AND p.day=? AND p.start<=? AND p.end>?'
        dayargs=args+['5',now,now-7*86400000]
        page='SELECT p.payload FROM programs p WHERE '
        order=' ORDER BY p.start DESC,p.id LIMIT 60 OFFSET '
        cases={
          'count_day':('SELECT count(*) FROM programs p WHERE '+daywhere,dayargs),
          'page_first':(page+daywhere+order+'0',dayargs),
          'page_next':(page+daywhere+order+'60',dayargs),
          'page_deep':(page+daywhere+order+'480',dayargs),
          'day_previous':(page+daywhere+order+'0',args+['4',now,now-7*86400000]),
          'search':(page+daywhere+' AND instr(p.search,?)>0'+order+'0',dayargs+['title 2']),
          'genre':(page+daywhere+' AND p.genre=?'+order+'0',dayargs+['genre0']),
          # This fixture has only 48 saved rows: exercise the bounded lookup branch.
          'saved':('SELECT p.payload FROM saved s CROSS JOIN programs p WHERE '+base+' AND s.scope=p.scope AND s.id=p.id'+order+'0',args),
          'genres':(re.search(r'String GENRES_SEEK = "([^"]+)";',
              (SOURCE.parent/'ArchiveQuerySql.java').read_text()).group(1),['scope','scope']),
          'strip':('SELECT payload FROM programs WHERE scope=? AND channel=? AND start<=? AND end>=? ORDER BY start DESC,id LIMIT ?',['scope','0',now,now-7*86400000,40])
        }
        report={}
        for name,(sql,params) in cases.items():
            times=[];expected=None;first=None
            for iteration in range(samples+1):
                t=time.perf_counter_ns();result=db.execute(sql,params).fetchall();ms=(time.perf_counter_ns()-t)/1e6
                fingerprint=digest(result)
                if expected is None:expected=fingerprint;first=ms
                assert fingerprint==expected, 'Unstable results: '+name
                if iteration:times.append(ms)
            times.sort()
            report[name]={'first_query_ms':round(first,3),'median_ms':round(statistics.median(times),3),
              'p95_ms':round(times[math.ceil(.95*len(times))-1],3),'rows':len(result),'result_sha256':expected,
              'query_sha256':digest([sql,params]),'plan':[r[3] for r in db.execute('EXPLAIN QUERY PLAN '+sql,params)]}
        size=db.execute('PRAGMA page_count').fetchone()[0]*db.execute('PRAGMA page_size').fetchone()[0]
        db.close()
    return {'fixture':fixture,'fixture_sha256':digest(fixture),'sqlite':sqlite3.sqlite_version,
            'machine':platform.machine(),'source_sha256':hashlib.sha256(source.encode()).hexdigest(),
            'samples':samples,'database_bytes':size,'cases':report,
            'limits':'Synthetic desktop SQLite; first query is NOT cold OS cache; no UI/network/decode measurements.'}
def compare(current,baseline):
    if current['fixture_sha256']!=baseline['fixture_sha256']:raise ValueError('Different fixture')
    if current['sqlite']!=baseline['sqlite'] or current['machine']!=baseline['machine']:raise ValueError('Different SQLite or architecture')
    if current['cases'].keys()!=baseline['cases'].keys():raise ValueError('Different scenarios')
    out={}
    for name,now in current['cases'].items():
        old=baseline['cases'][name]
        for key in ('result_sha256','query_sha256','rows'):
            if now[key]!=old[key]:raise ValueError('Changed '+key+': '+name)
        out[name]={'median_delta_ms':round(now['median_ms']-old['median_ms'],3),
                   'p95_delta_ms':round(now['p95_ms']-old['p95_ms'],3)}
    return out
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--channels',type=int,default=180)
    p.add_argument('--samples',type=int,default=30);p.add_argument('--baseline',type=Path);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    if not 2<=a.channels<=1000 or not 5<=a.samples<=200:p.error('channels 2..1000; samples 5..200')
    report=run(a.channels,a.samples)
    if a.baseline:report['comparison']=compare(report,json.loads(a.baseline.read_text()))
    # Exclusive output prevents overwriting a reference report by mistake.
    with a.output.open('x') as f:json.dump(report,f,indent=2);f.write('\n')
    print(json.dumps({'cases':len(report['cases']),'database_bytes':report['database_bytes'],'output':str(a.output)}))
if __name__=='__main__':main()
