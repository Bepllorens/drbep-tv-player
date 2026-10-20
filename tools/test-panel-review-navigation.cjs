const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('let metadataReviewCursor='),html.indexOf("$('metadataReviewLoad').onclick"));
function fixture(){
 const nodes={},pending=[],requests=[],labels={};
 const node=()=>({disabled:false,children:[],append(...items){this.children.push(...items)},replaceChildren(...items){this.children=items}});
 const c={$:id=>nodes[id]||(nodes[id]=node()),document:{createElement:node},text:(id,v)=>labels[id]=v,message:()=>{},
 api:(url,opts)=>{requests.push(JSON.parse(opts.body));return new Promise((resolve,reject)=>pending.push({resolve,reject}));}};
 vm.createContext(c);vm.runInContext(code,c);c.$('metadataReviewState').value='unmatched';c.clearMetadataReview();
 return {c,nodes,pending,requests,labels};
}
const page=(next,more,title='Title')=>({next,more,rows:[{title,provider:'Test',kind:'movie'}]});
async function respond(f,reset,next,more,previous=false){
 const work=f.c.reviewMetadata(reset,previous);f.pending.at(-1).resolve(page(next,more));await work;
}
test('forward and previous use original cursors and reset starts from zero',async()=>{
 const f=fixture();
 await respond(f,true,50,true);await respond(f,false,100,true);await respond(f,false,150,false);
 await respond(f,false,100,true,true);await respond(f,false,50,true,true);
 assert.deepEqual(f.requests.map(r=>r.after),[0,50,100,50,0]);
 assert.equal(f.nodes.metadataReviewPrevious.disabled,true);
 await respond(f,true,50,true);assert.equal(f.requests.at(-1).after,0);
});
test('failed forward preserves visible page and enables retry with same cursor',async()=>{
 const f=fixture();await respond(f,true,50,true);
 const row=f.nodes.metadataReviewRows.children[0];
 const work=f.c.reviewMetadata(false);f.pending.at(-1).reject(Error('offline'));await work;
 assert.equal(f.nodes.metadataReviewRows.children[0],row);assert.equal(f.nodes.metadataReviewNext.disabled,false);
 await respond(f,false,100,false);assert.deepEqual(f.requests.map(r=>r.after),[0,50,50]);
});
test('failed previous leaves history available for retry',async()=>{
 const f=fixture();await respond(f,true,50,true);await respond(f,false,100,false);
 const work=f.c.reviewMetadata(false,true);f.pending.at(-1).reject(Error('offline'));await work;
 assert.equal(f.nodes.metadataReviewPrevious.disabled,false);assert.equal(f.nodes.metadataReviewNext.disabled,true);
 await respond(f,false,50,true,true);assert.deepEqual(f.requests.map(r=>r.after),[0,50,0,0]);
});
test('clearing invalidates pending responses and all navigation history',async()=>{
 const f=fixture();await respond(f,true,50,true);const work=f.c.reviewMetadata(false);
 f.c.clearMetadataReview();f.pending.at(-1).resolve(page(100,true));await work;
 assert.equal(f.nodes.metadataReviewRows.children.length,0);
 assert.equal(f.nodes.metadataReviewPrevious.disabled,true);assert.equal(f.nodes.metadataReviewNext.disabled,true);
 assert.equal(vm.runInContext('metadataReviewHistory.length',f.c),0);
});
test('history is bounded and stores cursors only',async()=>{
 const f=fixture();await respond(f,true,50,true);
 for(let i=1;i<=120;i++)await respond(f,false,(i+1)*50,true);
 assert.equal(vm.runInContext('metadataReviewHistory.length',f.c),100);
 assert.equal(vm.runInContext('metadataReviewHistory.every(Number.isFinite)',f.c),true);
});
