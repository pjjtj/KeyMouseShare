package com.keymouseshare.uifx;

import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.bean.VirtualDesktopStorage;
import com.keymouseshare.bean.VirtualDesktopStorageListener;
import com.keymouseshare.network.DeviceDiscovery;
import com.keymouseshare.util.NetUtil;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 屏幕预览UI组件
 */
public class ScreenPreviewUI extends VBox implements VirtualDesktopStorageListener {

    private static final Logger logger = Logger.getLogger(ScreenPreviewUI.class.getName());

    private int scale = 15;
    private GridPane screenGrid;
    private Map<StackPane, String> screenMap = new HashMap<>();
    private StackPane draggedScreen = null;
    private double mouseXOffset = 0;
    private double mouseYOffset = 0;
    private DeviceDiscovery deviceDiscovery;
    private Button saveVirtualDesktopButton = new Button("应用设置");
    private VirtualDesktopStorage virtualDesktopStorage;

    // 吸附阈值
    private static final double REAL_TIME_SNAP_THRESHOLD = 20.0;

    public ScreenPreviewUI(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
        // 初始化虚拟桌面存储引用
        this.virtualDesktopStorage = VirtualDesktopStorage.getInstance();
        // 添加监听器
        this.virtualDesktopStorage.addListener(this);
        // 初始化界面
        initializeUI();
    }

    public void setDeviceDiscovery(DeviceDiscovery deviceDiscovery) {
        this.deviceDiscovery = deviceDiscovery;
    }

    private void initializeUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        // 设置左边框为虚线
        this.setStyle("-fx-border-color: gray; -fx-border-width: 0 0 0 1; -fx-border-style: dashed;");

        Label titleLabel = new Label("屏幕预览");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        screenGrid = new GridPane();
        screenGrid.setHgap(10);
        screenGrid.setVgap(10);
        screenGrid.setPrefHeight(500);

