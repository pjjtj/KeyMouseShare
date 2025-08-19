#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR

# 检查jar文件是否存在
if [ ! -f "target/KeyMouseShare-1.0-SNAPSHOT.jar" ]; then
    echo "Jar文件不存在，请先运行 mvn package 命令进行打包"
    exit 1
fi

# 构建类路径，包含所有依赖
CP="target/KeyMouseShare-1.0-SNAPSHOT.jar"
for jar in target/lib/*.jar; do
    CP="$CP:$jar"
done

# 运行应用程序
java -cp "$CP" com.keymouseshare.MainApplication