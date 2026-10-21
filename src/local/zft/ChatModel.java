package local.zft;
import org.json.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import android.util.Base64;
public final class ChatModel {
 public static String messageTime(String value){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:^|[T ])([0-2][0-9]:[0-5][0-9])(?::[0-5][0-9])?").matcher(value);return m.find()?m.group(1):"";}
 public static void cleanRealtime(JSONObject event)throws JSONException {if(!"chat_room_send_message".equals(event.optString("Kind")))return;JSONObject data=event.optJSONObject("Data");if(data==null)return;String content=data.optString("Content");int start=content.lastIndexOf("【");if(start>=0&&start<content.lastIndexOf("】"))data.put("Content",content.substring(0,start));}
 public final TreeMap<Long,JSONObject> messages=new TreeMap<>();
 public JSONObject circle=new JSONObject();public String user="";public boolean admin;
 public void permissions(JSONObject data){circle=data;admin=data.optInt("master")==1;JSONArray admins=data.optJSONArray("admins");if(admins!=null)for(int i=0;i<admins.length();i++)if(admins.optJSONObject(i)!=null&&user.equals(admins.optJSONObject(i).optString("user_id")))admin=true;}
 public boolean visible(JSONObject m){
  if(m.optInt("State")==2)return false;
  JSONArray arr=m.optJSONArray("OriginData");JSONObject origin=arr==null?null:arr.optJSONObject(0);
  if(origin!=null&&origin.optInt("State")==1&&!admin){
   if(!user.equals(m.optString("UserId"))&&!user.equals(origin.optString("UserId")))return false;
  }return true;
 }
 public boolean acceptRealtime(JSONObject m){
  if(circle.optInt("chat_room_style")==1){if(m.optInt("State")>0)return false;JSONArray a=m.optJSONArray("OriginData");JSONObject origin=a==null?null:a.optJSONObject(0);if(!admin&&circle.optString("user_id").equals(m.optString("UserId"))&&origin!=null&&origin.optInt("State")==1&&!user.equals(origin.optString("UserId")))return false;}return visible(m);
 }
 public boolean isOwner(JSONObject m){return !circle.optString("user_id").isEmpty()&&circle.optString("user_id").equals(m.optString("UserId"));}
 public boolean isLeader(JSONObject m){if(isOwner(m))return true;JSONArray admins=circle.optJSONArray("admins");if(admins!=null)for(int i=0;i<admins.length();i++){JSONObject a=admins.optJSONObject(i);if(a!=null&&a.optString("user_id").equals(m.optString("UserId")))return true;}return false;}
 public boolean relatesToMe(JSONObject m){if(user.equals(m.optString("UserId")))return true;JSONArray a=m.optJSONArray("OriginData");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&user.equals(o.optString("UserId")))return true;}return false;}
 public void merge(JSONArray arr){if(arr!=null)for(int i=0;i<arr.length();i++){JSONObject m=arr.optJSONObject(i);if(m!=null&&m.optLong("Id")>0){if(m.optInt("State")==2)messages.remove(m.optLong("Id"));else messages.put(m.optLong("Id"),m);}}}
 public ArrayList<JSONObject> visibleMessages(){ArrayList<JSONObject> out=new ArrayList<>();for(JSONObject m:messages.values())if(visible(m))out.add(m);return out;}
 public static String decode(String raw)throws Exception{
  if(raw.startsWith("{")||raw.equals("pong")||raw.equals("Success"))return raw;
  if(raw.length()<33)throw new IllegalArgumentException("invalid socket payload");
  String key=raw.substring(0,32);Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key.getBytes("UTF-8"),"AES"),new IvParameterSpec(key.substring(16).getBytes("UTF-8")));
  return new String(cipher.doFinal(Base64.decode(raw.substring(32),Base64.DEFAULT)),"UTF-8");
 }
}
