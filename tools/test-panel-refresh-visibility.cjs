const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('async function refresh(automatic='),html.indexOf('function poll()',html.indexOf('async function refresh(automatic=')));
function fixture(){
 const calls=[],pending=[];
 const c={token:'session',refreshing:false,document:{hidden:false},currentView:'catchup',catchupUpdated:0,
 api:()=>{calls.push('status-request');return new Promise((resolve,reject)=>pending.push({resolve,reject}))},
 status:()=>calls.push('status-render'),jobs:async()=>calls.push('jobs'),lists:async()=>calls.push('lists'),
 loadRemote:async()=>calls.push('remote'),loadCatchup:async()=>calls.push('catchup'),message:()=>calls.push('error')};
 vm.createContext(c);vm.runInContext(code,c);return {c,calls,pending};
}
test('hidden automatic refresh issues no requests',async()=>{
 const {c,calls}=fixture();c.document.hidden=true;await c.refresh(true);assert.deepEqual(calls,[]);assert.equal(c.refreshing,false);
});
test('hiding during status stops remaining queries and visible retry resumes',async()=>{
 const {c,calls,pending}=fixture();let work=c.refresh(true);c.document.hidden=true;pending[0].resolve({});await work;
 assert.deepEqual(calls,['status-request']);assert.equal(c.refreshing,false);
 c.document.hidden=false;work=c.refresh(true);pending[1].resolve({});await work;
 assert.deepEqual(calls,['status-request','status-request','status-render','jobs','lists','catchup']);
});
test('hiding after jobs prevents lists and catchup',async()=>{
 const {c,calls,pending}=fixture();c.jobs=async()=>{calls.push('jobs');c.document.hidden=true};
 const work=c.refresh(true);pending[0].resolve({});await work;
 assert.deepEqual(calls,['status-request','status-render','jobs']);
});
test('old session status cannot render or launch work in new session',async()=>{
 const {c,calls,pending}=fixture();const work=c.refresh();c.token='new-session';pending[0].resolve({});await work;
 assert.deepEqual(calls,['status-request']);assert.equal(c.refreshing,false);
});
test('automatic hidden failure is quiet and releases refresh guard',async()=>{
 const {c,calls,pending}=fixture();const work=c.refresh(true);c.document.hidden=true;pending[0].reject(Error('offline'));await work;
 assert.deepEqual(calls,['status-request']);assert.equal(c.refreshing,false);
});
test('manual refresh retains its existing behavior and overlapping call is ignored',async()=>{
 const {c,calls,pending}=fixture();c.document.hidden=true;
 const work=c.refresh();await c.refresh();pending[0].resolve({});await work;
 assert.deepEqual(calls,['status-request','status-render','jobs','lists','catchup']);
});
