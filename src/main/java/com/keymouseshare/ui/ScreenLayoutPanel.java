package com.keymouseshare.ui;

import com.keymouseshare.screen.DeviceScreen;
import com.keymouseshare.screen.ScreenLayoutConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * 屏幕布局面板，用于可视化配置和显示屏幕排列
 */
public class ScreenLayoutPanel extends JPanel {
    private static final int GRID_SIZE = 20;
    private static final Color SCREEN_COLOR = new Color(70, 130, 180, 200);
    private static final Color CURRENT_SCREEN_COLOR = new Color(60, 179, 113, 200);
    private static final Color SELECTED_SCREEN_COLOR = new Color(255, 165, 0, 200);
    private static final Color GRID_COLOR = new Color(200, 200, 200);
    
    // 吸附距离阈值
    private static final int SNAP_DISTANCE = 200;
    
    private ScreenLayoutConfig layoutConfig;
    private DeviceScreen selectedScreen;
    private List<Rectangle> screenRectangles;
    private int offsetX = 50;
    private int offsetY = 50;
    private double scale = 0.5; // 缩放比例
    
    // 用于保持屏幕相对尺寸的基准屏幕大小
    private int baseScreenWidth;
    private int baseScreenHeight;
    
    // 拖拽相关变量
    private DeviceScreen draggedScreen = null;
    private Point dragStartPoint = null;
    private Point screenStartPoint = null;
    
    public ScreenLayoutPanel(ScreenLayoutConfig layoutConfig) {
        this.layoutConfig = layoutConfig;
        this.screenRectangles = new ArrayList<>();
        
        // 获取实际屏幕分辨率，如果获取不到则使用默认值1920x1080
        int[] screenSize = getScreenSize();
        this.baseScreenWidth = screenSize[0];
        this.baseScreenHeight = screenSize[1];
        
        // 设置面板属性
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
        
        // 添加鼠标监听器
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * 处理鼠标点击事件
     * @param e 鼠标事件
     */
    private void handleMouseClick(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        // 检查是否点击了某个屏幕
        DeviceScreen clickedScreen = null;
        for (int i = 0; i < screenRectangles.size(); i++) {
            Rectangle rect = screenRectangles.get(i);
            if (rect.contains(x, y)) {
                List<DeviceScreen> screens = layoutConfig.getAllScreens();
                if (i < screens.size()) {
                    clickedScreen = screens.get(i);
                    break;
                }
            }
        }
        
        // 更新选中屏幕
        if (clickedScreen != selectedScreen) {
            selectedScreen = clickedScreen;
            repaint();
            
            // 通知监听器屏幕被选中
            firePropertyChange("selectedScreen", null, selectedScreen);
        }
    }
    
    /**
     * 处理鼠标按下事件
     * @param e 鼠标事件
     */
    private void handleMousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        // 检查是否点击了某个屏幕以开始拖拽
        for (int i = 0; i < screenRectangles.size(); i++) {
            Rectangle rect = screenRectangles.get(i);
            if (rect.contains(x, y)) {
                List<DeviceScreen> screens = layoutConfig.getAllScreens();
                if (i < screens.size()) {
                    draggedScreen = screens.get(i);
                    dragStartPoint = new Point(x, y);
                    screenStartPoint = new Point(draggedScreen.getX(), draggedScreen.getY());
                    selectedScreen = draggedScreen;
                    repaint();
                    break;
                }
            }
        }
    }
    
    /**
     * 处理鼠标释放事件
     * @param e 鼠标事件
     */
    private void handleMouseReleased(MouseEvent e) {
        if (draggedScreen != null) {
            // 应用吸附效果
            applySnapEffect(draggedScreen);
            
            // 更新布局配置中的屏幕位置
            layoutConfig.updateScreen(draggedScreen);
            draggedScreen = null;
            dragStartPoint = null;
            screenStartPoint = null;
            repaint();
            
            // 通知监听器屏幕位置已更新
            firePropertyChange("screenLayoutChanged", null, layoutConfig);
        }
    }
    
