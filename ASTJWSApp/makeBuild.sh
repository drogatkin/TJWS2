#!/bin/bash
# makeBuild.sh
#
# Build Android Studio project (MeetX-H5).
# The debug build is automatically signed with a debug key provided by the SDK tools (it's insecure
# and you cannot publish this APK to Google Play Store), and the release build must be signed with
# your own private key. The DEBUG build APK is created under named module_name-debug.apk in
# project_name/module_name/build/outputs/apk/
#
# Commands:
# ./gradlew assembleDebug
# ./gradlew assembleDebug --debug --stacktrace
#
# OR
#
# ./gradlew assembleRelease
# ./gradlew assembleRelease --debug --stacktrace
#

clear
./gradlew clean build
#./gradlew assembleDebug --debug --stacktrace
#./gradlew assembleRelease --debug --stacktrace
