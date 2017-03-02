### What's CostTime ?

A lib to compute the method cost time on the JVM.

You only need add **@Cost** annotation on any method's head you want to compute.

### USAGE

There are two ways to use it, one is **run Demo.java** directly, another one is **use -javaagent option**.

#### run Demo.java

Demo.java uses reflection to instantiate an object and invoke the method.

#### use -javaagent option

Run **jar -cvfm lib/cost-time.jar src/main/META-INF/MANIFEST.MF out/production/CostTime/main/java/CostClassFileTransformer.class** in root direction, you will get cost-time.jar.

After you get **cost-time.jar**, you can run command **java -javaagent:lib/cost-time.jar test.java.Demo2.java**.

If you don't be familiar with command, you can configurate IntelliJ IDEA as below:

**Windows, Linux, some Macs:**

ALT+SHIFT+F10, Right, E, Enter, Tab, enter your command line parameters, Enter. ;-)

**Mac with "OS X 10.5" key schema:**

CTRL+ALT+R, Right, E, Enter, Tab, enter your command line parameters, Enter.

Have fun ! 

### EFFECT

the second method didn't add @Cost, so it did't be injected extra code.

```
========start=========
巴掌菜比
0
10
20
30
40
50
method: newFunc1 main 953833 ns
========end=========
巴掌菜比22
0
8
16
24
32
40
48

```
