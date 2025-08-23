package com.keymouseshare.ui;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.DeviceType;
import com.keymouseshare.network.ControlRequestManager;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.util.NetUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 设备列表UI组件
 */
public class DeviceListUI extends VBox {
    private static final Logger logger = Logger.getLogger(DeviceListUI.class.getName());

    private ListView<HBox> deviceListView;
    private Button startServerButton = new Button("启动服务器");
    private DeviceDiscovery deviceDiscovery;
    private ControlRequestManager controlRequestManager;

    private Runnable onDeviceSelected; // 设备选中回调
    private java.util.function.Consumer<String> onDeviceSelectedWithIP; // 带IP参数的设备选中回调

    public void setOnDeviceSelected(Runnable callback) {
        this.onDeviceSelected = callback;
    }

    public void setOnDeviceSelectedWithIP(java.util.function.Consumer<String> callback) {
        this.onDeviceSelectedWithIP = callback;
    }

    public DeviceListUI() {
        // 初始化界面
        initializeUI();
    }

    public DeviceListUI(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化界面
        initializeUI();
        // 初始化设备列表
        initializeDeviceList();
    }

    /**
     * 设置设备发现服务
     * @param deviceDiscovery 设备发现服务
     */
    public void setDeviceDiscovery(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化设备列表
        initializeDeviceList();
    }

    /**
     * 设置控制请求管理器
     * @param controlRequestManager 控制请求管理器
     */
    public void setControlRequestManager(ControlRequestManager controlRequestManager) {
        this.controlRequestManager = controlRequestManager;
    }

    /**
     * 获取控制请求管理器
     * @return controlRequestManager 控制请求管理器
     */
    public ControlRequestManager getControlRequestManager() {
        return controlRequestManager;
    }

