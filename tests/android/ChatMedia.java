package local.zfttests;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class ChatMedia extends Instrumentation {
 Activity a;Class<?> chat;Object model;Class<?> type;
 void require(boolean b,String why){if(!b)throw new AssertionError(why);}
 Object field(String name)throws Exception{Field f=chat.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 Object call(String name,Class<?>[] types,Object... args)throws Exception{Method m=chat.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(a,args);}
 JSONObject message(long id,int user,int state,int contentType,String content)throws Exception{return new JSONObject().put("Id",id).put("UserId",user).put("State",state).put("ContentType",contentType).put("Content",content).put("nick_name","测试成员");}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();ServerSocket server=null;try{
  a=startActivitySync(new Intent().setClassName(getTargetContext(),"local.zft.NativeChat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));chat=a.getClass();
  runOnMainSync(()->{try{Field fixture=chat.getDeclaredField("fixture");fixture.setAccessible(true);fixture.setBoolean(a,true);call("fixture",new Class<?>[]{});model=field("model");type=model.getClass();}catch(Exception e){throw new RuntimeException(e);}});
  Method accepts=type.getMethod("acceptRealtime",JSONObject.class),withdraw=type.getMethod("canWithdraw",JSONObject.class),permissions=type.getMethod("permissions",JSONObject.class);
  JSONObject circle=new JSONObject().put("name","测试圈子").put("chat_room_style",1).put("user_id",8).put("is_forbid_download_image",1);
  JSONObject photo=message(100,7,1,4,"download.png").put("images_json",new JSONObject().put("url","//example.invalid/picture.png").toString());
  permissions.invoke(model,circle);
  require((Boolean)accepts.invoke(model,photo),"own web image pending moderation must be visible");
  require(!(Boolean)accepts.invoke(model,message(101,9,1,4,"other.png")),"other user's unapproved image stays hidden");
  require(!(Boolean)accepts.invoke(model,message(101,7,2,2,"deleted")),"withdrawn own image stays hidden");
  require((Boolean)withdraw.invoke(model,photo),"own image can be withdrawn");
  require(!(Boolean)withdraw.invoke(model,message(101,9,0,1,"other")),"regular member cannot withdraw others");
  permissions.invoke(model,new JSONObject(circle.toString()).put("master",1));
  require((Boolean)withdraw.invoke(model,message(101,9,0,1,"other")),"owner can moderate others");
  permissions.invoke(model,circle);
  require("https://example.invalid/picture.png".equals(type.getMethod("imageUrl",String.class).invoke(null," //example.invalid/picture.png ")),"protocol-relative web image normalized");
  runOnMainSync(()->{try{
   Map<?,?> params=(Map<?,?>)call("withdrawParams",new Class<?>[]{JSONObject.class},photo);require("100".equals(params.get("msg_id"))&&"1".equals(params.get("del_type")),"original own withdrawal parameters");
   type.getField("admin").setBoolean(model,true);params=(Map<?,?>)call("withdrawParams",new Class<?>[]{JSONObject.class},message(101,9,0,1,"other"));require("0".equals(params.get("del_type")),"original admin withdrawal parameters");type.getField("admin").setBoolean(model,false);
   JSONObject response=new JSONObject().put("circle",circle).put("data",new JSONArray().put(message(99,8,0,1,"latest"))).put("user_view_self_chat",new JSONArray().put(photo));
   ((ArrayList<JSONObject>)field("pendingEvents")).add(new JSONObject().put("Kind","chat_room_del_message").put("Data",99));
   call("mergeHistory",new Class<?>[]{JSONObject.class,boolean.class,int.class},response,false,0);
   TreeMap<Long,JSONObject> messages=(TreeMap<Long,JSONObject>)type.getField("messages").get(model);
   require(messages.containsKey(1L)&&messages.containsKey(100L)&&!messages.containsKey(99L),"reconciliation keeps older history, restores own web image, replays concurrent withdrawal");
   call("refresh",new Class<?>[]{});
   BaseAdapter adapter=(BaseAdapter)field("adapter");View row=adapter.getView(adapter.getCount()-1,null,(ListView)field("list"));require(row.findViewWithTag("https://example.invalid/picture.png")!=null,"image-text attachment rendered");require(((TextView)row.findViewWithTag("local.zft.message-text")).getText().toString().equals("download.png"),"image caption rendered");
   JSONObject removed=new JSONObject(photo.toString()).put("State",2);type.getMethod("merge",JSONArray.class).invoke(model,new JSONArray().put(removed));call("refresh",new Class<?>[]{});JSONObject tombstone=messages.get(100L);require(tombstone!=null&&!tombstone.has("Content")&&!tombstone.has("images_json"),"withdrawn own media replaced with content-free notice");View notice=adapter.getView(adapter.getCount()-1,null,(ListView)field("list"));require("local.zft.withdrawn".equals(notice.getTag())&&notice.findViewWithTag("https://example.invalid/picture.png")==null,"withdrawal notice does not redisplay removed image");
  }catch(Exception e){throw new RuntimeException(e);}});
  // Serve a generated test image locally so the original viewer is exercised without private media.
  Bitmap bitmap=Bitmap.createBitmap(900,1600,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);c.drawColor(0xffd7f4ed);Paint paint=new Paint();paint.setColor(0xff146b5f);paint.setTextSize(80);for(int y=100;y<1600;y+=150)c.drawText("IMAGE TEST "+y,30,y,paint);ByteArrayOutputStream png=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,png);bitmap.recycle();byte[] data=png.toByteArray();
  server=new ServerSocket(0,4,InetAddress.getByName("127.0.0.1"));final ServerSocket serving=server;
  Thread http=new Thread(()->{while(!serving.isClosed())try(Socket s=serving.accept()){s.setSoTimeout(3000);BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream()));String line;while((line=r.readLine())!=null&&!line.isEmpty()){}OutputStream stream=s.getOutputStream();stream.write(("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));stream.write(data);}catch(Exception ignored){}});http.setDaemon(true);http.start();
  String url="http://127.0.0.1:"+server.getLocalPort()+"/image.png";
  ActivityMonitor monitor=addMonitor("com.zhongfutong.main.ImagePagerActivity",null,false);
  runOnMainSync(()->{try{call("openPicture",new Class<?>[]{View.class,String.class},a.findViewById(android.R.id.content),url);}catch(Exception e){throw new RuntimeException(e);}});
  Activity viewer=waitForMonitorWithTimeout(monitor,5000);require(viewer!=null,"original full-screen viewer launches");Thread.sleep(2200);waitForIdleSync();
  require(!viewer.getIntent().getBooleanExtra("image_save",true),"circle image-save permission retained");
  require((viewer.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)==0,"full-screen images remain screenshot-friendly");
  View photoView=findPhoto(viewer.getWindow().getDecorView());require(photoView!=null,"original zoomable photo view present");
  runOnMainSync(()->{require(photoView.getWidth()>=a.getResources().getDisplayMetrics().widthPixels*.9,"viewer fills screen width");require(((ImageView)photoView).getDrawable()!=null&&((ImageView)photoView).getDrawable() instanceof android.graphics.drawable.BitmapDrawable&&((android.graphics.drawable.BitmapDrawable)((ImageView)photoView).getDrawable()).getBitmap().getPixel(0,0)==0xffd7f4ed,"original viewer loaded full-resolution fixture");});
  float before=((Number)photoView.getClass().getMethod("getScale").invoke(photoView)).floatValue();int[] xy=new int[2];runOnMainSync(()->{photoView.getLocationOnScreen(xy);xy[0]+=photoView.getWidth()/2;xy[1]+=photoView.getHeight()/2;});
  for(int i=0;i<2;i++){long now=SystemClock.uptimeMillis();sendPointerSync(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,xy[0],xy[1],0));sendPointerSync(MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,xy[0],xy[1],0));Thread.sleep(70);}Thread.sleep(600);
  float after=((Number)photoView.getClass().getMethod("getScale").invoke(photoView)).floatValue();require(after>before,"physical double tap zooms image");
  Bitmap shot=getUiAutomation().takeScreenshot();try(FileOutputStream f=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"chat-image-fullscreen.png"))){shot.compress(Bitmap.CompressFormat.PNG,100,f);}shot.recycle();
  sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitForIdleSync();require(!a.isFinishing(),"back returns to chat");removeMonitor(monitor);
  out.putString("result","PASS own web image/privacy filters, original withdrawal permissions/parameters, caption and image rendering, history reconciliation with concurrent withdrawal, original full-screen viewer loading/double-tap zoom/back. No live message sent or withdrawn.");finish(-1,out);
 }catch(Throwable e){out.putString("result","FAIL: "+e);finish(0,out);}finally{if(server!=null)try{server.close();}catch(Exception ignored){}}}
 View findPhoto(View v){if(v.getClass().getName().equals("com.myview.dragclose.DragPhotoView"))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=findPhoto(g.getChildAt(i));if(found!=null)return found;}}return null;}
}
