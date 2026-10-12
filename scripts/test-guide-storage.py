"""Execute the actual schema migration statements with SQLite and populated v6 data."""
from pathlib import Path
import sqlite3,re,json
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java').read_text()
def body(name):
    start=source.index('{',source.index(name));level=1;i=start+1
    while level:
        if source[i]=='{':level+=1
        if source[i]=='}':level-=1
        i+=1
    return source[start+1:i-1]
def statements(name):
    return [json.loads('"'+s+'"') for s in re.findall(r'db.execSQL\("((?:\\.|[^"\\])*)"\)',body(name))]
db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
for statement in statements('void onCreate'):
    db.execute(statement)
for n in range(2,7):
    for statement in statements('void migrateV'+str(n)):db.execute(statement)
db.execute("INSERT INTO sources(id,name,origin,epg_url) VALUES('a','A','file','https://example.com/epg')")
db.execute("INSERT INTO sources(id,name,origin,epg_url) VALUES('b','B','file','https://example.com/epg')")
db.execute("INSERT INTO preferences(source,identity,occurrence,favorite,resume_ms) VALUES('b','title',0,1,90000)")
db.execute("INSERT INTO guide_feeds(id,url) VALUES('f','https://example.com/epg')")
for statement in statements('void migrateV7'):db.execute(statement)
assert db.execute('SELECT DISTINCT guide_mode FROM sources').fetchall()==[('manual',)]
assert db.execute('SELECT favorite,resume_ms FROM preferences').fetchone()==(1,90000)
db.execute("INSERT INTO guide_channels VALUES('f','g','a','canal')")
db.execute("INSERT INTO guide_channels VALUES('f','g','b','canal')")
assert len(db.execute("SELECT DISTINCT channel FROM guide_channels WHERE feed='f' AND generation='g' AND name='canal' LIMIT 2").fetchall())==2
assert 'metadata' in [r[1] for r in db.execute('PRAGMA table_info(guide_items)')]
cleanup=statements('void maintainGuides')
db.execute("DELETE FROM sources WHERE id='a'")
for statement in cleanup:db.execute(statement)
assert db.execute('SELECT count(*) FROM guide_feeds').fetchone()[0]==1
assert db.execute('SELECT favorite,resume_ms FROM preferences').fetchone()==(1,90000)
db.execute("DELETE FROM sources WHERE id='b'")
for statement in cleanup:db.execute(statement)
assert db.execute('SELECT count(*) FROM guide_feeds').fetchone()[0]==0
print('PASS: actual v6→v7 schema, populated favourites/resume, manual migration, shared feed retention and orphan cleanup')
