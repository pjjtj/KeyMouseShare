# KeyMouseShare - 跨平台鼠标键盘共享工具

KeyMouseShare是一个跨平台的鼠标键盘共享工具，支持在Windows和Mac系统之间共享鼠标和键盘操作，同时支持文件拖拽共享功能。

## 功能特性

1. **跨平台支持**：支持Windows和Mac操作系统
2. **屏幕布局配置**：可自定义多个屏幕的相对位置关系，通过可视化界面进行配置
3. **鼠标键盘共享**：在多个设备间无缝共享鼠标和键盘操作
4. **文件拖拽共享**：支持跨设备的文件拖拽传输
5. **自动识别**：自动识别局域网内的其他设备
6. **图形界面**：提供友好的图形用户界面

## 技术架构

### 核心组件

- **网络通信模块**：基于Netty实现设备间的稳定通信
- **输入设备监听模块**：监听本地鼠标键盘事件
- **输入设备控制模块**：在远程设备上重现鼠标键盘事件
- **屏幕布局配置模块**：管理多个屏幕的相对位置关系
- **文件传输模块**：实现跨设备的文件传输功能
- **设备发现模块**：自动发现局域网内的其他设备

### 技术栈

- Java 8
- Netty (网络通信)
- Gson (JSON处理)
- SLF4J + Logback (日志处理)
- JNA (本地库调用)
- Swing (图形界面)

## 项目结构

```
KeyMouseShare/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── keymouseshare/
│   │   │           ├── Application.java         # 主程序入口
│   │   │           ├── core/                    # 核心控制器
│   │   │           ├── config/                  # 配置管理
│   │   │           ├── input/                   # 输入事件处理
│   │   │           │   ├── AbstractInputListenerManager.java  # 抽象输入监听管理器
│   │   │           │   ├── BasicInputListenerManager.java     # 基本输入监听管理器
│   │   │           │   ├── WindowsInputListenerManager.java   # Windows输入监听管理器
│   │   │           │   ├── MacInputListenerManager.java       # Mac输入监听管理器
│   │   │           │   ├── InputListenerManagerFactory.java   # 输入监听管理器工厂
│   │   │           │   └── ...                  # 其他输入相关类
│   │   │           ├── network/                 # 网络通信
│   │   │           │   ├── NetworkManager.java   # 网络管理器
│   │   │           │   ├── DeviceInfo.java       # 设备信息类
│   │   │           │   ├── DataPacket.java       # 数据包类
│   │   │           │   ├── ServerHandler.java    # 服务器处理器
│   │   │           │   ├── ClientHandler.java    # 客户端处理器
│   │   │           │   └── ...                   # 其他网络相关类
│   │   │           ├── screen/                  # 屏幕布局管理
│   │   │           ├── filetransfer/            # 文件传输管理
│   │   │           ├── util/                    # 工具类
│   │   │           └── ui/                      # 用户界面
│   │   └── resources/
│   │       └── logback.xml                      # 日志配置
│   └── test/
├── logs/                                        # 日志文件目录
├── pom.xml                                     # Maven配置文件
└── README.md                                   # 项目说明文件
```

## 局域网设备发现机制

KeyMouseShare使用UDP广播机制自动发现局域网内的其他设备：

### 工作原理

1. **UDP广播发现**：
   - 使用UDP协议在端口8889上发送和接收广播消息
   - 定期广播发现消息"KEYMOUSESHARE_DISCOVERY"到局域网
   - 双线程工作机制：一个线程发送广播消息（每3秒一次），另一个线程接收和处理广播消息

2. **设备信息交换**：
   - 发现消息包含设备的详细信息，如设备ID、名称、IP地址、屏幕信息等
   - 使用JSON格式序列化设备信息，便于跨平台解析

3. **设备去重与管理**：
   - 使用设备唯一ID避免重复记录同一设备
   - 定期更新设备时间戳，自动清理离线设备
   - 自动过滤本地设备，避免发现自身

4. **跨平台兼容**：
   - 支持在Windows、Mac和Linux设备间相互发现
   - 适配不同操作系统的网络接口

### 设备信息内容

每个设备广播的信息包括：
- 设备唯一ID（基于MAC地址或UUID）
- 设备名称（主机名）
- IP地址
- 操作系统信息
- 屏幕配置信息
- 时间戳（用于判断在线状态）

## 使用方法

### 编译项目

```bash
mvn clean compile
```

### 运行程序

```bash
# 以图形界面模式运行（默认）
java -cp "target/classes;target/dependency/*" com.keymouseshare.Application

# 以服务器模式运行
java -cp "target/classes;target/dependency/*" com.keymouseshare.Application --server

# 以客户端模式运行
java -cp "target/classes;target/dependency/*" com.keymouseshare.Application --client
```

### 打包项目

```bash
mvn clean package
```

这将生成一个包含所有依赖的可执行JAR文件，位于`target/KeyMouseShare-1.0-SNAPSHOT.jar`。

## 屏幕布局配置

屏幕布局配置功能允许用户可视化地配置多个设备屏幕的相对位置关系。通过"配置屏幕布局"按钮可以打开配置对话框，该对话框提供以下功能：

1. **可视化显示**：以图形化方式显示当前所有设备屏幕的相对位置
2. **添加屏幕**：可以添加新的设备屏幕到布局中
3. **编辑屏幕**：可以修改现有屏幕的属性（名称、尺寸、位置等）
4. **拖拽排列**：可以通过拖拽方式调整屏幕位置
5. **边缘吸附**：拖拽屏幕时支持边缘自动吸附功能