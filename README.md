# KeyMouseShare 跨平台键鼠共享客户端

这是一个跨平台的键鼠共享客户端，允许用户通过网络在多个设备之间共享键盘和鼠标控制。

## 功能特性

- 跨平台支持（Windows、macOS、Linux）
- 实时设备状态显示
- 屏幕预览功能
- 客户端/服务器模式
- 多屏支持
- 虚拟桌面配置
- 全局键盘鼠标事件监听
- 设备自动发现

## 技术架构

- **GUI框架**: JavaFX（跨平台图形界面）
- **网络通信**: Netty（基于TCP协议）+ UDP（设备发现）
- **系统调用**: JNA（捕获键盘/鼠标事件）
- **数据交换**: Gson（JSON序列化/反序列化）
- **日志系统**: SLF4J + Logback
- **全局事件监听**: JNativeHook

## 环境要求

- Java 21 或更高版本
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
│       │           ├── bean/                     # 数据模型类
│       │           ├── keyboard/                 # 键盘鼠标控制模块
│       │           ├── listener/                 # 事件监听器
│       │           ├── network/                  # 网络通信模块
│       │           ├── storage/                  # 数据存储模块
│       │           ├── uifx/                     # UI界面组件
│       │           └── util/                     # 工具类
│       └── resources/                            # 资源文件
├── target/                                       # 构建输出目录
├── pom.xml                                       # Maven配置文件
├── run_app.bat                                   # Windows运行脚本
├── run_jar.sh                                    # Jar包运行脚本
├── run_simple.sh                                 # 简化版运行脚本
└── README.md                                     # 项目说明文件
```

## 核心模块说明

### bean（数据模型）

包含项目中使用的主要数据类：
- [DeviceInfo](src/main/java/com/keymouseshare/bean/DeviceInfo.java)：设备信息
- [ScreenInfo](src/main/java/com/keymouseshare/bean/ScreenInfo.java)：屏幕信息
- [ControlEvent](src/main/java/com/keymouseshare/bean/ControlEvent.java)：控制事件
- [DiscoveryMessage](src/main/java/com/keymouseshare/bean/DiscoveryMessage.java)：设备发现消息

### keyboard（键盘鼠标控制）

跨平台键盘鼠标控制实现：
- [MouseKeyBoardFactory](src/main/java/com/keymouseshare/keyboard/MouseKeyBoardFactory.java)：鼠标键盘工厂类
- [BaseMouseKeyBoard](src/main/java/com/keymouseshare/keyboard/BaseMouseKeyBoard.java)：基础实现类
- 各平台具体实现（win/mac/nux）

### listener（事件监听）

事件监听器接口和实现：
- [DeviceListener](src/main/java/com/keymouseshare/listener/DeviceListener.java)：设备状态监听
- [JNativeHookInputMonitor](src/main/java/com/keymouseshare/listener/JNativeHookInputMonitor.java)：全局输入监听
- [VirtualDesktopStorageListener](src/main/java/com/keymouseshare/listener/VirtualDesktopStorageListener.java)：虚拟桌面存储监听

### network（网络通信）

网络通信相关类：
- [DeviceDiscovery](src/main/java/com/keymouseshare/network/DeviceDiscovery.java)：UDP设备发现
- [ControlServer](src/main/java/com/keymouseshare/network/ControlServer.java)：控制服务端
- [ControlClient](src/main/java/com/keymouseshare/network/ControlClient.java)：控制客户端
- [ControlRequestManager](src/main/java/com/keymouseshare/network/ControlRequestManager.java)：控制请求管理

### storage（数据存储）

数据存储和管理类：
- [DeviceStorage](src/main/java/com/keymouseshare/storage/DeviceStorage.java)：设备存储
- [VirtualDesktopStorage](src/main/java/com/keymouseshare/storage/VirtualDesktopStorage.java)：虚拟桌面存储

### uifx（UI界面）

JavaFX用户界面组件：
- [DeviceListUI](src/main/java/com/keymouseshare/uifx/DeviceListUI.java)：设备列表界面
- [ScreenPreviewUI](src/main/java/com/keymouseshare/uifx/ScreenPreviewUI.java)：屏幕预览界面
- [MousePositionDisplay](src/main/java/com/keymouseshare/uifx/MousePositionDisplay.java)：鼠标位置显示
- [FullScreenOverlay](src/main/java/com/keymouseshare/uifx/FullScreenOverlay.java)：全屏覆盖层

### util（工具类）

各种工具类：
- [NetUtil](src/main/java/com/keymouseshare/util/NetUtil.java)：网络工具类
- [DeviceTools](src/main/java/com/keymouseshare/util/DeviceTools.java)：设备工具类
- [MouseEdgeDetector](src/main/java/com/keymouseshare/util/MouseEdgeDetector.java)：鼠标边缘检测

## 界面说明

### 设备列表（左侧）

- 显示局域网内发现的设备IP地址
- 状态指示器：
  - 绿色"C"：已连接的客户端
  - 绿色"S"：已连接的服务器
  - 橙色"C"：未连接的客户端
  - 橙色"S"：未连接的服务端
- 选中设备会有高亮显示
- 支持启动本地服务端

### 屏幕预览（中心）

- 显示各设备的屏幕布局预览
- 不同设备用不同颜色区分
- 当前选中设备有蓝色边框标识
- 支持鼠标滚轮缩放（配合Ctrl键）
- 支持拖拽调整屏幕位置
- 支持屏幕位置吸附对齐

### 鼠标位置显示（底部）

- 实时显示鼠标在虚拟桌面中的坐标
- 显示当前鼠标所在的屏幕信息

## 开发指南

1. UI界面组件位于 [src/main/java/com/keymouseshare/uifx](src/main/java/com/keymouseshare/uifx) 目录下
2. 网络通信相关代码位于 [src/main/java/com/keymouseshare/network](src/main/java/com/keymouseshare/network) 目录下
3. 输入捕获相关代码位于 [src/main/java/com/keymouseshare/input](src/main/java/com/keymouseshare/input) 目录下

## 功能
- [x] 局域网设备发现
- [ ] 控制中心
  - [x] Windows 控制主机
  - [ ] MacOS 控制主机
  - [ ] Linux 控制主机
- [ ] 虚拟桌面
  - [x] 布局控制
  - [x] 扩展
- [x] 鼠标键盘共享
  - [x] Windows 从机
  - [x] MacOS 从机
  - [x] Linux 从机
- [ ] 数据传输
  - [ ] 剪切板共享
    - [ ] 文本
    - [ ] 文件
    - [ ] 图片信息 
  - [ ] 文件拖拽

## 许可证

[待定]