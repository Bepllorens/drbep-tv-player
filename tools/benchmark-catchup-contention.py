#!/usr/bin/env python3
"""Synthetic WAL contention: no device, network or production database."""
import argparse, hashlib, json, math, re, sqlite3, statistics, tempfile, threading, time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def fingerprint(rows):
    return hashlib.sha256(json.dumps(rows,separators=(',',':')).encode()).hexdigest()
def stats(values):
    values=sorted(values)
    return {'median_ms':round(statistics.median(values),3),'p95_ms':round(values[math.ceil(len(values)*.95)-1],3)}
def run(channels=180,samples=50):
    source=(ROOT/'app/src/main/java/com/drbep/tvplayer/U7dCatalogStore.java').read_text()
    ddl=re.findall(r'db.execSQL\("(CREATE[^"\n]+)"\)',source)
    assert any('programs_page ON' in s for s in ddl)
    with tempfile.TemporaryDirectory(prefix='drbep-contention-') as folder:
        path=str(Path(folder)/'synthetic.sqlite')
        db=sqlite3.connect(path,timeout=5)
        assert db.execute('PRAGMA journal_mode=WAL').fetchone()[0]=='wal'
        for sql in ddl: db.execute(sql)
        db.executemany('INSERT INTO programs VALUES(?,?,?,?,?,?,?,?,?)',(
            ('scope',f'{ch}-{day}-{n}',str(ch),str(day),day*86400000+n*1800000,
             day*86400000+(n+1)*1800000,'genre'+str(ch%8),'title '+str(n),
             json.dumps({'id':f'{ch}-{day}-{n}','description':'x'*1000}))
            for ch in range(channels) for day in range(8) for n in range(48)))
        db.commit()
        query="SELECT payload FROM programs WHERE scope=? AND day=? ORDER BY start DESC,id LIMIT 60"
        args=('scope','5')
        expected=fingerprint(db.execute(query,args).fetchall())
        def sample():
            before=time.perf_counter_ns();rows=db.execute(query,args).fetchall()
            elapsed=(time.perf_counter_ns()-before)/1e6
            if fingerprint(rows)!=expected: raise AssertionError('Results changed')
            return elapsed
        idle=[sample() for _ in range(samples)]
        ready=threading.Event();stop=threading.Event();errors=[];writes=[]
        def writer():
            conn=sqlite3.connect(path,timeout=5)
            try:
                i=0
                while not stop.is_set():
                    before=time.perf_counter_ns()
                    # Change metadata genuinely while keeping selected payloads identical.
                    conn.execute('UPDATE programs SET search=? WHERE scope=? AND channel=? AND day=?',('revision '+str(i),'scope',str(i%channels),'5'))
                    conn.commit();writes.append((time.perf_counter_ns()-before)/1e6)
                    ready.set();i+=1
            except Exception as error: errors.append(type(error).__name__);ready.set()
            finally: conn.close()
        thread=threading.Thread(target=writer)
        thread.start()
        try:
            if not ready.wait(10): raise RuntimeError('Writer startup timeout')
            busy=[sample() for _ in range(samples)]
        finally:
            stop.set();thread.join(10);db.close()
        if thread.is_alive(): raise RuntimeError('Writer did not stop')
        if errors: raise RuntimeError('Writer failed: '+','.join(errors))
        return {'channels':channels,'rows':channels*8*48,'samples':samples,
                'sqlite':sqlite3.sqlite_version,'journal_mode':'wal',
                'result_sha256':expected,'query':query,'idle':stats(idle),'with_writer':stats(busy),
                'write_transactions':len(writes),'write_latency':stats(writes),
                'source_sha256':hashlib.sha256(source.encode()).hexdigest(),
                'limits':'Synthetic Linux SQLite; changing search-metadata writes, one reader, one writer. Not Android/UI/network or a real EPG sync. Warm cache; concurrency varies with scheduler.'}
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--channels',type=int,default=180);p.add_argument('--samples',type=int,default=50)
    p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    if not 2<=a.channels<=1000 or not 5<=a.samples<=200:p.error('channels 2..1000; samples 5..200')
    report=run(a.channels,a.samples)
    with a.output.open('x') as out:json.dump(report,out,indent=2);out.write('\n')
    print(json.dumps(report))
if __name__=='__main__':main()
