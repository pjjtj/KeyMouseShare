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

/**
 * UDP网络发现模块，用于发现局域网内的设备及其屏幕信息
 */
public class DeviceDiscovery {
    // UDP广播端口
    private static final int DISCOVERY_PORT = 8888;
    // 广播地址
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
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
        DISCOVERY_RESPONSE   // 发现响应
    }
    
    /**
     * 消息结构
     */
    private static class DiscoveryMessage {
        private MessageType type;
        private String deviceName;
        private List<ScreenInfo> screens;
        
        public DiscoveryMessage() {}
        
        public DiscoveryMessage(MessageType type, String deviceName, List<ScreenInfo> screens) {
            this.type = type;
            this.deviceName = deviceName;
            this.screens = screens;
        }
        
        // Getters and setters
        public MessageType getType() { return type; }
        public void setType(MessageType type) { this.type = type; }
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public List<ScreenInfo> getScreens() { return screens; }
        public void setScreens(List<ScreenInfo> screens) { this.screens = screens; }
    }
    
    /**
     * 设备发现监听器接口
     */
    public interface DeviceDiscoveryListener {
        void onDeviceDiscovered(DeviceInfo device);
        void onDeviceLost(DeviceInfo device);
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
     * @param listener 监听器
     */
    public void setDeviceDiscoveryListener(DeviceDiscoveryListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置本机屏幕信息
     * @param screens 屏幕信息列表
     */
    public void setLocalScreens(List<ScreenInfo> screens) {
        this.localScreens = screens;
        // 更新本地设备的屏幕信息
        this.localDevice.setScreens(new ArrayList<>(screens));
    }
    
    /**
     * 设置本机设备名称
     * @param deviceName 设备名称
     */
    public void setLocalDeviceName(String deviceName) {
        // 更新本地设备的名称
        this.localDevice.setDeviceName(deviceName);
    }
    
    /**
     * 获取本地设备信息
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
            System.out.println("  屏幕" + (i+1) + ": " + screen.getScreenName() + 
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
                if (currentTime - device.getLastSeen() > DEVICE_TIMEOUT) {
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
     * 发送发现请求
     */
    private void sendDiscoveryRequest() throws IOException {
        DiscoveryMessage message = new DiscoveryMessage(MessageType.DISCOVERY_REQUEST, 
                                                       "DiscoveryRequest", 
                                                       new ArrayList<>());
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
                                                       localDevice.getScreens()); // 使用本地设备的屏幕信息
        String jsonMessage = gson.toJson(message);
        byte[] buffer = jsonMessage.getBytes(StandardCharsets.UTF_8);
        
        InetAddress broadcastAddress = InetAddress.getByName(localBroadcastAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
    }
    
    /**
     * 处理接收到的消息
     * @param message 消息内容
     * @param senderAddress 发送方地址
     */
    private void handleMessage(String message, String senderAddress) {
        try {
            Type messageType = new TypeToken<DiscoveryMessage>(){}.getType();
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
            }
        } catch (Exception e) {
            System.err.println("处理发现消息时出错: " + e.getMessage());
        }
    }
    
    /**
     * 更新设备信息
     * @param ipAddress 设备IP地址
     * @param discoveryMessage 发现消息
     */
    private void updateDeviceInfo(String ipAddress, DiscoveryMessage discoveryMessage) {
        // 如果是本地设备，直接返回（避免将本地设备添加到发现设备列表）
        if (ipAddress.equals(localIpAddress)) {
            return;
        }
        
        DeviceInfo device = new DeviceInfo(ipAddress, 
                                          discoveryMessage.getDeviceName(), 
                                          discoveryMessage.getScreens());
        
        boolean isNewDevice = !discoveredDevices.containsKey(ipAddress);
        discoveredDevices.put(ipAddress, device);
        
        if (listener != null) {
            if (isNewDevice) {
                listener.onDeviceDiscovered(device);
                System.out.println("发现新设备: " + ipAddress);
            }
        }
    }
    
    /**
     * 获取所有已发现的设备
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
     * @return 屏幕信息列表
     */
    private List<ScreenInfo> getLocalScreens() {
        List<ScreenInfo> screens = new ArrayList<>();

        // 使用JavaFX的Screen类获取屏幕信息
        javafx.stage.Screen.getPrimary(); // 确保Toolkit已初始化
        java.util.List<javafx.stage.Screen> javafxScreens = javafx.stage.Screen.getScreens();

        for (int i = 0; i < javafxScreens.size(); i++) {
            javafx.stage.Screen fxScreen = javafxScreens.get(i);
            javafx.geometry.Rectangle2D bounds = fxScreen.getBounds();

            String screenName = "Screen" + (char) ('A' + i);
            int width = (int) bounds.getWidth();
            int height = (int) bounds.getHeight();

            screens.add(new ScreenInfo(screenName, width, height));
        }

        // 如果JavaFX方法失败，使用传统方法
        if (screens.isEmpty()) {
            // 使用AWT获取屏幕设备
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice[] gs = ge.getScreenDevices();

            for (int i = 0; i < gs.length; i++) {
                java.awt.GraphicsConfiguration gc = gs[i].getDefaultConfiguration();
                java.awt.Rectangle bounds = gc.getBounds();

                String screenName = "Screen" + (char) ('A' + i);
                int width = bounds.width;
                int height = bounds.height;

                screens.add(new ScreenInfo(screenName, width, height));
            }
        }

        return screens;
    }
}