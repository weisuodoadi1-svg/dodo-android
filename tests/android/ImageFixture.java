package local.zfttests;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.graphics.Bitmap;import java.io.*;
public class ImageFixture extends ContentProvider {
 public boolean onCreate(){return true;}public String getType(Uri uri){return "image/png";}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{File f=new File(getContext().getCacheDir(),"sample.png");try{Bitmap b=Bitmap.createBitmap(3000,2000,Bitmap.Config.ARGB_8888);b.eraseColor(0xff168b82);try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(IOException e){throw new FileNotFoundException();}return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}public Uri insert(Uri u,ContentValues v){return null;}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
