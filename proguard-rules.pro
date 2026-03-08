# 1. IGNORE THE REST OF THE MOD (Crucial so Forge doesn't crash)
-keep class !com.github.ptran779.breach_ptc.email.** { *; }

# 2. THE VAULT DOORS (Keep only the public classes, methods, and fields)
-keep public class com.github.ptran779.breach_ptc.email.** {
    public *;
}

# (Notice what's missing? We don't have to explicitly tell it to scramble protected/private.
# ProGuard destroys EVERYTHING that isn't explicitly protected by a -keep rule.)

# 3. SAFETY RULES FOR FORGE
-dontshrink
-dontoptimize
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,EnclosingMethod