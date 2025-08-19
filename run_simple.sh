#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR

# 下载JavaFX库
echo "正在检查JavaFX库..."
mkdir -p lib
cd lib

# 检查是否已下载JavaFX库
if [ ! -f "javafx-controls-17.0.1.jar" ]; then
  echo "正在从Maven仓库下载JavaFX库..."
  curl -L -o javafx-controls-17.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/17.0.1/javafx-controls-17.0.1.jar"
  curl -L -o javafx-base-17.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/17.0.1/javafx-base-17.0.1.jar"
  curl -L -o javafx-graphics-17.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/17.0.1/javafx-graphics-17.0.1.jar"
  
  # 根据操作系统下载对应平台的库
  if [[ "$OSTYPE" == "darwin"* ]]; then
    curl -L -o javafx-base-17.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/17.0.1/javafx-base-17.0.1-mac.jar"
    curl -L -o javafx-controls-17.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/17.0.1/javafx-controls-17.0.1-mac.jar"
    curl -L -o javafx-graphics-17.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/17.0.1/javafx-graphics-17.0.1-mac.jar"
  fi
fi

cd ..

# 创建类路径
CP="src/main/java:target/classes:lib/*"

# 添加Maven仓库中的依赖到类路径
for jar in ~/.m2/repository/io/netty/netty-all/4.1.68.Final/*.jar; do
    CP="$CP:$jar"
done

for jar in ~/.m2/repository/com/google/code/gson/gson/2.8.8/*.jar; do
    CP="$CP:$jar"
done

for jar in ~/.m2/repository/org/slf4j/slf4j-api/1.7.32/*.jar; do
    CP="$CP:$jar"
done

for jar in ~/.m2/repository/ch/qos/logback/logback-classic/1.2.6/*.jar; do
    CP="$CP:$jar"
done

for jar in ~/.m2/repository/net/java/dev/jna/jna/5.8.0/*.jar; do
    CP="$CP:$jar"
done

for jar in ~/.m2/repository/net/java/dev/jna/jna-platform/5.8.0/*.jar; do
    CP="$CP:$jar"
done

# 编译Java文件
mkdir -p target/classes
javac --module-path lib --add-modules javafx.controls,javafx.graphics,javafx.base -cp "$CP" -d target/classes src/main/java/com/keymouseshare/MainApplication.java src/main/java/com/keymouseshare/ui/*.java

# 检查编译是否成功
if [ $? -eq 0 ]; then
    echo "编译成功！"
    # 运行应用程序
    java --module-path lib --add-modules javafx.controls,javafx.graphics,javafx.base -cp "$CP:target/classes" com.keymouseshare.MainApplication
else
    echo "编译失败，请检查代码。"
fi