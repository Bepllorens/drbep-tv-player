
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const state=html.slice(html.indexOf('let metadataSelectionLoaded='),html.indexOf('async function loadMetadataGroups'));
const code=html.slice(html.indexOf('async function loadMetadataIndex(){'),html.indexOf('\nfor(const [id,command]',html.indexOf('async function loadMetadataIndex(){')));
function fixture(){
 const nodes={},pending=[],labels={};let reviews=0;
 const c={$:id=>nodes[id]||(nodes[id]={value:'',disabled:false}),text:(id,v)=>labels[id]=v,
  options:(id,rows,value)=>{c.$(id).value=value},
  clearMetadataReview:()=>reviews++,loadMetadataGroups:async()=>{},
  api:()=>new Promise(resolve=>pending.push(resolve))};
 c.updateTaskOverview=()=>{};vm.createContext(c);vm.runInContext(state+code,c);return {c,nodes,pending,labels,reviews:()=>reviews};
}
const result=(enabled,state,done=0)=>({enabled,state,done,total:10,sources:[],source:'',kind:''});
test('older response cannot overwrite latest progress or enabled controls',async()=>{
 const {c,pending,labels,nodes}=fixture();
 const old=c.loadMetadataIndex(),latest=c.loadMetadataIndex();
 pending[1](result(false,'paused',7));await latest;
 pending[0](result(true,'running',2));await old;
 assert.match(labels.metadataState,/pausa/);assert.match(labels.metadataCounts,/7 de 10/);assert.equal(nodes.metadataStart.disabled,false);
});
test('response waiting for groups cannot overwrite newer status',async()=>{
 const {c,pending,labels}=fixture();let release;
 c.loadMetadataGroups=()=>new Promise(r=>release=r);
 const first=c.loadMetadataIndex();pending[0](result(true,'running',1));await new Promise(setImmediate);assert.equal(typeof release,'function');
 const second=c.loadMetadataIndex();pending[1](result(false,'complete',10));await second;
 release();await first;assert.equal(labels.metadataState,'Proceso terminado');assert.match(labels.metadataCounts,/10 de 10/);
});
test('disconnect invalidates pending response and clears session progress',async()=>{
 const {c,pending,labels,nodes,reviews}=fixture();const work=c.loadMetadataIndex();
 c.resetMetadataSession();pending[0](result(true,'running',8));await work;
 assert.equal(labels.metadataState,'Consultando estado…');assert.equal(nodes.metadataProgress.value,0);assert.equal(reviews(),1);
 assert.match(html,/function disconnect\(expired=false\)\{resetTaskOverview\(\);resetMetadataSession\(\)/);
});
test('reconnection reloads groups and selection',async()=>{
 const {c,pending}=fixture();let loads=0;c.loadMetadataGroups=async()=>loads++;
 let work=c.loadMetadataIndex();pending[0](result(false,'paused'));await work;
 c.resetMetadataSession();work=c.loadMetadataIndex();pending[1](result(false,'paused'));await work;
 assert.equal(loads,2);
});

test('running work changing source or kind reloads matching groups',async()=>{
 const {c,pending}=fixture();const selected=[];
 c.loadMetadataGroups=async value=>selected.push([c.$('metadataSource').value,c.$('metadataKind').value,value]);
 let work=c.loadMetadataIndex();pending.at(-1)({...result(true,'running'),source:'a',kind:'movie',group:'Cinema',sources:[{id:'a',name:'A'},{id:'b',name:'B'}]});await work;
 work=c.loadMetadataIndex();pending.at(-1)({...result(true,'running'),source:'b',kind:'series',group:'Drama',sources:[{id:'a',name:'A'},{id:'b',name:'B'}]});await work;
 assert.deepEqual(selected,[['a','movie','Cinema'],['b','series','Drama']]);
});
test('unchanged progress does not reload groups or discard manual selection',async()=>{
 const {c,pending}=fixture();let loads=0;c.loadMetadataGroups=async()=>loads++;
 let work=c.loadMetadataIndex();pending.at(-1)(result(false,'paused'));await work;
 c.$('metadataGroup').value='Manual';
 work=c.loadMetadataIndex();pending.at(-1)(result(false,'paused',4));await work;
 assert.equal(loads,1);assert.equal(c.$('metadataGroup').value,'Manual');
});
