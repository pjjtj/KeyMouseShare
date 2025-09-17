#!/bin/bash
BASEDIR=$(dirname "$0")
cd $BASEDIR

echo "检查JavaFX运行时环境..."

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
echo "检测到Java版本: $JAVA_VERSION"

# 创建lib目录（如果不存在）
mkdir -p lib

# 下载JavaFX运行时库
if [ ! -f "lib/javafx-controls-21.0.1.jar" ]; then
    echo "正在下载JavaFX运行时库..."
    
    # 根据操作系统下载对应平台的库
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "检测到macOS系统"
        curl -L -o lib/javafx-controls-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1.jar"
        curl -L -o lib/javafx-graphics-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1-mac.jar"
        curl -L -o lib/javafx-controls-21.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1-mac.jar"
        curl -L -o lib/javafx-graphics-21.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1-mac.jar"
        curl -L -o lib/javafx-fxml-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1.jar"
        curl -L -o lib/javafx-fxml-21.0.1-mac.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1-mac.jar"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "检测到Linux系统"
        curl -L -o lib/javafx-controls-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1.jar"
        curl -L -o lib/javafx-graphics-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1-linux.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1-linux.jar"
        curl -L -o lib/javafx-controls-21.0.1-linux.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1-linux.jar"
        curl -L -o lib/javafx-graphics-21.0.1-linux.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1-linux.jar"
        curl -L -o lib/javafx-fxml-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1.jar"
        curl -L -o lib/javafx-fxml-21.0.1-linux.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1-linux.jar"
    else
        echo "检测到Windows系统或未知系统"
        curl -L -o lib/javafx-controls-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1.jar"
        curl -L -o lib/javafx-graphics-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1.jar"
        curl -L -o lib/javafx-base-21.0.1-win.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.1/javafx-base-21.0.1-win.jar"
        curl -L -o lib/javafx-controls-21.0.1-win.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.1/javafx-controls-21.0.1-win.jar"
        curl -L -o lib/javafx-graphics-21.0.1-win.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.1/javafx-graphics-21.0.1-win.jar"
        curl -L -o lib/javafx-fxml-21.0.1.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1.jar"
        curl -L -o lib/javafx-fxml-21.0.1-win.jar "https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21.0.1/javafx-fxml-21.0.1-win.jar"
    fi
fi

# 构建项目
echo "正在构建项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "项目构建失败"
    exit 1
fi

# 设置JavaFX模块路径
JAVAFX_MODULES="lib/javafx-controls-21.0.1.jar:lib/javafx-base-21.0.1.jar:lib/javafx-graphics-21.0.1.jar:lib/javafx-fxml-21.0.1.jar"

# 根据操作系统添加平台特定的jar
if [[ "$OSTYPE" == "darwin"* ]]; then
    JAVAFX_MODULES="$JAVAFX_MODULES:lib/javafx-base-21.0.1-mac.jar:lib/javafx-controls-21.0.1-mac.jar:lib/javafx-graphics-21.0.1-mac.jar:lib/javafx-fxml-21.0.1-mac.jar"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    JAVAFX_MODULES="$JAVAFX_MODULES:lib/javafx-base-21.0.1-linux.jar:lib/javafx-controls-21.0.1-linux.jar:lib/javafx-graphics-21.0.1-linux.jar:lib/javafx-fxml-21.0.1-linux.jar"
else
    JAVAFX_MODULES="$JAVAFX_MODULES:lib/javafx-base-21.0.1-win.jar:lib/javafx-controls-21.0.1-win.jar:lib/javafx-graphics-21.0.1-win.jar:lib/javafx-fxml-21.0.1-win.jar"
fi

# 运行应用程序
echo "正在运行应用程序..."
java --module-path lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target/KeyMouseShare-1.0-SNAPSHOT.jar:$JAVAFX_MODULES" com.keymouseshare.MainApplication