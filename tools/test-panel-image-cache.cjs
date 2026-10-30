const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),test=require('node:test');
const html=fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const source=html.slice(html.indexOf("$('imageCacheRead').onclick="),html.indexOf("$('report').onclick="));
function setup(api){
 const nodes={imageCacheRead:{disabled:false}},labels={};
 const c={token:'session',api,$:id=>nodes[id],text:(id,value)=>labels[id]=value,Date,Number};
 vm.runInNewContext(source,c);return {c,nodes,labels};
}
test('manual only, renders actual budget and measurement',async()=>{
 let count=0;
 const {nodes,labels}=setup(async(path,options)=>{count++;assert.equal(path,'/image-cache');assert.equal(options.method,'POST');return {glide:{bytes:1048576,files:2,complete:true},svg:{bytes:0,files:0,complete:true},glide_budget_bytes:33554432,svg_budget_bytes:8388608,measured_at:1};});
 assert.equal(count,0);await nodes.imageCacheRead.onclick();assert.equal(count,1);
 assert.match(labels.imageCacheResult,/32 MiB/);assert.match(labels.imageCacheResult,/2 archivos/);
 assert.equal(nodes.imageCacheRead.disabled,false);
});
test('old session response cannot render',async()=>{
 let resolve;const {c,nodes,labels}=setup(()=>new Promise(r=>resolve=r));
 const pending=nodes.imageCacheRead.onclick();c.token='new';resolve({});await pending;
 assert.equal(labels.imageCacheResult,undefined);assert.equal(nodes.imageCacheRead.disabled,false);
});
test('request error releases button',async()=>{
 const {nodes,labels}=setup(async()=>{throw Error('Sin conexión');});
 await nodes.imageCacheRead.onclick();assert.equal(labels.imageCacheResult,'Sin conexión');assert.equal(nodes.imageCacheRead.disabled,false);
});
