package com.keymouseshare.network;

import com.keymouseshare.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 设备发现功能，用于在局域网中发现其他设备
 */
public class DeviceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(DeviceDiscovery.class);
    
    private static final int DEFAULT_DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "KEYMOUSESHARE_DISCOVERY";
    private static final int DISCOVERY_TIMEOUT = 5000; // 5秒超时
    
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private Set<String> discoveredDevices;
    private DeviceDiscoveryListener listener;
    private int discoveryPort;
    private boolean isRunning = false;
    
    public DeviceDiscovery() {
        this(DEFAULT_DISCOVERY_PORT);
    }
    
    public DeviceDiscovery(int discoveryPort) {
        this.discoveryPort = discoveryPort;
        discoveredDevices = new HashSet<>();
        scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 开始设备发现
     */
    public void startDiscovery() {
        if (isRunning) {
            logger.warn("Device discovery is already running");
            return;
        }
        
        try {
            // 尝试绑定到指定端口，如果失败则尝试其他端口
            socket = bindToAvailablePort(discoveryPort);
            socket.setBroadcast(true);
            
            isRunning = true;
            logger.info("Device discovery started on port {}", socket.getLocalPort());
            
            // 打印网络接口信息用于调试
            logNetworkInterfaces();
            
            // 启动接收线程
            startReceiver();
            
            // 启动广播线程
            startBroadcaster();
            
        } catch (IOException e) {
            logger.error("Failed to start device discovery", e);
        }
    }
    
    /**
     * 绑定到可用端口
     * @param preferredPort 首选端口
     * @return DatagramSocket
     * @throws IOException 如果无法绑定到任何端口
     */
    private DatagramSocket bindToAvailablePort(int preferredPort) throws IOException {
        // 首先尝试首选端口
        try {
            DatagramSocket s = new DatagramSocket(preferredPort);
            logger.debug("Successfully bound to preferred port {}", preferredPort);
            return s;
        } catch (BindException e) {
            logger.warn("Failed to bind to preferred port {}, trying alternative ports", preferredPort);
        }
        
        // 尝试附近的端口
        for (int i = 0; i < 10; i++) {
            int port = preferredPort + i;
            if (port != preferredPort) {
                try {
                    DatagramSocket s = new DatagramSocket(port);
                    logger.info("Successfully bound to alternative port {}", port);
                    return s;
                } catch (BindException e) {
                    logger.debug("Failed to bind to port {}, trying next", port);
                }
            }
        }
        
        // 尝试更低的端口范围
        for (int i = 1; i <= 10; i++) {
            int port = preferredPort - i;
            if (port > 0) {
                try {
                    DatagramSocket s = new DatagramSocket(port);
                    logger.info("Successfully bound to alternative port {}", port);
                    return s;
                } catch (BindException e) {
                    logger.debug("Failed to bind to port {}, trying next", port);
                }
            }
        }
        
        // 让系统选择一个端口
        DatagramSocket s = new DatagramSocket();
        logger.info("Bound to system assigned port {}", s.getLocalPort());
        return s;
    }
    
    /**
     * 记录网络接口信息用于调试
     */
    private void logNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            logger.info("Available network interfaces:");
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                try {
                    logger.info("Interface: {} ({}) - UP: {}, LOOPBACK: {}, VIRTUAL: {}", 
                               networkInterface.getName(), 
                               networkInterface.getDisplayName(),
                               networkInterface.isUp(),
                               networkInterface.isLoopback(),
                               networkInterface.isVirtual());
                    
                    if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress address = interfaceAddress.getAddress();
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            logger.info("  Address: {}, Broadcast: {}, Prefix: {}", 
                                       address, 
                                       broadcast, 
                                       interfaceAddress.getNetworkPrefixLength());
                        }
                    }
                } catch (SocketException e) {
                    logger.warn("Error getting interface info for {}", networkInterface.getName(), e);
                }
            }
        } catch (SocketException e) {
            logger.error("Failed to get network interfaces", e);
        }
    }
    
    /**
     * 启动接收线程
     */
    private void startReceiver() {
        scheduler.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (!socket.isClosed() && isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (DISCOVERY_MESSAGE.equals(message)) {
                        String deviceAddress = packet.getAddress().getHostAddress();
                        // 不要添加自己的地址
                        try {
                            InetAddress localAddress = InetAddress.getLocalHost();
                            if (localAddress.getHostAddress().equals(deviceAddress)) {
                                continue;
                            }
                        } catch (UnknownHostException e) {
                            // 无法获取本地地址，继续处理
                        }
                        
                        if (!discoveredDevices.contains(deviceAddress)) {
                            discoveredDevices.add(deviceAddress);
                            logger.info("Discovered device: {}", deviceAddress);
                            
                            if (listener != null) {
                                DeviceConfig.Device device = new DeviceConfig.Device();
                                device.setIpAddress(deviceAddress);
                                device.setDeviceName("Unknown Device (" + deviceAddress + ")");
                                listener.onDeviceDiscovered(device);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed() && isRunning) {
                    logger.error("Error receiving discovery packets", e);
                }
            }
        });
    }
    
    /**
     * 启动广播线程
     */
    private void startBroadcaster() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning) {
                try {
                    broadcastDiscoveryMessage();
                } catch (IOException e) {
                    logger.error("Error broadcasting discovery message", e);
                }
            }
        }, 0, 3, TimeUnit.SECONDS); // 每3秒广播一次
    }
    
    /**
     * 广播发现消息
     */
    private void broadcastDiscoveryMessage() throws IOException {
        if (socket == null || socket.isClosed()) {
            return;
        }
        
        byte[] data = DISCOVERY_MESSAGE.getBytes();
        
        // 获取所有网络接口并广播
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
                
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, socket.getLocalPort());
                socket.send(packet);
                logger.debug("Sent discovery packet to {}", broadcast);
            }
        }
    }
    
    /**
     * 停止设备发现
     */
    public void stopDiscovery() {
        isRunning = false;
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        logger.info("Device discovery stopped");
    }
    
    /**
     * 检查设备发现是否正在运行
     * @return true表示正在运行，false表示未运行
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 设置设备发现监听器
     * @param listener 监听器
     */
    public void setListener(DeviceDiscoveryListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设备发现监听器接口
     */
    public interface DeviceDiscoveryListener {
        /**
         * 当发现新设备时调用
         * @param device 设备信息
         */
        void onDeviceDiscovered(DeviceConfig.Device device);
    }
}