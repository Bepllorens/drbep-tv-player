
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('let jobsRequest=0;'),html.indexOf('\n}',html.indexOf('async function jobs(){'))+2);
class Node {
 constructor(){this.children=[];this.textContent='';this.className='';this.hidden=false;this.moves=0}
 append(...nodes){for(const n of nodes)this.insertBefore(n,null)}
 insertBefore(n,before){n.remove();const at=before?this.children.indexOf(before):this.children.length;this.children.splice(at,0,n);n.parent=this;this.moves++}
 remove(){if(this.parent){this.parent.children.splice(this.parent.children.indexOf(this),1);this.parent=null}}
 querySelector(){return this.children.find(n=>n.className==='empty')}
 replaceChildren(){for(const n of [...this.children])n.remove()}
}
function fixture(){
 const root=new Node(),texts={},calls=[];
 const c={document:{createElement:()=>new Node()},$:()=>root,text:(k,v)=>texts[k]=v,
 activeStates:['queued','running','retry'],stateNames:{running:'Importando',done:'Completada'},
 currentView:'overview',loadMetadataIndex:async()=>{},manage:async()=>({jobs:[]}),
 openEditor:(...args)=>calls.push(args)};
 c.updateTaskOverview=()=>{};vm.createContext(c);vm.runInContext(code,c);return {c,root,texts,calls};
}
const row=(id='one',count=1)=>({id,count,state:'running',name:'Lista',phase:'Leyendo'});
test('unchanged polls retain row and cancel button without DOM moves',()=>{
 const {c,root}=fixture();c.renderJobs([row()]);const first=root.children[0],button=first.children[1],moves=root.moves;
 c.renderJobs([row()]);assert.equal(root.children[0],first);assert.equal(first.children[1],button);assert.equal(root.moves,moves);
});
test('progress and completed state update same row',()=>{
 const {c,root}=fixture();c.renderJobs([row()]);const first=root.children[0];
 c.renderJobs([{...row('one',24),state:'done'}]);
 assert.equal(root.children[0],first);assert.match(first.children[0].children[2].textContent,/24/);assert.equal(first.children[1].hidden,true);
});
test('remove reorder and empty state do not keep obsolete jobs',()=>{
 const {c,root}=fixture();c.renderJobs([row('a'),row('b')]);const b=root.children[1];
 c.renderJobs([row('b')]);assert.equal(root.children.length,1);assert.equal(root.children[0],b);
 c.renderJobs([]);const empty=root.children[0];c.renderJobs([]);assert.equal(root.children[0],empty);
 c.renderJobs([row('a')]);assert.equal(root.children.length,1);assert.notEqual(root.children[0],empty);
});
test('titles remain text and cancel uses job identity',async()=>{
 const {c,root,calls}=fixture();c.renderJobs([{...row('abc'),name:'<img onerror=alert(1)>'}]);
 assert.equal(root.children[0].children[0].children[1].textContent,'<img onerror=alert(1)>');
 root.children[0].children[1].onclick();
 let id;c.manage=async(action,args)=>{if(action==='cancel-job')id=args.job;return {jobs:[]}};
 await calls[0][3]();assert.equal(id,'abc');
});
test('late poll cannot overwrite newer result',async()=>{
 const {c,root}=fixture();const pending=[];c.manage=()=>new Promise(resolve=>pending.push(resolve));
 const old=c.jobs(),latest=c.jobs();
 pending[1]({jobs:[row('new')]});await latest;
 const newer=root.children[0];pending[0]({jobs:[row('old')]});await old;
 assert.equal(root.children[0],newer);
});
test('disconnect generation invalidates in-flight poll',async()=>{
 const {c,root}=fixture();let resolve;c.manage=()=>new Promise(r=>resolve=r);
 const pending=c.jobs();vm.runInContext('jobsRequest++;jobNodes.clear();',c);
 resolve({jobs:[row()]});await pending;assert.equal(root.children.length,0);
});

test('metadata status failure does not block import status',async()=>{
 const {c,root}=fixture();c.currentView='tasks';c.message=()=>{};
 c.loadMetadataIndex=async()=>{throw Error('synthetic failure')};
 c.manage=async()=>({jobs:[row()]});await c.jobs();
 assert.equal(root.children.length,1);assert.equal(root.children[0].children[0].children[1].textContent,'Lista');
});
