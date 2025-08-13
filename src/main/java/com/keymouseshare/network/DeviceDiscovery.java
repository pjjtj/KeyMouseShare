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
    
    private static final int DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "KEYMOUSESHARE_DISCOVERY";
    private static final int DISCOVERY_TIMEOUT = 5000; // 5秒超时
    
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;
    private Set<String> discoveredDevices;
    private DeviceDiscoveryListener listener;
    
    public DeviceDiscovery() {
        discoveredDevices = new HashSet<>();
        scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 开始设备发现
     */
    public void startDiscovery() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setBroadcast(true);
            
            // 启动接收线程
            startReceiver();
            
            // 启动广播线程
            startBroadcaster();
            
            logger.info("Device discovery started on port {}", DISCOVERY_PORT);
        } catch (IOException e) {
            logger.error("Failed to start device discovery", e);
        }
    }
    
    /**
     * 启动接收线程
     */
    private void startReceiver() {
        scheduler.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (!socket.isClosed()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (DISCOVERY_MESSAGE.equals(message)) {
                        String deviceAddress = packet.getAddress().getHostAddress();
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
                if (!socket.isClosed()) {
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
            try {
                broadcastDiscoveryMessage();
            } catch (IOException e) {
                logger.error("Error broadcasting discovery message", e);
            }
        }, 0, 3, TimeUnit.SECONDS); // 每3秒广播一次
    }
    
    /**
     * 广播发现消息
     */
    private void broadcastDiscoveryMessage() throws IOException {
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
                
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);
                socket.send(packet);
            }
        }
    }
    
    /**
     * 停止设备发现
     */
    public void stopDiscovery() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        logger.info("Device discovery stopped");
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