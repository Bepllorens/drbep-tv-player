const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/device-panel.html'),'utf8');
const code=html.slice(html.indexOf('async function loadSeriesSources()'),html.indexOf("$('seriesSource').onchange"));
function fixture(){
 const nodes={},pending=[],labels={},errors=[];
 const make=()=>({value:'',checked:false,hidden:true,disabled:false,children:[],append(...items){this.children.push(...items)},replaceChildren(...items){this.children=items}});
 const c={token:'session',currentView:'series',seriesRevision:0,episodeRevision:0,seriesOffset:0,episodeOffset:0,selectedSeries:'A',sources:[{id:'list',name:'List'}],sourceId:'list',
 $:id=>nodes[id]||(nodes[id]=make()),text:(id,value)=>labels[id]=value,options:(id,rows,value)=>c.$(id).value=value,
 message:e=>errors.push(e),panelButton:()=>make(),document:{createElement:make},
 manage:command=>new Promise((resolve,reject)=>pending.push({command,resolve,reject}))};
 vm.createContext(c);vm.runInContext(code,c);c.$('seriesSource').value='list';
 return {c,nodes,pending,labels,errors};
}
const episodes={seasons:[1],season:1,rows:[]};
test('pending episode detail cannot reopen after sources reset',async()=>{
 const f=fixture();const old=f.c.loadEpisodes(true),reset=f.c.loadSeriesSources();
 f.pending[0].resolve(episodes);await old;
 assert.equal(f.nodes.episodePanel.hidden,true);assert.equal(f.labels.episodeTitle,undefined);
 f.pending[1].resolve({rows:[]});await reset;
});
test('changed series ignores previous response even without a replacement query',async()=>{
 const f=fixture();const old=f.c.loadEpisodes(true);f.c.selectedSeries='B';
 f.pending[0].resolve(episodes);await old;assert.equal(f.labels.episodeTitle,undefined);
});
test('changed list rejects pending series catalogue',async()=>{
 const f=fixture();const old=f.c.loadSeries();f.c.$('seriesSource').value='other';
 f.pending[0].resolve({rows:[{title:'Old',episodes:1}]});await old;assert.equal(f.labels.seriesPage,undefined);
});
test('old session errors are quiet',async()=>{
 const f=fixture();const old=f.c.loadEpisodes(true);f.c.token='new';f.pending[0].reject(Error('old'));await old;
 assert.equal(f.errors.length,0);
});
test('valid current detail still renders seasons and pagination',async()=>{
 const f=fixture();const work=f.c.loadEpisodes(true);f.pending[0].resolve(episodes);await work;
 assert.equal(f.labels.episodeTitle,'A');assert.equal(f.nodes.episodePanel.hidden,false);
 assert.equal(f.nodes.episodeNext.disabled,true);assert.equal(f.labels.episodePage,'Página 1');
});
