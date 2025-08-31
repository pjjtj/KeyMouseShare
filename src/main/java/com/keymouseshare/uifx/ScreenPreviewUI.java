package com.keymouseshare.uifx;

import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.bean.VirtualDesktopStorage;
import com.keymouseshare.util.NetUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 屏幕预览UI组件
 */
public class ScreenPreviewUI extends VBox {

    private static final Logger logger = Logger.getLogger(ScreenPreviewUI.class.getName());

    private double scale = 10.0;
    private Pane screenPane;
    private Circle virtualPoint;
    private Label virtualPointLabel;
    private Map<StackPane, String> screenMap = new HashMap<>();
    private StackPane draggedScreen = null;
    private int mouseXOffset = 0;
    private int mouseYOffset = 0;
    private Button saveVirtualDesktopButton = new Button("应用设置");
    private VirtualDesktopStorage virtualDesktopStorage;
    private ScheduledExecutorService virtualDesktopExecutor = Executors.newScheduledThreadPool(1);

    // 缩放相关属性
    private ScrollPane scrollPane;
    private Scale scaleTransform;
    private double currentScale = 1.0;
    private static final double SCALE_DELTA = 0.1;
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;

    // 吸附阈值
    private static final double REAL_TIME_SNAP_THRESHOLD = 20.0;

    public ScreenPreviewUI(VirtualDesktopStorage virtualDesktopStorage) {
        // 添加监听器
        this.virtualDesktopStorage = virtualDesktopStorage;
        // 初始化界面
        initializeUI();
    }

    private void initializeUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        // 设置左边框为虚线
        this.setStyle("-fx-border-color: gray; -fx-border-width: 0 0 0 1; -fx-border-style: dashed;");

        Label titleLabel = new Label("屏幕预览");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        screenPane = new Pane();
        screenPane.setPrefSize(800, 600); // 设置默认大小
        
        // 创建缩放变换
        scaleTransform = new Scale(currentScale, currentScale, 0, 0);
        screenPane.getTransforms().add(scaleTransform);

        // 添加鼠标滚轮缩放支持
        screenPane.addEventFilter(ScrollEvent.ANY, this::handleScroll);

        VBox bottomBox = new VBox();
        bottomBox.getChildren().add(saveVirtualDesktopButton);
        saveVirtualDesktopButton.setVisible(false);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);

        this.getChildren().addAll(titleLabel, screenPane, bottomBox);

        // 加载虚拟桌面中的屏幕信息
        loadVirtualDesktopScreens();

        saveVirtualDesktopButton.setOnAction(event -> {
            virtualDesktopStorage.applyVirtualDesktopScreen(screenMap, scale);
//            virtualDesktopExecutor.scheduleAtFixedRate(this::showVirtualDesktopInfo, 0, 5, TimeUnit.MILLISECONDS);
        });
    }

