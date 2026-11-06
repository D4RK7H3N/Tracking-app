# Default ProGuard rules. Release minification is disabled for the demo,
# these rules keep kotlinx.serialization working if you enable it.

-keepattributes *Annotation*, InnerClasses, Signature

# kotlinx.serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.sagay.touristnav.**$$serializer { *; }
-keepclassmembers class com.sagay.touristnav.** {
    *** Companion;
}
-keepclasseswithmembers class com.sagay.touristnav.** {
    kotlinx.serialization.KSerializer serializer(...);
}
