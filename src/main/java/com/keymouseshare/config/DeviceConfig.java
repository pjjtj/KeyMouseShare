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
    }
}