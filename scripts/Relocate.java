import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
public class Relocate {
 static String rename(String value){return value.replace("org/slf4j", "local/zft/deps/slf4j").replace("org/java_websocket", "local/zft/deps/websocket").replace("org.slf4j", "local.zft.deps.slf4j").replace("org.java_websocket", "local.zft.deps.websocket");}
 public static void main(String[] args)throws Exception {
  Remapper remapper=new Remapper(){public String map(String name){return rename(name);}public Object mapValue(Object value){return value instanceof String?rename((String)value):super.mapValue(value);}};
  try(JarOutputStream out=new JarOutputStream(new FileOutputStream(args[0]))){
   for(int i=1;i<args.length;i++)try(JarFile jar=new JarFile(args[i])){
    java.util.Enumeration<JarEntry> entries=jar.entries();while(entries.hasMoreElements()){
     JarEntry entry=entries.nextElement();if(!entry.getName().endsWith(".class")||entry.getName().startsWith("META-INF/")||entry.getName().equals("module-info.class"))continue;
     ClassReader reader=new ClassReader(jar.getInputStream(entry));ClassWriter writer=new ClassWriter(0);reader.accept(new ClassRemapper(writer,remapper),0);
     out.putNextEntry(new JarEntry(rename(entry.getName())));out.write(writer.toByteArray());out.closeEntry();
    }
   }
  }
 }
}
