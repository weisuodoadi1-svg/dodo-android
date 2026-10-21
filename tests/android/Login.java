package local.zfttests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.lang.reflect.*;
public class Login extends Instrumentation {
 void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 View view(Activity a,String n){return a.findViewById(a.getResources().getIdentifier(n,"id",a.getPackageName()));}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();try{
  ActivityMonitor wx=addMonitor("com.zhongfutong.main.wxapi.WXEntryActivity",null,true);
  for(String activity:new String[]{"SecretFreeAct","SecretFreeAct2"}){
   Activity a=startActivitySync(new Intent().setClassName(getTargetContext(),"com.zhongfutong.main."+activity).putExtra("loginType","wx").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
   runOnMainSync(()->{try{
    require(!a.getIntent().hasExtra("loginType"),"legacy third-party launch removed");
    require(view(a,"ll_input").isShown()&&view(a,"tv_login").isShown(),"phone form visible");
    require(view(a,"lyAgreement").isShown(),"agreement retained");
    require(!((CheckBox)view(a,"chkItem")).isChecked(),"agreement not automatically accepted");
    for(String id:new String[]{"tv_login_old","ll_third_party_login","iv_weixin_login","iv_qq_login","iv_weibo_login"})require(!view(a,id).isShown(),id+" hidden");
    require(((TextView)view(a,"tv_get_code")).getText().toString().contains("验证码"),"SMS action retained");
    Field mode=a.getClass().getDeclaredField(activity.equals("SecretFreeAct")?"f4":"g4");mode.setAccessible(true);require((activity.equals("SecretFreeAct")?"0":"1").equals(mode.get(a)),"original SMS mode selected");
    EditText code=(EditText)view(a,"et_code");code.setText("123456");callActivityOnPause(a);callActivityOnStop(a);callActivityOnStart(a);callActivityOnResume(a);require("123456".contentEquals(code.getText()),"resume retains input");
    require(!view(a,"iv_weixin_login").performClick(),"hidden provider cannot launch");
   }catch(Exception e){throw new RuntimeException(e);}});
   android.graphics.Bitmap image=getUiAutomation().takeScreenshot();try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),activity+"-sms.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}image.recycle();
   runOnMainSync(()->a.finish());waitForIdleSync();
  }
  require(wx.getHits()==0,"no WeChat activity launched");removeMonitor(wx);out.putString("result","PASS: both login routes use original SMS mode, hide third-party/password choices, retain unchecked agreement and entered code across resume; no SMS sent.");finish(-1,out);
 }catch(Throwable e){out.putString("result","FAIL: "+e);finish(0,out);}}
}