        VBox bottomBox = new VBox();
        bottomBox.getChildren().add(saveVirtualDesktopButton);
        saveVirtualDesktopButton.setVisible(false);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);

        this.getChildren().addAll(titleLabel, screenGrid, bottomBox);

        // 加载虚拟桌面中的屏幕信息
        loadVirtualDesktopScreens();

        saveVirtualDesktopButton.setOnAction(event -> {
            screenMap.keySet().forEach(screen -> {
                ScreenInfo screenInfo = VirtualDesktopStorage.getInstance().getScreens().get(screenMap.get(screen));
                // screenInfo 获取屏幕坐标
                screenInfo.setMx(screen.getBoundsInParent().getMinX());
                screenInfo.setMy(screen.getBoundsInParent().getMinY());
                System.out.println(screenInfo.getMx()+"-----"+screenInfo.getMy());
                VirtualDesktopStorage.getInstance().addScreen(screenInfo);
            });
        });
    }

    /**
     * 从虚拟桌面加载屏幕信息
     */
    private void loadVirtualDesktopScreens() {
        // 清空现有屏幕
        screenGrid.getChildren().clear();
        screenMap.clear();
        
        // 从虚拟桌面获取所有屏幕
        Map<String, ScreenInfo> screens = virtualDesktopStorage.getScreens();
        
        if (screens != null) {
            int col = 0;
            int row = 0;
            for (Map.Entry<String, ScreenInfo> entry : screens.entrySet()) {
                ScreenInfo screenInfo = entry.getValue();
                String screenName = screenInfo.getDeviceIp() + ":" + screenInfo.getScreenName();
                
                // 添加屏幕项到网格中
                addScreenItem(screenInfo, col, row, false);
                
                col++;
                if (col > 2) { // 每行最多3个屏幕
                    col = 0;
                    row++;
                }
            }
        }
    }

    private void addScreenItem(ScreenInfo screenInfo, int col, int row, boolean isSelected) {
        // 创建屏幕预览框，根据实际屏幕尺寸设置大小
        double screenWidth = (double) screenInfo.getWidth() / scale; // 缩放比例，最小100像素
        double screenHeight = (double) screenInfo.getHeight() / scale; // 缩放比例，最小80像素
        Rectangle screenRect = new Rectangle(screenWidth, screenHeight);
        screenRect.setArcWidth(10);
        screenRect.setArcHeight(10);

        // 设置屏幕颜色（根据设备IP设置不同颜色）
        String deviceIp = screenInfo.getDeviceIp();
        if (deviceIp != null) {
            int hash = deviceIp.hashCode();
            Color color = Color.rgb(
                Math.abs(hash) % 256,
                Math.abs(hash >> 8) % 256,
                Math.abs(hash >> 16) % 256,
                0.7
            );
            screenRect.setFill(color);
        } else {
            screenRect.setFill(Color.LIGHTGRAY);
        }

        // 如果是选中设备，添加边框
        if (isSelected) {
            screenRect.setStroke(Color.BLUE);
            screenRect.setStrokeWidth(2);
        }

        // 创建中心点标记
        Circle centerPoint = new Circle(3, Color.RED);

        // 创建中心点坐标标签
        Label centerLabel = new Label();
        centerLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: black; -fx-font-weight: bold;");
        centerLabel.setVisible(false); // 默认不显示坐标

        // 创建屏幕标签
        Label screenLabel = new Label(screenInfo.getDeviceIp()+"\n"+screenInfo.getScreenName()+ "\n" + screenInfo.getWidth() + "x" + screenInfo.getHeight());
        screenLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");

        // 创建包含容器，使标签悬浮在屏幕上
        StackPane screenContainer = new StackPane();
        screenContainer.getChildren().addAll(screenRect, centerPoint, centerLabel, screenLabel);
        StackPane.setAlignment(centerPoint, Pos.CENTER); // 中心点居中
        StackPane.setAlignment(screenLabel, Pos.CENTER);
        StackPane.setAlignment(centerLabel, Pos.TOP_CENTER); // 坐标标签在顶部居中
        StackPane.setMargin(centerLabel, new Insets(5, 0, 0, 0)); // 设置坐标标签边距

        // 添加鼠标悬停事件来显示/隐藏中心点坐标
        screenContainer.setOnMouseEntered(e -> {
            centerLabel.setVisible(true);

            // 获取屏幕容器的中心点坐标
            Bounds bounds = screenContainer.getBoundsInParent();
            double centerX = bounds.getMinX() + bounds.getWidth() / 2;
            double centerY = bounds.getMinY() + bounds.getHeight() / 2;

            // 更新坐标标签文本
            centerLabel.setText(String.format("(%.0f, %.0f)", centerX, centerY));
        });

        screenContainer.setOnMouseExited(e -> centerLabel.setVisible(false));

        // 添加拖拽支持
        addDragSupport(screenContainer, screenInfo.getDeviceIp()+screenInfo.getScreenName());

        // 使用mx, my作为起点位置
        if (screenInfo.getMx() != 0 || screenInfo.getMy() != 0) {
            // 如果mx和my已经设置，则使用它们作为起始位置
            screenContainer.setTranslateX(screenInfo.getMx());
            screenContainer.setTranslateY(screenInfo.getMy());
        }

        // 添加到网格和映射中
        screenGrid.add(screenContainer, col, row);

        screenMap.put(screenContainer, screenInfo.getDeviceIp()+screenInfo.getScreenName());
    }

    /**
     * 设置指定屏幕为选中状态
     * @param IpAddr 屏幕名称
     */
    public void selectScreen(String IpAddr) {
        // 遍历所有屏幕项
        for (Map.Entry<StackPane, String> entry : screenMap.entrySet()) {
            StackPane screenContainer = entry.getKey();
            String name = entry.getValue();

            // 找到对应的屏幕项
            if (name.startsWith(IpAddr)) {
                // 获取屏幕矩形（第一个子节点应该是Rectangle）
                if (!screenContainer.getChildren().isEmpty() &&
                        screenContainer.getChildren().get(0) instanceof Rectangle) {

                    Rectangle screenRect = (Rectangle) screenContainer.getChildren().get(0);

                    // 设置选中状态的边框
                    screenRect.setStroke(Color.BLUE);
                    screenRect.setStrokeWidth(2);
                }
            } else {
                // 取消其他屏幕的选中状态
                if (!screenContainer.getChildren().isEmpty() &&
                        screenContainer.getChildren().get(0) instanceof Rectangle) {

                    Rectangle screenRect = (Rectangle) screenContainer.getChildren().get(0);

                    // 移除选中状态的边框
                    screenRect.setStroke(null);
                    screenRect.setStrokeWidth(0);
                }
            }
        }
    }

    private void addDragSupport(StackPane screenContainer,String screenId) {
        screenContainer.setOnMousePressed((MouseEvent event) -> {
            // 记录被拖拽的屏幕和鼠标位置
            draggedScreen = screenContainer;
            mouseXOffset = event.getSceneX() - screenContainer.getTranslateX();
            mouseYOffset = event.getSceneY() - screenContainer.getTranslateY();
            event.consume();
        });

        screenContainer.setOnMouseDragged((MouseEvent event) -> {
            if (draggedScreen != null) {
                // 计算新位置
                double newX = event.getSceneX() - mouseXOffset;
                double newY = event.getSceneY() - mouseYOffset;

                logger.log(Level.FINE, "Mouse dragged to (" + event.getSceneX() + ", " + event.getSceneY() +
                        "), calculating new position (" + newX + ", " + newY + ")");

                draggedScreen.setTranslateX(newX);
                draggedScreen.setTranslateY(newY);

                // 实时边缘吸附预览效果
                performRealTimeSnapping(draggedScreen,newX,newY);

            }
            event.consume();
        });
    }

    /**
     * 实时边缘吸附（用于拖拽过程中提供视觉反馈）
     * @param screen 被拖拽的屏幕
     * @return 应用了实时吸附的坐标数组 [x, y]
     */
    private void performRealTimeSnapping(StackPane screen,double newX,double newY) {
        Bounds screenBounds = screen.getBoundsInParent();
        String sourceScreenName = screenMap.get(screen);

        // 获取屏幕的实际宽度和高度
        double screenActualWidth = screenBounds.getWidth();
        double screenActualHeight = screenBounds.getHeight();

        // 计算当前屏幕在容器中的位置
        double screenContainerX = screenBounds.getMinX();
        double screenContainerY = screenBounds.getMinY();

        // 计算当前屏幕的中心点
        double screenCenterX = screenContainerX + screenActualWidth / 2;
        double screenCenterY = screenContainerY + screenActualHeight / 2;

        // 计算当前屏幕的四条边
        double screenLeft = screenContainerX;
        double screenRight = screenContainerX + screenActualWidth;
        double screenTop = screenContainerY;
        double screenBottom = screenContainerY + screenActualHeight;

        // 第一步：遍历获取一个中心距离最近，且不存在重叠的目标矩形
        StackPane bestTarget = null;
        String targetScreenName = "";
        double minCenterDistance = Double.MAX_VALUE;

        for (Node node : screenGrid.getChildren()) {
            if (node instanceof StackPane && node != screen) {
                StackPane otherScreen = (StackPane) node;
                String otherScreenName = screenMap.get(otherScreen);
                Bounds otherBounds = otherScreen.getBoundsInParent();

                // 获取其他屏幕的实际宽度和高度
                double otherActualWidth = otherBounds.getWidth();
                double otherActualHeight = otherBounds.getHeight();

                // 计算其他屏幕在容器中的位置
                double otherContainerX = otherBounds.getMinX();
                double otherContainerY = otherBounds.getMinY();

                // 计算其他屏幕的中心点
                double otherCenterX = otherContainerX + otherActualWidth / 2;
                double otherCenterY = otherContainerY + otherActualHeight / 2;

                // 计算中心点距离
                double centerDistance = Math.sqrt(Math.pow(screenCenterX - otherCenterX, 2) +
                        Math.pow(screenCenterY - otherCenterY, 2));

                // 检查是否重叠
                double otherLeft = otherContainerX;
                double otherRight = otherContainerX + otherActualWidth;
                double otherTop = otherContainerY;
                double otherBottom = otherContainerY + otherActualHeight;

                boolean isOverlapping = !(screenRight < otherLeft ||
                        screenLeft > otherRight ||
                        screenBottom < otherTop ||
                        screenTop > otherBottom);

                // 如果不重叠且距离更近，则更新最佳目标
                if (!isOverlapping && centerDistance < minCenterDistance) {
                    minCenterDistance = centerDistance;
                    bestTarget = otherScreen;
                    targetScreenName = otherScreenName;
                }
            }
        }

        // 如果找到了最近的不重叠目标，则进行后续处理
        if (bestTarget != null) {
            logger.log(Level.INFO, "Found nearest non-overlapping target: source screen '" + sourceScreenName +
                    "' to target screen '" + targetScreenName + "', center distance: " + minCenterDistance);

            // 第二步：计算源矩形的四条边与目标矩形四条边的对应距离
            Bounds targetBounds = bestTarget.getBoundsInParent();
            double targetActualWidth = targetBounds.getWidth();
            double targetActualHeight = targetBounds.getHeight();
            double targetContainerX = targetBounds.getMinX();
            double targetContainerY = targetBounds.getMinY();

            // 计算目标屏幕的四条边
            double targetLeft = targetContainerX;
            double targetRight = targetContainerX + targetActualWidth;
            double targetTop = targetContainerY;
            double targetBottom = targetContainerY + targetActualHeight;

            // 计算源矩形的四条边与目标矩形四条边的对应距离
            // 源矩形的左边与目标矩形的右边
            double distanceLeftToRight = Math.abs(screenLeft - targetRight);

            // 源矩形的右边与目标矩形的左边
            double distanceRightToLeft = Math.abs(screenRight - targetLeft);

            // 源矩形的上边与目标矩形的下边
            double distanceTopToBottom = Math.abs(screenTop - targetBottom);

            // 源矩形的下边与目标矩形的上边
            double distanceBottomToTop = Math.abs(screenBottom - targetTop);


            // 获取最小距离
            double minEdgeDistance = Math.min(
                    Math.min(distanceLeftToRight, distanceRightToLeft),
                    Math.min(distanceTopToBottom, distanceBottomToTop)
            );

            // 判断是否满足吸附阈值
            if (minEdgeDistance < REAL_TIME_SNAP_THRESHOLD) {
                logger.log(Level.INFO, "Real-time snapping triggered: source screen '" + sourceScreenName +
                        "' to target screen '" + targetScreenName + "', min edge distance: " + minEdgeDistance +
                        ", threshold: " + REAL_TIME_SNAP_THRESHOLD);
                // 垂直有重叠
                boolean b = (screenTop > targetBottom  || screenBottom < targetTop);
                if(minEdgeDistance == distanceLeftToRight && !b){
                    screen.setTranslateX(newX-minEdgeDistance);
                }
                if(minEdgeDistance == distanceRightToLeft && !b){
                    screen.setTranslateX(newX+minEdgeDistance);
                }
                // 水平有重叠
                boolean c = (screenLeft > targetRight  || screenRight < targetLeft);
                if(minEdgeDistance ==  distanceTopToBottom && !c){
                    screen.setTranslateY(newY-minEdgeDistance);
                }
                if(minEdgeDistance == distanceBottomToTop && !c){
                    screen.setTranslateY(newY+minEdgeDistance);
                }
            } else {
                logger.log(Level.INFO, "No real-time snapping: source screen '" + sourceScreenName +
                        "' to target screen '" + targetScreenName + "', min edge distance: " + minEdgeDistance +
                        ", threshold: " + REAL_TIME_SNAP_THRESHOLD);
            }
        } else {
            logger.log(Level.INFO, "No non-overlapping target found for screen '" + sourceScreenName + "'");
        }

    }

    public void serverDeviceStart() {
        // 如果当前设备是服务器则，启动服务器按钮变为停止服务器。如果不是则禁用该按钮
        if(NetUtil.getLocalIpAddress().equals(DeviceStorage.getInstance().getSeverDevice().getIpAddress())){
            // 隐藏保存虚拟桌面按钮
            saveVirtualDesktopButton.setVisible(true);
            saveVirtualDesktopButton.setText("应用设置");
            saveVirtualDesktopButton.setDisable(false);
        }else{
            saveVirtualDesktopButton.setText("请在控制端设置屏幕");
            saveVirtualDesktopButton.setDisable(true);
        }
    }

    public void serverDeviceStop() {
        // 隐藏保存虚拟桌面按钮
        saveVirtualDesktopButton.setVisible(false);
    }

    /**
     * 刷新屏幕预览，从虚拟桌面重新加载屏幕信息
     */
    public void refreshScreens() {
        loadVirtualDesktopScreens();
    }
    
    /**
     * 当虚拟桌面发生变化时调用此方法
     */
    @Override
    public void onVirtualDesktopChanged() {
        // 在JavaFX应用程序线程中刷新屏幕预览
        javafx.application.Platform.runLater(this::refreshScreens);
    }
}