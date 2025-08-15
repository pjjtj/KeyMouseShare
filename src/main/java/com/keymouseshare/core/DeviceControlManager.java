package com.keymouseshare.core;

import com.keymouseshare.screen.ScreenInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 设备控制权限管理器
 * 管理设备间的控制权限，包括允许/延迟提醒等选项
 */
public class DeviceControlManager {
    public enum ControlPermission {
        ALLOWED,           // 允许
        DELAY_5_MINUTES,   // 5分钟后提醒
        DELAY_10_MINUTES,  // 10分钟后提醒
        DELAY_30_MINUTES   // 30分钟后提醒
    }
    
    private Controller controller;
    private Map<String, ControlPermission> devicePermissions;
    private Map<String, Long> permissionExpiryTimes;
    private ScheduledExecutorService scheduler;
    
    public DeviceControlManager(Controller controller) {
        this.controller = controller;
        this.devicePermissions = new HashMap<>();
        this.permissionExpiryTimes = new HashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * 设置设备控制权限
     */
    public void setDevicePermission(String deviceId, ControlPermission permission) {
        devicePermissions.put(deviceId, permission);
        
        // 如果是延迟权限，设置定时器
        if (permission != ControlPermission.ALLOWED) {
            long delayMinutes = getDelayMinutes(permission);
            long expiryTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000);
            permissionExpiryTimes.put(deviceId, expiryTime);
            
            // 安排提醒任务
            schedulePermissionReminder(deviceId, delayMinutes);
        } else {
            // 移除过期时间
            permissionExpiryTimes.remove(deviceId);
        }
    }
    
    /**
     * 获取设备控制权限
     */
    public ControlPermission getDevicePermission(String deviceId) {
        // 检查是否已过期
        Long expiryTime = permissionExpiryTimes.get(deviceId);
        if (expiryTime != null && System.currentTimeMillis() >= expiryTime) {
            // 权限已过期，默认允许控制
            devicePermissions.put(deviceId, ControlPermission.ALLOWED);
            permissionExpiryTimes.remove(deviceId);
            return ControlPermission.ALLOWED;
        }
        
        return devicePermissions.getOrDefault(deviceId, ControlPermission.ALLOWED);
    }
    
    /**
     * 检查设备是否被允许控制
     */
    public boolean isDeviceAllowed(String deviceId) {
        ControlPermission permission = getDevicePermission(deviceId);
        return permission == ControlPermission.ALLOWED;
    }
    
    /**
     * 获取延迟分钟数
     */
    private long getDelayMinutes(ControlPermission permission) {
        switch (permission) {
            case DELAY_5_MINUTES:
                return 5;
            case DELAY_10_MINUTES:
                return 10;
            case DELAY_30_MINUTES:
                return 30;
            default:
                return 0;
        }
    }
    
    /**
     * 安排权限提醒任务
     */
    private void schedulePermissionReminder(String deviceId, long delayMinutes) {
        scheduler.schedule(() -> {
            // 时间到了，提醒用户
            remindUserForPermission(deviceId);
        }, delayMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * 提醒用户权限设置
     */
    private void remindUserForPermission(String deviceId) {
        // 在实际应用中，这里应该触发UI提醒
        System.out.println("Reminder: Device " + deviceId + " is requesting control permission");
        
        // 可以通过controller通知主窗口显示提醒对话框
        // controller.getMainWindow().showPermissionReminder(deviceId);
    }
    
    /**
     * 获取设备信息（用于UI显示）
     */
    public String getDeviceDisplayName(String deviceId) {
        // 简化实现，实际应用中应该从设备管理器获取详细信息
        if (deviceId.contains(":")) {
            String[] parts = deviceId.split(":", 2);
            return parts[0]; // 返回IP地址部分
        }
        return deviceId;
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}