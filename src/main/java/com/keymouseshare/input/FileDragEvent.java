package com.keymouseshare.input;

import java.util.List;

/**
 * 文件拖拽事件类，表示文件拖拽相关的操作
 */
public class FileDragEvent extends InputEvent {
    // 文件路径列表
    private List<String> filePaths;
    
    // 源设备ID
    private String sourceDeviceId;
    
    // 目标设备ID
    private String targetDeviceId;
    
    public FileDragEvent(EventType type) {
        super(type);
    }
    
    public List<String> getFilePaths() {
        return filePaths;
    }
    
    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }
    
    public String getSourceDeviceId() {
        return sourceDeviceId;
    }
    
    public void setSourceDeviceId(String sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }
    
    public String getTargetDeviceId() {
        return targetDeviceId;
    }
    
    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }
}