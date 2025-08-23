package com.keymouseshare.bean;

import com.keymouseshare.util.NetUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备存储
 */
public class DeviceStorage {

    private final Map<String, DeviceInfo> discoveredDevices = new ConcurrentHashMap<>();

    public DeviceInfo getLocalDevice() {
        // 便利discoveredDevices，找到第一个类型为SERVER的设备
        for (DeviceInfo device : discoveredDevices.values()) {
            if (device.getIpAddress().equals(NetUtil.getLocalIpAddress())) {
                return device;
            }
        }
        return null;
    }

    public DeviceInfo getSeverDevice() {
        return discoveredDevices.get(NetUtil.getLocalIpAddress());
    }


    public Map<String, DeviceInfo> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public void setDiscoveryDevice(DeviceInfo device) {
        discoveredDevices.put(device.getIpAddress(), device);
    }

    public void removeDiscoveryDevice(String ipAddress) {
        discoveredDevices.remove(ipAddress);
    }

    public void printLocalDevices() {
        DeviceInfo localDevice = getLocalDevice();
        System.out.println("本地设备信息:");
        System.out.println("  IP地址: " + localDevice.getIpAddress());
        System.out.println("  设备名称: " + localDevice.getDeviceName());
        System.out.println("  屏幕数量: " + localDevice.getScreens().size());

        for (int i = 0; i < localDevice.getScreens().size(); i++) {
            ScreenInfo screen = localDevice.getScreens().get(i);
            System.out.println("  屏幕" + (i + 1) + ": " + screen.getScreenName() +
                    " (" + screen.getWidth() + "x" + screen.getHeight() + ")");
        }
    }
}

