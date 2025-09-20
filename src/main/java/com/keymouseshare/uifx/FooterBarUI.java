package com.keymouseshare.uifx;

import com.keymouseshare.network.ControlRequestManager;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.storage.VirtualDesktopStorage;
import com.keymouseshare.util.NetUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.util.List;


/**
 * 鼠标位置显示器
 * 用于实时显示鼠标在屏幕中的位置
 */
public class FooterBarUI extends HBox {

    private Logger logger = LoggerFactory.getLogger(FooterBarUI.class);

    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    
    private Label xPositionLabel;
    private Label yPositionLabel;
    private Label screenPositionLabel;

    private DeviceDiscovery deviceDiscovery;
    private ControlRequestManager controlRequestManager;

    private Button startServerButton = new Button("设为主控");
    private Button applayScreenButton = new Button("应用设置");
    
    public FooterBarUI(ControlRequestManager controlRequestManager, DeviceDiscovery deviceDiscovery) {
        initializeUI();
        this.controlRequestManager = controlRequestManager;
        this.deviceDiscovery = deviceDiscovery;
    }
    
    private void initializeUI() {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setStyle("-fx-border-color: lightgray; -fx-border-width: 1 0 0 0; -fx-border-radius: 0;");
        this.setPrefHeight(50);
        // 设置HBox可增长以填充父容器
        HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS);

        Button userInfoButton = new Button("访客");
        userInfoButton.setPrefHeight(40);
        userInfoButton.setPrefWidth(40);
        userInfoButton.setStyle("-fx-background-color: #858585; -fx-text-fill: white; -fx-font-size: 8px;-fx-background-radius: 50%");
        // 添加点击事件，弹出登录框
        userInfoButton.setOnAction(event -> showLoginDialog(userInfoButton.getScene().getWindow()));

        Label mouseLabel = new Label("鼠标位置:");
        mouseLabel.setFont(Font.font(12));
        
        xPositionLabel = new Label("X: 0");
        xPositionLabel.setFont(Font.font(12));
        xPositionLabel.setStyle("-fx-text-fill: blue;");
        
        yPositionLabel = new Label("Y: 0");
        yPositionLabel.setFont(Font.font(12));
        yPositionLabel.setStyle("-fx-text-fill: blue;");
        
        screenPositionLabel = new Label();
        screenPositionLabel.setFont(Font.font(12));
        screenPositionLabel.setStyle("-fx-text-fill: gray;");

        HBox positionBox = new HBox(10);
        positionBox.getChildren().addAll(userInfoButton, mouseLabel, xPositionLabel, yPositionLabel, screenPositionLabel);
        positionBox.setAlignment(Pos.CENTER_LEFT); // 垂直居中，左对齐
        
