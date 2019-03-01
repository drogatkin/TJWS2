#!/bin/sh
# This script works only when the device is rooted.
# Author: Rohtash Singh Lakra (rohtash.singh@gmail.com)

adb push ./TJWSApp/build/outputs/apk/debug/TJWSApp-debug.apk /sdcard/
adb shell su -c 'rm /system/app/TJWSApp-debug.apk'
adb shell su -c 'cp /sdcard/TJWSApp-debug.apk /system/app/'
sleep 5
adb shell am start com.rslakra.android.tjwsasapp/.SplashActivity