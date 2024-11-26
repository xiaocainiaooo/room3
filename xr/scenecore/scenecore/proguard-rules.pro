# Keep all classes in the top and impl packages since they can be created by native code.
-keep class androidx.xr.scenecore.** { *; }
-keep class androidx.xr.scenecore.**$* { *; }
-keep class androidx.xr.scenecore.impl** { *; }
-keep class androidx.xr.scenecore.impl**$* { *; }
-keep class * extends androidx.xr.scenecore.** { *; }
-keep class * extends androidx.xr.scenecore.**$* { *; }
-keep class * extends androidx.xr.scenecore.impl** { *; }
-keep class * extends androidx.xr.scenecore.impl**$* { *; }
