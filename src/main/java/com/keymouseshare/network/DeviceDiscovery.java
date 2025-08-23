package com.keymouseshare.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.keymouseshare.bean.*;
import com.keymouseshare.listener.DeviceListener;
import com.keymouseshare.util.DeviceTools;
import com.keymouseshare.util.NetUtil;
import javafx.application.Platform;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * UDP网络发现模块，用于发现局域网内的设备及其屏幕信息，并提供设备控制请求功能。
 */
public class DeviceDiscovery {
    private static final Logger logger = Logger.getLogger(DeviceDiscovery.class.getName());

    // UDP广播端口
    private static final int DISCOVERY_PORT = 8888;
    // 设备信息过期时间(毫秒)
    private static final long DEVICE_TIMEOUT = 30000; // 30秒

    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private DeviceListener listener;
    private Gson gson = new Gson();
    private DeviceStorage deviceStorage;
    private String localBroadcastAddress;
    private String localIpAddress;

    /**
     * 构造函数
     */
    public DeviceDiscovery() throws SocketException {
        this.socket = new DatagramSocket(DISCOVERY_PORT);
        this.socket.setBroadcast(true);
        this.localIpAddress = NetUtil.getLocalIpAddress();
        this.localBroadcastAddress = NetUtil.getLocalBroadcastAddress();
        this.deviceStorage = new DeviceStorage();
        this.deviceStorage.setDiscoveryDevice(new DeviceInfo(this.localIpAddress, "LOCAL_DEVICE", DeviceTools.getLocalScreens(), DeviceType.CLIENT.name(), ConnectType.DISCONNECTED.name()));
    }

    public DeviceStorage getDeviceStorage() {
        return deviceStorage;
    }

    /**
     * 设置设备发现监听器
     *
     * @param listener 监听器
     */
    public void setDeviceListener(DeviceListener listener) {
        this.listener = listener;
    }

    /**
     * 启动设备发现服务
     */
    public void startDiscovery() {
        // 启动接收线程
        startReceiverThread();

        // 启动定时广播线程
        startBroadcastThread();

        // 启动设备清理线程
        startDeviceCleanupThread();

        System.out.println("设备发现服务已启动，本地IP: " + localIpAddress + "，广播地址: " + localBroadcastAddress);
        // 打印本机信息
        deviceStorage.printLocalDevices();
    }

    /**
     * 停止设备发现服务
     */
    public void stopDiscovery() {
        if (scheduler != null) {
            scheduler.shutdown();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("设备发现服务已停止");
    }

    /**
     * 启动接收线程
     */
    private void startReceiverThread() {
        Thread receiverThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    handleMessage(message, packet.getAddress().getHostAddress());
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * 启动广播线程
     */
    private void startBroadcastThread() {
        scheduler = Executors.newScheduledThreadPool(1);

        // 每3秒发送一次心跳广播
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendDeviceHeartBeatBroadcast();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, 3, TimeUnit.SECONDS);

    }

    /**
     * 发送设备心跳
     */
    private void sendDeviceHeartBeatBroadcast() throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DEVICE_HEARTBEAT, deviceStorage.getLocalDevice()); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }


    /**
     * 启动设备清理线程
     */
    private void startDeviceCleanupThread() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, DeviceInfo>> iterator = deviceStorage.getDiscoveredDevices().entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, DeviceInfo> entry = iterator.next();
                DeviceInfo device = entry.getValue();

                // 如果设备超过30秒没有响应，则认为设备已离线
                // 但不要清理本地设备
                if (!device.getIpAddress().equals(localIpAddress) && currentTime - device.getLastSeen() > DEVICE_TIMEOUT) {
                    deviceStorage.removeDiscoveryDevice(device.getIpAddress());
                    listener.onDeviceLost(device);
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 处理接收到的消息
     *
     * @param message       消息内容
     * @param senderAddress 发送方地址
     */
    private void handleMessage(String message, String senderAddress) {
        Type messageType = new TypeToken<DiscoveryMessage>() {
        }.getType();
        DiscoveryMessage discoveryMessage = gson.fromJson(message, messageType);
        try {
            if (discoveryMessage == null) {
                return;
            }

            switch (discoveryMessage.getType()) {

                case DEVICE_HEARTBEAT:
                    handleDeviceHeartBeat(discoveryMessage);
                    break;

                case SERVER_START:
                    // 添加设备到列表
                    handleServerStart(discoveryMessage);
                    break;

                case SERVER_STOP:
                    // 收到控制请求，显示授权对话框
                    handleServerClose(senderAddress);
                    break;

                case CONTROL_REQUEST:
                    // 收到控制请求，显示授权对话框
                    // 忽略来自本机的消息
                    if (senderAddress.equals(localIpAddress)) {
                        return;
                    }
                    handleControlRequest(senderAddress);
                    break;

            }
        } catch (Exception e) {
            System.err.println("处理发现消息时出错: " + e);
        }
    }

    /**
     * 更新设备信息
     *
     * @param discoveryMessage 发现消息
     */
    private void handleDeviceHeartBeat(DiscoveryMessage discoveryMessage) {
        DeviceInfo device = discoveryMessage.getDeviceInfo();
        // 更新设备的最后_seen时间
        device.setLastSeen(System.currentTimeMillis());
        deviceStorage.setDiscoveryDevice(device);
        listener.onDeviceUpdate(device);
    }

    /**
     * 处理服务器启动消息
     */
    private void handleServerStart(DiscoveryMessage discoveryMessage) {
        DeviceInfo deviceServer = discoveryMessage.getDeviceInfo();
        // 更新设备的最后_seen时间
        deviceServer.setLastSeen(System.currentTimeMillis());
        deviceStorage.setDiscoveryDevice(deviceServer);
        if (listener != null) {
            listener.onServerStart();
        }
    }


    private void handleServerClose(String senderAddress) {
        DeviceInfo serviceDevice = deviceStorage.getSeverDevice();
        if (serviceDevice != null && serviceDevice.getIpAddress().equals(senderAddress)) {
            deviceStorage.getDiscoveredDevices().values().forEach(device -> {
                device.setDeviceType(DeviceType.CLIENT.name());
                device.setConnectionStatus(ConnectType.DISCONNECTED.name());
            });
        }
        // 在JavaFX线程中显示权限对话框
        listener.onServerClose();
    }

    /**
     * 处理控制请求
     *
     * @param senderAddress 发送方地址
     */
    private void handleControlRequest(String senderAddress) {
        // 在JavaFX线程中显示权限对话框
        Platform.runLater(() -> {
            listener.onControlRequest(senderAddress);
        });
    }

    /**
     * 发送控制请求
     *
     * @param targetIpAddress 目标设备IP地址
     */
    public void sendControlRequest(String targetIpAddress) throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.CONTROL_REQUEST);
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * 发送服务器关闭广播
     *
     * @throws IOException
     */
    public void sendServerStartBroadcast() throws IOException {
        DeviceInfo localDevice = deviceStorage.getLocalDevice();
        localDevice.setDeviceType(DeviceType.SERVER.name());
        localDevice.setConnectionStatus(ConnectType.CONNECTED.name());
        deviceStorage.setDiscoveryDevice(localDevice);
        DiscoveryMessage message = new DiscoveryMessage(MessageType.SERVER_START, localDevice); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * 发送服务器关闭广播
     *
     * @throws IOException
     */
    public void sendServerCloseBroadcast() throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.SERVER_STOP); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

}