package com.keymouseshare.network;

import com.keymouseshare.screen.ScreenInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备信息类
 * 包含设备的详细信息，用于设备发现和连接过程
 */
public class DeviceInfo {
    private String deviceId;
    private String deviceName;
    private String ipAddress;
    private int port = 8888; // 默认端口
    private List<ScreenInfo> screens;
    private String osName;
    private String osVersion;
    private long timestamp; // 时间戳，用于判断设备是否在线
    
    public DeviceInfo() {
        this.screens = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public DeviceInfo(String deviceId, String deviceName, String ipAddress) {
        this();
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
    }
    
    // Getters and Setters
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
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public List<ScreenInfo> getScreens() {
        return screens;
    }
    
    public void setScreens(List<ScreenInfo> screens) {
        this.screens = screens;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public void setOsName(String osName) {
        this.osName = osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 检查设备是否在线（5秒内有更新认为在线）
     */
    public boolean isOnline() {
        return (System.currentTimeMillis() - timestamp) < 5000;
    }
    
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", osName='" + osName + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", screenCount=" + (screens != null ? screens.size() : 0) +
                '}';
    }
}