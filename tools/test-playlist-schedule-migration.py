"""Synthetic schema migration and real legacy query regression; never opens device data."""
from pathlib import Path
import re,sqlite3,json
root=Path(__file__).resolve().parents[1]
text=(root/'app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java').read_text()
migration=text.split('private void migrateV10(',1)[1].split('private void migrateV2(',1)[0]
statements=re.findall(r'db.execSQL\("([^"]+)"\)',migration)
assert len(statements)==2
db=sqlite3.connect(':memory:')
db.execute("CREATE TABLE sources(id TEXT PRIMARY KEY,name TEXT,locator TEXT,origin TEXT,auto_refresh INTEGER,updated INTEGER)")
rows=[('legacy','A','https://example.invalid/a','url',1,1),('manual','B','https://example.invalid/b','url',0,1),
('file','C','','file',1,1),('server','D','','server',1,1),('fresh','E','https://example.invalid/e','url',1,999)]
db.executemany("INSERT INTO sources VALUES(?,?,?,?,?,?)",rows);db.commit()
old="SELECT id,name,locator,origin FROM sources WHERE auto_refresh=1 AND (origin='server' OR locator<>'') AND updated<?"
expected=db.execute(old,(500,)).fetchall()
for sql in statements:db.execute(sql)
query=re.search(r'public Cursor dueSources.*?rawQuery\("([^"]+)"',text).group(1)
assert db.execute(query,(500,)).fetchall()==expected
assert db.execute("SELECT id,name,locator,origin,auto_refresh,updated FROM sources").fetchall()==rows
db.execute("UPDATE sources SET refresh_schedule='daily:480:240:4',refresh_slot=123 WHERE id='legacy'")
assert [r[0] for r in db.execute(query,(500,))]==['server']
db.commit()
db.execute("BEGIN")
db.execute("UPDATE sources SET auto_refresh=0,refresh_schedule='',refresh_slot=456 WHERE id='legacy'")
db.rollback()
assert db.execute("SELECT auto_refresh,refresh_schedule,refresh_slot FROM sources WHERE id='legacy'").fetchone()==(1,'daily:480:240:4',123)
print(json.dumps({'migration_statements':len(statements),'preserved_sources':len(rows),'legacy_equal':True,'custom_excluded':True,'rollback_atomic':True}))
