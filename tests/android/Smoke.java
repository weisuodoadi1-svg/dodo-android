package local.zfttests;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;
public final class Smoke extends Instrumentation {
 private Class<?> home,chat,modelClass;private Activity nativeActivity;
 private static void require(boolean yes,String s){if(!yes)throw new AssertionError(s);}
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle out=new Bundle();try{
  ClassLoader loader=getTargetContext().getClassLoader();home=loader.loadClass("local.zft.ChatHome");chat=loader.loadClass("local.zft.NativeChat");modelClass=loader.loadClass("local.zft.ChatModel");
  Class<?> socket=loader.loadClass("local.zft.deps.websocket.drafts.Draft_6455");require(socket.newInstance()!=null,"isolated socket dependency initializes alongside original app");
  Method time=modelClass.getMethod("messageTime",String.class);require("10:55".equals(time.invoke(null,"2026-10-21 10:55:55")),"time without date or seconds");require("09:30".equals(time.invoke(null,"09:30")),"short time preserved");require("".equals(time.invoke(null,"bad")),"invalid time hidden");
  JSONObject event=new JSONObject("{\"Kind\":\"chat_room_send_message\",\"Data\":{\"Content\":\"收到[笑哭]【545756】\"}}");modelClass.getMethod("cleanRealtime",JSONObject.class).invoke(null,event);require(event.getJSONObject("Data").getString("Content").equals("收到[笑哭]"),"original realtime suffix cleanup preserves emoji");
  Object model=modelClass.newInstance();modelClass.getField("user").set(model,"7");Method permissions=modelClass.getMethod("permissions",JSONObject.class),merge=modelClass.getMethod("merge",JSONArray.class),visible=modelClass.getMethod("visibleMessages");
  permissions.invoke(model,new JSONObject("{\"chat_room_style\":0,\"admins\":[]}"));
  merge.invoke(model,new JSONArray("[{\"Id\":3,\"Content\":\"new\"},{\"Id\":1},{\"Id\":2},{\"Id\":3,\"Content\":\"updated\"}]"));
  List<?> rows=(List<?>)visible.invoke(model);require(rows.size()==3,"dedupe");require(((JSONObject)rows.get(0)).getLong("Id")==1,"chronological sort");require(((JSONObject)rows.get(2)).getString("Content").equals("updated"),"update duplicate");
  merge.invoke(model,new JSONArray("[{\"Id\":2,\"State\":2},{\"Id\":4,\"UserId\":8,\"OriginId\":9,\"OriginData\":[{\"State\":1,\"UserId\":9}]}]"));require(((List<?>)visible.invoke(model)).size()==2,"deleted/private reply hidden");
  permissions.invoke(model,new JSONObject("{\"chat_room_style\":1,\"admins\":[]}"));require(!(Boolean)modelClass.getMethod("acceptRealtime",JSONObject.class).invoke(model,new JSONObject("{\"Id\":5,\"State\":1}")),"pending live event hidden");merge.invoke(model,new JSONArray("[{\"Id\":5,\"UserId\":7,\"State\":1}]"));require(((List<?>)visible.invoke(model)).size()==3,"own pending history retained");
  String key="0123456789abcdef0123456789abcdef",plain="{\"Id\":8,\"Kind\":\"chat_room_send_message\"}";javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"),"AES"),new javax.crypto.spec.IvParameterSpec(key.substring(16).getBytes("UTF-8")));String encoded=key+android.util.Base64.encodeToString(cipher.doFinal(plain.getBytes("UTF-8")),android.util.Base64.NO_WRAP);require(plain.equals(modelClass.getMethod("decode",String.class).invoke(null,encoded)),"socket decrypt");boolean rejected=false;try{modelClass.getMethod("decode",String.class).invoke(null,"invalid");}catch(InvocationTargetException e){rejected=true;}require(rejected,"invalid socket frame rejected");
  ActivityMonitor coldChat=addMonitor("local.zft.NativeChat",null,false);
  Activity main=startActivitySync(new Intent().setClassName(getTargetContext(),"com.zhongfutong.main.MainTabActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Thread.sleep(600);require(waitForMonitorWithTimeout(coldChat,3000)!=null,"cold main routes directly to native chat");removeMonitor(coldChat);
  runOnMainSync(()->{View old=main.findViewById(main.getResources().getIdentifier("ll_01","id",main.getPackageName()));require(old.getVisibility()==View.GONE,"old navigation hidden");for(int i=0;i<5;i++)require(main.getWindow().getDecorView().findViewWithTag("local.zft.tab."+i)!=null,"five main tabs");require(main.getWindow().getDecorView().findViewWithTag("local.zft.tab.0").isSelected(),"startup selects chat, never circle");main.getWindow().getDecorView().findViewWithTag("local.zft.tab.2").performClick();});waitForIdleSync();runOnMainSync(()->{try{require(main.getClass().getField("x4").getInt(main)==4,"mine selected");}catch(Exception e){throw new RuntimeException(e);}});
  capture("mine-preview.png");
  Activity circle=startActivitySync(new Intent().setClassName(getTargetContext(),"com.zhongfutong.main.WebviewForumActivity").putExtra("pageType","forumMain").putExtra("linkUrl","about:blank").putExtra("forum_id","1").putExtra("local.zft.manualCircle",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Thread.sleep(700);
  runOnMainSync(()->{for(int i=0;i<5;i++)require(circle.getWindow().getDecorView().findViewWithTag("local.zft.tab."+i)!=null,"circle five tabs");require(circle.getWindow().getDecorView().findViewWithTag("local.zft.tab.1").isSelected(),"circle tab selected");circle.getWindow().getDecorView().findViewWithTag("local.zft.tab.2").performClick();});waitForIdleSync();

  nativeActivity=startActivitySync(new Intent().setClassName(getTargetContext(),"local.zft.NativeChat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  runOnMainSync(()->{try{Field f=chat.getDeclaredField("fixture");f.setAccessible(true);f.setBoolean(nativeActivity,true);Method fixture=chat.getDeclaredMethod("fixture");fixture.setAccessible(true);fixture.invoke(nativeActivity);nativeActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);for(int i=0;i<5;i++)require(nativeActivity.getWindow().getDecorView().findViewWithTag("local.zft.tab."+i)!=null,"native tabs");require(!hasWeb(nativeActivity.getWindow().getDecorView()),"chat must not contain WebView");}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
  // Exercise the actual native event path without contacting the service.
  runOnMainSync(()->{try{Field id=chat.getDeclaredField("forum");id.setAccessible(true);id.set(nativeActivity,"1");Field gen=chat.getDeclaredField("generation");gen.setAccessible(true);Method receive=chat.getDeclaredMethod("receive",String.class,int.class);receive.setAccessible(true);receive.invoke(nativeActivity,"{\"Id\":1,\"Kind\":\"chat_room_send_message\",\"Data\":{\"Id\":3,\"UserId\":8,\"Content\":\"event\"}}",gen.getInt(nativeActivity));receive.invoke(nativeActivity,"{\"Id\":2,\"Kind\":\"chat_room_send_message\",\"Data\":{\"Id\":4}}",gen.getInt(nativeActivity));}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
  runOnMainSync(()->{try{Field data=chat.getDeclaredField("rows");data.setAccessible(true);require(((List<?>)data.get(nativeActivity)).size()==3,"matching-room event accepted, foreign-room event ignored");Field gen=chat.getDeclaredField("generation");gen.setAccessible(true);Method receive=chat.getDeclaredMethod("receive",String.class,int.class);receive.setAccessible(true);receive.invoke(nativeActivity,"{\"Id\":1,\"Kind\":\"chat_room_del_message\",\"Data\":3}",gen.getInt(nativeActivity));}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
  runOnMainSync(()->{try{Field data=chat.getDeclaredField("rows");data.setAccessible(true);require(((List<?>)data.get(nativeActivity)).size()==2,"native withdrawal event");}catch(Exception e){throw new RuntimeException(e);}});
  verifyChatRevision();
  verifyStyles();
  verifyEmoji();
  capture("native-chat-preview.png");
  runOnMainSync(()->{try{((ListView)field("list")).setSelection(2);}catch(Exception e){throw new RuntimeException(e);}});
  capture("native-chat-media-preview.png");
  runOnMainSync(()->nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.emoji").performClick());
  capture("native-emoji-preview.png");
  sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
  // Historical CSS reference is retained separately; current preview follows approved design.
  out.putString("result","PASS: five tabs on main/circle/native chat + mine navigation; native chat without WebView; ordering/dedupe/delete/privacy filters; AES socket decode + malformed-frame rejection; native realtime event routing and withdrawal. avatars/left-right layout; filters and stale-request background/resume regression. modern mint styling/16sp text/quote/image/top filters/startup chat selection; 111 offline emoji assets, token rendering and draft insertion. No live messages sent.");finish(-1,out);
 }catch(Throwable t){out.putString("result","FAIL: "+t);finish(0,out);}}
 private void verifyEmoji()throws Exception {
  runOnMainSync(()->{try{
   Object emoji=field("emoji");Class<?> type=emoji.getClass();Method render=type.getDeclaredMethod("render",CharSequence.class,int.class);render.setAccessible(true);
   String raw="[笑哭][dog捂脸]文字[不存在的表情]";android.text.Spanned text=(android.text.Spanned)render.invoke(emoji,raw,30);
   require(raw.equals(text.toString()),"emoji transport text preserved");require(text.getSpans(0,text.length(),android.text.style.ImageSpan.class).length==2,"known emoji rendered, unknown preserved");
   Field files=type.getDeclaredField("files");files.setAccessible(true);Map<String,String> all=(Map<String,String>)files.get(emoji);require(all.size()==111,"complete original emoji pack");for(String name:all.keySet()){android.text.Spanned one=(android.text.Spanned)render.invoke(emoji,"["+name+"]",30);require(one.getSpans(0,one.length(),android.text.style.ImageSpan.class).length==1,"offline asset: "+name);}
   EditText input=(EditText)field("input");input.setText("前后");input.setSelection(1);Method insert=type.getDeclaredMethod("insert",EditText.class,String.class);insert.setAccessible(true);insert.invoke(emoji,input,"笑哭");require(input.getText().toString().equals("前[笑哭]后"),"emoji inserts at cursor");input.setSelection(1,5);insert.invoke(emoji,input,"吃瓜");require(input.getText().toString().equals("前[吃瓜]后"),"emoji replaces selection");input.setEnabled(false);insert.invoke(emoji,input,"笑哭");require(input.getText().toString().equals("前[吃瓜]后"),"muted input unchanged");input.setEnabled(true);require(!(Boolean)field("sending"),"picker does not send");
  }catch(Exception e){throw new RuntimeException(e);}});
 }
 private Object field(String name)throws Exception{Field f=chat.getDeclaredField(name);f.setAccessible(true);return f.get(nativeActivity);}
 private void set(String name,Object value)throws Exception{Field f=chat.getDeclaredField(name);f.setAccessible(true);f.set(nativeActivity,value);}
 private void verifyChatRevision()throws Exception {
  runOnMainSync(()->{try{
   BaseAdapter adapter=(BaseAdapter)field("adapter");ListView list=(ListView)field("list");
   View owner=adapter.getView(0,null,list),member=adapter.getView(1,null,list);
   require("local.zft.owner-left".equals(owner.getTag()),"owner on left");
   require("local.zft.member-right".equals(member.getTag()),"other member on right");
   require(owner.findViewWithTag("local.zft.avatar")!=null&&member.findViewWithTag("local.zft.avatar")!=null,"avatars restored");
   nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.1").performClick();
   Object model=field("model");modelClass.getMethod("merge",JSONArray.class).invoke(model,new JSONArray("[{\"Id\":10,\"UserId\":8},{\"Id\":11,\"UserId\":9}]"));
   Method refresh=chat.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(nativeActivity);
   require(((List<?>)field("rows")).size()==1,"leader filter excludes regular member");
   nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.2").performClick();
   require(((Integer)field("filterMode"))==2&&((List<?>)field("rows")).isEmpty(),"my messages has separate result set");
   require((Boolean)modelClass.getMethod("relatesToMe",JSONObject.class).invoke(model,new JSONObject("{\"UserId\":9,\"OriginData\":[{\"UserId\":7}]}")),"replies to me included");
   require(!(Boolean)modelClass.getMethod("relatesToMe",JSONObject.class).invoke(model,new JSONObject("{\"UserId\":9}")),"unrelated member excluded");
  }catch(Exception e){throw new RuntimeException(e);}});
  java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1),started=new java.util.concurrent.CountDownLatch(1),completed=new java.util.concurrent.CountDownLatch(1);
  java.util.concurrent.atomic.AtomicBoolean staleCleanup=new java.util.concurrent.atomic.AtomicBoolean(false);
  Class<?> work=chat.getClassLoader().loadClass("local.zft.NativeChat$Work"),done=chat.getClassLoader().loadClass("local.zft.NativeChat$Done");
  Object slow=Proxy.newProxyInstance(work.getClassLoader(),new Class[]{work},(proxy,m,args)->{started.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS);completed.countDown();return new JSONObject();});
  Object callback=Proxy.newProxyInstance(done.getClassLoader(),new Class[]{done},(proxy,m,args)->{throw new AssertionError("stale request applied after resume");});
  runOnMainSync(()->{try{set("loading",true);Method task=chat.getDeclaredMethod("task",work,done,Runnable.class);task.setAccessible(true);task.invoke(nativeActivity,slow,callback,(Runnable)()->{staleCleanup.set(true);});}catch(Exception e){throw new RuntimeException(e);}});
  require(started.await(3,java.util.concurrent.TimeUnit.SECONDS),"slow request started");
  runOnMainSync(()->{try{callActivityOnPause(nativeActivity);require(!(Boolean)field("loading"),"background clears old loading gate");callActivityOnResume(nativeActivity);require(((Integer)field("filterMode"))==2,"resume retains current filter");}catch(Exception e){throw new RuntimeException(e);}});
  release.countDown();require(completed.await(3,java.util.concurrent.TimeUnit.SECONDS),"old request finished");Thread.sleep(150);waitForIdleSync();require(!staleCleanup.get(),"stale completion cannot change new load state");
  runOnMainSync(()->{try{nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.0").performClick();Method fixture=chat.getDeclaredMethod("fixture");fixture.setAccessible(true);fixture.invoke(nativeActivity);}catch(Exception e){throw new RuntimeException(e);}});
 }
 private void verifyStyles()throws Exception {
  runOnMainSync(()->{try{
   Object model=field("model");java.util.Map<Long,JSONObject> messages=(java.util.Map<Long,JSONObject>)modelClass.getField("messages").get(model);
   messages.clear();
   JSONObject owner=new JSONObject().put("Id",21).put("UserId",8).put("nick_name","圈主").put("CreateTime","09:30").put("ContentType",1).put("Content","这是一条较长的示例消息，用于核对原版的字号、行距和自动换行。第二行仍应完整显示。");
   JSONObject other=new JSONObject().put("Id",22).put("UserId",9).put("nick_name","圈友").put("CreateTime","09:32").put("ContentType",1).put("Content","收到，谢谢。").put("OriginId",21).put("OriginData",new JSONArray().put(new JSONObject().put("nick_name","圈主").put("ContentType",1).put("Content","引用内容与回复正文分开展示。")));
   JSONObject photo=new JSONObject().put("Id",23).put("UserId",7).put("nick_name","测试用户").put("CreateTime","09:33").put("ContentType",2).put("Content","https://fixture.invalid/chart.png");
   android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(600,280,android.graphics.Bitmap.Config.ARGB_8888);
   android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(0xffe7f2fa);canvas.scale(2,2);android.graphics.Paint paint=new android.graphics.Paint(3);paint.setColor(0xff48a0ff);paint.setStrokeWidth(5);canvas.drawLine(20,110,100,70,paint);canvas.drawLine(100,70,170,90,paint);canvas.drawLine(170,90,280,25,paint);
   ((android.util.LruCache<String,android.graphics.Bitmap>)field("bitmaps")).put(photo.getString("Content"),bitmap);
   modelClass.getMethod("merge",JSONArray.class).invoke(model,new JSONArray().put(owner).put(other).put(photo));
   Method refresh=chat.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(nativeActivity);BaseAdapter adapter=(BaseAdapter)field("adapter");ListView list=(ListView)field("list");
   View left=adapter.getView(0,null,list),right=adapter.getView(1,null,list),image=adapter.getView(2,null,list);
   android.graphics.drawable.GradientDrawable blue=(android.graphics.drawable.GradientDrawable)left.findViewWithTag("local.zft.bubble").getBackground();
   android.graphics.drawable.GradientDrawable white=(android.graphics.drawable.GradientDrawable)right.findViewWithTag("local.zft.bubble").getBackground();
   require(blue.getColor().getDefaultColor()==0xffe6f6f3&&white.getColor().getDefaultColor()==0xffffffff,"approved mint and white bubbles");
   require(right.findViewWithTag("local.zft.quote")!=null,"separate quote block");
   require(image.findViewWithTag(photo.getString("Content")) instanceof ImageView,"native image renders");
   require(((ImageView)image.findViewWithTag(photo.getString("Content"))).getDrawable()!=null,"image cache displayed");
   TextView text=(TextView)left.findViewWithTag("local.zft.message-text");require(Math.abs(text.getTextSize()/getTargetContext().getResources().getDisplayMetrics().scaledDensity-16)<.1,"approved normal font 16sp");
   View topTabs=nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.0");
   require(topTabs.getParent()==nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.2").getParent(),"top two segments");
   require(nativeActivity.findViewById(android.R.id.content).findViewWithTag("local.zft.filter.3")==null,"stocks removed from chat filters");
   text.performClick();require(((Long)field("replyId"))==21L,"tap text selects reply");require(((TextView)field("replySummary")).getText().toString().contains("示例消息"),"reply previews original content");require(((View)field("replyCard")).getVisibility()==View.VISIBLE,"reply card visible");((TextView)field("reply")).performClick();require(((Long)field("replyId"))==0L,"cancel reply clears target");require(nativeActivity.getWindow().getDecorView().findViewWithTag("local.zft.photo")!=null,"native image picker present");
   list.setSelection(0);
  }catch(Exception e){throw new RuntimeException(e);}});
 }
 private void captureCssReference()throws Exception {
  Object model=field("model");java.util.Map<Long,JSONObject> messages=(java.util.Map<Long,JSONObject>)modelClass.getField("messages").get(model);
  String css=new String(java.nio.file.Files.readAllBytes(new java.io.File(getTargetContext().getExternalFilesDir(null),"reference-style.css").toPath()),"UTF-8");
  String a=" data-v-644040f8",b=" data-v-4af396d5";StringBuilder html=new StringBuilder("<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>"+css+"</style></head><body><header>原版 CSS · 相同示例消息</header><ul class='articel_list'"+b+"><div class='h_agq_page_chatting_wrap_nav'"+b+"><div"+b+"><button class='bind_this'"+b+">全部</button><button"+b+">我的消息</button></div></div><div class='chatting_wrap'"+b+">");
  for(JSONObject m:messages.values()){
   boolean owner=m.optInt("UserId")==8;html.append("<li class='circle_item h_agq_page_li "+(owner?"is_master":"")+"'"+a+"><div class='h_agq_page_li_image fixture-avatar'"+a+">"+(owner?"圈":"友")+"</div><div class='h_agq_page_li_box'"+a+"><div class='h_agq_page_li_wrap'"+a+"><div class='h_agq_page_li_info'"+a+"><span class='h_agq_page_li_name'"+a+">"+m.optString("nick_name")+"</span>"+(owner?"<span class='h_agq_page_li_id'"+a+">圈主</span>":"")+"<span class='h_agq_page_li_time'"+a+">"+m.optString("CreateTime")+"</span></div><div class='h_agq_page_li_content normal'"+a+">");
   JSONArray origins=m.optJSONArray("OriginData");if(origins!=null){JSONObject o=origins.getJSONObject(0);html.append("<div class='h_agq_page_li_re_content'"+a+"><p class='h_agq_page_li_re_name'"+a+">@"+o.optString("nick_name")+"</p><p class='h_agq_page_li_re_content_box'"+a+">"+o.optString("Content")+"</p></div>");}
   html.append("<div class='h_agq_page_li_content_box'"+a+">");if(m.optInt("ContentType")==2){android.graphics.Bitmap image=((android.util.LruCache<String,android.graphics.Bitmap>)field("bitmaps")).get(m.getString("Content"));java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);html.append("<img style='width:220px' src='data:image/png;base64,"+android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP)+"'>");}else html.append("<p"+a+">"+android.text.TextUtils.htmlEncode(m.optString("Content"))+"</p>");html.append("</div></div></div></div></li>");
  }html.append("</div><div style='text-align:right;font-size:12px;padding:8px'>□ 只看圈主</div></ul></body></html>");
  java.io.File file=new java.io.File(getTargetContext().getExternalFilesDir(null),"original-css-reference.html");java.nio.file.Files.write(file.toPath(),html.toString().getBytes("UTF-8"));
  runOnMainSync(()->{android.webkit.WebView web=new android.webkit.WebView(nativeActivity);web.getSettings().setBlockNetworkLoads(true);nativeActivity.setContentView(web);web.loadDataWithBaseURL("https://fixture.invalid",html.toString(),"text/html","UTF-8",null);});
  capture("original-css-reference.png");
 }
 private void capture(String name)throws Exception {Thread.sleep(4000);android.graphics.Bitmap b=getUiAutomation().takeScreenshot();if(b!=null)try(java.io.FileOutputStream f=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}}
 private static boolean hasWeb(View v){if(v instanceof android.webkit.WebView)return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(hasWeb(g.getChildAt(i)))return true;}return false;}
}
