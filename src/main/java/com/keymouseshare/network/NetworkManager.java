package com.keymouseshare.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import com.keymouseshare.core.Controller;
import com.keymouseshare.input.InputEvent;
import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 网络管理器，负责处理设备间的通信
 */
public class NetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Channel clientChannel;
    private Controller controller;
    private DeviceDiscovery deviceDiscovery;
    private ConcurrentHashMap<String, Channel> clientChannels;
    private boolean deviceDiscoveryStarted = false;
    
    public NetworkManager(Controller controller) {
        this.controller = controller;
        this.clientChannels = new ConcurrentHashMap<>();
        this.deviceDiscovery = new DeviceDiscovery(); // 使用默认端口
    }
    
    /**
     * 启动服务器模式
     * @param port 监听端口
     */
    public void startServer(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new InputEventDecoder());
                     ch.pipeline().addLast(new InputEventEncoder());
                     ch.pipeline().addLast(new InputEventHandler(controller));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // 绑定端口并开始接收连接
            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();
            logger.info("Server started on port {}", port);
            
            // 通知控制器服务器已启动
            controller.onServerStarted();
            
            // 记录服务器IP地址信息
            logServerAddresses(port);
            
            // 启动设备发现
            startDeviceDiscovery();
        } catch (InterruptedException e) {
            logger.error("Failed to start server", e);
        }
    }
    
    /**
     * 记录服务器地址信息
     * @param port 服务器端口
     */
    private void logServerAddresses(int port) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            logger.info("Server is listening on port {}. Available server addresses:", port);
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    networkInterface.getInterfaceAddresses().stream()
                        .filter(addr -> addr.getAddress() instanceof java.net.Inet4Address)
                        .forEach(addr -> {
                            logger.info("  {}:{}", addr.getAddress().getHostAddress(), port);
                        });
                }
            }
        } catch (SocketException e) {
            logger.warn("Failed to get server addresses", e);
        }
    }
    
    /**
     * 启动客户端模式
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public void startClient(String host, int port) {
        workerGroup = new NioEventLoopGroup();
        
        try {
            logger.info("Attempting to connect to server {}:{}", host, port);
            
            // 检查网络连接性
            checkNetworkConnectivity(host, port);
            
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new InputEventDecoder());
                     ch.pipeline().addLast(new InputEventEncoder());
                     ch.pipeline().addLast(new InputEventHandler(controller));
                 }
             });
            
            // 连接到服务器
            ChannelFuture f = b.connect(new InetSocketAddress(host, port)).sync();
            clientChannel = f.channel();
            logger.info("Connected to server {}:{}", host, port);
            
            // 通知控制器客户端已连接
            controller.onClientConnected();
            
            DeviceConfig.Device serverDevice = new DeviceConfig.Device();
            String deviceId = "server-" + host.replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + ":" + port;
            serverDevice.setDeviceId(deviceId != null ? deviceId : UUID.randomUUID().toString());
            serverDevice.setDeviceName("Server (" + host + ":" + port + ")");
            serverDevice.setIpAddress(host);
            // 设置默认屏幕尺寸
            serverDevice.setScreenWidth(1920);
            serverDevice.setScreenHeight(1080);
            // 设置默认网络位置
            serverDevice.setNetworkX(0);
            serverDevice.setNetworkY(0);
            controller.onClientConnected(serverDevice);
            
            // 启动设备发现
            startDeviceDiscovery();
        } catch (InterruptedException e) {
            logger.error("Failed to connect to server {}:{}", host, port, e);
        } catch (Exception e) {
            logger.error("Failed to connect to server {}:{} - {}", host, port, e.getMessage(), e);
        }
    }
    
    /**
     * 检查网络连接性
     * @param host 服务器地址
     * @param port 服务器端口
     */
    private void checkNetworkConnectivity(String host, int port) {
        try {
            // 尝试解析主机名
            InetAddress[] addresses = InetAddress.getAllByName(host);
            logger.info("Resolved host {} to addresses: {}", host, (Object[]) addresses);
            
            // 检查是否能连接到端口
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000); // 5秒超时
                logger.info("Successfully connected to {}:{}", host, port);
            } catch (Exception e) {
                logger.warn("Failed to connect to {}:{} - {}", host, port, e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve host {} - {}", host, e.getMessage());
        }
    }
    
    /**
     * 启动设备发现功能
     */
    private void startDeviceDiscovery() {
        // 避免重复启动设备发现
        if (deviceDiscoveryStarted) {
            logger.debug("Device discovery already started, skipping");
            return;
        }
        
        deviceDiscovery.setListener(new DeviceDiscovery.DeviceDiscoveryListener() {
            @Override
            public void onDeviceDiscovered(DeviceConfig.Device device) {
                logger.info("New device discovered: {}", device.getIpAddress());
                // 可以在这里自动连接到新发现的设备
                controller.onClientConnected(device);
            }
        });
        deviceDiscovery.startDiscovery();
        deviceDiscoveryStarted = true;
    }
    
    /**
     * 发送数据到连接的对端
     * @param data 要发送的数据
     */
    public void sendData(Object data) {
        // 检查当前活动设备
        String activeDeviceId = controller.getActiveDeviceId();
        String localDeviceId = controller.getDeviceConfig().getDeviceId();
        
        // 如果当前活动设备是本地设备，则在本地处理
        if (activeDeviceId.equals(localDeviceId)) {
            logger.debug("Processing event locally");
            return;
        }
        
        // 如果当前活动设备是服务器（作为客户端模式）
        if (clientChannel != null && clientChannel.isActive() && 
            activeDeviceId.startsWith("server-")) {
            clientChannel.writeAndFlush(data);
            return;
        }
        
        // 如果当前活动设备是某个客户端（作为服务器模式）
        Channel targetChannel = clientChannels.get(activeDeviceId);
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.writeAndFlush(data);
            return;
        }
        
        // 如果没有找到特定的目标设备，则广播到所有客户端（作为服务器模式）
        if (serverChannel != null && serverChannel.isActive()) {
            // 在服务器模式下，广播到所有客户端
            for (Channel channel : clientChannels.values()) {
                if (channel.isActive()) {
                    channel.writeAndFlush(data);
                }
            }
        }
        
        logger.warn("No active connection found for device: {}", activeDeviceId);
    }
    
    /**
     * 发送数据到特定设备
     * @param data 要发送的数据
     * @param targetDeviceId 目标设备ID
     */
    public void sendDataTo(Object data, String targetDeviceId) {
        // 如果目标设备是本地设备，则在本地处理
        String localDeviceId = controller.getDeviceConfig().getDeviceId();
        if (targetDeviceId.equals(localDeviceId)) {
            logger.debug("Processing event locally for target device");
            return;
        }
        
        // 如果目标设备是服务器（作为客户端模式）
        if (clientChannel != null && clientChannel.isActive() && 
            targetDeviceId.startsWith("server-")) {
            clientChannel.writeAndFlush(data);
            return;
        }
        
        // 如果目标设备是某个客户端（作为服务器模式）
        Channel targetChannel = clientChannels.get(targetDeviceId);
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.writeAndFlush(data);
            return;
        }
        
        logger.warn("No active connection found for target device: {}", targetDeviceId);
    }
    
    /**
     * 关闭网络连接
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (clientChannel != null) {
            clientChannel.close();
            
            // 通知控制器客户端已断开连接
            if (clientChannel.remoteAddress() instanceof InetSocketAddress) {
                InetSocketAddress address = (InetSocketAddress) clientChannel.remoteAddress();
                String deviceId = "server-" + address.getHostString().replaceAll("[^a-zA-Z0-9\\-\\.]", "_") + ":" + address.getPort();
                controller.onClientDisconnected(deviceId != null ? deviceId : "");
            }
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (deviceDiscovery != null) {
            deviceDiscovery.stopDiscovery();
        }
    }
    
    /**
     * 检查客户端连接是否处于活动状态
     * @return true表示连接活动，false表示连接不活动
     */
    public boolean isClientActive() {
        return clientChannel != null && clientChannel.isActive();
    }
    
    /**
     * 检查服务器是否处于活动状态
     * @return true表示服务器活动，false表示服务器不活动
     */
    public boolean isServerActive() {
        return serverChannel != null && serverChannel.isActive();
    }
    
    /**
     * 添加客户端通道
     * @param deviceId 设备ID
     * @param channel 客户端通道
     */
    public void addClientChannel(String deviceId, Channel channel) {
        if (deviceId == null) {
            deviceId = "client-" + channel.id().asShortText();
            logger.warn("Null deviceId provided, generated new ID: {}", deviceId);
        }
        
        // 检查是否已存在相同的通道
        if (clientChannels.containsKey(deviceId)) {
            logger.debug("Channel for device {} already exists, replacing", deviceId);
        }
        
        clientChannels.put(deviceId, channel);
        logger.info("Client channel added: {}", deviceId);
    }
    
    /**
     * 移除客户端通道
     * @param deviceId 设备ID
     */
    public void removeClientChannel(String deviceId) {
        if (deviceId == null) {
            deviceId = "";
            logger.warn("Null deviceId provided for removal");
        }
        
        Channel removedChannel = clientChannels.remove(deviceId);
        if (removedChannel != null) {
            logger.info("Client channel removed: {}", deviceId);
        } else {
            logger.debug("No client channel found for removal: {}", deviceId);
        }
        
        // 通知控制器客户端已断开连接
        controller.onClientDisconnected(deviceId);
    }
    
    /**
     * 检查设备发现是否已启动
     * @return true表示已启动，false表示未启动
     */
    public boolean isDeviceDiscoveryStarted() {
        return deviceDiscoveryStarted;
    }
}