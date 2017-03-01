### What's CostTime ?

A lib to compute the method cost time on the JVM.

You only need add **@Cost** annotation on any method's head you want to compute.

### USAGE

There are two ways to use it, one is **run Demo.java** directly, another one is **use -javaagent option**.

#### run Demo.java

Demo.java uses reflection to instantiate an object and invoke the method.

#### use -javaagent option

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
