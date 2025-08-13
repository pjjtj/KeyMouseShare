package com.keymouseshare.screen;

/**
 * 设备屏幕信息类，表示一个设备屏幕的位置和尺寸信息
 */
public class DeviceScreen {
    // 设备ID
    private String deviceId;
    
    // 设备名称
    private String deviceName;
    
    // 屏幕宽度
    private int width;
    
    // 屏幕高度
    private int height;
    
    // 屏幕在网络布局中的X坐标
    private int x;
    
    // 屏幕在网络布局中的Y坐标
    private int y;
    
    public DeviceScreen() {
    }
    
    public DeviceScreen(String deviceId, String deviceName, int width, int height, int x, int y) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
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
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DeviceScreen that = (DeviceScreen) o;
        
        return deviceId != null ? deviceId.equals(that.deviceId) : that.deviceId == null;
    }
    
    @Override
    public int hashCode() {
        return deviceId != null ? deviceId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "DeviceScreen{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}