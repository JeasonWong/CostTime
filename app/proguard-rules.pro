# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/wscn/Documents/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontshrink                         #不压缩输入的类文件
-ignorewarnings                     # 忽略警告，避免打包时某些警告出现
-optimizationpasses 5               #指定代码的压缩级别
-dontusemixedcaseclassnames         #包明不混合大小写
-dontskipnonpubliclibraryclasses    #不去忽略非公共的库类jar
-dontskipnonpubliclibraryclassmembers
-dontoptimize                       #优化  不优化输入的类文件
-dontpreverify                      #预校验
-verbose                            #混淆时是否记录日志
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 混淆时所采用的算法
-keepattributes *Annotation*        #保护注解
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable