# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK location.
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}
