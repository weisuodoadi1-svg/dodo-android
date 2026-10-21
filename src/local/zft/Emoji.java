package local.zft;

import android.app.*;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.ImageSpan;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.regex.*;

final class Emoji {
 private final Context context;
 private final LinkedHashMap<String,String> files=new LinkedHashMap<>();
 private final HashMap<String,Drawable.ConstantState> images=new HashMap<>();
 private static final Pattern TOKEN=Pattern.compile("\\[([^\\[\\]\\r\\n]+)\\]");
 Emoji(Context context){this.context=context;try(InputStream in=context.getAssets().open("local-zft-emoji/index.json")){
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
  JSONObject index=new JSONObject(out.toString("UTF-8"));Iterator<String> names=index.keys();while(names.hasNext()){String name=names.next();files.put(name,index.getString(name));}
 }catch(Exception e){android.util.Log.w("ZftEmoji","Cannot load bundled emoji",e);}}
 private Drawable icon(String name){try{Drawable.ConstantState state=images.get(name);if(state==null){try(InputStream in=context.getAssets().open("local-zft-emoji/"+files.get(name))){Drawable d=Drawable.createFromStream(in,null);if(d==null)return null;state=d.getConstantState();if(state==null)return d;images.put(name,state);}}return state.newDrawable(context.getResources()).mutate();}catch(IOException e){return null;}}
 CharSequence render(CharSequence value,int pixels){SpannableString result=new SpannableString(value);Matcher matcher=TOKEN.matcher(value);while(matcher.find()){String name=matcher.group(1);if(!files.containsKey(name))continue;Drawable d=icon(name);if(d!=null){d.setBounds(0,0,pixels,pixels);result.setSpan(new ImageSpan(d,ImageSpan.ALIGN_BOTTOM),matcher.start(),matcher.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}}return result;}
 void insert(EditText input,String name){if(!input.isEnabled()||!files.containsKey(name))return;int start=input.getSelectionStart(),end=input.getSelectionEnd();if(start<0||end<0)start=end=input.length();input.getText().replace(Math.min(start,end),Math.max(start,end),"["+name+"]");}
 void show(EditText input){if(!input.isEnabled())return;ArrayList<String> names=new ArrayList<>(files.keySet());GridView grid=new GridView(context);grid.setNumColumns(5);grid.setPadding(12,12,12,12);grid.setAdapter(new BaseAdapter(){
  public int getCount(){return names.size();}public Object getItem(int p){return names.get(p);}public long getItemId(int p){return p;}
  public View getView(int p,View recycled,ViewGroup parent){String name=names.get(p);TextView cell=Ui.text(context,name,11,Ui.INK);cell.setGravity(Gravity.CENTER);cell.setContentDescription("插入表情："+name);cell.setPadding(0,Ui.dp(context,8),0,Ui.dp(context,8));Drawable d=icon(name);if(d!=null){int size=Ui.dp(context,32);d.setBounds(0,0,size,size);cell.setCompoundDrawables(null,d,null,null);}cell.setLayoutParams(new AbsListView.LayoutParams(-1,Ui.dp(context,68)));return cell;}
 });AlertDialog dialog=new AlertDialog.Builder(context).setTitle("表情 · 点选后加入输入框").setView(grid).setNegativeButton("关闭",null).create();grid.setOnItemClickListener((parent,view,position,id)->{insert(input,names.get(position));dialog.dismiss();});dialog.show();dialog.getWindow().setLayout(-1,Ui.dp(context,420));}
}
