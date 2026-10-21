import {fetchSnapshot} from './original-api.mjs';
import { normalizeStock, calculateMA5 } from './market-api.mjs';
import {search} from './search.mjs';
export const key=s=>`${s.market}:${s.code}`;
export const riseColor='#f43838',fallColor='#09a875',flatColor='#7b8491';
export const quoteColor=change=>change>0?riseColor:change<0?fallColor:flatColor;
export const fmt=(v,n=2)=>Number.isFinite(v)?v.toFixed(n):'—';
export function watchlist(raw){const values=JSON.parse(raw);if(!Array.isArray(values)||values.length>50)throw Error('自选数据无效');const found=new Set();return values.map(normalizeStock).filter(s=>{const k=key(s);if(found.has(k))return false;found.add(k);return true;});}
export function tradingMinute(time){const m=String(time).match(/(?:T|\s)(\d{2}):(\d{2})/);if(!m)return null;const n=+m[1]*60 + +m[2];return n>=570&&n<=690?n-570:n>=780&&n<=900?n-660:null;}
export function retain(previous,next){if(next.quote.latest===null&&previous?.quote.latest!==null&&previous){return {...previous,stale:true,staleReason:'刷新失败，显示上次数据',error:next.error||'行情暂不可用'};}return next;}
const provider={original:'中富通',tencent:'腾讯',eastmoney:'东方财富',sina:'新浪'};
const $=id=>document.getElementById(id);
const el=(tag,text,cls)=>{const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;};
function quoted(s){const q=s.quote;return q.quotedAt?new Date(q.quotedAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false}):q.date?`${q.date} 日线收盘`:'报价时间未知';}
function source(s){return `分时：${provider[s.sources?.intraday]||'无'} · 日 K：${provider[s.sources?.daily]||'无'}`;}
function warning(s){return (s.staleReason||'').replace(/，暂停实时提醒/g,'')+(s.error?`${s.staleReason?'；':''}${s.error}`:'');}
let managing=false;let stocks=[],active=false,version=0,controller,timer,searchController,searchVersion=0,selected=null,chartMode='minute';const snapshots=new Map();
function save(next){const error=window.StockStorage.save(JSON.stringify(next));if(error)throw Error(error);stocks=next;render();}
function render(){
 $('count').textContent=`我的自选 · ${stocks.length}`;$('manage').textContent=managing?'完成':'管理';$('manage').disabled=!stocks.length&&!managing;const list=$('cards');list.replaceChildren();if(!stocks.length)list.append(el('p','暂无自选，输入名称、代码或拼音添加','empty'));
 for(const stock of stocks){
  const s=snapshots.get(key(stock)),q=s?.quote,tone=q?.change>0?'up':q?.change<0?'down':'';
  const row=el('section',undefined,'stock-row'),button=el('button',undefined,'stock-open');button.setAttribute('aria-label',`查看${stock.name}行情详情`);button.disabled=!s;button.onclick=()=>showDetail(key(stock));
  const identity=el('span',undefined,'stock-identity');identity.append(el('span',stock.name,'name'),el('span',`${stock.market==='SSE'?'沪':'深'} ${stock.code}${s?.stale?' · 延迟':''}`,'meta'));button.append(identity);
  const price=el('span',undefined,'stock-price');price.append(el('span',fmt(q?.latest),'price '+tone),el('span',q?.quotedAt?new Date(q.quotedAt).toLocaleTimeString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false,hour:'2-digit',minute:'2-digit'}):'—','quote-time'));button.append(price);
  const change=el('span',undefined,'stock-change');change.append(el('span',`${q?.changePercent>0?'+':''}${fmt(q?.changePercent)}${Number.isFinite(q?.changePercent)?'%':''}`,'percent '+tone),el('span',`${q?.change>0?'+':''}${fmt(q?.change)}`,'change-amount '+tone));button.append(change);row.append(button);
  if(managing){const remove=el('button','移除','remove');remove.setAttribute('aria-label',`移除${stock.name}`);remove.onclick=()=>{try{save(stocks.filter(s=>key(s)!==key(stock)));snapshots.delete(key(stock));}catch(e){$('status').textContent=e.message;}};row.append(remove);}list.append(row);
 }
}
export function intradayRange(values,previousClose){const valid=values.filter(v=>Number.isFinite(v)&&v>0);const low=Math.min(...valid),high=Math.max(...valid);const spread=Math.max(high-previousClose,previousClose-low,previousClose*.002);return {low:previousClose-spread,high:previousClose+spread};}
function draw(canvas,s,daily,info,selectedIndex){
 const points=daily?s.candles:s.intraday,detail=!!info,width=canvas.clientWidth||320,height=detail?300:150,ratio=window.devicePixelRatio||1;
 canvas.width=width*ratio;canvas.height=height*ratio;canvas.style.height=height+'px';const ctx=canvas.getContext('2d');ctx.scale(ratio,ratio);
 const left=46,right=width-(daily?12:48),top=18,bottom=detail?211:109,volumeTop=bottom+19,volumeBottom=height-20;
 ctx.font='10px sans-serif';if(!points.length){ctx.fillStyle=flatColor;ctx.fillText(daily?'日 K 数据暂不可用':'分时数据暂不可用',left,55);return;}
 const values=daily?points.flatMap(p=>[p.high,p.low]):points.flatMap(p=>[p.price,p.averagePrice]);
 const averages=[5,10,20].map(n=>points.map((_,i)=>calculateMA5(points.slice(0,i+1),n)));
 if(daily)averages.forEach(a=>values.push(...a));const valid=values.filter(Number.isFinite);let low=Math.min(...valid),high=Math.max(...valid);const prev=s.quote.previousClose;
 if(!daily&&Number.isFinite(prev)&&prev>0){({low,high}=intradayRange(valid,prev));}else{const pad=Math.max((high-low)*.08,high*.002,.01);low-=pad;high+=pad;}
 const y=v=>bottom-(v-low)/(high-low)*(bottom-top),step=(right-left)/Math.max(points.length,1);
 const x=(p,i)=>daily?left+step*(i+.5):left+(right-left)*(tradingMinute(p.time)??0)/240;
 for(let i=0;i<=4;i++){const yy=top+(bottom-top)*i/4,value=high-(high-low)*i/4;ctx.strokeStyle='#e6e8eb';ctx.lineWidth=.5;ctx.beginPath();ctx.moveTo(left,yy);ctx.lineTo(right,yy);ctx.stroke();ctx.fillStyle=daily?flatColor:quoteColor(value-prev);ctx.textAlign='left';ctx.fillText(fmt(value),0,yy+3);if(!daily&&prev>0){ctx.textAlign='right';ctx.fillText(fmt((value/prev-1)*100)+'%',width,yy+3);}}
 for(let i=0;i<=4;i++){const xx=left+(right-left)*i/4;ctx.strokeStyle='#eceef1';ctx.beginPath();ctx.moveTo(xx,top);ctx.lineTo(xx,bottom);ctx.stroke();}ctx.textAlign='left';
 function line(get,color){ctx.strokeStyle=color;ctx.lineWidth=1.2;ctx.beginPath();let started=false;points.forEach((p,i)=>{const v=get(p,i);if(!Number.isFinite(v)){started=false;return;}if(!started)ctx.moveTo(x(p,i),y(v));else ctx.lineTo(x(p,i),y(v));started=true;});ctx.stroke();}
 const barWidth=Math.max(1,Math.min(8,step*.68));
 if(daily){points.forEach((p,i)=>{const xx=x(p,i),rising=p.close>=p.open;ctx.strokeStyle=ctx.fillStyle=quoteColor(p.close-p.open);ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(xx,y(p.high));ctx.lineTo(xx,y(p.low));ctx.stroke();const yy=Math.min(y(p.open),y(p.close)),hh=Math.max(1,Math.abs(y(p.open)-y(p.close)));if(rising){ctx.fillStyle='#fff';ctx.fillRect(xx-barWidth/2,yy,barWidth,hh);ctx.strokeRect(xx-barWidth/2,yy,barWidth,hh);}else ctx.fillRect(xx-barWidth/2,yy,barWidth,hh);});['#c6a617','#49baf9','#ff75ce'].forEach((color,i)=>line((_,j)=>averages[i][j],color));}
 else {ctx.setLineDash([3,3]);ctx.strokeStyle='#aab1ba';ctx.beginPath();ctx.moveTo(left,y(prev));ctx.lineTo(right,y(prev));ctx.stroke();ctx.setLineDash([]);line(p=>p.price,'#3d7ce0');line(p=>p.averagePrice,'#c6a617');}
 const maxVolume=Math.max(...points.map(p=>Number.isFinite(p.volume)?p.volume:0),1);ctx.fillStyle=flatColor;ctx.fillText('成交量',left,volumeTop-4);
 points.forEach((p,i)=>{if(!Number.isFinite(p.volume))return;const diff=daily?p.close-p.open:p.price-(i?points[i-1].price:prev);ctx.fillStyle=quoteColor(diff);const h=p.volume/maxVolume*(volumeBottom-volumeTop);ctx.fillRect(x(p,i)-barWidth/2,volumeBottom-h,daily?barWidth:Math.max(1,(right-left)/240),h);});
 ctx.fillStyle=flatColor;ctx.textAlign='left';ctx.fillText(daily?points[0].time.slice(5,10):'09:30',left,height-4);ctx.textAlign='right';ctx.fillText(daily?points.at(-1).time.slice(5,10):'15:00',right,height-4);ctx.textAlign='center';ctx.fillText(daily?points[Math.floor(points.length/2)].time.slice(5,10):'11:30 / 13:00',(left+right)/2,height-4);ctx.textAlign='left';
 if(info){const at=selectedIndex??points.length-1,p=points[at];info.textContent=daily?`${p.time}  开 ${fmt(p.open)}  高 ${fmt(p.high)}  低 ${fmt(p.low)}  收 ${fmt(p.close)}`:`${p.time.slice(11)}  价格 ${fmt(p.price)}  均价 ${fmt(p.averagePrice)}  涨跌 ${prev>0?fmt((p.price/prev-1)*100):'—'}%`;
  if(selectedIndex!==undefined){ctx.strokeStyle='#808a96';ctx.setLineDash([3,3]);ctx.beginPath();ctx.moveTo(x(p,at),top);ctx.lineTo(x(p,at),volumeBottom);ctx.moveTo(left,y(daily?p.close:p.price));ctx.lineTo(right,y(daily?p.close:p.price));ctx.stroke();ctx.setLineDash([]);}
  const inspect=event=>{const wanted=event.clientX-canvas.getBoundingClientRect().left;let best=0;points.forEach((p,i)=>{if(Math.abs(x(p,i)-wanted)<Math.abs(x(points[best],best)-wanted))best=i;});draw(canvas,s,daily,info,best);};
  canvas.onpointerdown=event=>{canvas.setPointerCapture?.(event.pointerId);inspect(event);};canvas.onpointermove=event=>{if(event.buttons)inspect(event);};
 }
}
function showDetail(id){const snapshot=snapshots.get(id);if(!snapshot)return;selected=snapshot;chartMode='minute';renderDetail();$('detail').showModal();}
function renderDetail(){
 const s=selected;if(!s)return;const q=s.quote,tone=q.change>0?'up':q.change<0?'down':'';$('detail-title').textContent=s.symbol.name;$('detail-code').textContent=`${s.symbol.market==='SSE'?'沪':'深'} ${s.symbol.code}`;
 const quote=$('detail-quote');quote.replaceChildren();const headline=el('div',undefined,'quote-head');headline.append(el('strong',fmt(q.latest),'detail-price '+tone));const change=el('div',undefined,'quote-change '+tone);change.append(el('span',`${q.change>0?'+':''}${fmt(q.change)}`),el('span',`${q.changePercent>0?'+':''}${fmt(q.changePercent)}%`));headline.append(change);quote.append(headline,el('p',quoted(s),'meta'));
 const grid=el('div',undefined,'detail-grid');for(const [label,value] of [['今开',q.open],['最高',q.high],['最低',q.low],['昨收',q.previousClose]]){const item=el('div');item.append(el('span',label),el('strong',fmt(value),value>q.previousClose?'up':value<q.previousClose?'down':''));grid.append(item);}quote.append(grid);
 const area=$('detail-body');area.replaceChildren();const ma=el('div',undefined,'ma-legend');if(chartMode==='daily')[5,10,20].forEach((n,i)=>{const value=el('span',`MA${n} ${fmt(s['ma'+n])}`);value.className='ma'+n;ma.append(value);});else ma.append(el('span','价格','minute-price'),el('span','均价','ma5'));area.append(ma);
 const info=el('p','','chart-info'),canvas=el('canvas',undefined,'chart detail-chart'+(chartMode==='daily'?' daily':''));canvas.setAttribute('aria-label',`${s.symbol.name}${chartMode==='daily'?'日K':'分时'}图，触摸滑动查看数据`);area.append(info,canvas,el('p','触摸图表查看对应时间和价格','chart-hint'));
 const details=el('details',undefined,'data-notes');details.append(el('summary','行情来源与说明'),el('p',source(s),'meta'));if(s.fallbackReason)details.append(el('p','切换来源原因：'+s.fallbackReason,'meta'));if(warning(s))details.append(el('p',warning(s),'meta error'));details.append(el('p',chartMode==='daily'?'K线：涨红跌绿；MA5 黄、MA10 蓝、MA20 紫。不同来源复权口径可能不同，中富通接口未标明。':'分时：蓝色价格线、黄色均价线；红绿坐标表示相对昨收的涨跌。备用源的日内高低可能为分钟采样值。','legend'));area.append(details);
 $('minute').classList.toggle('selected',chartMode==='minute');$('daily').classList.toggle('selected',chartMode==='daily');requestAnimationFrame(()=>draw(canvas,s,chartMode==='daily',info));
}
async function refresh(){if(!active)return;clearTimeout(timer);controller?.abort();const own=++version;controller=new AbortController();const signal=controller.signal;$('refresh').disabled=true;$('status').textContent=stocks.length?'正在刷新…':'首次使用，请搜索添加股票';const queue=[...stocks];let index=0;
 try{await Promise.all(Array.from({length:Math.min(3,queue.length)},async()=>{while(index<queue.length&&!signal.aborted){const stock=queue[index++];try{const s=await fetchSnapshot(stock,{signal});if(own!==version||!active)return;if(stocks.some(v=>key(v)===key(stock))){snapshots.set(key(stock),retain(snapshots.get(key(stock)),s));render();if(selected&&key(selected.symbol)===key(stock)&&$('detail').open){selected=snapshots.get(key(stock));renderDetail();}}}catch(e){if(!signal.aborted)$('status').textContent=e.message;}}}));if(own===version&&active)$('status').textContent=stocks.length?'每15秒刷新 · 点股票查看行情详情':'首次使用，请搜索添加股票';}finally{if(own===version){$('refresh').disabled=false;if(active)timer=setTimeout(refresh,15000);}}}
