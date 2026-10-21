package local.zft;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import android.util.Log;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.zip.*;
public final class ChatHome implements Application.ActivityLifecycleCallbacks {
 private static final String MAIN="com.zhongfutong.main.MainTabActivity",FORUM="com.zhongfutong.main.WebviewForumActivity",CIRCLE="价值投研小圈",BAR="local.zft.navigation";
 private static ChatHome instance;
 private Activity active;
 private WeakReference<Activity> main=new WeakReference<>(null);
 private Intent circleIntent;
 private String forumId="";
 private int selected;
 private final Set<Activity> entered=Collections.newSetFromMap(new WeakHashMap<Activity,Boolean>());
    // The original certificate is required to decode the unchanged business payload.
    public static void boot(String cache, String pkg, String apk) throws Exception {
        File carrier=new File(cache,"original-signer.apk");
        try(ZipFile z=new ZipFile(apk); InputStream in=z.getInputStream(z.getEntry("assets/local-zft-signer.apk")); OutputStream out=new FileOutputStream(carrier)) {
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
        }
        Class.forName("com.yqsh.protector.shell.JniBridge").getMethod("initApp",String.class,String.class,String.class).invoke(null,cache,pkg,carrier.getPath());
    }
    public static void install(Application app) {
        if(instance==null){instance=new ChatHome();app.registerActivityLifecycleCallbacks(instance);Log.i("ZftClean","installed");}
    }

