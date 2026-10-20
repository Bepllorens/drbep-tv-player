const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('let remoteStatusRequest='),html.indexOf("for(const b of $('remoteControls').querySelectorAll('button'))b.onclick",html.indexOf('let remoteStatusRequest=')));
function fixture(){
 const buttons=['seek','previous','next','live','pause'].map(command=>({dataset:{command},disabled:false}));
 const pending=[],labels={},errors=[];
 const c={token:'one',currentView:'remote',$:()=>({querySelectorAll:()=>buttons}),
 text:(id,value)=>labels[id]=value,message:e=>errors.push(e),
 manage:(command,args)=>new Promise((resolve,reject)=>pending.push({command,args,resolve,reject}))};
 vm.createContext(c);vm.runInContext(code,c);return {c,buttons,pending,labels,errors};
}
const state=(title,hasMedia=true)=>({title,hasMedia,seekable:false,canPrevious:false,canNext:true,canLive:false});
test('newer response wins and capability buttons follow it',async()=>{
 const f=fixture();const a=f.c.loadRemote(),b=f.c.loadRemote();
 f.pending[1].resolve(state('new'));await b;f.pending[0].resolve(state('old',false));await a;
 assert.equal(f.labels.remoteTitle,'new');assert.deepEqual(f.buttons.map(b=>b.disabled),[true,true,false,true,false]);
});
test('disconnect clears controls and ignores previous status',async()=>{
 const f=fixture();const work=f.c.loadRemote();f.c.resetRemoteStatus();f.c.token='';
 f.pending[0].resolve(state('old'));await work;
 assert.equal(f.labels.remoteTitle,'Sin reproducción');assert.ok(f.buttons.every(b=>b.disabled));
});
test('leaving remote hides late errors',async()=>{
 const f=fixture();const work=f.c.loadRemote();f.c.currentView='overview';f.pending[0].reject(Error('old'));await work;
 assert.equal(f.errors.length,0);
});
test('command invalidates pre-command status then refreshes once',async()=>{
 const f=fixture();const read=f.c.loadRemote(),command=f.c.sendRemoteCommand(f.buttons[0]);
 f.pending[0].resolve(state('old'));await read;assert.equal(f.labels.remoteTitle,undefined);
 f.pending[1].resolve({});await new Promise(setImmediate);
 assert.equal(f.pending[2].command,'remote-status');f.pending[2].resolve(state('updated'));await command;
 assert.equal(f.labels.remoteTitle,'updated');
});
test('old session command completion cannot query new session',async()=>{
 const f=fixture();const command=f.c.sendRemoteCommand(f.buttons[0]);f.c.token='two';
 f.pending[0].resolve({});await command;assert.equal(f.pending.length,1);
});
test('failed command in current session reports error and refreshes controls',async()=>{
 const f=fixture();const command=f.c.sendRemoteCommand(f.buttons[0]);f.pending[0].reject(Error('failed'));
 await new Promise(setImmediate);assert.equal(f.errors.length,1);
 f.pending[1].resolve(state('same'));await command;assert.equal(f.labels.remoteTitle,'same');
});