    /**
     * 处理鼠标拖拽事件
     * @param e 鼠标事件
     */
    private void handleMouseDragged(MouseEvent e) {
        if (draggedScreen != null && dragStartPoint != null && screenStartPoint != null) {
            int deltaX = e.getX() - dragStartPoint.x;
            int deltaY = e.getY() - dragStartPoint.y;
            
            // 计算新的屏幕位置（考虑缩放）
            int newX = screenStartPoint.x + (int) (deltaX / scale);
            int newY = screenStartPoint.y + (int) (deltaY / scale);
            
            // 更新屏幕位置
            draggedScreen.setX(newX);
            draggedScreen.setY(newY);
            
            repaint();
        }
    }
    
    /**
     * 应用屏幕吸附效果，使用实际屏幕尺寸进行计算
     * @param draggedScreen 被拖拽的屏幕
     */
    private void applySnapEffect(DeviceScreen draggedScreen) {
        // 获取被拖拽屏幕的实际尺寸
        int draggedWidth = getScreenWidth(draggedScreen);
        int draggedHeight = getScreenHeight(draggedScreen);
        
        // 获取所有其他屏幕
        List<DeviceScreen> allScreens = layoutConfig.getAllScreens();
        
        // 遍历所有其他屏幕，寻找最近的边缘进行吸附
        for (DeviceScreen otherScreen : allScreens) {
            // 跳过自己
            if (otherScreen == draggedScreen) {
                continue;
            }
            
            // 获取其他屏幕的实际尺寸
            int otherWidth = getScreenWidth(otherScreen);
            int otherHeight = getScreenHeight(otherScreen);
            
            // 计算两个屏幕的边界坐标
            Rectangle draggedRect = new Rectangle(draggedScreen.getX(), draggedScreen.getY(), draggedWidth, draggedHeight);
            Rectangle otherRect = new Rectangle(otherScreen.getX(), otherScreen.getY(), otherWidth, otherHeight);
            
            // 检查水平方向的吸附
            checkHorizontalSnap(draggedRect, otherRect);
            
            // 检查垂直方向的吸附
            checkVerticalSnap(draggedRect, otherRect);
        }
    }
    
    /**
     * 获取屏幕的实际宽度，如果无效则使用基准宽度
     * @param screen 设备屏幕
     * @return 屏幕宽度
     */
    private int getScreenWidth(DeviceScreen screen) {
        return screen.getWidth() > 0 ? screen.getWidth() : baseScreenWidth;
    }
    
    /**
     * 获取屏幕的实际高度，如果无效则使用基准高度
     * @param screen 设备屏幕
     * @return 屏幕高度
     */
    private int getScreenHeight(DeviceScreen screen) {
        return screen.getHeight() > 0 ? screen.getHeight() : baseScreenHeight;
    }
    
    /**
     * 检查水平方向的吸附
     * @param draggedRect 拖拽屏幕的矩形区域
     * @param otherRect 其他屏幕的矩形区域
     */
    private void checkHorizontalSnap(Rectangle draggedRect, Rectangle otherRect) {
        // 计算边缘距离
        int leftToRight = Math.abs(draggedRect.x - (otherRect.x + otherRect.width));
        int rightToLeft = Math.abs((draggedRect.x + draggedRect.width) - otherRect.x);
        int leftToLeft = Math.abs(draggedRect.x - otherRect.x);
        int rightToRight = Math.abs((draggedRect.x + draggedRect.width) - (otherRect.x + otherRect.width));
        
        // 找出最小的吸附距离
        int minDistance = Math.min(Math.min(Math.min(leftToRight, rightToLeft), leftToLeft), rightToRight);
        
        // 如果在吸附范围内，则调整位置
        if (minDistance < SNAP_DISTANCE) {
            if (leftToRight == minDistance) {
                // 拖拽屏幕的左边缘吸附到其他屏幕的右边缘
                draggedScreen.setX(otherRect.x + otherRect.width);
            } else if (rightToLeft == minDistance) {
                // 拖拽屏幕的右边缘吸附到其他屏幕的左边缘
                draggedScreen.setX(otherRect.x - draggedRect.width);
            } else if (leftToLeft == minDistance) {
                // 拖拽屏幕的左边缘吸附到其他屏幕的左边缘
                draggedScreen.setX(otherRect.x);
            } else if (rightToRight == minDistance) {
                // 拖拽屏幕的右边缘吸附到其他屏幕的右边缘
                draggedScreen.setX(otherRect.x + otherRect.width - draggedRect.width);
            }
        }
    }
    
