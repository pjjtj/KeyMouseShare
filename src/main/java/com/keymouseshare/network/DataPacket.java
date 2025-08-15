package com.keymouseshare.network;

/**
 * 数据包类
 * 用于在网络中传输各种类型的数据
 */
public class DataPacket {
    public static final String TYPE_MOUSE_MOVE = "MOUSE_MOVE";
    public static final String TYPE_MOUSE_PRESS = "MOUSE_PRESS";
    public static final String TYPE_MOUSE_RELEASE = "MOUSE_RELEASE";
    public static final String TYPE_MOUSE_WHEEL = "MOUSE_WHEEL";
    public static final String TYPE_KEY_PRESS = "KEY_PRESS";
    public static final String TYPE_KEY_RELEASE = "KEY_RELEASE";
    public static final String TYPE_DEVICE_INFO = "DEVICE_INFO";
    public static final String TYPE_FILE_TRANSFER_REQUEST = "FILE_TRANSFER_REQUEST";
    public static final String TYPE_FILE_TRANSFER_RESPONSE = "FILE_TRANSFER_RESPONSE";
    public static final String TYPE_FILE_DATA = "FILE_DATA";
    
    private String type;
    private String deviceId;
    private String data;
    private String extraData;
    
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
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "DataPacket{" +
                "type='" + type + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", data='" + data + '\'' +
                ", extraData='" + extraData + '\'' +
                '}';
    }
}