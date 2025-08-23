@echo off
chcp 65001 > nul
setlocal

echo 正在运行KeyMouseShare应用程序...

java ^
  --module-path "target/lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -Dsun.java2d.uiScale=1.0 ^
  -Dsun.java2d.dpiaware=false ^
  -Djavafx.css.scalingFactor=1.0 ^
  -Dprism.allowhidpi=false ^
  -Dfile.encoding=UTF-8 ^
  -cp "target/lib/*;target/KeyMouseShare-1.0-SNAPSHOT.jar" ^
  com.keymouseshare.MainApplication

if %errorlevel% neq 0 (
    echo.
    echo 运行失败，请检查以下几点：
    echo 1. 确保已执行 mvn package 命令构建项目
    echo 2. 确保Java环境已正确配置
    echo 3. 确保JavaFX依赖已正确添加
    echo.
    pause
)