    /**
     * 检查垂直方向的吸附
     * @param draggedRect 拖拽屏幕的矩形区域
     * @param otherRect 其他屏幕的矩形区域
     */
    private void checkVerticalSnap(Rectangle draggedRect, Rectangle otherRect) {
        // 计算边缘距离
        int topToBottom = Math.abs(draggedRect.y - (otherRect.y + otherRect.height));
        int bottomToTop = Math.abs((draggedRect.y + draggedRect.height) - otherRect.y);
        int topToTop = Math.abs(draggedRect.y - otherRect.y);
        int bottomToBottom = Math.abs((draggedRect.y + draggedRect.height) - (otherRect.y + otherRect.height));
        
        // 找出最小的吸附距离
        int minDistance = Math.min(Math.min(Math.min(topToBottom, bottomToTop), topToTop), bottomToBottom);
        
        // 如果在吸附范围内，则调整位置
        if (minDistance < SNAP_DISTANCE) {
            if (topToBottom == minDistance) {
                // 拖拽屏幕的上边缘吸附到其他屏幕的下边缘
                draggedScreen.setY(otherRect.y + otherRect.height);
            } else if (bottomToTop == minDistance) {
                // 拖拽屏幕的下边缘吸附到其他屏幕的上边缘
                draggedScreen.setY(otherRect.y - draggedRect.height);
            } else if (topToTop == minDistance) {
                // 拖拽屏幕的上边缘吸附到其他屏幕的上边缘
                draggedScreen.setY(otherRect.y);
            } else if (bottomToBottom == minDistance) {
                // 拖拽屏幕的下边缘吸附到其他屏幕的下边缘
                draggedScreen.setY(otherRect.y + otherRect.height - draggedRect.height);
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制网格
        drawGrid(g2d);
        
        // 绘制屏幕
        drawScreens(g2d);
        
        g2d.dispose();
    }
    
    /**
     * 绘制网格
     * @param g2d 图形上下文
     */
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        
        int width = getWidth();
        int height = getHeight();
        
        // 绘制垂直线
        for (int x = offsetX % GRID_SIZE; x < width; x += GRID_SIZE) {
            g2d.drawLine(x, 0, x, height);
        }
        
        // 绘制水平线
        for (int y = offsetY % GRID_SIZE; y < height; y += GRID_SIZE) {
            g2d.drawLine(0, y, width, y);
        }
        
        g2d.setStroke(new BasicStroke());
    }
    
    /**
     * 绘制屏幕
     * @param g2d 图形上下文
     */
    private void drawScreens(Graphics2D g2d) {
        List<DeviceScreen> screens = layoutConfig.getAllScreens();
        screenRectangles.clear();
        
        if (screens.isEmpty()) {
            // 如果没有屏幕，显示提示信息
            g2d.setColor(Color.GRAY);
            Font font = new Font("Arial", Font.PLAIN, 16);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            String message = "暂无设备屏幕信息";
            int textWidth = fm.stringWidth(message);
            int textHeight = fm.getHeight();
            g2d.drawString(message, (getWidth() - textWidth) / 2, (getHeight() - textHeight) / 2 + fm.getAscent());
            return;
        }
        
        // 计算边界以居中显示
        calculateScreenBounds(screens);
        
        // 绘制每个屏幕
        for (DeviceScreen screen : screens) {
            drawScreen(g2d, screen);
        }
        
        // 绘制屏幕间连接线
        drawConnectionLines(g2d, screens);
    }
    
    /**
     * 计算屏幕边界以进行居中显示
     * @param screens 屏幕列表
     */
    private void calculateScreenBounds(List<DeviceScreen> screens) {
        if (screens.isEmpty()) {
            return;
        }
        
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        // 获取面板可用尺寸
        int panelWidth = getWidth() - 100;
        int panelHeight = getHeight() - 100;
        
        // 计算所有屏幕的边界
        for (DeviceScreen screen : screens) {
            // 获取当前屏幕的有效尺寸
            int width = screen.getWidth() > 0 ? screen.getWidth() : baseScreenWidth;
            int height = screen.getHeight() > 0 ? screen.getHeight() : baseScreenHeight;
            
            minX = Math.min(minX, screen.getX());
            minY = Math.min(minY, screen.getY());
            maxX = Math.max(maxX, screen.getX() + width);
            maxY = Math.max(maxY, screen.getY() + height);
        }
        
        // 计算布局尺寸
        int layoutWidth = maxX - minX;
        int layoutHeight = maxY - minY;
        
        // 计算缩放比例，保持屏幕的宽高比
        scale = 1.0;
        if (layoutWidth > 0 && layoutHeight > 0) {
            double widthRatio = panelWidth / (double) layoutWidth;
            double heightRatio = panelHeight / (double) layoutHeight;
            
            // 使用最小的比例确保整个布局可见
            scale = Math.min(widthRatio, heightRatio);
            
            // 限制缩放比例在合理范围内
            scale = Math.min(scale, 0.5); // 最大缩放比例为0.5
            scale = Math.max(scale, 0.05); // 最小缩放比例为0.05，确保小屏幕也能看到
        }
        
        // 计算偏移量以居中显示
        int scaledLayoutWidth = (int) (layoutWidth * scale);
        int scaledLayoutHeight = (int) (layoutHeight * scale);
        
        offsetX = (getWidth() - scaledLayoutWidth) / 2 - (int) (minX * scale);
        offsetY = (getHeight() - scaledLayoutHeight) / 2 - (int) (minY * scale);
    }
    
    /**
     * 绘制单个屏幕
     * @param g2d 图形上下文
     * @param screen 屏幕信息
     */
    private void drawScreen(Graphics2D g2d, DeviceScreen screen) {
        // 确保屏幕有有效的尺寸
        int width = screen.getWidth() > 0 ? screen.getWidth() : baseScreenWidth;
        int height = screen.getHeight() > 0 ? screen.getHeight() : baseScreenHeight;
        
        int x = (int) (screen.getX() * scale) + offsetX;
        int y = (int) (screen.getY() * scale) + offsetY;
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        
        // 创建屏幕矩形
        Rectangle rect = new Rectangle(x, y, scaledWidth, scaledHeight);
        screenRectangles.add(rect);
        
        // 选择颜色
        Color screenColor;
        if (screen == selectedScreen) {
            screenColor = SELECTED_SCREEN_COLOR;
        } else if (screen == layoutConfig.getCurrentScreen()) {
            screenColor = CURRENT_SCREEN_COLOR;
        } else {
            screenColor = SCREEN_COLOR;
        }
        
        // 如果是正在拖拽的屏幕，添加视觉反馈
        if (screen == draggedScreen) {
            // 绘制拖拽时的阴影效果
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(x + 5, y + 5, scaledWidth, scaledHeight);
        }
        
        // 绘制屏幕背景
        g2d.setColor(screenColor);
        g2d.fillRect(x, y, scaledWidth, scaledHeight);
        
        // 绘制屏幕边框
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, scaledWidth, scaledHeight);
        
        // 绘制屏幕名称和尺寸信息
        g2d.setColor(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, Math.max(10, (int) (12 * scale)));
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        String name = screen.getDeviceName();
        if (name == null || name.isEmpty()) {
            name = "Unknown Device";
        }
        
        // 添加屏幕尺寸信息
        String sizeInfo = String.format("%s (%dx%d)", name, width, height);
        int textWidth = fm.stringWidth(sizeInfo);
        int textHeight = fm.getHeight();
        
        // 确保文本在屏幕矩形内
        int textX = x + Math.max(5, (scaledWidth - textWidth) / 2);
        int textY = y + Math.max(textHeight, (scaledHeight + fm.getAscent()) / 2);
        
        // 如果文本太宽放不进矩形内，则只显示设备名称
        if (textWidth > scaledWidth - 10) {
            String shortInfo = String.format("%s", name);
            textWidth = fm.stringWidth(shortInfo);
            textX = x + Math.max(5, (scaledWidth - textWidth) / 2);
            g2d.drawString(shortInfo, textX, textY);
            
            // 如果还有空间，再显示尺寸信息在下方
            if (scaledHeight > textHeight * 2) {
                String dimensionInfo = String.format("(%dx%d)", width, height);
                int dimTextWidth = fm.stringWidth(dimensionInfo);
                int dimTextX = x + Math.max(5, (scaledWidth - dimTextWidth) / 2);
                g2d.drawString(dimensionInfo, dimTextX, textY + textHeight);
            }
        } else {
            g2d.drawString(sizeInfo, textX, textY);
        }
    }
    
