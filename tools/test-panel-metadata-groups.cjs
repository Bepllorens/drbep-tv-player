const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('async function loadMetadataGroups'),html.indexOf("$('metadataSource').onchange"));
function fixture(){
 const nodes={},pending=[],labels={};
 const c={metadataGroupRequest:0,metadataGroupReady:true,metadataRunning:false,
 $:id=>nodes[id]||(nodes[id]={value:'',disabled:false}),text:(id,v)=>labels[id]=v,message:()=>{},
 options:(id,rows,value)=>{c.$(id).rows=rows;c.$(id).value=value},
 api:()=>new Promise(resolve=>pending.push(resolve))};
 vm.createContext(c);vm.runInContext(code,c);
 c.$('metadataSource').value='a';c.$('metadataKind').value='movie';
 return {c,nodes,pending,labels};
}
test('late groups cannot replace newer source groups',async()=>{
 const {c,pending,nodes}=fixture();const first=c.loadMetadataGroups();
 c.$('metadataSource').value='b';const second=c.loadMetadataGroups();
 pending[1]({groups:['B']});await second;pending[0]({groups:['A']});await first;
 assert.deepEqual(Array.from(nodes.metadataGroup.rows,r=>r[0]),['','B']);
 assert.equal(nodes.metadataStart.disabled,false);
});
test('programmatic source change rejects old scope even before replacement request',async()=>{
 const {c,pending,nodes}=fixture();const work=c.loadMetadataGroups();
 c.$('metadataSource').value='b';pending[0]({groups:['A']});await work;
 assert.deepEqual(Array.from(nodes.metadataGroup.rows,r=>r[0]),['']);
 assert.equal(nodes.metadataStart.disabled,true);
});
test('no selected source enables all-groups without network request',async()=>{
 const {c,pending,nodes}=fixture();c.$('metadataSource').value='';await c.loadMetadataGroups();
 assert.equal(pending.length,0);assert.equal(nodes.metadataStart.disabled,false);assert.equal(nodes.metadataGroup.disabled,true);
});
