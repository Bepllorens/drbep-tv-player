
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('let taskOverviewImports='),html.indexOf('let metadataSelectionLoaded='));
function fixture(){const labels={},c={text:(k,v)=>labels[k]=v};vm.createContext(c);vm.runInContext(code,c);return {c,labels}}
test('absence of data is not reported as idle',()=>{
 const {c}=fixture();assert.match(c.taskOverviewText(null,null)[0],/no disponible/);assert.match(c.taskOverviewText(null,null)[1],/no disponible/);
});
test('import states include running queued errors completed and cancelled',()=>{
 const {c}=fixture();const text=c.taskOverviewText(['running','queued','retry','completed','failed','cancelled'].map(state=>({state})),null)[0];
 assert.match(text,/1 en curso/);assert.match(text,/2 en cola/);assert.match(text,/1 completadas/);assert.match(text,/1 con error/);assert.match(text,/1 canceladas/);
});
test('unmatched metadata is distinct from errors and pause',()=>{
 const {c}=fixture();const text=c.taskOverviewText([],{state:'paused',done:20,unmatched:3,error:1})[1];
 assert.match(text,/en pausa/);assert.match(text,/20 con ficha/);assert.match(text,/3 sin coincidencia/);assert.match(text,/1 errores/);
});
test('reset drops both session snapshots',()=>{
 const {c,labels}=fixture();vm.runInContext("taskOverviewImports=[];taskOverviewMetadata={state:'complete'};updateTaskOverview();",c);
 c.resetTaskOverview();assert.match(labels.taskImportsSummary,/no disponible/);assert.match(labels.taskMetadataSummary,/no disponible/);
});
