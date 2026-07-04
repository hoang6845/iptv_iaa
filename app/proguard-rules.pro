# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-optimizationpasses 5            # 指定代码的压缩级别
-dontusemixedcaseclassnames      # 是否使用大小写混合
-verbose                         # 混淆时是否记录日志
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod     # 保持注解
-dontoptimize                    # 优化不优化输入的类文件

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 混淆时所采用的算法

# 避免混淆泛型
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.akexorcist.localizationactivity.** { *; }
-dontwarn com.akexorcist.localizationactivity.**
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int i(...);
#    public static int w(...);
#    public static int d(...);
#    public static int e(...);
#}

-keep class com.connectsdk.**{ * ; }

-keep class hoang.dqm.codebase.utils.BindingReflex

-keep class ** extends androidx.viewbinding.ViewBinding {
     public static ** inflate( android.view.LayoutInflater);
}

-keep class ** extends androidx.viewbinding.ViewBinding {
     public static ** inflate( android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class * implements androidx.viewbinding.ViewBinding {
 public static * bind(android.view.View); public static * inflate(android.view.LayoutInflater);
}

-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
  public static * inflate(android.view.LayoutInflater);
  public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

-keep class * implements androidx.viewbinding.ViewBinding {
 public static * bind(android.view.View); public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

-keep class com.google.android.material.** { *; }
-dontwarn kotlin.**

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keep class * implements java.io.Serializable {
  public static final java.io.Serializable *;
}

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.** { *; }

##---------------End: proguard configuration for Gson  ----------

-keep class kotlin.coroutines.Continuation

-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *;}

-keepattributes InnerClasses

-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class com.daimajia.easing.** { *; }
-keep interface com.daimajia.easing.** { *; }
-keepattributes *Annotation*


-keepnames class info.dvkr.screenstream.** { *; }
-keep class **.R$* {
    <fields>;
}

#Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }

#KTOR
-keep class kotlin.reflect.jvm.internal.**
-keep class kotlin.text.RegexOption

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn com.daimajia.easing.Glider
-dontwarn com.daimajia.easing.Skill
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE


-dontwarn kotlin.reflect.jvm.internal.**

-keep class kotlin.reflect.jvm.internal.** { *; }

-keep interface javax.annotation.Nullable

# -printmapping out.map

 -keep public class * {
     public protected *;
 }

 -keepparameternames
 -renamesourcefileattribute SourceFile
 -keepattributes Signature,Exceptions,*Annotation*,
                 InnerClasses,PermittedSubclasses,EnclosingMethod,
                 Deprecated,SourceFile,LineNumberTable

 -keepclasseswithmembernames,includedescriptorclasses class * {
     native <methods>;
 }

 -keepclassmembers,allowoptimization enum * {
     public static **[] values();
     public static ** valueOf(java.lang.String);
 }

 -keepclassmembers class * implements java.io.Serializable {
     static final long serialVersionUID;
     private static final java.io.ObjectStreamField[] serialPersistentFields;
     private void writeObject(java.io.ObjectOutputStream);
     private void readObject(java.io.ObjectInputStream);
     java.lang.Object writeReplace();
     java.lang.Object readResolve();
 }
-dontwarn java.lang.reflect.AnnotatedType
-keep class kotlin.Metadata
-keep class com.shuyu.gsyvideoplayer.video.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.**
-keep class com.shuyu.gsyvideoplayer.video.base.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.base.**
-keep class com.shuyu.gsyvideoplayer.utils.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.utils.**
-keep class com.shuyu.gsyvideoplayer.player.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.player.**
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**
-keep class androidx.media3.** {*;}
-keep interface androidx.media3.**

-keep class com.shuyu.alipay.** {*;}
-keep interface com.shuyu.alipay.**

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class com.alivc.**{*;}
-keep class com.aliyun.**{*;}
-keep class com.cicada.**{*;}
-dontwarn com.alivc.**
-dontwarn com.aliyun.**
-dontwarn com.cicada.**
-keep class ** extends androidx.viewbinding.ViewBinding {
     public static ** inflate( android.view.LayoutInflater);
}

-keep class ** extends androidx.viewbinding.ViewBinding {
     public static ** inflate( android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class * implements androidx.viewbinding.ViewBinding {
 public static * bind(android.view.View); public static * inflate(android.view.LayoutInflater);
}

-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
  public static * inflate(android.view.LayoutInflater);
  public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

-keep class * implements androidx.viewbinding.ViewBinding {
 public static * bind(android.view.View); public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

-keep class com.google.android.material.** { *; }
-dontwarn kotlin.**

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keep class * implements java.io.Serializable {
  public static final java.io.Serializable *;
}

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.** { *; }

##---------------End: proguard configuration for Gson  ----------

-keep class kotlin.coroutines.Continuation

-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *;}

-keepattributes InnerClasses

-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class com.daimajia.easing.** { *; }
-keep interface com.daimajia.easing.** { *; }
-keepattributes *Annotation*


-keepnames class info.dvkr.screenstream.** { *; }
-keep class **.R$* {
    <fields>;
}

#Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }

#KTOR
-keep class kotlin.reflect.jvm.internal.**
-keep class kotlin.text.RegexOption

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn com.daimajia.easing.Glider
-dontwarn com.daimajia.easing.Skill
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE


-dontwarn kotlin.reflect.jvm.internal.**

-keep class kotlin.reflect.jvm.internal.** { *; }

-keep interface javax.annotation.Nullable

# -printmapping out.map

 -keep public class * {
     public protected *;
 }

 -keepparameternames
 -renamesourcefileattribute SourceFile
 -keepattributes Signature,Exceptions,*Annotation*,
                 InnerClasses,PermittedSubclasses,EnclosingMethod,
                 Deprecated,SourceFile,LineNumberTable

 -keepclasseswithmembernames,includedescriptorclasses class * {
     native <methods>;
 }

 -keepclassmembers,allowoptimization enum * {
     public static **[] values();
     public static ** valueOf(java.lang.String);
 }

 -keepclassmembers class * implements java.io.Serializable {
     static final long serialVersionUID;
     private static final java.io.ObjectStreamField[] serialPersistentFields;
     private void writeObject(java.io.ObjectOutputStream);
     private void readObject(java.io.ObjectInputStream);
     java.lang.Object writeReplace();
     java.lang.Object readResolve();
 }
-dontwarn java.lang.reflect.AnnotatedType
-keep class kotlin.Metadata
-keep class com.shuyu.gsyvideoplayer.video.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.**
-keep class com.shuyu.gsyvideoplayer.video.base.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.video.base.**
-keep class com.shuyu.gsyvideoplayer.utils.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.utils.**
-keep class com.shuyu.gsyvideoplayer.player.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.player.**
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**
-keep class androidx.media3.** {*;}
-keep interface androidx.media3.**

-keep class com.shuyu.alipay.** {*;}
-keep interface com.shuyu.alipay.**

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class com.alivc.**{*;}
-keep class com.aliyun.**{*;}
-keep class com.cicada.**{*;}
-dontwarn com.alivc.**
-dontwarn com.aliyun.**
-dontwarn com.cicada.**
-dontwarn com.google.firebase.ktx.Firebase