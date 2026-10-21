package local.zft;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
public final class StockActivity extends Activity {
 private StockPanel stocks;
 private android.window.OnBackInvokedCallback systemBack;
 public void onCreate(Bundle saved){super.onCreate(saved);getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(-1);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);LinearLayout root=Ui.column(this);root.setFitsSystemWindows(true);root.setBackgroundColor(Ui.BG);TextView title=Ui.text(this,"我的股票",22,Ui.INK);title.setTypeface(null,1);title.setPadding(Ui.dp(this,20),Ui.dp(this,16),0,Ui.dp(this,8));LinearLayout header=new LinearLayout(this);header.setGravity(android.view.Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView search=Ui.text(this,"⌕ 搜索",15,Ui.ACCENT);search.setTag("local.zft.stock-search");search.setContentDescription("搜索并添加股票");search.setMinHeight(Ui.dp(this,48));search.setPadding(Ui.dp(this,16),0,Ui.dp(this,20),0);search.setFocusable(true);search.setOnClickListener(v->stocks.evaluateJavascript("window.toggleStockSearch&&window.toggleStockSearch()",null));header.addView(search);root.addView(header);String user="";try{user=(String)Class.forName("com.androidUtil.network.AndroidUtils").getMethod("r",Context.class,String.class,String.class).invoke(null,this,"LOGIN","LOGIN_ID");}catch(Exception ignored){}stocks=new StockPanel(this,user);root.addView(stocks,new LinearLayout.LayoutParams(-1,0,1));root.addView(Ui.navigation(this,3,i->ChatHome.navigate(this,i)));setContentView(root);if(android.os.Build.VERSION.SDK_INT>=33){systemBack=this::onBackPressed;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBack);}}
 public void onBackPressed(){stocks.evaluateJavascript("(function(){var d=document.getElementById('detail');if(d&&d.open){d.close();return true;}if(window.closeStockSearch)return window.closeStockSearch();return false;})()",closed->{if(!isFinishing()&&!"true".equals(closed))finish();});}
 public void onResume(){super.onResume();stocks.setActive(true);}
 public void onPause(){stocks.setActive(false);super.onPause();}
 public void onDestroy(){if(android.os.Build.VERSION.SDK_INT>=33&&systemBack!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBack);stocks.destroy();super.onDestroy();}
}