        // 创建启动服务器按钮（底部）
        startServerButton.setPrefHeight(40);
        startServerButton.setPrefWidth(160);
        startServerButton.setTextFill(Color.WHITE);
        startServerButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(90,153,197);-fx-border-radius: 10px;-fx-font-weight: bolder");
        applayScreenButton.setOnMouseEntered(event -> {
            if(virtualDesktopStorage.isApplyVirtualDesktopScreen()){
                startServerButton.setStyle("-fx-opacity: 0.5;-fx-background-color: rgb(0,147,253);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }else{
                startServerButton.setStyle("-fx-opacity: 0.5;-fx-background-color: rgb(115,5,0);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }
        });
        applayScreenButton.setOnMouseExited(event -> {
            if(virtualDesktopStorage.isApplyVirtualDesktopScreen()){
                startServerButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(90,153,197);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }else{
                startServerButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(115,5,0);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }
        });


        startServerButton.setOnAction(event -> {
            if (controlRequestManager != null) {
                boolean isCurrentlyServer = controlRequestManager.isServerMode();
                controlRequestManager.setServerMode(!isCurrentlyServer);
                if (!isCurrentlyServer) {
                    try {
                        deviceDiscovery.sendServerStartBroadcast();
                    } catch (Exception e) {
                        logger.error("发送服务器启动广播失败: {}", e.getMessage());
                    }
                } else {
                    try {
                        deviceDiscovery.sendServerCloseBroadcast();
                    } catch (Exception e) {
                        logger.error("发送服务器关闭广播失败: {}", e.getMessage());
                    }
                }
            }
        });

        applayScreenButton.setOnAction(event -> {
            VirtualDesktopStorage.getInstance().applyVirtualDesktopScreen();
        });

        applayScreenButton.setVisible(false);

        applayScreenButton.setPrefHeight(40);
        applayScreenButton.setPrefWidth(160);
        applayScreenButton.setTextFill(Color.WHITE);
        applayScreenButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(90,153,197);-fx-border-radius: 10px;-fx-font-weight: bolder");
        applayScreenButton.setOnMouseEntered(event -> {
            if(virtualDesktopStorage.isApplyVirtualDesktopScreen()){
                applayScreenButton.setStyle("-fx-opacity: 0.5;-fx-background-color: rgb(90,153,197);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }
        });
        applayScreenButton.setOnMouseExited(event -> {
            if(virtualDesktopStorage.isApplyVirtualDesktopScreen()){
                applayScreenButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(90,153,197);-fx-border-radius: 10px;-fx-font-weight: bolder");
            }
        });
        // 使用HBox实现水平布局
        HBox bottomBox = new HBox(10); // 设置组件间间距为10px
        bottomBox.getChildren().addAll(applayScreenButton,startServerButton);
        bottomBox.setPadding(new Insets(0,20,0,0));
        bottomBox.setAlignment(Pos.CENTER_RIGHT); // 垂直居中，右对齐
        
        // 使用Region填充中间空间，实现两端对齐
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // 创建主容器，将positionBox、spacer和bottomBox水平排列
        HBox mainContainer = new HBox(positionBox, spacer, bottomBox);
        mainContainer.setAlignment(Pos.CENTER_LEFT); // 垂直居中
        HBox.setHgrow(mainContainer, javafx.scene.layout.Priority.ALWAYS);
        this.getChildren().add(mainContainer);
    }
    
    /**
     * 显示登录对话框
     * @param ownerWindow 父窗口
     */
    private void showLoginDialog(Window ownerWindow) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(ownerWindow);
        dialog.setTitle("用户登录");
        dialog.setHeaderText(null);

        // 创建头像图片
        ImageView avatar = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/KBMS.png"));
            avatar.setImage(image);
            avatar.setFitWidth(60);
            avatar.setFitHeight(60);
        } catch (Exception e) {
            // 如果图片加载失败，则创建一个替代的文本
            avatar.setFitWidth(60);
            avatar.setFitHeight(60);
            // 可以使用一个简单的矩形替代图片
        }
        
        // 创建用户名和密码输入框
        TextField usernameField = new TextField();
        usernameField.setPromptText("用户名");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("密码");

        // 创建注册链接
        Text registerLink = new Text("还没有账号？点击注册");
        registerLink.setFill(Color.BLUE);
        registerLink.setUnderline(true);
        registerLink.setOnMouseEntered(e -> registerLink.setCursor(javafx.scene.Cursor.HAND));
        registerLink.setOnMouseExited(e -> registerLink.setCursor(javafx.scene.Cursor.DEFAULT));
        registerLink.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/keymouseshare"));
                } catch (Exception e) {
                    // 忽略异常或显示错误消息
                    e.printStackTrace();
                }
            }
        });

        // 创建布局
        HBox avatarBox = new HBox();
        avatarBox.setAlignment(Pos.CENTER);
        avatarBox.getChildren().add(avatar);
        avatarBox.setPadding(new Insets(0, 0, 10, 0));
        
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(
            avatarBox,
            new Label("用户名:"),
            usernameField,
            new Label("密码:"),
            passwordField,
            registerLink
        );
        vbox.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 显示对话框并等待用户操作
        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // 这里可以处理登录逻辑
                String username = usernameField.getText();
                String password = passwordField.getText();
                
                // 简单示例：更新按钮文本为用户名首字母
                // 在实际应用中，这里应该验证用户凭据
                if (!username.isEmpty()) {
                    // 获取userInfoButton并更新文本
                    HBox positionBox = (HBox) getChildren().get(0);
                    Button userInfoButton = (Button) positionBox.getChildren().get(0);
                    userInfoButton.setText(username.substring(0, Math.min(2, username.length())));
                }
            }
        });
    }
    
    /**
     * 更新鼠标位置显示
     * @param x X坐标
     * @param y Y坐标
     */
    public void updateMousePosition(int x, int y) {
        xPositionLabel.setText("X: " + x);
        yPositionLabel.setText("Y: " + y);
        
        // 是主控端端时才显示虚拟屏幕信息
        updateScreenInfo();
    }
    
    /**
     * 使用JavaFX获取并更新屏幕信息
     */
    public void updateScreenInfo() {
        // 显示更详细的信息，包括屏幕分辨率和完整坐标空间信息
        if(virtualDesktopStorage.isApplyVirtualDesktopScreen()){
            screenPositionLabel.setText("虚拟桌面【"+virtualDesktopStorage.getActiveScreen().getScreenName() +"】坐标: ("+ virtualDesktopStorage.getMouseLocation()[0]+","+ virtualDesktopStorage.getMouseLocation()[1]+")");
        }
    }

    public void serverDeviceStart() {
        // 如果当前设备是服务器则，启动服务器按钮变为停止服务器。如果不是则禁用该按钮
        if (NetUtil.getLocalIpAddress().equals(DeviceStorage.getInstance().getSeverDevice().getIpAddress())) {
            startServerButton.setStyle("-fx-opacity: 0.8;-fx-background-color: rgb(115,5,0);-fx-border-radius: 10px;-fx-font-weight: bolder");
            startServerButton.setText("关闭");
            // 隐藏保存虚拟桌面按钮
            applayScreenButton.setVisible(true);
            applayScreenButton.setText("应用设置");
            applayScreenButton.setDisable(false);
        } else {
            startServerButton.setDisable(true);
            applayScreenButton.setText("请在控制端设置屏幕");
            applayScreenButton.setDisable(true);
        }
    }

    public void serverDeviceStop() {
        startServerButton.setDisable(false);
        startServerButton.setText("设为主控");
        applayScreenButton.setVisible(false);
    }

}