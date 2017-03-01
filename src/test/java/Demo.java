package test.java;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import main.java.Cost;
import main.java.CostClassVisitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static jdk.internal.org.objectweb.asm.ClassReader.EXPAND_FRAMES;

/**
 * Created by bazhang on 2017/3/1.
 */
public class Demo extends ClassLoader{

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassReader cr = new ClassReader(Bazhang.class.getName());
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new CostClassVisitor(cw);

        cr.accept(cv, EXPAND_FRAMES);

        // 获取生成的class文件对应的二进制流
        byte[] code = cw.toByteArray();

        //将二进制流写到out/下
        FileOutputStream fos = new FileOutputStream("out/production/CostTime/test/java/Demo$Bazhang.class");
        fos.write(code);
        fos.close();

        Demo loader = new Demo();
        Class hw = loader.defineClass("test.java.Demo$Bazhang", code, 0, code.length);
        Object o = hw.newInstance();
        Method method1 = o.getClass().getMethod("newFunc1", String.class);
        method1.invoke(o, "巴掌菜比");
        Method method2 = o.getClass().getMethod("newFunc2", String.class);
        method2.invoke(o, "巴掌菜比22");
    }

    public static class Bazhang {

        @Cost
        public void newFunc1(String str) {
            System.out.println(str);
            for (int i = 0; i < 100; i++) {
                if (i % 10 == 0) {
                    System.out.println(i);
                }
                if (i == 50) {
                    return;
                }
            }
        }

        public void newFunc2(String str) {
            System.out.println(str);
            for (int i = 0; i < 100; i++) {
                if (i % 8 == 0) {
                    System.out.println(i);
                }
                if (i > 50) {
                    return;
                }
            }
        }
    }

}
