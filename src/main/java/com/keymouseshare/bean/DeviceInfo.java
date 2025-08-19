package com.keymouseshare.bean;

/**
 * @Description TODO
 * @Author pengjiajia
 * @Date 2025/8/18 17:37
 **/

import com.keymouseshare.network.DeviceDiscovery;

import java.util.List;

/**
 * 设备信息类
 */
public class DeviceInfo {
    private String ipAddress;
    private long lastSeen;
    private List<ScreenInfo> screens;
    private String deviceName;

    public DeviceInfo() {}

    public DeviceInfo(String ipAddress, String deviceName, List<ScreenInfo> screens) {
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.screens = screens;
        this.lastSeen = System.currentTimeMillis();
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
}