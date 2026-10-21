package local.zfttests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.graphics.*;import android.graphics.drawable.*;import java.io.*;
public class Splash extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();try{
  Activity a=startActivitySync(new Intent().setClassName(getTargetContext(),"com.zhongfutong.main.WelcomeView").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  runOnMainSync(()->{
   android.util.TypedValue background=new android.util.TypedValue();
   if(!a.getTheme().resolveAttribute(android.R.attr.windowBackground,background,true))throw new AssertionError("missing starting window background");
   if(!"dodo_starting_window".equals(a.getResources().getResourceEntryName(background.resourceId)))throw new AssertionError("wrong starting window resource");
   LayerDrawable drawable=(LayerDrawable)a.getDrawable(background.resourceId);
   if(drawable.getNumberOfLayers()!=2)throw new AssertionError("unexpected extra splash layers");
   Bitmap original=((BitmapDrawable)drawable.getDrawable(1)).getBitmap();
   if(original.getWidth()!=793||original.getHeight()!=1983)throw new AssertionError("wrong compressed artwork");
   View decor=a.getWindow().getDecorView();int width=decor.getWidth(),height=decor.getHeight();
   Bitmap frame=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,width,height);drawable.draw(new Canvas(frame));
   Rect bounds=drawable.getDrawable(1).getBounds();
   if(bounds.left<0||bounds.top<0||bounds.right>width||bounds.bottom>height||Math.abs(bounds.width()/(float)bounds.height()-.4f)>.002)throw new AssertionError("artwork cropped or distorted");
   if(decor.findViewWithTag("local.zft.splash")!=null)throw new AssertionError("duplicate overlay remains");
   if((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0)throw new AssertionError("screenshot blocked");
   try(FileOutputStream stream=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"splash-window-preview.png"))){frame.compress(Bitmap.CompressFormat.PNG,100,stream);}catch(Exception e){throw new RuntimeException(e);}frame.recycle();
  });
  out.putString("result","PASS: launch window contains original compressed ALL IN AI artwork, aspect ratio and bounds preserved, no second image overlay, no screenshot flag.");finish(-1,out);
 }catch(Throwable e){out.putString("result","FAIL: "+e);finish(0,out);}}
}
