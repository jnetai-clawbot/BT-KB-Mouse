# Bluetooth HID classes
-keep class android.bluetooth.BluetoothHidDevice$Callback { *; }
-keep class android.bluetooth.BluetoothHidDevice { *; }
-keep class android.bluetooth.BluetoothHidDeviceAppSdpSettings { *; }
-keep class android.bluetooth.BluetoothHidDeviceAppQosSettings { *; }

# Room entities
-keep class com.jnetaol.btkbmouse.data.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
