package com.keymouseshare.bean;

import com.keymouseshare.bean.ScreenInfo;

import java.util.List;

/**
 * 设备信息类
 * 包含设备的IP地址、名称、屏幕信息等
 */
public class DeviceInfo {
    private String ipAddress;
    private long lastSeen;
    private List<ScreenInfo> screens;
    private String deviceName;
    private String deviceType; // 设备类型：SERVER:S 或 CLIENT:C
    private String connectionStatus; // 连接状态：CONNECTED, DISCONNECTED, PENDING_AUTHORIZATION

    public DeviceInfo() {
        this.deviceType = "C"; // 默认设备类型为客户端
        this.connectionStatus = "DISCONNECTED"; // 默认连接状态为未连接
    }

    public DeviceInfo(String ipAddress, String deviceName, List<ScreenInfo> screens) {
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.screens = screens;
        this.lastSeen = System.currentTimeMillis();
        this.deviceType = "C"; // 默认设备类型为客户端
        this.connectionStatus = "DISCONNECTED"; // 默认连接状态为未连接
    }

    // Getters and setters
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public List<ScreenInfo> getScreens() { return screens; }
    public void setScreens(List<ScreenInfo> screens) { this.screens = screens; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
}