# Aperture release rules.
# Services, activities and the accessibility config are referenced from the manifest, so R8 keeps them already.
# Nothing is loaded by reflection. Keep line numbers so crash reports from Play point at real source lines.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
