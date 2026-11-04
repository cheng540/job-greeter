#!/bin/bash
set -e

SDK="E:/develop/android-sdk"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-34/android.jar"
JDK="E:/develop/jdk-21"
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

cd "E:/WorkBuddy_work/BOSS话术助手/app"
rm -rf build
mkdir -p build/classes build/gen build/dex

echo "[1/7] 编译资源"
"$BT/aapt2.exe" compile --dir res -o build/res.zip

echo "[2/7] 链接资源与清单"
"$BT/aapt2.exe" link \
  -o build/base.apk \
  -I "$JAR" \
  --manifest AndroidManifest.xml \
  -R build/res.zip \
  --java build/gen \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 26 --version-name 3.5 \
  --auto-add-overlay

echo "[3/7] 编译 Java"
find src build/gen -name "*.java" > build/sources.txt
"$JDK/bin/javac.exe" -J-Duser.language=en --release 8 -encoding UTF-8 -nowarn \
  -classpath "$JAR" -d build/classes @build/sources.txt
if [ ! -f build/classes/com/liule/talkhelper/MainActivity.class ]; then
  echo "!! Java 编译失败"; exit 1
fi

echo "[4/7] 转 dex"
find build/classes -name "*.class" > build/classes.txt
"$BT/d8.bat" --min-api 26 --lib "$JAR" --output build/dex @build/classes.txt

echo "[5/7] 打包 dex"
E:/develop/Python/python.exe -c "
import zipfile, shutil, os
shutil.copy('build/base.apk', 'build/unsigned.apk')
z = zipfile.ZipFile('build/unsigned.apk', 'a', zipfile.ZIP_DEFLATED)
z.write('build/dex/classes.dex', 'classes.dex')
z.close()
print('  classes.dex 已加入')
"

echo "[6/7] 对齐"
"$BT/zipalign.exe" -f -p 4 build/unsigned.apk build/aligned.apk

echo "[7/7] 签名"
if [ ! -f debug.keystore ]; then
  "$JDK/bin/keytool.exe" -genkeypair -keystore debug.keystore -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi
"$BT/apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out "E:/WorkBuddy_work/BOSS话术助手/打招呼助手.apk" build/aligned.apk

echo ""
echo "=== 完成 ==="
ls -la "E:/WorkBuddy_work/BOSS话术助手/打招呼助手.apk"
