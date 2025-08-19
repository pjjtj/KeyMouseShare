package com.keymouseshare;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.bean.ScreenInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.keymouseshare.ui.DeviceListUI;
import com.keymouseshare.ui.ScreenPreviewUI;
import com.keymouseshare.network.DeviceDiscovery;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import java.net.SocketException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

/**
 * 主应用程序类
 */
public class MainApplication extends Application {
    
    private DeviceDiscovery deviceDiscovery;
    private DeviceListUI deviceListUI;
    private ScreenPreviewUI screenPreviewUI;
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // 创建设备列表UI（左侧）
        deviceListUI = new DeviceListUI();
        root.setLeft(deviceListUI);
        
        // 创建屏幕预览UI（中心）
        screenPreviewUI = new ScreenPreviewUI();
        root.setCenter(screenPreviewUI);

        // 设置设备选中回调，实现设备列表与屏幕预览的联动
        deviceListUI.setOnDeviceSelectedWithIP((ipAddress) -> {
            // 处理设备选中逻辑
            System.out.println("设备 " + ipAddress + " 被选中");
            
            // 根据选中的设备IP选择对应的屏幕
            screenPreviewUI.selectScreen(ipAddress);
        });

        // 初始化并启动设备发现服务
        initDeviceDiscovery();
        
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("KeyMouseShare");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * 初始化设备发现服务
     */
    private void initDeviceDiscovery() {
        try {
            deviceDiscovery = new DeviceDiscovery();
            

            
            // 设置设备发现监听器
            deviceDiscovery.setDeviceDiscoveryListener(new DeviceDiscovery.DeviceDiscoveryListener() {
                @Override
                public void onDeviceDiscovered(DeviceInfo device) {
                    System.out.println("发现设备: " + device.getIpAddress());
                    // 可以在这里更新UI，添加新发现的设备到设备列表
                }
                
                @Override
                public void onDeviceLost(DeviceInfo device) {
                    System.out.println("设备离线: " + device.getIpAddress());
                    // 可以在这里更新UI，从设备列表中移除离线设备
                }
            });
            
            // 启动设备发现服务
            deviceDiscovery.startDiscovery();
            
        } catch (SocketException e) {
            System.err.println("创建设备发现服务时出错: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("启动设备发现服务时出错: " + e.getMessage());
        }
    }
    

    
    @Override
    public void stop() {
        // 应用程序关闭时停止设备发现服务
        if (deviceDiscovery != null) {
            deviceDiscovery.stopDiscovery();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}