 private static View id(Activity a,String name){int id=a.getResources().getIdentifier(name,"id",a.getPackageName());return id==0?null:a.findViewById(id);}
 private static boolean mainTab(Activity a){return MAIN.equals(a.getClass().getName());}
 private static boolean forum(Activity a){return FORUM.equals(a.getClass().getName());}
 private static void selectTab(Activity a,int tab){try{a.getClass().getMethod("T5",int.class,boolean.class).invoke(a,tab,false);}catch(Exception e){Log.e("ZftClean","tab selection",e);}}
 public static void navigate(Activity a,int tab){
  ChatHome h=instance;if(h==null)return;h.selected=tab;
  if(tab==0){h.openChat(a);return;}
  if(tab==4){if(!(a instanceof AiActivity))startTab(a,new Intent(a,AiActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));return;}
  if(tab==3){if(!(a instanceof StockActivity))startTab(a,new Intent(a,StockActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));return;}
  if(tab==1&&h.circleIntent!=null){startTab(a,new Intent(h.circleIntent).putExtra("local.zft.manualCircle",true).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));return;}
  Activity m=h.main.get();if(m!=null&&!m.isDestroyed()){if(tab==1)h.entered.remove(m);else h.entered.add(m);selectTab(m,tab==2?4:0);h.addBar(m,tab);}
  startTab(a,new Intent().setClassName(a,MAIN).putExtra("tabId",tab==2?4:0).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
 }
 private static void startTab(Activity a,Intent intent){a.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),ActivityOptions.makeCustomAnimation(a,0,0).toBundle());a.overridePendingTransition(0,0);}
 static String cachedForum(Activity a){ChatHome h=instance;if(h==null)return "";String key=h.account(a);if(key.equals("circle.")||key.equals("circle.unknown")||key.equals("circle.null"))return "";String id=a.getSharedPreferences("local.zft.navigation",0).getString(key,"");return id.matches("[1-9][0-9]*")?id:"";}
 static void rememberForum(Activity a,String id){ChatHome h=instance;if(h==null||!id.matches("[1-9][0-9]*"))return;String key=h.account(a);if(key.equals("circle.")||key.equals("circle.unknown")||key.equals("circle.null"))return;h.forumId=id;a.getSharedPreferences("local.zft.navigation",0).edit().putString(key,id).apply();}
 private void openChat(Activity a){selected=0;forumId=cachedForum(a);if(a instanceof NativeChat){((NativeChat)a).ensureRoom();return;}startTab(a,new Intent(a,NativeChat.class).putExtra("forum_id",forumId).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));}
 public static void openOriginalChat(Activity a,String forum){try{String url=(String)Class.forName("pc6").getMethod("d",String.class).invoke(null,"https://mapp.zx017.net/chattingRoom/"+forum);a.startActivity(new Intent().setClassName(a,FORUM).putExtra("linkUrl",url).putExtra("pageType","chat").putExtra("forum_id",forum).putExtra("local.zft.original",true));}catch(Exception e){Toast.makeText(a,"暂时无法打开原版聊天室",Toast.LENGTH_SHORT).show();}}
 private void hideTabs(Activity a){View old=id(a,"ll_01");if(old!=null&&old.getVisibility()!=View.GONE)old.setVisibility(View.GONE);}
 private boolean openOnlyCircle(Activity a,View v){
  if(v instanceof TextView&&CIRCLE.contentEquals(((TextView)v).getText())&&v.isShown()){View child=v;ViewParent p=v.getParent();while(p instanceof View){if(p instanceof AdapterView){AdapterView<?> list=(AdapterView<?>)p;int pos=list.getPositionForView(v);if(pos>=0){entered.add(a);boolean opened=list.performItemClick(child,pos,list.getItemIdAtPosition(pos));if(!opened)entered.remove(a);return opened;}}child=(View)p;p=p.getParent();}}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(openOnlyCircle(a,g.getChildAt(i)))return true;}return false;
 }
 private static WebView web(View v){if(v instanceof WebView)return (WebView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView w=web(g.getChildAt(i));if(w!=null)return w;}}return null;}
 private void poll(Activity a,int attempt){
  if(active!=a||a.isFinishing()||a.isDestroyed()||attempt>=40)return;
  if(mainTab(a)){hideTabs(a);addBar(a,selected);polish(a.getWindow().getDecorView());View login=id(a,"tv_go_login");if(login instanceof TextView){((TextView)login).setTextColor(-1);login.setBackground(Ui.shape(a,Ui.ACCENT,16));}if(selected==0){openChat(a);return;}if(selected==1&&!entered.contains(a))openOnlyCircle(a,a.getWindow().getDecorView());}
  else if(forum(a)){
   if("forumMain".equals(a.getIntent().getStringExtra("pageType"))){circleIntent=new Intent(a.getIntent());Object raw=a.getIntent().getExtras()==null?null:a.getIntent().getExtras().get("forum_id");String candidate=raw==null?"":raw.toString();if(candidate.matches("[0-9]+")){forumId=candidate;rememberForum(a,candidate);}addBar(a,selected==0?0:1);if(selected==0&&!a.getIntent().getBooleanExtra("local.zft.manualCircle",false)&&!entered.contains(a)){entered.add(a);openChat(a);}}
   WebView w=web(a.getWindow().getDecorView());
   if(w!=null&&w.getUrl()!=null){android.net.Uri u=android.net.Uri.parse(w.getUrl());String path=u.getPath();if("mapp.zx017.net".equals(u.getHost())&&path!=null&&path.matches("/chattingRoom/[0-9]+/?")){String foundId=path.split("/")[2];if(a.getIntent().getBooleanExtra("local.zft.original",false))addBar(a,0);else{forumId=foundId;rememberForum(a,foundId);selected=0;openChat(a);a.finish();return;}}}
   if(w!=null)w.evaluateJavascript("(function(){if(location.hostname!=='mapp.zx017.net')return;var s=document.getElementById('local-zft-style');if(!s){s=document.createElement('style');s.id='local-zft-style';s.textContent='body{font-family:system-ui,sans-serif!important;background:#f5f6fa!important;color:#222c3d!important;-webkit-font-smoothing:antialiased}p{line-height:1.65!important}.circle_item{border-radius:14px!important;margin:10px 12px!important;padding:16px!important;background:white!important;box-shadow:none!important}button{border-radius:10px!important}';document.head.appendChild(s)}})()",null);
  }
  a.getWindow().getDecorView().postDelayed(()->poll(a,attempt+1),500);
 }
 private void polish(View v){if(v instanceof TextView){TextView t=(TextView)v;if(!(v instanceof EditText)){t.setIncludeFontPadding(false);if(t.getTextSize()/v.getResources().getDisplayMetrics().scaledDensity>=13)t.setLineSpacing(Ui.dp(v.getContext(),2),1);}}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)polish(g.getChildAt(i));}}
 private void addBar(Activity a,int tab){
  ViewGroup content=a.findViewById(android.R.id.content);if(content==null)return;View found=content.findViewWithTag(BAR);LinearLayout wrap;
  if(found==null){if(content.getChildCount()!=1)return;View original=content.getChildAt(0);content.removeView(original);wrap=Ui.column(a);wrap.setTag(BAR);wrap.setFitsSystemWindows(true);wrap.addView(original,new LinearLayout.LayoutParams(-1,0,1));content.addView(wrap,new ViewGroup.LayoutParams(-1,-1));}
  else wrap=(LinearLayout)found;
  View old=wrap.findViewWithTag("local.zft.tabs");if(old!=null&&old.findViewWithTag("local.zft.tab."+tab).isSelected())return;if(old!=null)wrap.removeView(old);View nav=Ui.navigation(a,tab,i->navigate(a,i));wrap.addView(nav);a.getWindow().setNavigationBarColor(-1);
 }
 private String account(Activity a){try{return "circle."+Class.forName("com.androidUtil.network.AndroidUtils").getMethod("r",Context.class,String.class,String.class).invoke(null,a,"LOGIN","LOGIN_ID");}catch(Exception e){return "circle.unknown";}}
 public void onActivityCreated(Activity a,Bundle b){if("com.zhongfutong.main.WelcomeView".equals(a.getClass().getName())){selected=0;}if(mainTab(a)){forumId=a.getSharedPreferences("local.zft.navigation",0).getString(account(a),"");main=new WeakReference<>(a);a.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(()->hideTabs(a));}}
 public void onActivityResumed(Activity a){active=a;if(a instanceof AiActivity)selected=4;else if(a instanceof StockActivity)selected=3;else if(a instanceof NativeChat)selected=0;if(mainTab(a)||forum(a))poll(a,0);}
 public void onActivityPaused(Activity a){if(active==a)active=null;}
 public void onActivityDestroyed(Activity a){entered.remove(a);}
 public void onActivityStarted(Activity a){} public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){}
}
