package com.keymouseshare.filetransfer;

import com.keymouseshare.core.Controller;
import java.io.*;
import java.util.UUID;

/**
 * 文件传输管理器
 * 负责处理跨设备的文件拖拽传输功能
 */
public class FileTransferManager {
    private Controller controller;
    private static final int BUFFER_SIZE = 8192;
    
    public FileTransferManager(Controller controller) {
        this.controller = controller;
    }
    
    /**
     * 发送文件到指定设备
     * @param filePath 文件路径
     * @param targetDeviceId 目标设备ID
     */
    public void sendFile(String filePath, String targetDeviceId) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File not found: " + filePath);
                return;
            }
            
            // 创建文件传输请求
            FileTransferRequest request = new FileTransferRequest();
            request.setFileId(UUID.randomUUID().toString());
            request.setFileName(file.getName());
            request.setFileSize(file.length());
            request.setSourceDeviceId(getLocalDeviceId());
            request.setTargetDeviceId(targetDeviceId);
            
            // 发送文件传输请求
            controller.getNetworkManager().sendFileTransferRequest(request);
            
            // 发送文件数据
            sendFileData(file, request.getFileId(), targetDeviceId);
            
        } catch (Exception e) {
            System.err.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送文件数据
     */
    private void sendFileData(File file, String fileId, String targetDeviceId) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesSent = 0;
            long fileSize = file.length();
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                // 发送文件块
                FileDataPacket packet = new FileDataPacket();
                packet.setFileId(fileId);
                packet.setData(buffer.clone());
                packet.setOffset(totalBytesSent);
                packet.setLength(bytesRead);
                packet.setLastPacket(totalBytesSent + bytesRead >= fileSize);
                
                controller.getNetworkManager().sendFileData(packet, targetDeviceId);
                totalBytesSent += bytesRead;
                
                // 模拟进度更新
                int progress = (int) ((totalBytesSent * 100) / fileSize);
                System.out.println("File transfer progress: " + progress + "%");
            }
            
            System.out.println("File transfer completed: " + file.getName());
            
        } catch (Exception e) {
            System.err.println("Error sending file data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 接收文件传输请求
     */
    public void receiveFileTransferRequest(FileTransferRequest request) {
        System.out.println("Received file transfer request: " + request.getFileName() + 
                          " from " + request.getSourceDeviceId());
        
        // 创建文件接收响应
        FileTransferResponse response = new FileTransferResponse();
        response.setFileId(request.getFileId());
        response.setAccepted(true);
        response.setTargetDeviceId(request.getSourceDeviceId());
        
        // 发送响应
        controller.getNetworkManager().sendFileTransferResponse(response);
    }
    
    /**
     * 接收文件数据
     */
    public void receiveFileData(FileDataPacket packet) {
        try {
            // 确保接收目录存在
            File downloadDir = new File("downloads");
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            // 创建临时文件
            File tempFile = new File(downloadDir, packet.getFileId() + ".tmp");
            
            // 写入文件数据
            try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
                raf.seek(packet.getOffset());
                raf.write(packet.getData(), 0, packet.getLength());
            }
            
            // 如果是最后一个数据包，重命名文件
            if (packet.isLastPacket()) {
                File finalFile = new File(downloadDir, "received_file");
                if (tempFile.renameTo(finalFile)) {
                    System.out.println("File received successfully: " + finalFile.getAbsolutePath());
                } else {
                    System.err.println("Failed to rename temporary file");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error receiving file data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取本地设备ID
     */
    private String getLocalDeviceId() {
        // 简化实现，实际应用中应获取真实设备ID
        return "local_device";
    }
}