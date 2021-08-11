## witmotionDemoAndroidApp
# What is it
This is the source of the Android app demonstrating use of Witmotion IMU sensors
The original version of this project was received from Witmotion support, but I removed the signing key store and credentials.
It is made of 3 modules (subprojects) :
- Main (formerly wtapp) : the main entry point of the app
- Util (formerly WTFile) : provides File handling (to save data ?) as well as a "SharedUtil" static class used by the main module
- BT901 : handles all sensor-related operations

# How to compile
To compile the app, the APK will have to be signed, so please create an upload key and keystore by following the procedure at https://developer.android.com/studio/publish/app-signing#generate-key
Then create or edit a file called local.properties at the root of the project so it contains the following lines:
sdk.dir=<Path of Android SDKs>
storeFile=<Path of your Java KeyStore (.jks) file>
storePassword=<password of the store file>
keyAlias=<alias of the upload key>
keyPassword=<password of the upload key>

# About WitMotion sensors
For documentation please see https://github.com/WITMOTION