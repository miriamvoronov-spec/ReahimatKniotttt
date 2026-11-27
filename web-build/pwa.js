(function(){
 var standalone=function(){return matchMedia('(display-mode: standalone)').matches||matchMedia('(display-mode: window-controls-overlay)').matches||navigator.standalone===true};
 function mark(){document.body.classList.toggle('pwa-in',standalone())}
 mark();matchMedia('(display-mode: standalone)').addEventListener&&matchMedia('(display-mode: standalone)').addEventListener('change',mark);

 var deferred=null;
 addEventListener('beforeinstallprompt',function(e){e.preventDefault();deferred=e});
 addEventListener('appinstalled',function(){deferred=null;document.body.classList.add('pwa-in')});

 var ua=navigator.userAgent,ios=/iPad|iPhone|iPod/.test(ua)||(navigator.platform==='MacIntel'&&navigator.maxTouchPoints>1);
 var help=document.getElementById('pwahelp'),inner=document.getElementById('pwahelpin');
 function showHelp(){
  var steps;
  if(ios)steps=/CriOS|FxiOS|EdgiOS/.test(ua)
   ?['לוחצים על כפתור השיתוף <b>⬆︎</b> שליד שורת הכתובת.','בוחרים <b>הוספה למסך הבית</b> ואז <b>הוסף</b>.']
   :['לוחצים על כפתור השיתוף <b>⬆︎</b> בתחתית Safari.','גוללים ובוחרים <b>הוספה למסך הבית</b>.','לוחצים <b>הוסף</b>. האייקון של רשימרקט יופיע במסך הבית.'];
  else if(/Android/.test(ua))steps=['פותחים את התפריט <b>⋮</b> של Chrome.','בוחרים <b>התקנת אפליקציה</b> (או <b>הוספה למסך הבית</b>).'];
  else steps=['בשורת הכתובת של Chrome או Edge לוחצים על סמל ההתקנה <b>⊕</b>.','או מהתפריט <b>⋮</b> בוחרים <b>התקנת רשימרקט</b>.'];
  inner.innerHTML='<h3>📲 התקנת רשימרקט</h3><ol>'+steps.map(function(s){return'<li>'+s+'</li>'}).join('')+'</ol><button type="button" id="pwaok">הבנתי</button>';
  help.classList.add('open')}
 help.addEventListener('click',function(e){if(e.target===help||e.target.id==='pwaok')help.classList.remove('open')});
 document.addEventListener('click',function(e){var b=e.target.closest&&e.target.closest('[data-act="install"]');if(!b)return;
  e.stopPropagation();e.preventDefault();
  if(deferred){var d=deferred;deferred=null;d.prompt();d.userChoice.then(function(c){if(c&&c.outcome==='accepted')document.body.classList.add('pwa-in')})}
  else showHelp()},true);

 /* Updates: the service worker always fetches the newest version when online.
    When a new worker takes over, reload right away if the app was just opened,
    otherwise offer a one-tap refresh so nothing being typed is lost. */
 if(!('serviceWorker' in navigator))return;
 var opened=Date.now(),bar=document.getElementById('pwabar'),hadCtrl=!!navigator.serviceWorker.controller,reloading=false;
 function reload(){if(reloading)return;reloading=true;location.reload()}
 document.getElementById('pwareload').addEventListener('click',reload);
 navigator.serviceWorker.addEventListener('controllerchange',function(){
  if(!hadCtrl){hadCtrl=true;return}
  var sheetOpen=document.querySelector('.sw.open'),typing=document.activeElement&&/INPUT|TEXTAREA/.test(document.activeElement.tagName)&&document.activeElement.value;
  if(Date.now()-opened<10000&&!sheetOpen&&!typing)reload();else bar.classList.add('show')});
 navigator.serviceWorker.register('sw.js',{updateViaCache:'none'}).then(function(reg){
  function check(){reg.update().catch(function(){})}
  document.addEventListener('visibilitychange',function(){if(!document.hidden)check()});
  setInterval(check,30*60*1000)}).catch(function(){});
})();