//    private void showVirtualDesktopInfo() {
//        if(virtualDesktopStorage.getActiveScreen()!= null){
//            virtualPoint.setLayoutX(virtualDesktopStorage.getMouseLocation()[0]/scale);
//            virtualPoint.setLayoutY(virtualDesktopStorage.getMouseLocation()[1]/scale);
//            virtualPointLabel.setText(String.format("(%.2f, %.2f)",virtualDesktopStorage.getMouseLocation()[0],virtualDesktopStorage.getMouseLocation()[1]));
//            virtualPointLabel.setLayoutX(virtualDesktopStorage.getMouseLocation()[0]/scale);
//            virtualPointLabel.setLayoutY(virtualDesktopStorage.getMouseLocation()[1]/scale);
//            virtualPoint.setVisible(true);
//            virtualPointLabel.setVisible(true);
//        }
//    }


    
    /**
     * 处理鼠标滚轮事件实现缩放功能
     * @param event 滚轮事件
     */
    private void handleScroll(ScrollEvent event) {
        // 检查是否是Ctrl键配合滚轮操作（常见缩放快捷键）
        if (event.isControlDown()) {
            event.consume(); // 消费事件，防止滚动窗格滚动
            
            // 计算新的缩放比例
            if (event.getDeltaY() > 0) {
                // 向上滚动，放大
                currentScale += SCALE_DELTA;
            } else {
                // 向下滚动，缩小
                currentScale -= SCALE_DELTA;
            }
            
            // 限制缩放范围
            currentScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, currentScale));
            
            // 应用缩放变换
            scaleTransform.setX(currentScale);
            scaleTransform.setY(currentScale);
        }
    }

    /**
     * 从虚拟桌面加载屏幕信息
     */
    private void loadVirtualDesktopScreens() {
        // 清空现有屏幕
        screenPane.getChildren().clear();
        screenMap.clear();
        
        // 从虚拟桌面获取所有屏幕
        Map<String, ScreenInfo> screens = virtualDesktopStorage.getScreens();
        
        if (screens != null) {
            for (Map.Entry<String, ScreenInfo> entry : screens.entrySet()) {
                ScreenInfo screenInfo = entry.getValue();
                String screenName = screenInfo.getDeviceIp() + ":" + screenInfo.getScreenName();
                
                // 添加屏幕项到面板中
                addScreenItem(screenInfo, false);
            }
        }
    }

    private void addScreenItem(ScreenInfo screenInfo, boolean isSelected) {
        // 创建屏幕预览框，根据实际屏幕尺寸设置大小
        int screenWidth = (int) (screenInfo.getWidth() / scale); // 缩放比例，最小100像素
        int screenHeight = (int) (screenInfo.getHeight() / scale); // 缩放比例，最小80像素
        Rectangle screenRect = new Rectangle(screenWidth, screenHeight);
//        screenRect.setArcWidth(10);
//        screenRect.setArcHeight(10);

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

        // 如果是选中设备，设置透明度
        if (isSelected) {
            screenRect.setOpacity(0.8);
        }

        // 创建中心点标记
        Circle centerPoint = new Circle(3, Color.RED);

        // 创建中心点坐标标签
        Label centerLabel = new Label();
        centerLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: black; -fx-font-weight: bold;");
        centerLabel.setVisible(false); // 默认不显示坐标

        // 创建屏幕标签
        Label screenLabel = new Label(screenInfo.getDeviceIp()+"\n"+screenInfo.getScreenName()+ "\n" + screenWidth + "x" + screenHeight);
        screenLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");

        // 创建包含容器，使标签悬浮在屏幕上
        StackPane screenContainer = new StackPane();
        screenContainer.setStyle("-fx-background-color: grey;");
        screenContainer.setMinSize(screenWidth, screenHeight);
        screenContainer.setMaxSize(screenWidth, screenHeight);
        screenContainer.getChildren().addAll(screenRect, centerPoint,screenLabel,centerLabel);
        screenContainer.setStyle("-fx-padding: 0;");
        StackPane.setAlignment(screenRect, Pos.TOP_LEFT);
        StackPane.setAlignment(centerPoint, Pos.CENTER); // 中心点居中
        StackPane.setAlignment(screenLabel, Pos.CENTER);
        StackPane.setAlignment(centerLabel, Pos.TOP_CENTER); // 坐标标签在顶部居中

        // 添加鼠标悬停事件来显示/隐藏中心点坐标
        screenContainer.setOnMouseEntered(e -> {
            centerLabel.setVisible(true);

            // 获取屏幕容器的中心点坐标
            Bounds bounds = screenContainer.getBoundsInParent();

            // 更新坐标标签文本
            centerLabel.setText(String.format("(%.0f, %.0f, %.0f, %.0f)", bounds.getMinX(),  bounds.getMinY(), bounds.getWidth(), bounds.getHeight()));
        });

        screenContainer.setOnMouseExited(e -> centerLabel.setVisible(false));

        // 添加拖拽支持
        addDragSupport(screenContainer, screenInfo.getDeviceIp()+screenInfo.getScreenName());

        // 使用mx, my作为起点位置
        if (screenInfo.getMx() != 0 || screenInfo.getMy() != 0) {
            // 如果mx和my已经设置，则使用它们作为起始位置
            screenContainer.setLayoutX(screenInfo.getMx());
            screenContainer.setLayoutY(screenInfo.getMy());
        } else {
            // 默认位置
            screenContainer.setLayoutX(0.0);
            screenContainer.setLayoutY(0.0);
        }

        // 添加到面板和映射中

        // 创建虚拟鼠标标记
        virtualPoint = new Circle(3, Color.BLACK);
        virtualPoint.setVisible(false);
        // 创建中心点坐标标签
        virtualPointLabel = new Label();
        centerLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: black; -fx-font-weight: bold;");
        centerLabel.setVisible(false); // 默认不显示坐标
        screenPane.getChildren().addAll(screenContainer,virtualPoint,virtualPointLabel);

        screenMap.put(screenContainer, screenInfo.getDeviceIp()+screenInfo.getScreenName());
    }
    
    /**
     * 调整屏幕位置以避免重叠
     * @param screenContainer 屏幕容器
     */
    private void adjustPositionToAvoidOverlap(StackPane screenContainer) {
        // 获取当前屏幕的边界
        Bounds currentBounds = screenContainer.getBoundsInParent();
        int currentX = (int) currentBounds.getMinX();
        int currentY = (int) currentBounds.getMinY();
        int currentWidth = (int) currentBounds.getWidth();
        int currentHeight = (int) currentBounds.getHeight();
        
        boolean hasOverlap;
        do {
            hasOverlap = false;
            // 检查与所有已存在的屏幕是否有重叠
            for (Map.Entry<StackPane, String> entry : screenMap.entrySet()) {
                StackPane otherScreen = entry.getKey();
                Bounds otherBounds = otherScreen.getBoundsInParent();
                
                // 检查是否重叠
                if (currentX < otherBounds.getMaxX() && 
                    currentX + currentWidth > otherBounds.getMinX() &&
                    currentY < otherBounds.getMaxY() && 
                    currentY + currentHeight > otherBounds.getMinY()) {
                    
                    // 发现重叠，调整位置
                    currentX += currentWidth + 10; // 在X轴上移动，留出10像素间隔
                    screenContainer.setLayoutX(currentX);
                    currentBounds = screenContainer.getBoundsInParent();
                    hasOverlap = true;
                    break; // 重新开始检查
                }
            }
        } while (hasOverlap);
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

                    // 设置选中状态
                    screenRect.setOpacity(0.8);
                }
            } else {
                // 取消其他屏幕的选中状态
                if (!screenContainer.getChildren().isEmpty() &&
                        screenContainer.getChildren().get(0) instanceof Rectangle) {

                    Rectangle screenRect = (Rectangle) screenContainer.getChildren().get(0);

                    // 移除选中状态
                    screenRect.setOpacity(1);
                }
            }
        }
    }

    private void addDragSupport(StackPane screenContainer,String screenId) {
        screenContainer.setOnMousePressed((MouseEvent event) -> {
            // 记录被拖拽的屏幕和鼠标位置
            draggedScreen = screenContainer;
            mouseXOffset = (int) (event.getSceneX() - screenContainer.getLayoutX());
            mouseYOffset = (int) (event.getSceneY() - screenContainer.getLayoutY());
            event.consume();
        });

        screenContainer.setOnMouseDragged((MouseEvent event) -> {
            if (draggedScreen != null) {
                // 计算新位置
                int newX = (int) (event.getSceneX() - mouseXOffset);
                int newY = (int) (event.getSceneY() - mouseYOffset);

                logger.log(Level.FINE, "Mouse dragged to (" + event.getSceneX() + ", " + event.getSceneY() +
                        "), calculating new position (" + newX + ", " + newY + ")");

                draggedScreen.setLayoutX(newX);
                draggedScreen.setLayoutY(newY);

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
    private void performRealTimeSnapping(StackPane screen,int newX,int newY) {
        Bounds screenBounds = screen.getBoundsInParent();
        String sourceScreenName = screenMap.get(screen);

        // 获取屏幕的实际宽度和高度
        int screenActualWidth = (int) screenBounds.getWidth();
        int screenActualHeight = (int) screenBounds.getHeight();

        // 计算当前屏幕在容器中的位置
        int screenContainerX = (int) screenBounds.getMinX();
        int screenContainerY = (int) screenBounds.getMinY();

        // 计算当前屏幕的中心点
        int screenCenterX = screenContainerX + screenActualWidth / 2;
        int screenCenterY = screenContainerY + screenActualHeight / 2;

        // 计算当前屏幕的四条边
        int screenLeft = screenContainerX;
        int screenRight = screenContainerX + screenActualWidth;
        int screenTop = screenContainerY;
        int screenBottom = screenContainerY + screenActualHeight;

        // 第一步：遍历获取一个中心距离最近，且不存在重叠的目标矩形
        StackPane bestTarget = null;
        String targetScreenName = "";
        double minCenterDistance = Double.MAX_VALUE;

        for (Node node : screenPane.getChildren()) {
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

        // 如果找到了最佳目标，则计算边缘距离并进行实时吸附
        if (bestTarget != null) {
            Bounds targetBounds = bestTarget.getBoundsInParent();
            double targetLeft = targetBounds.getMinX();
            double targetRight = targetBounds.getMaxX();
            double targetTop = targetBounds.getMinY();
            double targetBottom = targetBounds.getMaxY();

            // 计算边缘距离
            double distanceLeftToRight =  Math.abs(screenLeft - targetRight);
            double distanceRightToLeft =  Math.abs(screenRight - targetLeft);
            double distanceTopToBottom =  Math.abs(screenTop - targetBottom);
            double distanceBottomToTop =  Math.abs(screenBottom - targetTop);

            // 找到最小边缘距离
            double minEdgeDistance = Math.min(
                    Math.min(distanceLeftToRight, distanceRightToLeft),
                    Math.min(distanceTopToBottom, distanceBottomToTop)
            );

            // 如果最小边缘距离小于阈值，则进行实时吸附
            if (minEdgeDistance < REAL_TIME_SNAP_THRESHOLD) {
                logger.log(Level.INFO, "Real-time snapping: source screen '" + sourceScreenName +
                        "' to target screen '" + targetScreenName + "', min edge distance: " + minEdgeDistance +
                        ", threshold: " + REAL_TIME_SNAP_THRESHOLD);

                // 根据最小边缘距离调整位置
                if (minEdgeDistance == distanceLeftToRight&&!(screenTop>targetBottom||screenBottom<targetTop)) {
                    logger.log(Level.INFO, "Left to right");
                    screen.setLayoutX(targetRight);
                }
                if (minEdgeDistance == distanceRightToLeft&&!(screenTop>targetBottom||screenBottom<targetTop)) {
                    logger.log(Level.INFO, "Right to left");
                    screen.setLayoutX(targetLeft-screen.getWidth());
                }
                if (minEdgeDistance == distanceTopToBottom&&!(screenRight>targetRight||screenLeft<targetLeft)) {
                    logger.log(Level.INFO, "Top to bottom");
                    screen.setLayoutY(targetBottom);
                }
                if (minEdgeDistance == distanceBottomToTop&&!(screenRight>targetRight||screenLeft<targetLeft)) {
                    logger.log(Level.INFO, "Bottom to top");
                    screen.setLayoutY(targetTop-screen.getHeight());
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

}