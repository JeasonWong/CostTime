package wangyuwei.costtime;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by bazhang on 2017/3/1.
 */
public class CostClassVisitor extends ClassVisitor {

  public CostClassVisitor(ClassVisitor classVisitor) {
    super(Opcodes.ASM5, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, String desc, String signature,
      String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv = new AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

      private boolean inject = false;

      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getDescriptor(Cost.class).equals(desc)) {
          inject = true;
        }
        return super.visitAnnotation(desc, visible);
      }

      @Override
      protected void onMethodEnter() {
        if (inject) {
          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn("========start=========");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);

          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
          mv.visitMethodInsn(INVOKESTATIC, "wangyuwei/costtime/TimeCache", "setStartTime",
              "(Ljava/lang/String;J)V", false);
        }
      }

      @Override
      protected void onMethodExit(int opcode) {
        if (inject) {
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
          mv.visitMethodInsn(INVOKESTATIC, "wangyuwei/costtime/TimeCache", "setEndTime",
              "(Ljava/lang/String;J)V", false);

          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(INVOKESTATIC, "wangyuwei/costtime/TimeCache", "getCostTime",
              "(Ljava/lang/String;)Ljava/lang/String;", false);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);

          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn("========end=========");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
              "(Ljava/lang/String;)V", false);
        }
      }
    };
    return mv;
  }
}
