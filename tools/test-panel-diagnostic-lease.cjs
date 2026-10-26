const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('// Diagnostic observer:'),html.indexOf('// End diagnostic observer.'));
function fixture(){
 const pending=[],calls=[],timers=new Map(),events={},labels={};let next=1;
 const c={token:'session',currentView:'diagnostics',document:{hidden:false},Math,Date,
  window:{addEventListener:(type,callback)=>{events[type]=callback}},
  api:(url,options)=>{calls.push({url,...JSON.parse(options.body)});return new Promise((resolve,reject)=>pending.push({resolve,reject}))},
  fetch:(url,options)=>{calls.push({url,...JSON.parse(options.body),release:true});return Promise.resolve({ok:true})},
  loadBlackBox:async()=>calls.push({summary:true}),text:(id,value)=>{labels[id]=value},
  setTimeout:(callback,ms)=>{const id=next++;timers.set(id,{callback,ms});return id},
  clearTimeout:id=>timers.delete(id)};
 vm.createContext(c);vm.runInContext(code,c);
 return {c,pending,calls,timers,events,labels};
}
const tick=()=>new Promise(resolve=>setImmediate(resolve));
test('default and hidden diagnostics issue no detail requests',()=>{
 const f=fixture();f.c.syncDetailLease();assert.equal(f.calls.length,0);
 f.c.document.hidden=true;f.c.toggleDetailLease();assert.equal(f.calls.length,0);assert.equal(f.timers.size,0);
});
test('visible observation renews then leaving diagnostics releases without a timer',async()=>{
 const f=fixture();f.c.toggleDetailLease();assert.equal(f.calls[0].active,true);
 f.pending[0].resolve({ok:true});await tick();assert.equal(f.timers.size,1);assert.equal([...f.timers.values()][0].ms,7000);
 f.c.currentView='overview';f.c.syncDetailLease();assert.equal(f.timers.size,0);
 assert.equal(f.calls.at(-1).active,false);assert.ok(f.calls.at(-1).sequence>f.calls[0].sequence);
});
test('old renewal response cannot restart detail after hiding or closing',async()=>{
 const f=fixture();f.c.toggleDetailLease();f.c.document.hidden=true;f.c.syncDetailLease();
 f.pending[0].resolve({ok:true});await tick();
 assert.equal(f.timers.size,0);assert.equal(f.calls.filter(x=>x.summary).length,0);
 f.c.document.hidden=false;f.c.syncDetailLease();assert.equal(f.calls.at(-1).active,true);
 f.events.pagehide();f.pending[1].resolve({ok:true});await tick();assert.equal(f.timers.size,0);
});
test('new visible observation survives a late response from the old one',async()=>{
 const f=fixture();f.c.toggleDetailLease();f.c.document.hidden=true;f.c.syncDetailLease();
 f.c.document.hidden=false;f.c.syncDetailLease();
 assert.ok(f.calls.at(-1).sequence>f.calls[1].sequence);
 f.pending[0].resolve({ok:true});await tick();assert.equal(f.timers.size,0);
 f.pending[1].resolve({ok:true});await tick();assert.equal(f.timers.size,1);
 assert.equal(f.calls.filter(x=>x.summary).length,1);
});
test('switching off and old-session completion cannot restart observation',async()=>{
 const f=fixture();f.c.toggleDetailLease();f.c.toggleDetailLease();f.c.token='';
 f.pending[0].resolve({ok:true});await tick();assert.equal(f.timers.size,0);
 assert.equal(f.calls.filter(x=>x.summary).length,1); // explicit refresh when switching off
});
