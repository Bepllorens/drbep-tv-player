const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),test=require('node:test');
const html=fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const detail=html.slice(html.indexOf('function bbEventDetail('),html.indexOf('const bbTimed='));
const render=html.slice(html.indexOf('function renderPlaybackTimings('),html.indexOf('async function loadBlackBox('));
function setup(){
 function node(){return {children:[],textContent:'',append(...items){this.children.push(...items)},replaceChildren(){this.children=[]}}}
 const box=node(),context={document:{createElement:node},$:()=>box};
 vm.runInNewContext(detail+'\n'+render,context);
 function texts(n=box){return [n.textContent,...n.children.flatMap(texts)].join('\n')}
 return {context,box,texts};
}
test('resolution and prepare-relative frame remain separate',()=>{
 const {context,texts}=setup();
 context.renderPlaybackTimings([{at:1,operation:2,resolution:{durationMs:180},stages:{PREPARE:0,FRAME:50}}]);
 assert.match(texts(),/Antes de preparar: 180 ms/);
 assert.match(texts(),/Primer fotograma: 50 ms desde preparar/);
 assert.doesNotMatch(texts(),/230 ms/);
});
test('legacy and incomplete traces never invent a previous duration',()=>{
 const {context,texts}=setup();
 for(const resolution of [undefined,{durationMs:-1},{durationMs:'123'}]){
  context.renderPlaybackTimings([{at:1,operation:2,resolution,stages:{PREPARE:0}}]);
  assert.match(texts(),/sin registro correlacionado/);
 }
});
test('no traces retains empty state',()=>{
 const {context,texts}=setup();context.renderPlaybackTimings([]);assert.match(texts(),/Todavía no hay hitos/);
});
test('HTTP headers and failure without body are distinguished',()=>{
 const {context}=setup();
 assert.match(context.bbEventDetail({event:'HTTP_RESPONSE_HEADERS',a:25,b:503}),/HTTP 503/);
 const failure=context.bbEventDetail({event:'HTTP_REQUEST_END',a:50,b:-1,c:2});
 assert.match(failure,/Total 50 ms/);assert.match(failure,/procesamiento Sin registro/);assert.match(failure,/fallo/);
});
test('cancellation is distinct from completed response',()=>{
 const {context}=setup();
 assert.match(context.bbEventDetail({event:'HTTP_REQUEST_END',a:1,b:0,c:1}),/cancelada/);
 assert.match(context.bbEventDetail({event:'HTTP_REQUEST_END',a:1,b:0,c:0}),/completada/);
});
test('stop does not claim confirmed playback cessation or durable progress',()=>{
 const {context}=setup();
 assert.match(context.bbEventDetail({event:'SAFETY_PROGRESS_SAVED',a:5}),/no confirma persistencia/);
 assert.match(context.bbEventDetail({event:'SAFETY_STOP_ISSUED',a:5}),/pendiente comprobar/);
 assert.match(context.bbEventDetail({event:'SAFETY_STOP_ACK',a:5,b:3}),/no se pudo encolar/);
});
test('unrecognized event keeps old formatter and ignores unexpected text fields',()=>{
 const {context}=setup();
 assert.equal(context.bbEventDetail({event:'OLD_EVENT'}),null);
 const out=context.bbEventDetail({event:'HTTP_RESPONSE_HEADERS',a:'secret-fixture',b:'secret-fixture',url:'secret-fixture'});
 assert.doesNotMatch(out,/secret-fixture/);assert.match(out,/desconocido/);
});

test('miniguide reports microseconds without claiming frame duration',()=>{
 const {context}=setup();
 assert.match(context.bbEventDetail({event:'GUIDE_BIND_CACHED',a:250,c:24}),/250 µs/);
 assert.match(context.bbEventDetail({event:'GUIDE_BIND_PHASES',a:1,b:2,c:3}),/no mide dibujo/);
 assert.doesNotMatch(context.bbEventDetail({event:'GUIDE_BIND_COLD',a:'private',c:'private'}),/private/);
});
