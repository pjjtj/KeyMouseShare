package com.keymouseshare.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备配置类，管理设备信息和屏幕排列
 */
public class DeviceConfig {
    // 当前设备ID
    private String deviceId;
    
    // 设备名称
    private String deviceName;
    
    // 屏幕宽度
    private int screenWidth;
    
    // 屏幕高度
    private int screenHeight;
    
    // 设备在网络中的位置X坐标
    private int networkX;
    
    // 设备在网络中的位置Y坐标
    private int networkY;
    
    // 连接的其他设备列表
    private List<Device> connectedDevices;
    
    public DeviceConfig() {
        connectedDevices = new ArrayList<>();
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public int getScreenWidth() {
        return screenWidth;
    }
    
    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }
    
    public int getScreenHeight() {
        return screenHeight;
    }
    
    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }
    
    public int getNetworkX() {
        return networkX;
    }
    
    public void setNetworkX(int networkX) {
        this.networkX = networkX;
    }
    
    public int getNetworkY() {
        return networkY;
    }
    
    public void setNetworkY(int networkY) {
        this.networkY = networkY;
    }
    
    public List<Device> getConnectedDevices() {
        return connectedDevices;
    }
    
    public void setConnectedDevices(List<Device> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }
    
    public void addDevice(Device device) {
        this.connectedDevices.add(device);
    }
    
    /**
     * 设备信息内部类
     */
    public static class Device {
        // 设备ID
        private String deviceId;
        
        // 设备名称
        private String deviceName;
        
        // 屏幕宽度
        private int screenWidth;
        
        // 屏幕高度
        private int screenHeight;
        
        // 设备在网络中的位置X坐标
        private int networkX;
        
        // 设备在网络中的位置Y坐标
        private int networkY;
        
        // 设备IP地址
        private String ipAddress;
        
        // 设备类型枚举
        private DeviceType deviceType = DeviceType.CONNECTABLE;
        
        // 连接状态枚举
        private ConnectionState connectionState = ConnectionState.DISCONNECTED;
        
        public enum DeviceType {
            SERVER,        // 服务器设备
            CLIENT,        // 客户端设备
            CONNECTABLE    // 可连接设备（通过UDP发现但尚未连接）
        }
        
        public enum ConnectionState {
            CONNECTED,     // 已连接
            DISCONNECTED,  // 已断开连接
            CONNECTING     // 连接中
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        
        public String getDeviceName() {
            return deviceName;
        }
        
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }
        
        public int getScreenWidth() {
            return screenWidth;
        }
        
        public void setScreenWidth(int screenWidth) {
            this.screenWidth = screenWidth;
        }
        
        public int getScreenHeight() {
            return screenHeight;
        }
        
        public void setScreenHeight(int screenHeight) {
            this.screenHeight = screenHeight;
        }
        
        public int getNetworkX() {
            return networkX;
        }
        
        public void setNetworkX(int networkX) {
            this.networkX = networkX;
        }
        
        public int getNetworkY() {
            return networkY;
        }
        
        public void setNetworkY(int networkY) {
            this.networkY = networkY;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
        
        public DeviceType getDeviceType() {
            return deviceType;
        }
        
        public void setDeviceType(DeviceType deviceType) {
            this.deviceType = deviceType != null ? deviceType : DeviceType.CONNECTABLE;
        }
        
        public ConnectionState getConnectionState() {
            return connectionState;
        }
        
        public void setConnectionState(ConnectionState connectionState) {
            this.connectionState = connectionState != null ? connectionState : ConnectionState.DISCONNECTED;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Device device = (Device) obj;
            
            // 通过设备ID或IP地址判断是否为同一设备
            if (deviceId != null && deviceId.equals(device.deviceId)) {
                return true;
            }
            
            return ipAddress != null && ipAddress.equals(device.ipAddress);
        }
        
        @Override
        public int hashCode() {
            // 优先使用设备ID作为哈希码，否则使用IP地址
            if (deviceId != null) {
                return deviceId.hashCode();
            }
            return ipAddress != null ? ipAddress.hashCode() : super.hashCode();
        }
    }
}