    private void initializeUI() {
        this.setPrefWidth(200);
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        Label titleLabel = new Label("设备列表");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        deviceListView = new ListView<>();
        deviceListView.setPrefHeight(500);

        // 添加点击事件监听器
        deviceListView.setOnMouseClicked(event -> {
            HBox selectedItem = deviceListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                // 清除之前选中项的样式
                deviceListView.getItems().forEach(item -> {
                    item.setStyle(""); // 清除自定义样式
                });

                // 获取选中项的IP地址
                if (!selectedItem.getChildren().isEmpty() && selectedItem.getChildren().get(1) instanceof Label) {
                    Label ipLabel = (Label) selectedItem.getChildren().get(1);
                    String text = ipLabel.getText();
                    String ipAddress = text.contains(" (本地)") ? text.replace(" (本地)", "") : text; // 移除本地标识

                    // 触发设备选中回调
                    if (onDeviceSelected != null) {
                        onDeviceSelected.run();
                    }

                    // 触发带IP的设备选中回调
                    if (onDeviceSelectedWithIP != null) {
                        onDeviceSelectedWithIP.accept(ipAddress);
                    }

                    // 如果是鼠标右键点击，则发起控制请求
                    if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        // 发起控制请求
                        if (controlRequestManager != null ) {
                            DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();;
                            if (localDevice != null && !localDevice.getIpAddress().equals(ipAddress) && localDevice.getDeviceType().equals(DeviceType.SERVER.name())) {
                                requestControl(ipAddress);
                            }
                        }
                    }
                }
            }
        });

        // 创建启动服务器按钮（底部）
        startServerButton.setOnAction(event -> {
            if (controlRequestManager != null) {
                boolean isCurrentlyServer = controlRequestManager.isServerMode();
                controlRequestManager.setServerMode(!isCurrentlyServer);
                if(!isCurrentlyServer){
                    try {
                        deviceDiscovery.sendServerStartBroadcast();
                    }catch (Exception e){
                        logger.severe("发送服务器启动广播失败: " + e.getMessage());
                    }
                }else{
                    try {
                        deviceDiscovery.sendServerCloseBroadcast();
                    }catch (Exception e){
                        logger.severe("发送服务器关闭广播失败: " + e.getMessage());
                    }
                }
            }
        });

        VBox bottomBox = new VBox();
        bottomBox.getChildren().add(startServerButton);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);

        this.getChildren().addAll(titleLabel, deviceListView, bottomBox);
    }

    /**
     * 发起控制请求
     * @param targetDeviceIp 目标设备IP
     */
    private void requestControl(String targetDeviceIp) {
        try {
            // 异步发送控制请求
            deviceDiscovery.sendControlRequest(targetDeviceIp);
        }catch (Exception e){
            logger.severe("发送服务器控制请求广播失败: " + e.getMessage());
        }
    }

    /**
     * 初始化设备列表
     */
    private void initializeDeviceList() {
        // 添加本地设备到列表顶部
        DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
        if (localDevice != null) {
            HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(), localDevice.getDeviceType(), localDevice.getConnectionStatus(), true);
            deviceListView.getItems().add(0, localDeviceItem); // 添加到列表顶部
        }
    }

    /**
     * 更新设备列表
     */
    public void updateDeviceList() {
        try {
            // 清空除本地设备外的所有设备
            DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
            if (!deviceListView.getItems().isEmpty()) {
                // 保留第一个项目（本地设备）
                HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(), localDevice.getDeviceType().substring(0,1), localDevice.getConnectionStatus(), true);
                deviceListView.getItems().clear();
                deviceListView.getItems().add(localDeviceItem);
            } else {
                deviceListView.getItems().clear();
            }

            // 添加所有发现的设备（除了本地设备）
            String localIpAddress = localDevice != null ? localDevice.getIpAddress() : null;

            for (DeviceInfo device : DeviceStorage.getInstance().getDiscoveredDevices().values()){
                String ipAddress = device.getIpAddress();
                // 跳过本地设备，因为我们已经在列表顶部添加了它
                if (localIpAddress != null && localIpAddress.equals(ipAddress)) {
                    continue;
                }

                // 创建设备项并添加到列表
                HBox deviceItem = createDeviceItem(ipAddress, device.getDeviceType().substring(0,1), device.getConnectionStatus(), false);
                deviceListView.getItems().add(deviceItem);
            }
        }catch (Exception e){
            logger.log(Level.FINE,"Error adding device to list: " + e.getMessage());
        }

    }

    private HBox createDeviceItem(String ipAddress, String role, String connectStatus, boolean isLocal) {
        HBox item = new HBox();
        item.setSpacing(10);
        item.setPadding(new Insets(5));
        item.setAlignment(Pos.CENTER_LEFT);

        // 创建状态指示器
        Circle statusIndicator = new Circle(8);
        if (connectStatus.equals("CONNECTED")) {
            statusIndicator.setFill(Color.GREEN);
        } else if(connectStatus.equals("DISCONNECTED")){
            statusIndicator.setFill(Color.GRAY);
        } else if(connectStatus.equals("PENDING_AUTHORIZATION")){
            statusIndicator.setFill(Color.ORANGE);
        } else {
            // 默认状态
            statusIndicator.setFill(Color.GRAY);
        }

        // 添加角色标识
        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        roleLabel.setTranslateY(-1);
        roleLabel.setTranslateX(-11);

        // 创建状态指示器容器
        HBox indicatorContainer = new HBox();
        indicatorContainer.setAlignment(Pos.CENTER);
        indicatorContainer.getChildren().addAll(statusIndicator, roleLabel);

        // 创建IP地址标签
        Label ipLabel = new Label(ipAddress + (isLocal ? " (本地)" : ""));
        ipLabel.setStyle("-fx-font-size: 12px;");

        item.getChildren().addAll(indicatorContainer, ipLabel);

        // 设置容器可扩展性
        HBox.setHgrow(ipLabel, Priority.ALWAYS);

        return item;
    }

    public void serverDeviceStart() {
        // 如果当前设备是服务器则，启动服务器按钮变为停止服务器。如果不是则禁用该按钮
        if(NetUtil.getLocalIpAddress().equals(DeviceStorage.getInstance().getSeverDevice().getIpAddress())){
            startServerButton.setText("关闭");
        }else{
            startServerButton.setDisable(true);
        }
    }

    public void serverDeviceStop() {
        startServerButton.setDisable(false);
        startServerButton.setText("启动配置中心");
    }
}