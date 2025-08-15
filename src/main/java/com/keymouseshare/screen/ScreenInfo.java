package com.keymouseshare.screen;

/**
 * 屏幕信息类
 * 存储单个屏幕的相关信息
 */
public class ScreenInfo {
    private String id;
    private String name;
    private int width;
    private int height;
    private int x;
    private int y;
    private DeviceType deviceType;
    private ConnectionStatus connectionStatus;
    
    public enum DeviceType {
        LOCAL,      // 本地设备
        SERVER,     // 服务器设备
        CLIENT      // 客户端设备
    }
    
    public enum ConnectionStatus {
        CONNECTED,     // 已连接
        DISCONNECTED,  // 已断开
        CONNECTING     // 连接中
    }
    
    // 构造函数
    public ScreenInfo() {
    }
    
    public ScreenInfo(String id, String name, int width, int height, int x, int y) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
    
    // Getter和Setter方法
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public DeviceType getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
    
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    
    @Override
    public String toString() {
        return "ScreenInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                ", deviceType=" + deviceType +
                ", connectionStatus=" + connectionStatus +
                '}';
    }
}