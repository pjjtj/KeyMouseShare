package com.keymouseshare.listener;

import com.keymouseshare.bean.DeviceInfo;

public interface DeviceListener {
    void onDeviceLost(DeviceInfo device);

    void onDeviceUpdate(DeviceInfo device);

    void onServerStart();

    void onServerClose();

    void onControlRequest(String senderAddress);
}
