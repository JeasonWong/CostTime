package wangyuwei.demo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import wangyuwei.costtime.CostClassVisitor;
import wangyuwei.costtime.TimeCache;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

/**
 * 巴掌
 * https://github.com/JeasonWong
 */

public class Demo3 extends ClassLoader {

  public static void main(String[] args)
      throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException,
      InvocationTargetException {
    System.out.println(System.getProperty("user.dir"));
    ClassReader cr = new ClassReader(Bazhang.class.getName());
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new CostClassVisitor(cw);

    cr.accept(cv, EXPAND_FRAMES);

    // 获取生成的class文件对应的二进制流
    byte[] code = cw.toByteArray();

    //将二进制流写到out/下
    FileOutputStream fos =
        new FileOutputStream(System.getProperty("user.dir")
            + "/javademo/build/classes/main/wangyuwei/demo/Bazhang.class");
    fos.write(code);
    fos.close();

    System.out.println(TimeCache.class);

    Demo3 loader = new Demo3();
    Class hw = loader.defineClass("wangyuwei.demo.Bazhang", code, 0, code.length);
    Object o = hw.newInstance();
    Method method1 = o.getClass().getMethod("newFunc1", String.class);
    method1.invoke(o, "巴掌菜比");
    Method method2 = o.getClass().getMethod("newFunc2", String.class);
    method2.invoke(o, "巴掌菜比22");
  }
}
