package com.keymouseshare.network;

import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.bean.ScreenInfo;

import java.lang.reflect.Type;
import java.util.logging.Logger;

import javafx.application.Platform;

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
    private final Map<String, DeviceInfo> discoveredDevices = new ConcurrentHashMap<>();
    private DeviceDiscoveryListener listener;
    private Gson gson = new Gson();

    // 本机设备信息
    private String localIpAddress;
    private String localBroadcastAddress;
    private List<ScreenInfo> localScreens = new ArrayList<>();
    private DeviceInfo localDevice; // 本地设备信息

    /**
     * 消息类型枚举
     */
    private enum MessageType {
        DISCOVERY_REQUEST,  // 发现请求
        DISCOVERY_RESPONSE,  // 发现请求
        DEVICE_UPDATE, // 发现
        CONTROL_REQUEST,    // 控制请求
        CONTROL_RESPONSE    // 控制响应
    }

    /**
     * 消息结构
     */
    private static class DiscoveryMessage {
        private MessageType type;
        private String deviceName;
        private String deviceType;
        private String connectionStatus;
        private List<ScreenInfo> screens;

        public DiscoveryMessage() {
        }

        public DiscoveryMessage(MessageType type, String deviceName, List<ScreenInfo> screens, String deviceType, String connectionStatus) {
            this.type = type;
            this.deviceName = deviceName;
            this.deviceType = deviceType;
            this.connectionStatus = connectionStatus;
            this.screens = screens;
        }

        // Getters and setters
        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getConnectionStatus() {
            return connectionStatus;
        }

        public void setConnectionStatus(String connectionStatus) {
            this.connectionStatus = connectionStatus;
        }

        public List<ScreenInfo> getScreens() {
            return screens;
        }

        public void setScreens(List<ScreenInfo> screens) {
            this.screens = screens;
        }
    }

    /**
     * 设备发现监听器接口
     */
    public interface DeviceDiscoveryListener {
        void onDeviceDiscovered(DeviceInfo device);

        void onDeviceLost(DeviceInfo device);

        void onDeviceUpdate(DeviceInfo device);
    }

    /**
     * 构造函数
     */
    public DeviceDiscovery() throws SocketException {
        this.socket = new DatagramSocket(DISCOVERY_PORT);
        this.socket.setBroadcast(true);
        this.localIpAddress = getLocalIpAddress();
        this.localScreens = getLocalScreens();
        this.localBroadcastAddress = getLocalBroadcastAddress();
        // 初始化本地设备信息
        this.localDevice = new DeviceInfo(this.localIpAddress, "LocalDevice", this.localScreens);
    }

    /**
     * 设置设备发现监听器
     *
     * @param listener 监听器
     */
    public void setDeviceDiscoveryListener(DeviceDiscoveryListener listener) {
        this.listener = listener;
    }


    /**
     * 设置本机设备名称
     *
     * @param deviceName 设备名称
     */
    public void setLocalDeviceName(String deviceName) {
        // 更新本地设备的名称
        this.localDevice.setDeviceName(deviceName);
    }

    /**
     * 获取本地设备信息
     *
     * @return 本地设备信息
     */
    public DeviceInfo getLocalDevice() {
        return this.localDevice;
    }

    /**
     * 启动设备发现服务
     */
    public void startDiscovery() throws IOException {
        // 启动接收线程
        startReceiverThread();

        // 将本地设备添加到发现列表中
        addLocalDeviceToList();

        // 启动定时广播线程
        startBroadcastThread();

        // 启动设备清理线程
        startDeviceCleanupThread();

        System.out.println("设备发现服务已启动，本地IP: " + localIpAddress + "，广播地址: " + localBroadcastAddress);
        printLocalDeviceInfo();
    }

    /**
     * 将本地设备添加到发现列表中
     */
    private void addLocalDeviceToList() {
        discoveredDevices.put(localIpAddress, localDevice);

        // 通知监听器本地设备已发现
        if (listener != null) {
            listener.onDeviceDiscovered(localDevice);
        }
    }

    /**
     * 打印本地设备信息
     */
    private void printLocalDeviceInfo() {
        System.out.println("本地设备信息:");
        System.out.println("  IP地址: " + localDevice.getIpAddress());
        System.out.println("  设备名称: " + localDevice.getDeviceName());
        System.out.println("  屏幕数量: " + localDevice.getScreens().size());

        for (int i = 0; i < localDevice.getScreens().size(); i++) {
            ScreenInfo screen = localDevice.getScreens().get(i);
            System.out.println("  屏幕" + (i + 1) + ": " + screen.getScreenName() +
                    " (" + screen.getWidth() + "x" + screen.getHeight() + ")");
        }
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
     * 获取本机IP地址
     *
     * @return 本机IP地址
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    /**
     * 获取本机广播地址
     *
     * @return 本机广播地址
     */
    private String getLocalBroadcastAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        // 计算广播地址 (将IP地址的最后一段替换为255)
                        String ip = address.getHostAddress();
                        int lastDot = ip.lastIndexOf('.');
                        if (lastDot != -1) {
                            return ip.substring(0, lastDot + 1) + "255";
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "255.255.255.255"; // 默认广播地址
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
        scheduler = Executors.newScheduledThreadPool(2);

        // 每5秒发送一次发现请求
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendDiscoveryRequest();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);

        // 每3秒发送一次发现响应
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendDiscoveryResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    /**
     * 启动设备清理线程
     */
    private void startDeviceCleanupThread() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, DeviceInfo>> iterator = discoveredDevices.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, DeviceInfo> entry = iterator.next();
                DeviceInfo device = entry.getValue();

                // 如果设备超过30秒没有响应，则认为设备已离线
                // 但不要清理本地设备
                if (!device.getIpAddress().equals(localIpAddress) &&
                        currentTime - device.getLastSeen() > DEVICE_TIMEOUT) {
                    iterator.remove();
                    if (listener != null) {
                        listener.onDeviceLost(device);
                    }
                    System.out.println("设备已离线: " + device.getIpAddress());
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
        try {
            Type messageType = new TypeToken<DiscoveryMessage>() {
            }.getType();
            DiscoveryMessage discoveryMessage = gson.fromJson(message, messageType);

            if (discoveryMessage == null) {
                return;
            }

            // 忽略来自本机的消息
            if (senderAddress.equals(localIpAddress)) {
                return;
            }

            switch (discoveryMessage.getType()) {
                case DISCOVERY_REQUEST:
                    // 收到发现请求，发送发现响应
                    sendDiscoveryResponse();
                    break;

                case DISCOVERY_RESPONSE:
                    // 收到发现响应，更新设备信息
                    updateDeviceInfo(senderAddress, discoveryMessage);
                    break;

                case DEVICE_UPDATE:
                    // 收到设备更新消息，更新设备信息
                    updateDeviceInfo(senderAddress, discoveryMessage);
                    break;

                case CONTROL_REQUEST:
                    // 收到控制请求，显示授权对话框
                    handleControlRequest(senderAddress, discoveryMessage);
                    break;
            }
        } catch (Exception e) {
            System.err.println("处理发现消息时出错: " + e.getMessage());
        }
    }

    /**
     * 处理控制请求
     *
     * @param senderAddress    发送方地址
     * @param discoveryMessage 发现消息
     */
    private void handleControlRequest(String senderAddress, DiscoveryMessage discoveryMessage) {
        // 在JavaFX线程中显示权限对话框
        Platform.runLater(() -> {
            if (listener instanceof ControlRequestListener) {
                ((ControlRequestListener) listener).onControlRequest(senderAddress, discoveryMessage.getDeviceName());
            }
        });
    }

    /**
     * 控制请求监听器接口
     */
    public interface ControlRequestListener extends DeviceDiscoveryListener {
        void onControlRequest(String requesterIpAddress, String requesterDeviceName);
    }

    /**
     * 更新设备信息
     *
     * @param ipAddress        设备IP地址
     * @param discoveryMessage 发现消息
     */
    private void updateDeviceInfo(String ipAddress, DiscoveryMessage discoveryMessage) {
        // 如果是本地设备，直接返回（避免将本地设备添加到发现设备列表）
        if (ipAddress.equals(localIpAddress)) {
            return;
        }

        DeviceInfo device = discoveredDevices.get(ipAddress);
        if (device == null) {
            device = new DeviceInfo(ipAddress, discoveryMessage.getDeviceName(), discoveryMessage.getScreens());
        }
        // 更新设备的最后_seen时间
        device.setLastSeen(System.currentTimeMillis());

        boolean isNewDevice = !discoveredDevices.containsKey(ipAddress);
        discoveredDevices.put(ipAddress, device);

        if (listener != null) {
            if (isNewDevice) {
                //
                listener.onDeviceDiscovered(device);
                System.out.println("发现新设备: " + ipAddress);
            }else{
                listener.onDeviceUpdate(device);
                System.out.println("更新设备信息: " + device);
            }
        }
    }

    /**
     * 获取所有已发现的设备
     *
     * @return 设备列表
     */
    public List<DeviceInfo> getDiscoveredDevices() {
        List<DeviceInfo> devices = new ArrayList<>(discoveredDevices.values());
        // 如果本地设备未被包含，添加本地设备
        if (!devices.contains(localDevice)) {
            devices.add(localDevice);
        }
        return devices;
    }

    /**
     * 根据IP地址获取设备信息
     *
     * @param ipAddress IP地址
     * @return 设备信息
     */
    public DeviceInfo getDevice(String ipAddress) {
        if (ipAddress.equals(localIpAddress)) {
            return localDevice;
        }
        return discoveredDevices.get(ipAddress);
    }

    /**
     * 获取本机屏幕信息
     *
     * @return 屏幕信息列表
     */
    private List<ScreenInfo> getLocalScreens() {
        List<ScreenInfo> screens = new ArrayList<>();

        // 首先尝试使用AWT Toolkit获取真实的屏幕分辨率
        try {
            // 在高DPI屏幕上获取真实的屏幕分辨率
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
            // 设置属性以获取真实的像素尺寸
            toolkit.setDynamicLayout(false);

            // 获取屏幕尺寸
            java.awt.Dimension screenSize = toolkit.getScreenSize();

            if (screenSize.width > 0 && screenSize.height > 0) {
                // 只有一个屏幕的情况
                screens.add(new ScreenInfo("ScreenA", screenSize.width, screenSize.height));
                return screens;
            }
        } catch (Exception e) {
            logger.warning("Failed to get screen info using Toolkit: " + e.getMessage());
        }

        // 使用JavaFX的Screen类获取屏幕信息
        try {
            javafx.stage.Screen.getPrimary(); // 确保Toolkit已初始化
            java.util.List<javafx.stage.Screen> javafxScreens = javafx.stage.Screen.getScreens();

            for (int i = 0; i < javafxScreens.size(); i++) {
                javafx.stage.Screen fxScreen = javafxScreens.get(i);
                javafx.geometry.Rectangle2D bounds = fxScreen.getBounds();
                javafx.geometry.Rectangle2D visualBounds = fxScreen.getVisualBounds();

                String screenName = "Screen" + (char) ('A' + i);
                // 使用完整边界来获取实际的屏幕尺寸（包括系统UI区域）
                screens.add(new ScreenInfo(screenName, (int) bounds.getWidth(), (int) bounds.getHeight()));
            }
        } catch (Exception e) {
            logger.warning("Failed to get screen info using JavaFX: " + e.getMessage());
        }

        // 如果上述方法都失败了，使用默认值
        if (screens.isEmpty()) {
            screens.add(new ScreenInfo("ScreenA", 1920, 1080)); // 默认1080p屏幕
        }

        return screens;
    }

    /**
     * 通知设备更新
     *
     * @param device 更新的设备信息
     */
    public void notifyDeviceUpdate(DeviceInfo device) {
        // 更新设备信息
        discoveredDevices.put(device.getIpAddress(), device);

        // 通过UDP广播发送设备更新消息给所有客户端
        try {
            // 如果是本地设备更新，则广播更新消息
            if (device.getIpAddress().equals(localIpAddress)) {
                sendDeviceUpdateBroadcast(device);
            }
        } catch (IOException e) {
            System.err.println("发送设备更新广播失败: " + e.getMessage());
        }

    }

    /**
     * 发送发现请求
     */
    private void sendDiscoveryRequest() throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DISCOVERY_REQUEST,
                localDevice.getDeviceName(), // 使用设备名称
                localDevice.getScreens(),
                localDevice.getDeviceType(),
                localDevice.getConnectionStatus()); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * 发送发现响应
     */
    private void sendDiscoveryResponse() throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DISCOVERY_RESPONSE,
                localDevice.getDeviceName(), // 使用设备名称
                localDevice.getScreens(),
                localDevice.getDeviceType(),
                localDevice.getConnectionStatus()
        ); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * 发送控制请求
     *
     * @param targetIpAddress 目标设备IP地址
     */
    public void sendControlRequest(String targetIpAddress) throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.CONTROL_REQUEST,
                localDevice.getDeviceName(),
                localDevice.getScreens(),
                localDevice.getDeviceType(),
                localDevice.getConnectionStatus());
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        InetAddress targetAddress = InetAddress.getByName(targetIpAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, DISCOVERY_PORT);
        socket.send(packet);
    }

    /**
     * 发送设备更新广播
     *
     * @param device 更新的设备信息
     */
    private void sendDeviceUpdateBroadcast(DeviceInfo device) throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DEVICE_UPDATE,
                device.getDeviceName(),
                device.getScreens(),
                device.getDeviceType(),
                device.getConnectionStatus()
        );
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);

        // 向所有网络接口广播设备更新消息
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            // 跳过环回接口和禁用的接口
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, DISCOVERY_PORT);
                socket.send(packet);
            }
        }
    }
}