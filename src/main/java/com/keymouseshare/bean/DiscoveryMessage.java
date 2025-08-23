package com.keymouseshare.bean;

public class DiscoveryMessage {
    private MessageType type;
    private DeviceInfo deviceInfo;

    public DiscoveryMessage() {
    }

    public DiscoveryMessage(MessageType type) {
        this.type = type;
    }

    public DiscoveryMessage(MessageType type, DeviceInfo deviceInfo) {
        this.type = type;
        this.deviceInfo = deviceInfo;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

}
