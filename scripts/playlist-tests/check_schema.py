"""Host SQL contract checks. Does not substitute for Android SQLite lifecycle tests."""
import pathlib, re, sqlite3
root = pathlib.Path(__file__).resolve().parents[2]
source = (root / 'app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java').read_text()
db = sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
for sql in re.findall(r'db\.execSQL\("((?:CREATE|ALTER) [^"]+)"\)', source):
    db.execute(sql)
db.execute("INSERT INTO sources(id,name,origin,active) VALUES ('s','Test','server','old')")
insert = 'INSERT INTO entries VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)'
def row(generation, identity, position):
    return ('s', generation, identity, 0, position, 'movie', 'Movie', '', -1, -1, 'Cinema', '', 'https://example.org', '{}', '{}')
db.execute(insert, row('old', 'same', 0))
db.execute("INSERT INTO preferences(source,identity,occurrence,favorite) VALUES ('s','same',0,1)")
db.commit()
page = re.search(r'rawQuery\("(SELECT e\.\* [^"]+)"', source).group(1)
# A committed staging batch must remain invisible after process failure.
db.execute(insert, row('pending', 'same', 0)); db.commit()
assert [r[1] for r in db.execute(page, ('s','movie',-1,50))] == ['old']
# Rollback of activation still leaves the previous catalogue available.
db.execute("UPDATE sources SET active='pending' WHERE id='s'"); db.rollback()
assert [r[1] for r in db.execute(page, ('s','movie',-1,50))] == ['old']
db.execute("UPDATE sources SET active='pending' WHERE id='s'"); db.commit()
assert [r[1] for r in db.execute(page, ('s','movie',-1,50))] == ['pending']
assert db.execute('SELECT favorite FROM preferences').fetchone() == (1,)
assert list(db.execute(page, ('s','movie',0,50))) == []
try:
    db.execute("INSERT INTO sources(id,name,origin) VALUES ('bad','bad','invalid')")
    raise AssertionError('invalid origin accepted')
except sqlite3.IntegrityError:
    pass
print('PASS: exact SQLite schema, hidden staging, activation rollback, preferences retained, keyset page, origin validation')
# Episodes must follow season/episode order even when the provider lists them backwards.
for position, season, episode in [(4,2,1),(5,1,2),(6,1,1)]:
    values=list(row('pending', 'ep'+str(position), position))
    values[5]='series'; values[7]='Show'; values[8]=season; values[9]=episode
    db.execute(insert,values)
episode_sql=re.search(r'rawQuery\("(SELECT e\.\* [^"]+ORDER BY e\.season[^"]+)"', source).group(1)
assert [r[4] for r in db.execute(episode_sql, ('s','Show',2,0))] == [6,5]
assert [r[4] for r in db.execute(episode_sql, ('s','Show',2,2))] == [4]
print('PASS: episode order and bounded episode pages')

# Android rawQuery binds selectionArgs as TEXT: numeric literals do not coerce
# a bare bound parameter, so filters must compare against textual flags.
browse=re.search(r'rawQuery\("(SELECT e\.\*,COALESCE[^"]+)"',source).group(1)
db.execute(insert,row('pending','second',7))
args=('s','movie','movie','','','0','0','50','0')
assert len(list(db.execute(browse,args)))==2
db.execute("UPDATE preferences SET hidden=1,label='Hidden' WHERE identity='same'")
assert len(list(db.execute(browse,args)))==1
assert len(list(db.execute(browse,('s','movie','movie','','','1','1','50','0'))))==1
assert len(list(db.execute(browse,('s','movie','movie','','','1','0','50','0'))))==0
print('PASS: Android TEXT parameter filter semantics, favorites and hidden entries')
