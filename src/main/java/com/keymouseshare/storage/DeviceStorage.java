package com.keymouseshare.storage;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.bean.DeviceType;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.util.NetUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备存储
 */
public class DeviceStorage {

    private final Map<String, DeviceInfo> discoveredDevices = new ConcurrentHashMap<>();

    private static final DeviceStorage INSTANCE = new DeviceStorage();

    public static DeviceStorage getInstance() {
        return INSTANCE;
    }

    public DeviceInfo getLocalDevice() {
        return discoveredDevices.get(NetUtil.getLocalIpAddress());
    }

    public DeviceInfo getSeverDevice() {
        // 便利discoveredDevices，找到第一个类型为SERVER的设备
        for (DeviceInfo device : discoveredDevices.values()) {
            if (device.getDeviceType().equals(DeviceType.SERVER.name())) {
                return device;
            }
        }
        return null;
    }


    public Map<String, DeviceInfo> getDiscoveredDevices() {
        // 返回不可修改的Map视图，防止外部直接修改内部数据结构
        return discoveredDevices;
    }

    public void setDiscoveryDevice(DeviceInfo device) {
        discoveredDevices.put(device.getIpAddress(), device);
    }

    /**
     * 根据设备IP地址获取设备的屏幕信息列表
     * @param deviceIpAddress 设备IP地址
     * @return 设备的屏幕信息列表，如果设备不存在则返回null
     */
    public List<ScreenInfo> getDeviceScreens(String deviceIpAddress) {
        DeviceInfo device = discoveredDevices.get(deviceIpAddress);
        return device != null ? device.getScreens() : null;
    }

    /**
     * 根据设备IP地址获取特定设备信息
     * @param deviceIpAddress 设备IP地址
     * @return 设备信息，如果设备不存在则返回null
     */
    public DeviceInfo getDevice(String deviceIpAddress) {
        return discoveredDevices.get(deviceIpAddress);
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