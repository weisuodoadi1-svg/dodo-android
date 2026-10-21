package local.zft;
import android.app.Activity;
import android.content.SharedPreferences;
import android.webkit.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

final class StockPanel extends WebView {
 private static final String ORIGIN="https://stocks.local.invalid";
 private final SharedPreferences prefs;
 private boolean active,ready;
 StockPanel(Activity activity,String user){super(activity);prefs=activity.getSharedPreferences("local.zft.stocks."+user,0);setBackgroundColor(Ui.BG);setTag("local.zft.stocks");
  getSettings().setJavaScriptEnabled(true);getSettings().setAllowFileAccess(false);getSettings().setAllowContentAccess(false);getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  addJavascriptInterface(new Storage(),"StockStorage");
  setWebViewClient(new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
   public void onPageFinished(WebView v,String url){if(url.equals(ORIGIN+"/index.html")){ready=true;setActive(active);}}
   public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){try{
    URI uri=new URI(r.getUrl().toString());if(!"GET".equals(r.getMethod()))return failure();
    if(ORIGIN.equals(uri.getScheme()+"://"+uri.getHost())){String path=uri.getPath();if(!Arrays.asList("/index.html","/panel.mjs","/search.mjs","/original-api.mjs","/market-api.mjs","/style.css").contains(path))return failure();String mime=path.endsWith("html")?"text/html":path.endsWith("css")?"text/css":"text/javascript";return response(mime,activity.getAssets().open("stocks"+path));}
    if(!allowed(uri))return failure();HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("Accept","application/json,text/plain,*/*");c.setRequestProperty("User-Agent","Mozilla/5.0");
    try{int status=c.getResponseCode();if(status!=200){android.util.Log.w("ZftStocks",uri.getHost()+" HTTP "+status);return failure("行情服务器返回 HTTP "+status);}try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Response too large");out.write(b,0,n);}return response("text/plain",new ByteArrayInputStream(out.toByteArray()));}}finally{c.disconnect();}
   }catch(Exception e){android.util.Log.w("ZftStocks",r.getUrl().getHost()+" "+e.getClass().getSimpleName()+": "+e.getMessage());return failure(e instanceof java.net.SocketTimeoutException?"行情连接超时":e instanceof javax.net.ssl.SSLException?"行情 HTTPS 连接失败":e instanceof java.net.UnknownHostException?"行情域名解析失败":"行情网络读取失败");}}
  });loadUrl(ORIGIN+"/index.html");
 }
 static boolean allowed(URI u){if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;String host=u.getHost(),path=u.getPath();return ("quote.aigupiao.com".equals(host)&&Arrays.asList("/api/stock_history_adv.php","/api/stock_min_new.php").contains(path))||("smartbox.gtimg.cn".equals(host)&&"/s3/".equals(path))||("web.ifzq.gtimg.cn".equals(host)&&Arrays.asList("/appstock/app/fqkline/get","/appstock/app/minute/query").contains(path))||("searchapi.eastmoney.com".equals(host)&&"/api/suggest/get".equals(path))||("push2his.eastmoney.com".equals(host)&&"/api/qt/stock/kline/get".equals(path))||("push2.eastmoney.com".equals(host)&&"/api/qt/stock/trends2/get".equals(path))||("quotes.sina.cn".equals(host)&&path.startsWith("/cn/api/jsonp_v2.php/"));}
 private WebResourceResponse response(String mime,InputStream in){Map<String,String> headers=new HashMap<>();headers.put("Access-Control-Allow-Origin",ORIGIN);headers.put("Cache-Control","no-store");return new WebResourceResponse(mime,"UTF-8",200,"OK",headers,in);}
 private WebResourceResponse failure(){return failure("行情请求不可用");}
 private WebResourceResponse failure(String reason){byte[] body=("{\"error\":"+JSONObject.quote(reason)+"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);return new WebResourceResponse("application/json","UTF-8",502,"Unavailable",Collections.singletonMap("Access-Control-Allow-Origin",ORIGIN),new ByteArrayInputStream(body));}
 void setActive(boolean value){active=value;if(ready)evaluateJavascript("window.stockActive&&window.stockActive("+value+")",null);}
 final class Storage {
  @JavascriptInterface public String read(){return prefs.getString("watchlist","[]");}
  @JavascriptInterface public String save(String raw){try{if(raw==null||raw.length()>16000)return "自选数据过大";JSONArray list=new JSONArray(raw);if(list.length()>50)return "最多添加50只股票";Set<String> seen=new HashSet<>();for(int i=0;i<list.length();i++){JSONObject s=list.getJSONObject(i);String code=s.getString("code"),market=s.getString("market");if(!code.matches("[0-9]{6}")||!Arrays.asList("SSE","SZSE").contains(market)||s.optString("name").length()>80||!seen.add(market+code))return "自选数据无效";}return prefs.edit().putString("watchlist",raw).commit()?"":"保存失败，请重试";}catch(Exception e){return "自选数据无效";}}
 }
}
