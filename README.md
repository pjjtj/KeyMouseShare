# KeyMouseShare 跨平台键鼠共享客户端

这是一个跨平台的键鼠共享客户端，允许用户通过网络在多个设备之间共享键盘和鼠标控制。

## 功能特性

- 跨平台支持（Windows、macOS、Linux）
- 实时设备状态显示
- 屏幕预览功能
- 客户端/服务器模式
- 多屏支持

## 技术架构

- **GUI框架**: JavaFX（跨平台图形界面）
- **网络通信**: Netty（基于TCP协议）
- **系统调用**: JNA（捕获键盘/鼠标事件）
- **数据交换**: Gson（JSON序列化/反序列化）
- **日志系统**: SLF4J + Logback

## 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本

## 构建和运行

### 方法1：使用Maven运行

```bash
# 编译项目
mvn compile

# 运行应用程序
mvn javafx:run
```

### 方法2：打包成Jar包运行

```bash
# 打包项目
mvn clean package

# 运行Jar包
./run_jar.sh
```

### 方法3：直接编译运行

```bash
# 使用简化脚本运行
./run_simple.sh
```

## 项目结构

```
KeyMouseShare/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── keymouseshare/
│       │           ├── MainApplication.java      # 主应用程序入口
│       │           ├── ui/
│       │           │   ├── DeviceListUI.java     # 设备列表界面
│       │           │   └── ScreenPreviewUI.java  # 屏幕预览界面
│       │           ├── network/                  # 网络通信模块
│       │           ├── input/                    # 输入捕获模块
│       │           └── service/                  # 业务逻辑模块
│       └── resources/                            # 资源文件
├── target/                                       # 构建输出目录
├── pom.xml                                       # Maven配置文件
├── run_jar.sh                                    # Jar包运行脚本
├── run_simple.sh                                 # 简化版运行脚本
└── README.md                                     # 项目说明文件
```

## 界面说明

### 设备列表（左侧）

- 显示连接的设备IP地址
- 状态指示器：
  - 绿色"C"：已连接的客户端
  - 绿色"S"：已连接的服务器
  - 橙色"C"：未连接的客户端
- 选中设备会有高亮显示

### 屏幕预览（右侧）

- 显示各设备的屏幕预览
- 不同设备用不同颜色区分
- 当前选中设备有蓝色边框标识

### 操作按钮（底部）

- 启动服务器：将当前设备作为服务器启动

## 开发指南

1. UI界面组件位于 [src/main/java/com/keymouseshare/ui](src/main/java/com/keymouseshare/uifx) 目录下
2. 网络通信相关代码位于 [src/main/java/com/keymouseshare/network](src/main/java/com/keymouseshare/network) 目录下
3. 输入捕获相关代码位于 [src/main/java/com/keymouseshare/input](src/main/java/com/keymouseshare/input) 目录下

## 许可证

[待定]