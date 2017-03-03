package wangyuwei.costtime;


import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Created by bazhang on 2017/3/1.
 */
public class CostClassFileTransformer implements ClassFileTransformer {

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new CostClassFileTransformer());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new CostClassVisitor(writer), 8);
        return writer.toByteArray();
    }
}