    /**
     * 绘制屏幕间连接线
     * @param g2d 图形上下文
     * @param screens 屏幕列表
     */
    private void drawConnectionLines(Graphics2D g2d, List<DeviceScreen> screens) {
        g2d.setColor(new Color(100, 100, 100, 150));
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 绘制相邻屏幕间的连接线
        for (int i = 0; i < screens.size(); i++) {
            DeviceScreen screen1 = screens.get(i);
            Rectangle rect1 = screenRectangles.get(i);
            
            for (int j = i + 1; j < screens.size(); j++) {
                DeviceScreen screen2 = screens.get(j);
                Rectangle rect2 = screenRectangles.get(j);
                
                // 检查屏幕是否相邻
                if (areScreensAdjacent(screen1, screen2)) {
                    // 计算连接点
                    Point p1 = calculateConnectionPoint(rect1, screen1, screen2);
                    Point p2 = calculateConnectionPoint(rect2, screen2, screen1);
                    
                    // 绘制连接线
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
    
    /**
     * 检查两个屏幕是否相邻
     * @param screen1 屏幕1
     * @param screen2 屏幕2
     * @return true表示相邻，false表示不相邻
     */
    private boolean areScreensAdjacent(DeviceScreen screen1, DeviceScreen screen2) {
        // 确保屏幕有有效的尺寸
        int width1 = screen1.getWidth() > 0 ? screen1.getWidth() : baseScreenWidth;
        int height1 = screen1.getHeight() > 0 ? screen1.getHeight() : baseScreenHeight;
        int width2 = screen2.getWidth() > 0 ? screen2.getWidth() : baseScreenWidth;
        int height2 = screen2.getHeight() > 0 ? screen2.getHeight() : baseScreenHeight;
        
        // 检查水平方向是否相邻
        boolean horizontalAdjacent = 
            (screen1.getX() + width1 == screen2.getX()) ||  // screen1在screen2左侧
            (screen2.getX() + width2 == screen1.getX());    // screen2在screen1左侧
            
        // 检查垂直方向是否相邻
        boolean verticalAdjacent = 
            (screen1.getY() + height1 == screen2.getY()) || // screen1在screen2上方
            (screen2.getY() + height2 == screen1.getY());   // screen2在screen1上方
            
        // 检查是否在同一条直线上
        boolean horizontallyAligned = 
            (screen1.getY() >= screen2.getY() && screen1.getY() <= screen2.getY() + height2) ||
            (screen2.getY() >= screen1.getY() && screen2.getY() <= screen1.getY() + height1);
            
        boolean verticallyAligned = 
            (screen1.getX() >= screen2.getX() && screen1.getX() <= screen2.getX() + width2) ||
            (screen2.getX() >= screen1.getX() && screen2.getX() <= screen1.getX() + width1);
            
        // 如果水平相邻且在同一条水平线上，或垂直相邻且在同一条垂直线上，则认为相邻
        return (horizontalAdjacent && horizontallyAligned) || 
               (verticalAdjacent && verticallyAligned);
    }
    
    /**
     * 计算连接点
     * @param rect 屏幕矩形
     * @param screen 屏幕信息
     * @param adjacentScreen 相邻屏幕信息
     * @return 连接点
     */
    private Point calculateConnectionPoint(Rectangle rect, DeviceScreen screen, DeviceScreen adjacentScreen) {
        int x = rect.x + rect.width / 2;
        int y = rect.y + rect.height / 2;
        
        // 获取当前屏幕的实际尺寸
        int actualWidth = screen.getWidth() > 0 ? screen.getWidth() : baseScreenWidth;
        int actualHeight = screen.getHeight() > 0 ? screen.getHeight() : baseScreenHeight;
        
        // 获取相邻屏幕的实际尺寸
        int adjacentActualWidth = adjacentScreen.getWidth() > 0 ? adjacentScreen.getWidth() : baseScreenWidth;
        int adjacentActualHeight = adjacentScreen.getHeight() > 0 ? adjacentScreen.getHeight() : baseScreenHeight;
        
        // 根据相邻屏幕的位置调整连接点
        if (adjacentScreen.getX() + adjacentActualWidth == screen.getX()) {
            // 相邻屏幕在左侧
            x = rect.x;
            y = rect.y + rect.height / 2;
        } else if (adjacentScreen.getX() == screen.getX() + actualWidth) {
            // 相邻屏幕在右侧
            x = rect.x + rect.width;
            y = rect.y + rect.height / 2;
        } else if (adjacentScreen.getY() + adjacentActualHeight == screen.getY()) {
            // 相邻屏幕在上方
            x = rect.x + rect.width / 2;
            y = rect.y;
        } else if (adjacentScreen.getY() == screen.getY() + actualHeight) {
            // 相邻屏幕在下方
            x = rect.x + rect.width / 2;
            y = rect.y + rect.height;
        }
        
        return new Point(x, y);
    }
    
    /**
     * 更新布局配置
     * @param layoutConfig 新的布局配置
     */
    public void setLayoutConfig(ScreenLayoutConfig layoutConfig) {
        this.layoutConfig = layoutConfig;
        repaint();
    }
    
    /**
     * 获取选中的屏幕
     * @return 选中的屏幕
     */
    public DeviceScreen getSelectedScreen() {
        return selectedScreen;
    }
    
    /**
     * 强制重新绘制面板
     */
    public void refresh() {
        repaint();
    }
    
    /**
     * 设置基准屏幕大小
     * @param width 基准宽度
     * @param height 基准高度
     */
    public void setBaseScreenSize(int width, int height) {
        this.baseScreenWidth = width;
        this.baseScreenHeight = height;
        repaint();
    }

    /**
     * 根据屏幕实际尺寸计算显示尺寸
     * @param actualWidth 实际屏幕宽度
     * @param actualHeight 实际屏幕高度
     * @return 计算后的显示尺寸数组，[0]为宽度，[1]为高度
     */
    private int[] calculateDisplaySize(int actualWidth, int actualHeight) {
        int[] result = new int[2];
        
        // 如果尺寸无效，使用基准尺寸
        if (actualWidth <= 0 || actualHeight <= 0) {
            result[0] = (int) (baseScreenWidth * scale);
            result[1] = (int) (baseScreenHeight * scale);
            return result;
        }
        
        // 计算基于基准比例的显示尺寸
        double widthRatio = (double) actualWidth / baseScreenWidth;
        double heightRatio = (double) actualHeight / baseScreenHeight;
        
        // 保持屏幕宽高比，根据缩放比例计算显示尺寸
        result[0] = (int) (actualWidth * scale);
        result[1] = (int) (actualHeight * scale);
        
        return result;
    }
    
    /**
     * 获取实际屏幕分辨率
     * @return 包含宽度和高度的数组，如果获取失败则返回默认值1920x1080
     */
    private int[] getScreenSize() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            
            // 遍历所有屏幕设备，找到主屏幕或选择最大屏幕
            GraphicsDevice primaryDevice = ge.getDefaultScreenDevice();
            DisplayMode dm = primaryDevice.getDisplayMode();
            
            int width = dm.getWidth();
            int height = dm.getHeight();
            
            // 确保获取到的尺寸是有效的
            if (width > 0 && height > 0) {
                return new int[]{width, height};
            }
            
            // 如果主屏幕无效，尝试其他屏幕
            for (GraphicsDevice screen : screens) {
                dm = screen.getDisplayMode();
                width = dm.getWidth();
                height = dm.getHeight();
                
                if (width > 0 && height > 0) {
                    return new int[]{width, height};
                }
            }
        } catch (Exception e) {
            // 默认返回1920x1080
            return new int[]{1920, 1080};
        }
        
        // 默认返回1920x1080
        return new int[]{1920, 1080};
    }
}