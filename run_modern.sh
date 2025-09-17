#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR

# 检查Maven是否可用
if ! command -v mvn &> /dev/null
then
    echo "Maven未安装或未在PATH中配置"
    echo "请先安装Maven或将其添加到PATH环境变量中"
    exit 1
fi

echo "使用Maven运行项目（推荐方式）"

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
echo "检测到Java版本: $JAVA_VERSION"

# 如果Java版本大于等于11，使用OpenJFX插件运行
if [ "$JAVA_VERSION" -ge 11 ]; then
    echo "使用JavaFX Maven插件运行..."
    mvn javafx:run
else
    echo "Java版本较低，尝试使用传统方式运行..."
    # 尝试构建并运行
    mvn clean package
    if [ $? -eq 0 ]; then
        java -jar target/KeyMouseShare-1.0-SNAPSHOT.jar
    else
        echo "构建失败，请检查项目配置"
        exit 1
    fi
fi