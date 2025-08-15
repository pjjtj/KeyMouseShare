package com.keymouseshare.filetransfer;

/**
 * 文件数据包类
 * 用于传输文件的实际数据
 */
public class FileDataPacket {
    private String fileId;
    private byte[] data;
    private long offset;
    private int length;
    private boolean lastPacket;
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    public long getOffset() {
        return offset;
    }
    
    public void setOffset(long offset) {
        this.offset = offset;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    public boolean isLastPacket() {
        return lastPacket;
    }
    
    public void setLastPacket(boolean lastPacket) {
        this.lastPacket = lastPacket;
    }
}