package com.keymouseshare.bean;

public enum MessageType {
    DEVICE_HEARTBEAT,  // 设备心跳广播消息 用于同步设备信息
    DEVICE_OFF,  // 设备离线
    SERVER_START, // 服务器启动
    SERVER_STOP, // 服务器关闭
    CONTROL_REQUEST,    // 控制请求
    CONTROL_RESPONSE,    // 控制响应
}
