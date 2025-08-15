package com.keymouseshare.network;

import com.google.gson.Gson;
import com.keymouseshare.core.Controller;
import com.keymouseshare.filetransfer.FileDataPacket;
import com.keymouseshare.filetransfer.FileTransferRequest;
import com.keymouseshare.filetransfer.FileTransferResponse;
import com.keymouseshare.screen.ScreenInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络管理器
 * 负责设备发现、连接管理等网络功能
 */
public class NetworkManager {
    private static final int DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "KEYMOUSESHARE_DISCOVERY";
    private static final String DISCOVERY_RESPONSE_MESSAGE = "KEYMOUSESHARE_DISCOVERY_RESPONSE";
    
    private Controller controller;
    private ScheduledExecutorService discoveryScheduler;
    private DatagramSocket discoverySocket;
    private boolean isServerRunning = false;
    private ServerBootstrap serverBootstrap;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Gson gson = new Gson();
    
    // 存储发现的设备信息
    private Map<String, DeviceInfo> discoveredDevices = new ConcurrentHashMap<>();
    
    // 存储已连接的设备
    private Map<String, DeviceInfo> connectedDevices = new ConcurrentHashMap<>();
    
    // 本地设备信息
    private DeviceInfo localDeviceInfo;
    
    public NetworkManager(Controller controller) {
        this.controller = controller;
        // 注意：此时Controller的其他组件可能尚未初始化完成，推迟初始化到start方法
    }
    
    /**
     * 初始化本地设备信息
     */
    private void initializeLocalDeviceInfo() {
        // 确保Controller的ScreenLayoutManager已经初始化
        if (controller.getScreenLayoutManager() == null) {
            System.err.println("ScreenLayoutManager not initialized yet, using empty screen list");
        }
        
        localDeviceInfo = new DeviceInfo();
        localDeviceInfo.setDeviceId(getLocalDeviceId());
        localDeviceInfo.setDeviceName(getLocalDeviceName());
        localDeviceInfo.setIpAddress(getLocalIpAddress());
        localDeviceInfo.setOsName(System.getProperty("os.name"));
        localDeviceInfo.setOsVersion(System.getProperty("os.version"));
        
        // 安全地设置屏幕信息
        if (controller.getScreenLayoutManager() != null) {
            localDeviceInfo.setScreens(controller.getScreenLayoutManager().getAllScreens());
        } else {
            localDeviceInfo.setScreens(new ArrayList<>());
        }
    }
    
