package com.keymouseshare.filetransfer;

/**
 * 文件传输响应类
 * 用于响应文件传输请求
 */
public class FileTransferResponse {
    private String fileId;
    private boolean accepted;
    private String targetDeviceId;
    private String errorMessage;
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    public String getTargetDeviceId() {
        return targetDeviceId;
    }
    
    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}