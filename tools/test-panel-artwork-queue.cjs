const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('function observeCatchupImages(){'),html.indexOf('async function loadCatchup()',html.indexOf('function observeCatchupImages(){')));
const flush=()=>new Promise(setImmediate);
function fixture(n=8){
 const imgs=Array.from({length:n},(_,i)=>({dataset:{programme:String(i)},isConnected:true})),pending=[],listeners=new Set(),created=[],revoked=[];
 let observer;
 const c={token:'session',AbortController,WeakSet,document:{hidden:false,addEventListener:(e,f)=>listeners.add(f),removeEventListener:(e,f)=>listeners.delete(f)},
 $:()=>({querySelectorAll:()=>imgs}),
 IntersectionObserver:class{constructor(callback){observer=callback}observe(){}unobserve(){}disconnect(){}},
 URL:{createObjectURL:()=>{const u='blob:'+created.length;created.push(u);return u},revokeObjectURL:u=>revoked.push(u)},
 fetch:(url,opts)=>new Promise((resolve,reject)=>pending.push({opts,resolve,reject}))};
 vm.createContext(c);vm.runInContext(code,c);
 const dispose=c.observeCatchupImages(),intersect=()=>observer(imgs.map(target=>({target,isIntersecting:true})));
 const complete=async i=>{pending[i].resolve({ok:true,blob:async()=>({})});await flush()};
 return {c,imgs,pending,listeners,created,revoked,dispose,intersect,complete,visible:()=>{c.document.hidden=false;for(const f of listeners)f()}};
}
test('hidden queue starts only when visible and keeps concurrency at four',async()=>{
 const f=fixture();f.c.document.hidden=true;f.intersect();assert.equal(f.pending.length,0);
 f.visible();assert.equal(f.pending.length,4);
 await f.complete(0);assert.equal(f.pending.length,5);assert.equal(f.created.length,1);f.dispose();
});
test('hiding stops draining queue after in-flight completion',async()=>{
 const f=fixture();f.intersect();f.c.document.hidden=true;await f.complete(0);assert.equal(f.pending.length,4);
 f.visible();assert.equal(f.pending.length,5);f.dispose();
});
test('duplicate observer entries do not duplicate downloads',async()=>{
 const f=fixture(1);f.intersect();f.intersect();await f.complete(0);assert.equal(f.pending.length,1);f.dispose();
});
test('disposal aborts work removes listener and revokes loaded urls',async()=>{
 const f=fixture();f.intersect();await f.complete(0);f.dispose();
 assert.equal(f.listeners.size,0);assert.deepEqual(f.revoked,f.created);
 assert.ok(f.pending.slice(1).every(p=>p.opts.signal.aborted));
 const count=f.pending.length;await f.complete(1);f.intersect();assert.equal(f.pending.length,count);assert.equal(f.created.length,1);
});
test('old session queue cannot render or start requests under a new session',async()=>{
 const f=fixture();f.intersect();f.c.token='new';await f.complete(0);f.visible();
 assert.equal(f.pending.length,4);assert.equal(f.created.length,0);f.dispose();
});
test('detached images are skipped before downloading',async()=>{
 const f=fixture(3);f.imgs[0].isConnected=false;f.intersect();
 assert.equal(f.pending.length,2);assert.deepEqual(f.pending.map(p=>JSON.parse(p.opts.body).id),['1','2']);f.dispose();
});