    /**
     * 获取本地设备ID
     */
    private String getLocalDeviceId() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        return sb.toString();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error getting local device ID: " + e.getMessage());
        }
        // 如果无法获取MAC地址，使用UUID
        return UUID.randomUUID().toString();
    }
    
    /**
     * 获取本地设备名称
     */
    private String getLocalDeviceName() {
        String hostname = System.getenv("COMPUTERNAME"); // Windows
        if (hostname == null) {
            hostname = System.getenv("HOSTNAME"); // Unix/Linux
        }
        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostname = "Unknown";
            }
        }
        return hostname;
    }
    
    /**
     * 获取本地IP地址
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && address.isSiteLocalAddress()) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error getting local IP address: " + e.getMessage());
        }
        return "127.0.0.1";
    }
    
    /**
     * 启动网络管理器
     */
    public void start() {
        // 延迟初始化本地设备信息，确保Controller的所有组件都已初始化
        if (localDeviceInfo == null) {
            initializeLocalDeviceInfo();
        }
        startDeviceDiscovery();
    }
    
    /**
     * 启动设备发现功能
     */
    private void startDeviceDiscovery() {
        try {
            discoverySocket = new DatagramSocket(DISCOVERY_PORT);
            discoveryScheduler = Executors.newScheduledThreadPool(3);
            
            // 启动发送广播消息的线程
            discoveryScheduler.scheduleAtFixedRate(() -> {
                try {
                    sendDiscoveryBroadcast();
                } catch (Exception e) {
                    System.err.println("Error sending discovery broadcast: " + e.getMessage());
                }
            }, 0, 3, TimeUnit.SECONDS);
            
            // 启动接收广播消息的线程
            discoveryScheduler.execute(this::receiveDiscoveryBroadcast);
            
            // 启动设备清理线程（定期清理离线设备）
            discoveryScheduler.scheduleAtFixedRate(() -> {
                try {
                    cleanupOfflineDevices();
                } catch (Exception e) {
                    System.err.println("Error cleaning up offline devices: " + e.getMessage());
                }
            }, 5, 5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            System.err.println("Failed to start device discovery: " + e.getMessage());
        }
    }
    
    /**
     * 发送设备发现广播
     */
    private void sendDiscoveryBroadcast() {
        try {
            // 发送简单的发现消息
            byte[] discoveryData = DISCOVERY_MESSAGE.getBytes();
            
            // 获取所有网络接口并发送广播
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过禁用或虚拟接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                
                // 遍历接口的所有地址
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        // 发送到该接口的广播地址
                        DatagramPacket sendPacket = new DatagramPacket(
                            discoveryData, discoveryData.length, broadcast, DISCOVERY_PORT);
                        discoverySocket.send(sendPacket);
                    }
                }
            }
            
            // 同时发送包含详细设备信息的响应消息
            // 更新本地设备信息，包括最新的屏幕配置
            if (localDeviceInfo != null) {
                localDeviceInfo.updateTimestamp();
                // 安全地更新屏幕信息
                if (controller.getScreenLayoutManager() != null) {
                    localDeviceInfo.setScreens(controller.getScreenLayoutManager().getAllScreens());
                }
                
                String deviceInfoJson = gson.toJson(localDeviceInfo);
                String responseMessage = DISCOVERY_RESPONSE_MESSAGE + ":" + deviceInfoJson;
                byte[] responseData = responseMessage.getBytes();
                
                // 广播设备信息
                interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    
                    if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                        continue;
                    }
                    
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            DatagramPacket sendPacket = new DatagramPacket(
                                responseData, responseData.length, broadcast, DISCOVERY_PORT);
                            discoverySocket.send(sendPacket);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending broadcast: " + e.getMessage());
        }
    }
    
    /**
     * 接收设备发现广播
     */
    private void receiveDiscoveryBroadcast() {
        try {
            byte[] receiveData = new byte[4096]; // 增大缓冲区以容纳设备信息
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            while (!discoverySocket.isClosed()) {
                discoverySocket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                
                String deviceIp = receivePacket.getAddress().getHostAddress();
                
                // 检查是否为本地IP地址，避免发现自身
                if (isLocalAddress(deviceIp)) {
                    continue;
                }
                
                // 处理发现消息
                if (message.equals(DISCOVERY_MESSAGE)) {
                    // 收到发现请求，发送设备信息响应
                    sendDiscoveryResponse(deviceIp);
                } 
                // 处理发现响应消息
                else if (message.startsWith(DISCOVERY_RESPONSE_MESSAGE + ":")) {
                    String deviceInfoJson = message.substring((DISCOVERY_RESPONSE_MESSAGE + ":").length());
                    try {
                        DeviceInfo deviceInfo = gson.fromJson(deviceInfoJson, DeviceInfo.class);
                        updateDiscoveredDevice(deviceInfo);
                    } catch (Exception e) {
                        System.err.println("Error parsing device info: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (!discoverySocket.isClosed()) {
                System.err.println("Error receiving broadcast: " + e.getMessage());
            }
        }
    }
    
    /**
     * 发送设备发现响应
     */
    private void sendDiscoveryResponse(String targetIp) {
        try {
            // 确保本地设备信息已初始化
            if (localDeviceInfo == null) {
                initializeLocalDeviceInfo();
            }
            
            // 更新本地设备信息，包括最新的屏幕配置
            if (localDeviceInfo != null) {
                localDeviceInfo.updateTimestamp();
                // 安全地更新屏幕信息
                if (controller.getScreenLayoutManager() != null) {
                    localDeviceInfo.setScreens(controller.getScreenLayoutManager().getAllScreens());
                }
                
                String deviceInfoJson = gson.toJson(localDeviceInfo);
                String responseMessage = DISCOVERY_RESPONSE_MESSAGE + ":" + deviceInfoJson;
                byte[] responseData = responseMessage.getBytes();
                
                InetAddress targetAddress = InetAddress.getByName(targetIp);
                DatagramPacket sendPacket = new DatagramPacket(
                    responseData, responseData.length, targetAddress, DISCOVERY_PORT);
                discoverySocket.send(sendPacket);
            }
        } catch (Exception e) {
            System.err.println("Error sending discovery response: " + e.getMessage());
        }
    }
    
    /**
     * 更新发现的设备信息
     */
    private void updateDiscoveredDevice(DeviceInfo deviceInfo) {
        String deviceId = deviceInfo.getDeviceId();
        DeviceInfo existingDevice = discoveredDevices.get(deviceId);
        
        if (existingDevice == null) {
            // 新设备
            discoveredDevices.put(deviceId, deviceInfo);
            System.out.println("Discovered new device: " + deviceInfo);
            // 通知控制器发现新设备
            controller.getMainWindow().onDeviceDiscovered(deviceInfo.getIpAddress());
        } else {
            // 更新现有设备的时间戳
            existingDevice.updateTimestamp();
            existingDevice.setScreens(deviceInfo.getScreens());
            existingDevice.setIpAddress(deviceInfo.getIpAddress());
        }
    }
    
    /**
     * 清理离线设备
     */
    private void cleanupOfflineDevices() {
        Iterator<Map.Entry<String, DeviceInfo>> iterator = discoveredDevices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DeviceInfo> entry = iterator.next();
            DeviceInfo deviceInfo = entry.getValue();
            
            if (!deviceInfo.isOnline()) {
                System.out.println("Removing offline device: " + deviceInfo.getDeviceName());
                iterator.remove();
                
                // 通知屏幕布局管理器移除该设备的屏幕
                controller.getScreenLayoutManager().removeRemoteScreens(deviceInfo.getDeviceId());
            }
        }
    }
    
    /**
     * 检查IP地址是否为本地地址
     */
    private boolean isLocalAddress(String ip) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (ip.equals(address.getHostAddress())) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error checking local addresses: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 启动服务器模式
     */
    public void startServer() {
        try {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            serverBootstrap = new ServerBootstrap();
            
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ServerHandler(controller));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // 绑定端口并启动服务器
            serverChannel = serverBootstrap.bind(8888).sync().channel();
            isServerRunning = true;
            
            // 更新本地设备信息为服务器模式
            if (localDeviceInfo != null) {
                localDeviceInfo.setPort(8888);
            }
            
            System.out.println("Server started on port 8888");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
    
    /**
     * 停止服务器模式
     */
    public void stopServer() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            
            isServerRunning = false;
            System.out.println("Server stopped");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * 连接到指定服务器
     */
    public void connectToServer(String serverIp) {
        try {
            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ClientHandler(controller));
                        }
                    });
            
            Channel channel = bootstrap.connect(serverIp, 8888).sync().channel();
            System.out.println("Connected to server: " + serverIp);
            
            // 添加到已连接设备列表
            DeviceInfo connectedDevice = new DeviceInfo();
            connectedDevice.setIpAddress(serverIp);
            connectedDevice.setDeviceId(serverIp); // 简化处理
            connectedDevice.setPort(8888);
            connectedDevices.put(serverIp, connectedDevice);
            
        } catch (Exception e) {
            System.err.println("Failed to connect to server " + serverIp + ": " + e.getMessage());
        }
    }
    
    /**
     * 停止网络管理器
     */
    public void stop() {
        try {
            // 停止设备发现
            if (discoverySocket != null && !discoverySocket.isClosed()) {
                discoverySocket.close();
            }
            
            if (discoveryScheduler != null) {
                discoveryScheduler.shutdown();
            }
            
            // 停止服务器（如果正在运行）
            if (isServerRunning) {
                stopServer();
            }
            
            discoveredDevices.clear();
            connectedDevices.clear();
        } catch (Exception e) {
            System.err.println("Error stopping network manager: " + e.getMessage());
        }
    }
    
    /**
     * 发送文件传输请求
     */
    public void sendFileTransferRequest(FileTransferRequest request) {
        // 实现文件传输请求发送逻辑
        System.out.println("Sending file transfer request: " + request.getFileName());
    }
    
    /**
     * 发送文件传输响应
     */
    public void sendFileTransferResponse(FileTransferResponse response) {
        // 实现文件传输响应发送逻辑
        System.out.println("Sending file transfer response for file: " + response.getFileId());
    }
    
    /**
     * 发送文件数据
     */
    public void sendFileData(FileDataPacket packet, String targetDeviceId) {
        // 实现文件数据发送逻辑
        System.out.println("Sending file data packet, offset: " + packet.getOffset() + 
                          ", length: " + packet.getLength());
    }
    
    /**
     * 发送数据包
     */
    public void sendDataPacket(DataPacket packet) {
        // 实现数据包发送逻辑
        System.out.println("Sending data packet: " + packet.getType() + " to device: " + packet.getDeviceId());
        // 在实际实现中，这里会通过Netty发送数据包到指定设备
    }
    
    /**
     * 获取发现的设备列表
     */
    public Collection<DeviceInfo> getDiscoveredDevices() {
        return discoveredDevices.values();
    }
    
    /**
     * 获取已连接的设备列表
     */
    public Collection<DeviceInfo> getConnectedDevices() {
        return connectedDevices.values();
    }
    
    /**
     * 当设备被允许控制时调用此方法
     */
    public void onDeviceControlAllowed(DeviceInfo deviceInfo) {
        if (deviceInfo != null) {
            // 将设备添加到已连接设备列表
            connectedDevices.put(deviceInfo.getDeviceId(), deviceInfo);
            
            // 通知屏幕布局管理器添加该设备的屏幕
            controller.getScreenLayoutManager().addRemoteScreens(deviceInfo);
            
            System.out.println("Device control allowed for: " + deviceInfo.getDeviceName());
        }
    }
    
    // Getter方法
    public boolean isServerRunning() {
        return isServerRunning;
    }
    
    public DeviceInfo getLocalDeviceInfo() {
        return localDeviceInfo;
    }
}