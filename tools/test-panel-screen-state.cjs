const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf("let screenBusy="),html.indexOf("$('screenLive').onchange"));
function fixture(){
 const nodes={},pending=[],created=[],revoked=[],labels=[];let disconnected=0;
 const node=()=>({checked:true,hidden:true,children:[],removeAttribute(k){delete this[k]},replaceChildren(){this.children=[]},append(v){this.children.push(v)}});
 const c={token:'one',currentView:'diagnostics',document:{hidden:false,createElement:node},
 $:id=>nodes[id]||(nodes[id]=node()),text:(id,v)=>labels.push([id,v]),disconnect:()=>disconnected++,
 URL:{createObjectURL:()=>{const u='blob:'+created.length;created.push(u);return u},revokeObjectURL:u=>revoked.push(u)},
 api:async()=>({nodes:[]}),fetch:()=>new Promise(resolve=>pending.push(resolve))};
 vm.createContext(c);vm.runInContext(code,c);
 return {c,nodes,pending,created,revoked,labels,disconnected:()=>disconnected};
}
const response=()=>({status:200,ok:true,blob:async()=>({})});
test('turning off before response prevents rendering',async()=>{
 const f=fixture();const work=f.c.loadScreen();f.c.$('screenLive').checked=false;f.c.clearScreen();f.pending[0](response());await work;
 assert.equal(f.created.length,0);assert.equal(f.nodes.screenImage.hidden,true);
 assert.match(html,/else clearScreen\(\)/);
});
test('old authorization response cannot disconnect new session',async()=>{
 const f=fixture();const work=f.c.loadScreen();f.c.token='two';f.pending[0]({status:401});await work;
 assert.equal(f.disconnected(),0);
});
test('current unauthorized response still disconnects',async()=>{
 const f=fixture();const work=f.c.loadScreen();f.pending[0]({status:401});await work;assert.equal(f.disconnected(),1);
});
test('disabling during blob decode prevents object URL allocation',async()=>{
 const f=fixture();let release;const work=f.c.loadScreen();
 f.pending[0]({status:200,ok:true,blob:()=>new Promise(r=>release=r)});await new Promise(setImmediate);
 f.c.$('screenLive').checked=false;release({});await work;assert.equal(f.created.length,0);
});
test('successful captures replace blob and clear releases last one',async()=>{
 const f=fixture();let work=f.c.loadScreen();f.pending[0](response());await work;
 work=f.c.loadScreen();f.pending[1](response());await work;
 assert.deepEqual(f.revoked,['blob:0']);assert.equal(f.nodes.screenImage.src,'blob:1');
 f.c.clearScreen();assert.deepEqual(f.revoked,['blob:0','blob:1']);assert.equal(f.nodes.screenImage.hidden,true);
});
test('hidden diagnostics sends no request',async()=>{
 const f=fixture();f.c.document.hidden=true;await f.c.loadScreen();assert.equal(f.pending.length,0);
});
