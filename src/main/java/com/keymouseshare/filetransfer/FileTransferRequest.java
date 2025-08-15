package com.keymouseshare.filetransfer;

/**
 * 文件传输请求类
 * 用于在设备间传输文件时的请求信息
 */
public class FileTransferRequest {
    private String fileId;
    private String fileName;
    private long fileSize;
    private String sourceDeviceId;
    private String targetDeviceId;
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
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