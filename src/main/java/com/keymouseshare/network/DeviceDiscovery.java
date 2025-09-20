package com.keymouseshare.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.keymouseshare.bean.*;
import com.keymouseshare.listener.DeviceListener;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.util.DeviceTools;
import com.keymouseshare.util.NetUtil;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UDP网络发现模块，用于发现局域网内的设备及其屏幕信息，并提供设备控制请求功能。
 */
public class DeviceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscovery.class);

    // UDP广播端口
    private static final int DISCOVERY_PORT = 8888;
    // 设备信息过期时间(毫秒)
    private static final long DEVICE_TIMEOUT = 30000; // 30秒

    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private DeviceListener listener;
    private Gson gson = new Gson();
    private String localBroadcastAddress;
    private String localIpAddress;

    /**
     * 构造函数
     */
    public DeviceDiscovery() throws SocketException {
        logger.debug("正在初始化DeviceDiscovery...");
        this.socket = new DatagramSocket(DISCOVERY_PORT);
        this.socket.setBroadcast(true);
        this.localIpAddress = NetUtil.getLocalIpAddress();
        this.localBroadcastAddress = NetUtil.getLocalBroadcastAddress();
        logger.info("DeviceDiscovery初始化完成，本地IP: {}，广播地址: {}", localIpAddress, localBroadcastAddress);

        DeviceStorage.getInstance().setDiscoveryDevice(new DeviceInfo(this.localIpAddress, System.getProperty("os.name"), DeviceTools.getLocalScreens(), DeviceType.CLIENT.name(), ConnectType.DISCONNECTED.name()));
        logger.debug("本地设备信息已设置到DeviceStorage");
    }

    /**
     * 设置设备发现监听器
     *
     * @param listener 监听器
     */
    public void setDeviceListener(DeviceListener listener) {
        logger.debug("设置设备监听器: {}", listener);
        this.listener = listener;
    }

    /**
     * 启动设备发现服务
     */
    public void startDiscovery() {
        logger.info("正在启动设备发现服务...");
        // 启动接收线程
        startReceiverThread();

        // 启动定时广播线程
        startBroadcastThread();

        // 启动设备清理线程
        startDeviceCleanupThread();

        logger.info("设备发现服务已启动，本地IP: {}，广播地址: {}", localIpAddress, localBroadcastAddress);
        // 打印本机信息
        DeviceStorage.getInstance().printLocalDevices();
    }

    /**
     * 停止设备发现服务
     */
    public void stopDiscovery() {
        logger.info("正在停止设备发现服务...");
        if (scheduler != null) {
            scheduler.shutdown();
            logger.debug("调度器已关闭");
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
            logger.debug("UDP套接字已关闭");
        }

        logger.info("设备发现服务已停止");
    }

    /**
     * 启动接收线程
     */
    private void startReceiverThread() {
        logger.debug("正在启动接收线程...");
        Thread receiverThread = new Thread(() -> {
            logger.debug("接收线程已启动");
            byte[] buffer = new byte[4096];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    logger.debug("接收到UDP数据包，来自: {}:{}", packet.getAddress().getHostAddress(), packet.getPort());
                    handleMessage(message, packet.getAddress().getHostAddress());
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        logger.error("接收广播时出错: {}", e.getMessage(), e);
                    }
                }
            }
            logger.debug("接收线程已结束");
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
        logger.debug("接收线程已启动为守护线程");
    }

    /**
     * 启动广播线程
     */
    private void startBroadcastThread() {
        logger.debug("正在启动广播线程...");
        scheduler = Executors.newScheduledThreadPool(1);
        logger.debug("调度器已创建");

        // 每3秒发送一次心跳广播
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.debug("发送设备心跳广播...");
                sendDeviceHeartBeatBroadcast();
                logger.debug("设备心跳广播发送完成");
            } catch (IOException e) {
                logger.error("发送设备心跳广播失败: {}", e.getMessage(), e);
            }
        }, 5, 3, TimeUnit.SECONDS);
        logger.debug("广播线程调度已设置");

    }

    /**
     * 发送设备心跳
     */
    private void sendDeviceHeartBeatBroadcast() throws IOException {
        logger.debug("准备发送设备心跳广播");
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DEVICE_HEARTBEAT, DeviceStorage.getInstance().getLocalDevice()); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        logger.debug("心跳消息: {}", jsonMessage);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
        logger.debug("设备心跳广播已发送到 {}:{}", localBroadcastAddress, DISCOVERY_PORT);
    }


    /**
     * 启动设备清理线程
     */
    private void startDeviceCleanupThread() {
        logger.debug("正在启动设备清理线程...");
        scheduler.scheduleAtFixedRate(() -> {
            logger.debug("执行设备清理任务");
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, DeviceInfo>> iterator = DeviceStorage.getInstance().getDiscoveredDevices().entrySet().iterator();

            int removedCount = 0;
            while (iterator.hasNext()) {
                Map.Entry<String, DeviceInfo> entry = iterator.next();
                DeviceInfo device = entry.getValue();

                // 如果设备超过30秒没有响应，则认为设备已离线
                // 但不要清理本地设备
                if (!device.getIpAddress().equals(localIpAddress) && currentTime - device.getLastSeen() > DEVICE_TIMEOUT) {
                    logger.info("设备 {} 已离线，最后在线时间: {}", device.getIpAddress(), device.getLastSeen());
                    DeviceStorage.getInstance().removeDiscoveryDevice(device.getIpAddress());
                    listener.onDeviceLost(device);
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                logger.debug("本次清理任务共移除 {} 个离线设备", removedCount);
            } else {
                logger.debug("本次清理任务未发现离线设备");
            }
        }, 10, 10, TimeUnit.SECONDS);
        logger.debug("设备清理线程调度已设置");
    }

    /**
     * 处理接收到的消息
     *
     * @param message       消息内容
     * @param senderAddress 发送方地址
     */
    private void handleMessage(String message, String senderAddress) {
        logger.debug("处理来自 {} 的消息: {}", senderAddress, message);
        Type messageType = new TypeToken<DiscoveryMessage>() {
        }.getType();
        DiscoveryMessage discoveryMessage = gson.fromJson(message, messageType);
        try {
            if (discoveryMessage == null) {
                logger.warn("无法解析消息内容: {}", message);
                return;
            }

            switch (discoveryMessage.getType()) {

                case DEVICE_HEARTBEAT:
                    logger.debug("处理设备心跳消息");
                    handleDeviceHeartBeat(discoveryMessage);
                    break;

                case SERVER_START:
                    logger.debug("处理服务器启动消息");
                    // 添加设备到列表
                    handleServerStart(discoveryMessage);
                    break;

                case SERVER_STOP:
                    logger.debug("处理服务器关闭消息");
                    // 收到控制请求，显示授权对话框
                    handleServerClose(senderAddress);
                    break;

                case CONTROL_REQUEST:
                    logger.debug("处理控制请求消息");
                    // 收到控制请求，显示授权对话框
                    // 忽略来自本机的消息
                    if (senderAddress.equals(localIpAddress)) {
                        logger.debug("忽略来自本机的消息");
                        return;
                    }
                    handleControlRequest(senderAddress);
                    break;

            }
        } catch (Exception e) {
            logger.error("处理发现消息时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新设备信息
     *
     * @param discoveryMessage 发现消息
     */
    private void handleDeviceHeartBeat(DiscoveryMessage discoveryMessage) {
        logger.debug("处理设备心跳: {}", discoveryMessage.getDeviceInfo());
        DeviceInfo device = discoveryMessage.getDeviceInfo();
        // 更新设备的最后_seen时间
        device.setLastSeen(System.currentTimeMillis());
        DeviceStorage.getInstance().setDiscoveryDevice(device);
        listener.onDeviceUpdate(device);
        logger.debug("设备心跳处理完成: {}", device.getIpAddress());
    }

    /**
     * 处理服务器启动消息
     */
    private void handleServerStart(DiscoveryMessage discoveryMessage) {
        logger.debug("处理服务器启动消息: {}", discoveryMessage.getDeviceInfo());
        DeviceInfo deviceServer = discoveryMessage.getDeviceInfo();
        DeviceStorage.getInstance().setDiscoveryDevice(deviceServer);
        if (listener != null) {
            listener.onServerStart();
            logger.debug("已通知监听器服务器启动");
        }
    }


    private void handleServerClose(String senderAddress) {
        logger.debug("处理服务器关闭消息，发送方: {}", senderAddress);
        DeviceInfo serviceDevice = DeviceStorage.getInstance().getSeverDevice();
        if (serviceDevice != null && serviceDevice.getIpAddress().equals(senderAddress)) {
            logger.debug("更新所有设备为客户端状态");
            DeviceStorage.getInstance().getDiscoveredDevices().values().forEach(device -> {
                device.setDeviceType(DeviceType.CLIENT.name());
                device.setConnectionStatus(ConnectType.DISCONNECTED.name());
            });
        }
        // 在JavaFX线程中显示权限对话框
        listener.onServerClose();
        logger.debug("已通知监听器服务器关闭");
    }

    /**
     * 处理控制请求
     *
     * @param senderAddress 发送方地址
     */
    private void handleControlRequest(String senderAddress) {
        logger.info("收到来自 {} 的控制请求", senderAddress);
        // 在JavaFX线程中显示权限对话框
        Platform.runLater(() -> {
            logger.debug("在JavaFX线程中处理控制请求");
            listener.onControlRequest(senderAddress);
        });
    }

    /**
     * 发送控制请求
     *
     * @param targetIpAddress 目标设备IP地址
     */
    public void sendControlRequest(String targetIpAddress) throws IOException {
        logger.info("发送控制请求到目标设备: {}", targetIpAddress);
        DiscoveryMessage message = new DiscoveryMessage(MessageType.CONTROL_REQUEST);
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, DISCOVERY_PORT);
        socket.send(packet);
        logger.debug("控制请求已发送到 {}:{}", targetIpAddress, DISCOVERY_PORT);
    }

    /**
     * 发送服务器关闭广播
     *
     * @throws IOException
     */
    public void sendServerStartBroadcast() throws IOException {
        logger.info("发送服务器启动广播");
        DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
        localDevice.setDeviceType(DeviceType.SERVER.name());
        localDevice.setConnectionStatus(ConnectType.CONNECTED.name());
        DeviceStorage.getInstance().setDiscoveryDevice(localDevice);
        DiscoveryMessage message = new DiscoveryMessage(MessageType.SERVER_START, localDevice); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
        logger.debug("服务器启动广播已发送到 {}:{}", localBroadcastAddress, DISCOVERY_PORT);
    }

    /**
     * 发送服务器关闭广播
     *
     * @throws IOException
     */
    public void sendServerCloseBroadcast() throws IOException {
        logger.info("发送服务器关闭广播");
        DiscoveryMessage message = new DiscoveryMessage(MessageType.SERVER_STOP); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
        logger.debug("服务器关闭广播已发送到 {}:{}", localBroadcastAddress, DISCOVERY_PORT);
    }

}