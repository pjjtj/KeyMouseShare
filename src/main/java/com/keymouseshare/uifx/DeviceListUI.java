package com.keymouseshare.uifx;

import com.keymouseshare.bean.DeviceInfo;
import com.keymouseshare.storage.DeviceStorage;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 设备列表UI组件
 */
public class DeviceListUI extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(DeviceListUI.class);

    private VBox deviceListContainer;
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
        // 初始化设备列表
        initializeDeviceList();
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
     *
     * @param deviceDiscovery 设备发现服务
     */
    public void setDeviceDiscovery(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化设备列表
        initializeDeviceList();
    }

    /**
     * 设置控制请求管理器
     *
     * @param controlRequestManager 控制请求管理器
     */
    public void setControlRequestManager(ControlRequestManager controlRequestManager) {
        this.controlRequestManager = controlRequestManager;
    }

    /**
     * 获取控制请求管理器
     *
     * @return controlRequestManager 控制请求管理器
     */
    public ControlRequestManager getControlRequestManager() {
        return controlRequestManager;
    }

    private void initializeUI() {
        this.setPrefWidth(220);
        this.setStyle("-fx-background-color: #058199;-fx-padding: 10px;");

        // 使用VBox替代ListView
        deviceListContainer = new VBox();
        deviceListContainer.setSpacing(10); // 设置行距为10
        deviceListContainer.setPadding(new Insets(10, 0, 0, 0));

        // 移除固定高度设置，使用自适应高度
        VBox.setVgrow(deviceListContainer, Priority.ALWAYS);


        this.getChildren().addAll(deviceListContainer);
    }

    /**
     * 发起控制请求
     *
     * @param targetDeviceIp 目标设备IP
     */
    private void requestControl(String targetDeviceIp) {
        try {
            // 异步发送控制请求
            deviceDiscovery.sendControlRequest(targetDeviceIp);
        } catch (Exception e) {
            logger.error("发送服务器控制请求广播失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化设备列表
     */
    private void initializeDeviceList() {
        // 添加本地设备到列表顶部
        DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
        if (localDevice != null) {
            HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(),localDevice.getDeviceName(),localDevice.getScreens().size(), localDevice.getDeviceType().substring(0, 1), localDevice.getConnectionStatus(), true);
            deviceListContainer.getChildren().add(0, localDeviceItem); // 添加到列表顶部
        }
    }

    /**
     * 更新设备列表
     */
    public void updateDeviceList() {
        try {
            // 清空设备列表容器中的所有子项（除了本地设备）
            DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
            if (!deviceListContainer.getChildren().isEmpty()) {
                // 保留第一个项目（本地设备）
                HBox localDeviceItem = createDeviceItem(localDevice.getIpAddress(),localDevice.getDeviceName(),localDevice.getScreens().size(), localDevice.getDeviceType().substring(0, 1), localDevice.getConnectionStatus(), true);
                deviceListContainer.getChildren().clear();
                deviceListContainer.getChildren().add(localDeviceItem);
            } else {
                deviceListContainer.getChildren().clear();
            }

            // 添加所有发现的设备（除了本地设备）
            String localIpAddress = localDevice != null ? localDevice.getIpAddress() : null;

            for (DeviceInfo device : DeviceStorage.getInstance().getDiscoveredDevices().values()) {
                String ipAddress = device.getIpAddress();
                // 跳过本地设备，因为我们已经在列表顶部添加了它
                if (localIpAddress != null && localIpAddress.equals(ipAddress)) {
                    continue;
                }

                // 创建设备项并添加到列表
                HBox deviceItem = createDeviceItem(ipAddress, device.getDeviceName(),device.getScreens().size(),device.getDeviceType().substring(0, 1), device.getConnectionStatus(), false);
                deviceListContainer.getChildren().add(deviceItem);
            }
        } catch (Exception e) {
            logger.error("Error adding device to list: {}", e.getMessage());
        }

    }

    private HBox createDeviceItem(String ipAddress,String os, Integer screenCount,String role, String connectStatus, boolean isLocal) {
        HBox item = new HBox();
        item.setPadding(new Insets(0, 0, 0, 16));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefHeight(80);
        item.setStyle("-fx-background-color: rgba(255,255,255,0.1);" +
                "-fx-background-radius: 0 8 8 0; " +
                "-fx-border-radius: 0 8 8 0;");


        // 创建状态指示器
        Circle statusIndicator = new Circle(8);
        if (connectStatus.equals("CONNECTED")) {
            statusIndicator.setFill(Color.GREEN);
        } else if (connectStatus.equals("DISCONNECTED")) {
            statusIndicator.setFill(Color.GRAY);
        } else if (connectStatus.equals("PENDING_AUTHORIZATION")) {
            statusIndicator.setFill(Color.ORANGE);
        } else {
            // 默认状态
            statusIndicator.setFill(Color.GRAY);
        }

        // 添加角色标识
        Label roleLabel = new Label(role);
        roleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 8px;");
        roleLabel.setTranslateY(0);
        roleLabel.setTranslateX(-11);

        // 创建IP地址标签
        Label ipLabel = new Label(ipAddress + (isLocal ? " (本地)" : ""));
        ipLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        // 创建状态指示器容器
        HBox indicatorContainer = new HBox();
        indicatorContainer.setAlignment(Pos.CENTER_LEFT);
        indicatorContainer.getChildren().addAll(statusIndicator, roleLabel , ipLabel);

        // 创建IP地址标签
        Label osScreenCountLabel = new Label(os+" | "+screenCount+"屏幕");
        osScreenCountLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        HBox osScreenCountContainer = new HBox();
        osScreenCountContainer.setAlignment(Pos.CENTER_LEFT);
        osScreenCountContainer.getChildren().addAll(osScreenCountLabel);
        osScreenCountContainer.setPadding(new Insets(0, 0, 0, 24)); // 与上方元素对齐

        // 使用VBox包装两行内容
        VBox contentContainer = new VBox();
        contentContainer.getChildren().addAll(indicatorContainer, osScreenCountContainer);
        contentContainer.setSpacing(10); // 设置两行之间的间距
        contentContainer.setAlignment(Pos.CENTER_LEFT); // 水平居中靠左展示

        item.getChildren().addAll(contentContainer);

        // 设置容器可扩展性
        HBox.setHgrow(contentContainer, Priority.ALWAYS);

        // 添加点击事件
        item.setOnMouseClicked(event -> {
            // 清除之前选中项的样式
            VBox parent = (VBox) item.getParent();
            parent.getChildren().forEach(child -> {
                if (child instanceof HBox) {
                    ((HBox) child).setStyle("-fx-background-color: rgba(255,255,255,0.1);" +
                            "-fx-background-radius: 0 8 8 0; " +
                            "-fx-border-radius: 0 8 8 0;");
                }
            });

            // 设置当前选中项的样式
            item.setStyle("-fx-background-color: rgba(255,255,255,0.3);" +
                    "-fx-background-radius: 0 8 8 0; " +
                    "-fx-border-radius: 0 8 8 0;");
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
                if (controlRequestManager != null) {
                    DeviceInfo localDevice = DeviceStorage.getInstance().getLocalDevice();
                    if (localDevice != null && !localDevice.getIpAddress().equals(ipAddress) && localDevice.getDeviceType().equals(DeviceType.SERVER.name())) {
                        requestControl(ipAddress);
                    }
                }
            }
        });

        return item;
    }
}