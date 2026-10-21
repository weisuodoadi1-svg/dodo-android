package local.zft;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
final class Ui {
 static final int BG=0xfff6f7f9,INK=0xff20262d,MUTED=0xff7b8491,ACCENT=0xff168b82;
 static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 static GradientDrawable bordered(Context c,int color,int radius){GradientDrawable d=shape(c,color,radius);d.setStroke(dp(c,1),0xffdce1e7);return d;}
 static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
 static View navigation(Activity a,int selected,java.util.function.IntConsumer action){
  LinearLayout row=new LinearLayout(a);row.setTag("local.zft.tabs");row.setBackgroundColor(Color.WHITE);row.setPadding(dp(a,8),dp(a,5),dp(a,8),dp(a,5));
  String[] names={"聊天室","圈子","我的","我的股票","AI 动态"};
  for(int i:new int[]{0,3,4,1,2}){final int index=i;LinearLayout item=column(a);item.setGravity(Gravity.CENTER);item.setContentDescription(names[i]);item.setTag("local.zft.tab."+i);item.setSelected(selected==i);item.setClickable(true);item.setFocusable(true);
   int color=selected==i?ACCENT:MUTED;item.addView(new Icon(a,i==4?8:i==3?5:i,color),new LinearLayout.LayoutParams(dp(a,25),dp(a,25)));TextView label=text(a,names[i],11,color);label.setGravity(Gravity.CENTER);item.addView(label);row.addView(item,new LinearLayout.LayoutParams(0,dp(a,52),1));item.setOnClickListener(v->action.accept(index));
  }return row;
 }
 static class Icon extends View {final Paint p=new Paint(3);final int kind,color;Icon(Context c,int k,int col){super(c);kind=k;color=col;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   if(kind==0){c.drawRoundRect(3,3,21,18,5,5,p);c.drawLine(8,18,6,21,p);c.drawLine(6,21,13,18,p);c.drawLine(8,9,16,9,p);c.drawLine(8,13,13,13,p);}
   else if(kind==1){c.drawCircle(9,7,3.5f,p);c.drawArc(2,13,16,24,180,180,false,p);c.drawArc(13,4,21,12,-90,180,false,p);c.drawArc(13,14,23,24,-90,90,false,p);}
   else if(kind==6){c.drawRoundRect(3,3,21,21,3,3,p);c.drawCircle(8,8,1.5f,p);c.drawLine(4,18,10,12,p);c.drawLine(10,12,14,16,p);c.drawLine(14,16,17,12,p);c.drawLine(17,12,21,17,p);}
   else if(kind==8){c.drawRoundRect(3,3,21,21,3,3,p);c.drawLine(7,8,17,8,p);c.drawLine(7,12,17,12,p);c.drawLine(7,16,13,16,p);}
   else if(kind==7){c.drawLine(6,6,18,18,p);c.drawLine(18,6,6,18,p);}
   else if(kind==5){c.drawLine(3,3,3,21,p);c.drawLine(3,21,22,21,p);c.drawLine(6,16,11,10,p);c.drawLine(11,10,15,13,p);c.drawLine(15,13,21,5,p);}
   else if(kind==4){p.setStyle(Paint.Style.FILL);for(int y:new int[]{5,12,19})c.drawCircle(12,y,1.7f,p);}
   else if(kind==3){c.drawCircle(12,12,9,p);c.drawPoint(9,9,p);c.drawPoint(15,9,p);c.drawArc(7,9,17,17,15,150,false,p);}
   else {c.drawCircle(12,7,4,p);c.drawArc(4,13,20,27,180,180,false,p);}c.restore();}
 }
}
