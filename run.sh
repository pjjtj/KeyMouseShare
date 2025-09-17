#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR

# 创建类路径
CP="src/main/java:target/classes"

# 添加依赖到类路径
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
javac -cp "$CP" -d target/classes src/main/java/com/keymouseshare/MainApplication.java src/main/java/com/keymouseshare/ui/*.java

# 运行应用程序
java -cp "$CP:target/classes" com.keymouseshare.MainApplication