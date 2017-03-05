先上demo地址：https://github.com/JeasonWong/CostTime

### 需求

实际业务开发中有很多需要不改变原业务代码，而需额外增加一些包括各种统计的需求，如APM、无数据埋点等，也就是耳熟能详的AOP，本文以统计方法耗时为例，不使用Aspectj，采用原生态的方式进行实践。

使用者所需要做的就是对所需要统计耗时的方法头部加指定注解@Cost就可以使用了。

### 目标

- 不影响现有逻辑

- 需要统计耗时的方法头部加上注解

- 支持混淆

### 方案

方案分两部分，一部分针对JVM，一部分针对Android平台。

#### JVM

- 自定义注解

- 使用ASM增加字节码

- 反射实例化

- 使用Instrumentation构建代理

#### Android

- gradle plugin 自定义Transform Api


### 实践

先让我们的方案能在JVM上运行起来。

#### 自定义注解

```java
@Target(ElementType.METHOD)
public @interface Cost {
}
```

我们先只对方法进行耗时统计，因此Target设为Method。

#### 使用ASM增加字节码

首先自定义一个**ClassVisitor**，然后重载**visitMethod**方法，这样我们便可以遍历所有类文件的方法了，然后利用**AdviceAdapter**类来重新实例化一个MethodVisitor，并且重载它的**visitAnnotation**、**onMethodEnter**、**onMethodExit**方法，其中含义就如方法名一样简单明了。

然后我们通过visitAnnotation方法来判断当前方法注解是否为我们自定义的注解，如果是指定注解，则插入代码，具体插入代码的内容我们接下来再讲，自定义**ClassVisitor**的代码如下：

```java
public class CostClassVisitor extends ClassVisitor {

    public CostClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
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
                    //坐等插代码
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                if (inject) {
                    //坐等插代码
                }
            }
        };
        return mv;
    }
}
```
再写ASM插入代码前，我们必须意识到一件事，那就是得知道我们会在**onMethodEnter**中存一个方法开始时间，再在**onMethodExit**中存一个方法结束时间，再去相减，那么问题来了，这个时间我们存哪呢？肯定不能是局部变量，因为两个方法间并不会共享局部变量，那么本文是将变量存为静态，方便不同方法间调用，具体可见**TimeCache.java**，代码比较简单，不多加介绍：

```java
public class TimeCache {

    public static Map<String, Long> sStartTime = new HashMap<>();
    public static Map<String, Long> sEndTime = new HashMap<>();

    public static void setStartTime(String methodName, long time) {
        sStartTime.put(methodName, time);
    }

    public static void setEndTime(String methodName, long time) {
        sEndTime.put(methodName, time);
    }

    public static String getCostTime(String methodName) {
        long start = sStartTime.get(methodName);
        long end = sEndTime.get(methodName);
        return "method: " + methodName + " main " + Long.valueOf(end - start) + " ns";
    }

}
```

