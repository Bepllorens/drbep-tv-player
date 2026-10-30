const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),test=require('node:test');
const html=fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const source=html.slice(html.indexOf('function refreshScheduleFromForm('),html.indexOf("$('remove').onclick="));
function fixture(info={}){
 const nodes={configure:{}},sent=[];let editor;
 const c={token:'one',sourceId:'s',sources:[{id:'s',name:'Lista',origin:'url'}],$:id=>nodes[id],
  manage:async(action,data)=>{sent.push({action,data});return action==='source-info'?info:{}},
  lists:async()=>{},message:()=>{},openEditor:(...args)=>editor=args,Date,Number};
 vm.runInNewContext(source,c);return {c,nodes,sent,get editor(){return editor}};
}
test('legacy twelve-hour default remains unless custom enabled',()=>{
 const f=fixture();assert.equal(f.c.refreshScheduleFromForm({customSchedule:false}),'');
 assert.equal(f.c.refreshScheduleFromForm({customSchedule:true,startTime:'08:00',interval:'240',times:'4'}),'daily:480:240:4');
});
test('invalid or repeated daily slots rejected',()=>{
 const f=fixture();
 for(const v of [{startTime:'24:00',interval:30,times:1},{startTime:'08:00',interval:0,times:1},
 {startTime:'08:00',interval:30.5,times:1},{startTime:'08:00',interval:720,times:4}])
  assert.throws(()=>f.c.refreshScheduleFromForm({customSchedule:true,...v}));
});
test('form round-trips schedule and omits unchanged URLs',async()=>{
 const f=fixture({automatic:true,refresh_schedule:'daily:1320:180:3',refresh_zone:'Europe/Madrid'});
 await f.nodes.configure.onclick();const fields=f.editor[2];
 assert.equal(fields.find(x=>x.name==='startTime').value,'22:00');
 await f.editor[3]({name:'Lista',automatic:true,customSchedule:true,startTime:'22:00',interval:'180',times:'3',url:'',epg:''});
 const request=f.sent.find(x=>x.action==='configure').data;
 assert.equal(request.refresh_schedule,'daily:1320:180:3');assert.equal(request.automatic,true);
 assert.equal(request.url,undefined);assert.equal(request.epg,undefined);
});
test('manual mode saves automatic false and old-session form cannot submit',async()=>{
 const f=fixture({automatic:true});await f.nodes.configure.onclick();
 await f.editor[3]({name:'Lista',automatic:false,customSchedule:false});
 assert.equal(f.sent.find(x=>x.action==='configure').data.automatic,false);
 f.c.token='two';await assert.rejects(f.editor[3]({name:'Lista',automatic:true,customSchedule:false}));
});