window.stockActive=value=>{active=!!value;clearTimeout(timer);if(active)refresh();else{version++;controller?.abort();searchVersion++;searchController?.abort();$('refresh').disabled=false;}};
window.closeStockSearch=()=>{const open=$('search-panel').open;$('search-panel').close();$('search-panel').hidden=true;++searchVersion;searchController?.abort();$('results').replaceChildren();$('query').value='';$('query').blur();$('search-status').textContent='';return open;};
window.toggleStockSearch=()=>{if(!$('search-panel').hidden){window.closeStockSearch();return;}$('detail').close();$('search-panel').hidden=false;$('search-panel').showModal();$('query').focus();};
$('search').onsubmit=async event=>{event.preventDefault();searchController?.abort();searchController=new AbortController();const own=++searchVersion;$('results').replaceChildren();$('search-status').textContent='正在搜索…';try{const {results,source}=await search($('query').value,{signal:searchController.signal,limit:12});if(own!==searchVersion)return;$('search-status').textContent=results.length?`${source} · 点选添加到自选`:`${source} · 没有找到匹配的沪深 A 股`;for(const stock of results){const button=el('button',`${stock.name}  ${stock.code} · ${stock.market==='SSE'?'沪':'深'}　＋`,'result');button.type='button';button.disabled=stocks.some(s=>key(s)===key(stock));button.onclick=()=>{try{if(stocks.length>=50)throw Error('最多添加50只股票');if(stocks.some(s=>key(s)===key(stock)))return;save([...stocks,stock]);++searchVersion;searchController?.abort();$('results').replaceChildren();$('query').value='';$('query').blur();window.closeStockSearch();refresh();}catch(e){$('search-status').textContent=e.message;}};$('results').append(button);}}catch(e){if(own===searchVersion)$('search-status').textContent=e.message;}};
$('manage').onclick=()=>{managing=!managing;render();};$('refresh').onclick=refresh;$('close').onclick=()=>$('detail').close();$('minute').onclick=()=>{chartMode='minute';renderDetail();};$('daily').onclick=()=>{chartMode='daily';renderDetail();};
try{stocks=watchlist(window.StockStorage.read());}catch(e){$('status').textContent='读取自选失败，原记录未改动';}render();

$('search-close').onclick=()=>window.closeStockSearch();
$('search-panel').addEventListener('cancel',event=>{event.preventDefault();window.closeStockSearch();});
$('search-panel').addEventListener('click',event=>{if(event.target!==$('search-panel'))return;const r=$('search-panel').getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)window.closeStockSearch();});
