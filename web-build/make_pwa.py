#!/usr/bin/env python3
"""Builds web/index.html (the installable app) from web-build/artifact.html.

artifact.html is a copy of the live claude.ai artifact. To ship a new version:
copy the artifact's HTML over artifact.html, run this script, commit and push
to main. The Pages workflow publishes web/ and installed apps update by
themselves the next time they open.
"""
import pathlib, re, sys, time

root = pathlib.Path(__file__).resolve().parent.parent
src = (root / 'web-build' / 'artifact.html').read_text(encoding='utf-8')
out = src


def sub(old, new, count=1):
    global out
    n = out.count(old)
    if n != count:
        sys.exit(f'expected {count}x {old[:40]!r}, found {n}')
    out = out.replace(old, new)


HEAD = '''<link rel="manifest" href="manifest.webmanifest">
<meta name="theme-color" content="#fff8ee" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#1c1620" media="(prefers-color-scheme: dark)">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-title" content="רשימרקט">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
<meta name="application-name" content="רשימרקט">
<link rel="apple-touch-icon" href="icons/apple-touch-icon.png">
<link rel="icon" type="image/png" sizes="32x32" href="icons/favicon-32.png">
<title>רשימרקט</title>
<style>
.htile.inst{grid-column:1/-1;min-height:64px;flex-direction:row;gap:12px;background:var(--ink);border-color:var(--ink);color:var(--bg)}
.htile.inst small{color:inherit;opacity:.75}
body.pwa-in .htile.inst{display:none}
#pwabar{position:fixed;z-index:80;left:50%;top:calc(10px + env(safe-area-inset-top,0px));transform:translateX(-50%);display:none;gap:12px;align-items:center;background:var(--ink);color:var(--bg);padding:8px 10px 8px 16px;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,.25);font:500 15px 'Rubik',system-ui,sans-serif;direction:rtl;max-width:calc(100vw - 32px)}
#pwabar.show{display:flex}
#pwabar button{border:0;border-radius:12px;background:var(--sun);color:#2a1a10;font:800 15px 'Rubik',system-ui,sans-serif;height:38px;padding:0 14px}
#pwahelp{position:fixed;inset:0;z-index:90;background:rgba(20,10,5,.45);display:none;align-items:flex-end;justify-content:center;direction:rtl}
#pwahelp.open{display:flex}
#pwahelp>div{width:min(560px,100%);background:var(--card);color:var(--ink);border-radius:26px 26px 0 0;padding:20px 20px calc(28px + env(safe-area-inset-bottom,0px));font:400 16px/1.5 'Rubik',system-ui,sans-serif}
#pwahelp h3{margin:0 0 10px;font-size:22px;font-weight:800}
#pwahelp ol{margin:0 0 16px;padding-inline-start:22px}
#pwahelp li{margin:6px 0}
#pwahelp button{height:46px;padding:0 18px;border-radius:14px;border:0;background:var(--ink);color:var(--bg);font:700 16px 'Rubik',system-ui,sans-serif}
</style>
<script>
/* Web stand-ins for the artifact runtime: no cloud db (lists stay on this
   device), voice input through the browser, backups saved as a file. */
(function(){
 function webDict(){
  var SR=window.SpeechRecognition||window.webkitSpeechRecognition;if(!SR)return null;
  var h={p:[],f:[],s:[]},rec=null;
  function emit(k,v){h[k].forEach(function(fn){try{fn(v)}catch(e){}})}
  return{
   onPartial:function(fn){h.p.push(fn)},onFinal:function(fn){h.f.push(fn)},onState:function(fn){h.s.push(fn)},
   start:function(o){return new Promise(function(res,rej){
    try{rec=new SR()}catch(e){rej({code:'unavailable'});return}
    rec.lang='he-IL';rec.interimResults=true;rec.continuous=false;rec.maxAlternatives=1;
    var started=false;
    rec.onstart=function(){started=true;emit('s',{state:'listening'});res()};
    rec.onresult=function(e){var t='',fin=false;for(var i=e.resultIndex;i<e.results.length;i++){t+=e.results[i][0].transcript;if(e.results[i].isFinal)fin=true}emit(fin?'f':'p',{text:t})};
    rec.onerror=function(e){var c=e.error==='not-allowed'||e.error==='service-not-allowed'?'not_granted':e.error==='aborted'?'stopped':e.error==='no-speech'?'stopped':'unavailable';if(!started)rej({code:c})};
    rec.onend=function(){rec=null;emit('s',{state:'idle'})};
    try{rec.start()}catch(e){rej({code:'unavailable'})}
   })},
   stop:function(){try{rec&&rec.stop()}catch(e){}return Promise.resolve()}
  }}
 function webDL(){return{save:async function(o){
  var blob=new Blob([o.data],{type:'application/json'});
  var touch=matchMedia('(pointer: coarse)').matches;
  if(touch&&navigator.canShare&&window.File){var f=new File([blob],o.filename,{type:'application/json'});
   if(navigator.canShare({files:[f]})){try{await navigator.share({files:[f],title:o.filename});return}catch(e){if(e&&e.name==='AbortError')throw{code:'cancelled'}}}}
  var a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=o.filename;document.body.appendChild(a);a.click();
  setTimeout(function(){URL.revokeObjectURL(a.href);a.remove()},4000)}}}
 window.claude={use:async function(n){return n==='dictation'?webDict():n==='downloads'?webDL():null}};
})();
</script>
'''
sub('</head>', HEAD + '</head>')

INSTALL_TILE = ('סיימתי קנייה</button><button class="htile inst" data-act="install">'
                '<span class="e" style="font-size:30px">📲</span><span>התקנה כאפליקציה'
                '<small style="display:block">אייקון במסך הבית, עובד גם בלי אינטרנט</small></span></button></div>')
sub('סיימתי קנייה</button></div>', INSTALL_TILE)
sub('נשמר בדפדפן הזה', 'נשמר במכשיר הזה', 4)
sub('הרשימה נשמרת אוטומטית בענן, וכל שינוי בדף הזה מתעדכן בלי להתקין כלום. כל מי שפותח את הדף מקבל רשימה פרטית משלו.',
    'הרשימה נשמרת במכשיר הזה. כדאי לשמור מדי פעם גיבוי לקובץ, ואפשר לשחזר אותו גם במכשיר אחר. האפליקציה מתעדכנת לבד בכל שינוי.')

TAIL = (root / 'web-build' / 'pwa.js').read_text(encoding='utf-8')
sub('</body>', '<div id="pwabar" role="status"><span>יש גרסה חדשה</span><button id="pwareload">לעדכן</button></div>'
    '<div id="pwahelp"><div id="pwahelpin"></div></div>\n<script>\n' + TAIL + '\n</script>\n</body>')

(root / 'web' / 'index.html').write_text(out, encoding='utf-8')
sw = (root / 'web-build' / 'sw.js').read_text(encoding='utf-8').replace('__BUILD__', str(int(time.time())))
(root / 'web' / 'sw.js').write_text(sw, encoding='utf-8')
print('web/index.html', len(out), 'bytes')
