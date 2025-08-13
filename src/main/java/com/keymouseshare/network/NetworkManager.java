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

import java.net.InetSocketAddress;
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
    
    public NetworkManager(Controller controller) {
        this.controller = controller;
        this.clientChannels = new ConcurrentHashMap<>();
        this.deviceDiscovery = new DeviceDiscovery();
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
            
            // 启动设备发现
            startDeviceDiscovery();
        } catch (InterruptedException e) {
            logger.error("Failed to start server", e);
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
            
            // 启动设备发现
            startDeviceDiscovery();
        } catch (InterruptedException e) {
            logger.error("Failed to connect to server {}:{}", host, port, e);
        }
    }
    
    /**
     * 启动设备发现功能
     */
    private void startDeviceDiscovery() {
        deviceDiscovery.setListener(new DeviceDiscovery.DeviceDiscoveryListener() {
            @Override
            public void onDeviceDiscovered(DeviceConfig.Device device) {
                logger.info("New device discovered: {}", device.getIpAddress());
                // 可以在这里自动连接到新发现的设备
            }
        });
        deviceDiscovery.startDiscovery();
    }
    
    /**
     * 发送数据到连接的对端
     * @param data 要发送的数据
     */
    public void sendData(Object data) {
        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(data);
        } else if (serverChannel != null && serverChannel.isActive()) {
            // 在服务器模式下，广播到所有客户端
            for (Channel channel : clientChannels.values()) {
                if (channel.isActive()) {
                    channel.writeAndFlush(data);
                }
            }
        }
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
}