然后便是插入时间统计代码了，我在之前的一篇文章就有介绍过 [手摸手增加字节码往方法体内插代码](http://www.wangyuwei.me/2017/01/22/%E6%89%8B%E6%91%B8%E6%89%8B%E5%A2%9E%E5%8A%A0%E5%AD%97%E8%8A%82%E7%A0%81%E5%BE%80%E6%96%B9%E6%B3%95%E4%BD%93%E5%86%85%E6%8F%92%E4%BB%A3%E7%A0%81/) ，我们可以借助一个intelliJ plugin -- 
**ASM Bytecode Outline**来方便我们写ASM代码，先把原java代码写好：

```java
System.out.println("========start=========");
TimeUtil.setsStartTime("newFunc", System.nanoTime());
        
TimeUtil.setEndTime("newFunc", System.nanoTime());
System.out.println(TimeCache.getCostTime("newFunc"));
System.out.println("========end=========");
```
然后利用插件，生成对应ASM代码，删除掉无用信息后，可得到：

```java
mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn("========start=========");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

mv.visitLdcInsn(name);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
mv.visitMethodInsn(INVOKESTATIC, "main/java/TimeCache", "setStartTime", "(Ljava/lang/String;J)V", false);

...

mv.visitLdcInsn(name);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
mv.visitMethodInsn(INVOKESTATIC, "main/java/TimeCache", "setEndTime", "(Ljava/lang/String;J)V", false);

mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn(name);
mv.visitMethodInsn(INVOKESTATIC, "main/java/TimeCache", "getCostTime", "(Ljava/lang/String;)Ljava/lang/String;", false);
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn("========end=========");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
```

那么我们将这部分ASM代码填充到刚刚遗留下来的**CostClassVisitor.java**里便可。

这样一来，我们的ASM工作已经结束，接下来就是来让插入的方法运行起来。

#### 反射实例化

先来一种简单的方案，就是将我们插入代码后二进制流手动生成.class文件并利用反射实例化它。

首先来看看插入前的代码：

```java
public class Bazhang {

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

  @Cost
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
```

我给**newFunc2**方法增加了@Cost注解，但没给**newFunc1**方法增加，然后开始用ASM提供的类生成插入代码后的二进制流：

```java
ClassReader cr = new ClassReader(Bazhang.class.getName());
ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
ClassVisitor cv = new CostClassVisitor(cw);

cr.accept(cv, EXPAND_FRAMES);

// 获取生成的class文件对应的二进制流
byte[] code = cw.toByteArray();

//将二进制流写到out/下
FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir")
            + "/javademo/build/classes/main/wangyuwei/demo/Bazhang.class");
fos.write(code);
fos.close();

Demo loader = new Demo();
Class hw = loader.defineClass("wangyuwei.demo.Bazhang", code, 0, code.length);
Object o = hw.newInstance();
Method method1 = o.getClass().getMethod("newFunc1", String.class);
method1.invoke(o, "巴掌菜比");
Method method2 = o.getClass().getMethod("newFunc2", String.class);
method2.invoke(o, "巴掌菜比22");

```
然后run一下，可以得到：

```java
巴掌菜比
0
10
20
30
40
50
========start=========
巴掌菜比22
0
8
16
24
32
40
48
method: newFunc2 main 1647919 ns
========end=========
```

只对newFunc2做了方法耗时统计。

#### 使用Instrumentation构建代理

Instrumentation是Java5提供的新特性，关于详细介绍，可以查看这篇文章：[Java 5 特性 Instrumentation 实践](https://www.ibm.com/developerworks/cn/java/j-lo-instrumentation/)，简单点说就是我们得在JVM执行main函数前动点手脚，自己实现一个代理，在得到虚拟机载入的正常的类的字节码后通过ASM提供的类生成一个插入代码后的字节流再丢给虚拟机，自定义的代理得实现ClassFileTransformer，并且提供**premain()**方法，写有premain方法的类得在MANIFEST.MF中显示调用，首先来看看我们自定义的代理类：

```java
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

```

再看看**MANIFEST.MF**：

```
Manifest-Version: 1.0
Premain-Class: wangyuwei.costtime.CostClassFileTransformer

```

这样写好代理类之后，我们便可以生成一个代理jar，之后为我们运行代码使用，进入module **javademo**，生成jar可直接使用命令：

```
jar -cvfm lib/cost-time.jar src/main/META-INF/MANIFEST.MF src/main/java/wangyuwei/costtime/CostClassFileTransformer.class
```
得到代理jar后，再使用命令行java -javaagent:{{lib的绝对地址/}}lib/cost-time.jar Demo2.java运行即可，如果在使用命令行运行时带来了问题，可以直接通过对IntelliJ IDE进行修改：

**Windows, Linux, some Macs:**

ALT+SHIFT+F10->Right->E->Enter->Tab->enter your command line parameters->Enter.

**Mac with "OS X 10.5" key schema:**

CTRL+ALT+R->Right->E->Enter->Tab->enter your command line parameters->Enter.

在VM options一栏填入-javaagent:lib/cost-time.jar即可。

以上便是运行在JVM上的操作，然而我们最终的目标是为Android平台所用。

#### gradle plugin 自定义Transform Api

Transform API允许第三方插件在class文件转为为dex文件前操作编译好的class文件，那么这就是我们的入口，拿到正常的class后再经过ASM插入字节码后得到新的class，再被dx转成dex。

首先自定义个plugin--**CostTimePlugin.groovy**：

```groovy
public class CostTimePlugin extends Transform implements Plugin<Project> {
  @Override public void apply(Project project) {

    def android = project.extensions.getByType(AppExtension)
    android.registerTransform(this)
  }

  @Override
  String getName() {
    return "bazhang"
  }

  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  @Override
  Set<QualifiedContent.Scope> getScopes() {
    return TransformManager.SCOPE_FULL_PROJECT
  }

  @Override
  boolean isIncremental() {
    return false
  }

  @Override
  void transform(Context context, Collection<TransformInput> inputs,
      Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
      boolean isIncremental) throws IOException, TransformException, InterruptedException {
    println '//===============asm visit start===============//'

    def startTime = System.currentTimeMillis()

    inputs.each { TransformInput input ->

      input.directoryInputs.each { DirectoryInput directoryInput ->

        //坐等遍历class并被ASM操作

        def dest = outputProvider.getContentLocation(directoryInput.name,
            directoryInput.contentTypes, directoryInput.scopes,
            Format.DIRECTORY)


        FileUtils.copyDirectory(directoryInput.file, dest)
      }

      input.jarInputs.each { JarInput jarInput ->
        def jarName = jarInput.name
        def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
        if (jarName.endsWith(".jar")) {
          jarName = jarName.substring(0, jarName.length() - 4)
        }

        def dest = outputProvider.getContentLocation(jarName + md5Name,
            jarInput.contentTypes, jarInput.scopes, Format.JAR)

        FileUtils.copyFile(jarInput.file, dest)
      }
    }

    def cost = (System.currentTimeMillis() - startTime) / 1000

    println "plugin cost $cost secs"
    println '//===============asm visit end===============//'
  }
}

```

我们预留了一行注释，去遍历build/intermediates/classes/release/下面生成的所有class，当然R.class、BuildConfig.class这些我们就可以直接跳过，ASM过滤一遍插入新代码之后再去覆盖原class，代码如下：

```groovy
if (directoryInput.file.isDirectory()) {
    directoryInput.file.eachFileRecurse { File file ->
        def name = file.name
        if (name.endsWith(".class") && !name.startsWith("R\$") &&
            !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {

            println name + ' is changing...'

            ClassReader cr = new ClassReader(file.bytes);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new CostClassVisitor(cw);

            cr.accept(cv, EXPAND_FRAMES);

            byte[] code = cw.toByteArray();

            FileOutputStream fos = new FileOutputStream(
                file.parentFile.absolutePath + File.separator + name);
            fos.write(code);
            fos.close();
        }
    }
}

```
这样一来，我们可以看看新生成的class是不是有被插入代码。

这是源代码MainActivity.java：

```java
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Cost
  public void show() {
    for (int i = 0; i < 100; i++) {

    }
  }
}

```

这是build/intermediates/classes/release/里的MainActivity.class：

```
public class MainActivity extends AppCompatActivity {
  public MainActivity() {
  }

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(2130968603);
  }

  @Cost
  public void show() {
    System.out.println("========start=========");
    TimeCache.setStartTime("show", System.nanoTime());

    for(int i = 0; i < 100; ++i) {
      ;
    }

    TimeCache.setEndTime("show", System.nanoTime());
    System.out.println(TimeCache.getCostTime("show"));
    System.out.println("========end=========");
  }
}
```

而且我们可以看下build过程：

```
:app:transformClassesWithBazhangForRelease
//===============asm visit start===============//
Demo.class is changing...
MainActivity.class is changing...
plugin cost 0.148 secs
//===============asm visit end===============//
:app:processReleaseJavaRes 
:app:transformResourcesWithMergeJavaResForRelease
:app:transformClassesAndResourcesWithProguardForRelease

```

我们的class操作是在混淆前的，那么此次AOP也完全不影响正常的混淆，可看混淆后的MainActivity.java：

```java

public class MainActivity extends f {
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_main);
    }

    @a
    public void p() {
        System.out.println("========start=========");
        b.a("show", System.nanoTime());
        for (int i = 0; i < 100; i++) {
        }
        b.b("show", System.nanoTime());
        System.out.println(b.a("show"));
        System.out.println("========end=========");
    }
}

```

### 尾语

我们采取的方案其实大有可为，计算方法耗时只是冰山一角，包括无数据埋点、性能的监控都是可以继续拓展的。以上方案难免有些漏洞，欢迎指出，有兴趣的朋友可以一起探讨。