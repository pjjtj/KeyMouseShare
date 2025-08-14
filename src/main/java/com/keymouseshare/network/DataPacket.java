package com.keymouseshare.network;

/**
 * 数据包类，用于在网络中传输数据
 */
public class DataPacket {
    private String type;        // 数据包类型
    private String deviceId;    // 设备ID
    private String data;        // 主要数据
    private String extraData;   // 额外数据
    
    public DataPacket() {
    }
    
    public DataPacket(String type, String deviceId, String data) {
        this.type = type;
        this.deviceId = deviceId;
        this.data = data;
    }
    
    public DataPacket(String type, String deviceId, String data, String extraData) {
        this.type = type;
        this.deviceId = deviceId;
        this.data = data;
        this.extraData = extraData;
    }
    
    // Getter和Setter方法
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getExtraData() {
        return extraData;
    }
    
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }
}