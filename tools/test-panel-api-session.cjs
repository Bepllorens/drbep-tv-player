const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('async function api('),html.indexOf('const manage=',html.indexOf('async function api(')));
function fixture(){
 const pending=[],connections=[];let disconnected=0;
 const c={token:'one',connection:ok=>connections.push(ok),disconnect:()=>{disconnected++;c.token=''},
 fetch:(url,options)=>new Promise((resolve,reject)=>pending.push({url,options,resolve,reject}))};
 vm.createContext(c);vm.runInContext(code,c);return {c,pending,connections,disconnected:()=>disconnected};
}
test('old network failure does not mark new session disconnected',async()=>{
 const f=fixture();const work=f.c.api('/test');f.c.token='two';f.pending[0].reject(Error('network'));
 await assert.rejects(work,/sesión ha cambiado/);assert.deepEqual(f.connections,[]);
});
test('old decoded success cannot escape into new session',async()=>{
 const f=fixture();let release;const work=f.c.api('/test');
 f.pending[0].resolve({ok:true,status:200,json:()=>new Promise(r=>release=r)});await new Promise(setImmediate);
 f.c.token='two';release({old:true});await assert.rejects(work,/sesión ha cambiado/);assert.deepEqual(f.connections,[]);
});
test('old decoded server error is replaced with session-change error',async()=>{
 const f=fixture();let release;const work=f.c.api('/test');
 f.pending[0].resolve({ok:false,status:500,json:()=>new Promise(r=>release=r)});await new Promise(setImmediate);
 f.c.token='two';release({error:'old error'});await assert.rejects(work,/sesión ha cambiado/);
});
test('current valid response preserves request and connection status',async()=>{
 const f=fixture();const work=f.c.api('/test',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'});
 assert.equal(f.pending[0].options.headers['X-Panel-Token'],'one');assert.equal(f.pending[0].options.body,'{}');
 const result={ok:true};f.pending[0].resolve({ok:true,status:200,json:async()=>result});
 assert.equal(await work,result);assert.deepEqual(f.connections,[true]);
});
test('current network failure marks connection unavailable',async()=>{
 const f=fixture();const work=f.c.api('/test');f.pending[0].reject(Error('network'));
 await assert.rejects(work,/contactar/);assert.deepEqual(f.connections,[false]);
});
test('current unauthorized response still disconnects',async()=>{
 const f=fixture();const work=f.c.api('/test');f.pending[0].resolve({status:401});
 await assert.rejects(work,/sesión ha terminado/);assert.equal(f.disconnected(),1);
});
test('unauthorized response from old session cannot disconnect new one',async()=>{
 const f=fixture();const work=f.c.api('/test');f.c.token='two';f.pending[0].resolve({status:401});
 await assert.rejects(work,/sesión ha cambiado/);assert.equal(f.disconnected(),0);
});
test('invalid current JSON never marks response successful',async()=>{
 const f=fixture();const work=f.c.api('/test');f.pending[0].resolve({ok:true,status:200,json:async()=>{throw Error('invalid JSON')}});
 await assert.rejects(work,/invalid JSON/);assert.deepEqual(f.connections,[]);
});
