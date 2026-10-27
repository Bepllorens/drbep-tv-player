// Browser integration checks against a local fixture server; never connects to a device.
const { chromium } = require('playwright');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const pageHtml = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/device-panel.html'));
const output = process.env.PANEL_TEST_OUTPUT || '/private/tmp/drbep-panel-preview';
fs.mkdirSync(output, { recursive: true });
let expired = false, favorite = false, requests = [], sources = [
 {id:'a',name:'Mi televisión',origin:'file',updated:1791636000000},
 {id:'b',name:'Cine y series',origin:'url',updated:1791636000000},
 {id:'c',name:'Servidor DRBEP',origin:'server',updated:1791636000000}
];
const server = http.createServer(async (req,res) => {
 let body='';for await(const chunk of req)body+=chunk;
 const send=(data,status=200)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(data))};
 if(req.url==='/'){res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});res.end(pageHtml);return}
 if(req.url==='/session'){send(body==='123456'?{token:'fixture-session'}:{},body==='123456'?200:401);return}
 if(expired||req.headers['x-panel-token']!=='fixture-session'){send({},401);return}
 if(req.url==='/status'||req.url==='/report'){send({'Versión':'2.0.700-beta-device-panel','Dispositivo':'Fire TV','Memoria de la app':'124 MB / 384 MB','Espacio disponible':'4380 MB','Reproductor':'READY','Resolución':'1920 × 1080','Códec de vídeo':'AVC','Preparación':'820 ms','Interrupciones de búfer':0,'Tiempo en búfer':'0 ms','Red':'Disponible','Recuperaciones recientes':[]});return}
 if(req.url==='/playlist'){requests.push({action:'upload',size:body.length});send({queued:true,job:'upload-1'});return}
 if(req.url==='/network-check'){assert.equal(req.method,'POST');assert.equal(body,'');requests.push({action:'network-check'});send({state:'complete',dns:'ok',dns_ms:12,connection:'ok',connection_ms:28,http_status:200});return}
 if(req.url==='/blackbox'){send({enabled:true,detailed:false,bytes:4096,lost:0,writeErrors:0,session:'fixture',detailObservers:0,events:[],startups:[],playbacks:[{at:1791636000000,operation:7,stages:{PREPARE:0,MANIFEST:45,MEDIA:80,VIDEO_DECODER:110,READY:140,FRAME:160},details:{MANIFEST:{durationMs:20,bytes:1200,trackType:-1},MEDIA:{durationMs:30,bytes:8192,trackType:2},VIDEO_DECODER:{durationMs:12,bytes:0,trackType:0}}}],stats:[]});return}
 if(req.url==='/metadata-index'){send({state:'idle',groups:[],sources:[]});return}
 if(req.url==='/ui'){send({nodes:[],dialog:false});return}
 if(req.url==='/screen'){res.writeHead(200,{'Content-Type':'image/png'});res.end(Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6VDEAAAAASUVORK5CYII=','base64'));return}
 if(!body){send({error:'Unknown fixture endpoint'},404);return}
 const data=JSON.parse(body);requests.push(data);
 if(data.action==='guide-channels'){send({mode:data.source==='__drbep'?'server':'manual',feed:'example.com',channels:[{id:'c1',name:'Canal de prueba',epgId:'canal1',occurrence:0}]});return}
 if(data.action==='guide-day'||data.action==='guide-detail'){
  const start=Date.now()-120000,end=Date.now()+120000;
  const row={start,end,title:data.day==='2026-10-11'?'Día anterior':'Programa de prueba',state:'En directo',meta:'Deportes · 2026',color:'#68C990',description:'Sinopsis legible',credits:{actor:'Reparto de prueba'},image:''};
  const response={programmes:[row],epgId:'canal1',now:Date.now(),timeZone:'Europe/Madrid'};
  if(data.day==='2026-10-10'){setTimeout(()=>send({...response,programmes:[{...row,title:'Respuesta antigua'}]}),300);return}
  send(response);return
 }
 if(data.action==='remote-status'){send({title:'La 1',hasMedia:true,playing:true,live:true,seekable:false,canPrevious:true,canNext:true,canLive:false});return}
 if(data.action==='series'){send({rows:[{title:'Serie de prueba',episodes:12}]});return}
 if(data.action==='episodes'){send({seasons:[1,2],season:data.season||1,rows:[{identity:'b'.repeat(64),occurrence:0,title:'Episodio de prueba',episode:1,position:120000,watched:false}]});return}
 if(data.action==='catchup-list'){send({day:'2026-10-10',days:['2026-10-10','2026-10-09'],channels:[{id:'la1',name:'La 1'}],genres:['Noticias'],rows:[{id:'p1',title:'Telediario',channel:'La 1',genre:'Noticias',description:'Información de prueba',start:1791636000000,end:1791639600000,available:true,saved:false,airing:false}]});return}
 if(req.url==='/artwork'){res.writeHead(404);res.end();return}
 if(data.action==='lists'){send({sources});return}
 if(data.action==='jobs'){send({jobs:[{id:'j1',name:'Cine y series',phase:'Leyendo lista',count:12840,state:'running',updated:Date.now()},{id:'j2',name:'Mi televisión',phase:'Importación terminada',count:186,state:'completed',updated:1791636000000}]});return}
 if(data.action==='source-info'){send({automatic:true});return}
 if(data.action==='preference'){favorite=data.favorite;send({ok:true});return}
 if(data.action==='configure'){sources=sources.map(s=>s.id===data.source?{...s,name:data.name}:s);send({ok:true});return}
 if(data.action==='delete'){sources=sources.filter(s=>s.id!==data.source);send({ok:true});return}
 if(data.action==='entries'){
  const prefix=data.source==='a'?'Canal':'Película';
  const entries=data.query==='nada'?[]:Array.from({length:data.offset===50?2:50},(_,i)=>({identity:'a'.repeat(64),occurrence:i+data.offset,title:i===0?'<img src=x onerror=alert(1)>':prefix+' '+(i+data.offset+1),kind:data.kind||'movie',favorite,hidden:false,order:7}));
  if(data.query==='lenta'){setTimeout(()=>send({entries}),180);return}
  send({entries});return
 }
 send({ok:true,queued:true});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const browser=await chromium.launch(process.env.PANEL_BROWSER_EXECUTABLE?{executablePath:process.env.PANEL_BROWSER_EXECUTABLE,headless:true}:{channel:'chrome',headless:true});
 try {
 const page=await browser.newPage({viewport:{width:1440,height:1000}});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:'+server.address().port);
 await page.locator('#pin').fill('000000');await page.locator('#connect').click();await page.locator('#loginError').filter({hasText:'PIN incorrecto'}).waitFor();
 await page.locator('#pin').fill('123456');await page.locator('#connect').click();await page.locator('#memoryValue').filter({hasText:'124 MB'}).waitFor();
 await page.screenshot({path:path.join(output,'resumen.png'),fullPage:true});
 await page.locator('[data-view="library"]').click();await page.locator('#entries .entry').first().waitFor();
 assert.equal(await page.locator('#entries img').count(),0,'playlist titles must render as text');
 await page.locator('#sources button').nth(1).click();await page.locator('#sourceName').filter({hasText:'Cine y series'}).waitFor();
 await page.locator('#kind').selectOption('series');await page.waitForFunction(()=>document.querySelector('#entries .entry-title span')?.textContent.includes('Serie'));
 await page.locator('#next').click();await page.locator('#pageLabel').filter({hasText:'Página 2'}).waitFor();assert.equal(await page.locator('#entries .entry').count(),2);
 await page.locator('#configure').click();await page.locator('#editor[open]').waitFor();assert.equal(await page.locator('#edit-automatic').isChecked(),true);
 await page.locator('#edit-name').fill('Mi cine');await page.locator('#editorSave').click();await page.locator('#sourceName').filter({hasText:'Mi cine'}).waitFor();
 await page.locator('#entries .entry').first().getByRole('button',{name:'Editar',exact:true}).click();assert.equal(await page.locator('#edit-order').inputValue(),'7');await page.locator('#editorCancel').click();
 await page.locator('#query').fill('nada');await page.locator('#filters').getByRole('button',{name:'Buscar'}).click();await page.locator('#entryEmpty').filter({hasText:'No hay resultados'}).waitFor();
 await page.locator('#query').fill('');await page.locator('#filters').getByRole('button',{name:'Buscar'}).click();await page.locator('#entries .entry').first().waitFor();
 await page.screenshot({path:path.join(output,'listas.png'),fullPage:true});
 const before=await page.locator('#sources button[aria-pressed=true]').innerText();await page.locator('#refresh').click();await page.waitForTimeout(400);assert.equal(await page.locator('#sources button[aria-pressed=true]').innerText(),before);
 await page.locator('[data-view="import"]').click();await page.locator('#urlName').fill('Nueva lista');await page.locator('#urlAddress').fill('https://example.com/lista.m3u');await page.locator('#urlGuide').fill('https://example.com/epg.xml.gz');await page.locator('#urlAuto').check();await page.locator('#sendUrl').click();await page.locator('#view-tasks:not([hidden])').waitFor();assert.ok(requests.some(r=>r.action==='import-url'&&r.automatic&&r.epg));
 await page.locator('[data-view="import"]').click();await page.locator('#file').setInputFiles({name:'prueba.m3u',mimeType:'audio/x-mpegurl',buffer:Buffer.from('#EXTM3U\n#EXTINF:-1,Prueba\nhttps://example.com/live\n')});await page.locator('#send').click();await page.locator('#view-tasks:not([hidden])').waitFor();assert.ok(requests.some(r=>r.action==='upload'&&r.size>0));
 await page.setViewportSize({width:390,height:844});await page.locator('[data-view="import"]').click();await page.screenshot({path:path.join(output,'movil.png'),fullPage:true});
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false,'mobile page must not overflow horizontally');
 await page.locator('[data-view="remote"]').click();await page.locator('#remoteTitle').filter({hasText:'La 1'}).waitFor();assert.equal(await page.locator('[data-command="seek"]').first().isDisabled(),true);await page.locator('[data-command="pause"]').click();assert.ok(requests.some(r=>r.action==='remote-command'&&r.command==='pause'));
 await page.locator('[data-view="series"]').click();await page.locator('#seriesRows').getByRole('button',{name:'Ver episodios'}).click();await page.locator('#episodeTitle').filter({hasText:'Serie de prueba'}).waitFor();await page.locator('#episodeRows').getByRole('button',{name:'Continuar',exact:true}).click();assert.ok(requests.some(r=>r.action==='play-entry'&&r.resume));await page.locator('#episodeRows').getByRole('button',{name:'Marcar visto',exact:true}).click();assert.ok(requests.some(r=>r.action==='watched'&&r.watched));await page.locator('#episodeSeason').selectOption('2');await page.waitForTimeout(100);assert.ok(requests.some(r=>r.action==='episodes'&&r.season===2));
 await page.locator('[data-view="catchup"]').click();await page.locator('#catchupRows h3').filter({hasText:'Telediario'}).waitFor();await page.locator('#catchupRows').getByRole('button',{name:'Reproducir',exact:true}).click();assert.ok(requests.some(r=>r.action==='catchup-play'&&r.id==='p1'));await page.locator('#catchupRows').getByRole('button',{name:'☆ Guardar'}).click();assert.ok(requests.some(r=>r.action==='catchup-save'));await page.locator('#catchupDay').selectOption('2026-10-09');await page.waitForTimeout(100);assert.ok(requests.some(r=>r.action==='catchup-list'&&r.day==='2026-10-09'));assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false,'catchup mobile must not overflow');
 await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:path.join(output,'catchup.png'),fullPage:true});
 await page.locator('[data-view="guide"]').click();await page.locator('#guideCards button').filter({hasText:'Programa de prueba'}).waitFor();assert.equal(await page.locator('#guideMode').isDisabled(),true);
 await page.locator('#guideSource').selectOption('a');await page.waitForTimeout(100);assert.equal(await page.locator('#guideMode').isDisabled(),false);
 await page.locator('#guideCards button').click();await page.locator('#guideDetail[open]').waitFor();await page.locator('#guideDetail summary').click();assert.match(await page.locator('#guideDetail').innerText(),/Reparto de prueba/);await page.locator('#guideClose').click();await page.waitForFunction(()=>document.querySelector('#guideCards button')===document.activeElement);
 await page.locator('#guideDay').fill('2026-10-10');await page.locator('#guideDay').dispatchEvent('change');await page.waitForTimeout(30);await page.locator('#guideDay').fill('2026-10-11');await page.locator('#guideDay').dispatchEvent('change');await page.waitForTimeout(400);assert.match(await page.locator('#guideCards').innerText(),/Día anterior/);assert.doesNotMatch(await page.locator('#guideCards').innerText(),/Respuesta antigua/);
 await page.screenshot({path:path.join(output,'programacion.png'),fullPage:true});await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false,'guide mobile overflow');await page.screenshot({path:path.join(output,'programacion-movil.png'),fullPage:true});
 await page.locator('[data-view="diagnostics"]').click();await page.locator('#stats dt').first().waitFor();
 await page.locator('#bbPlaybacks').getByText('Primer manifiesto: 45 ms desde preparar · carga 20 ms · 1200 bytes',{exact:true}).waitFor();
 assert.match(await page.locator('#bbPlaybacks').innerText(),/Primer fragmento: 80 ms desde preparar · carga 30 ms · 8192 bytes/);
 assert.match(await page.locator('#bbPlaybacks').innerText(),/Decodificador de vídeo: 110 ms desde preparar · inicialización 12 ms/);
 assert.doesNotMatch(await page.locator('#bbStatus').innerText(),/NaN|undefined/);
 await page.screenshot({path:path.join(output,'diagnostico-fases-movil.png'),fullPage:true});
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false,'diagnostics mobile overflow');

 assert.equal(requests.filter(r=>r.action==='network-check').length,0);
 await page.locator('#networkCheck').click();
 await page.waitForFunction(()=>document.querySelector('#networkResult').textContent.includes('HTTP 200'));
 assert.equal(requests.filter(r=>r.action==='network-check').length,1);
 assert.ok((await page.locator('#networkResult').textContent()).includes('12 ms'));
 expired=true;await page.evaluate(()=>refresh());await page.locator('#loginShell:not([hidden])').waitFor();assert.match(await page.locator('#loginError').innerText(),/sesión ha terminado/);
 assert.deepEqual(errors,[]);console.log('PASS: PIN, text safety, navigation, filters, pagination, edit, source retention, URL/file import, mobile layout, diagnostics, remote controls, seasons, progress, watched state, catchup filters/play/save and session expiry.');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1}).finally(()=>server.close());
