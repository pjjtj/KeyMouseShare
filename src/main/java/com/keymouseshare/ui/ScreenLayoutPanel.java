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
    
    private ScreenLayoutConfig layoutConfig;
    private DeviceScreen selectedScreen;
    private List<Rectangle> screenRectangles;
    private int offsetX = 50;
    private int offsetY = 50;
    private double scale = 0.5; // 缩放比例
    
    public ScreenLayoutPanel(ScreenLayoutConfig layoutConfig) {
        this.layoutConfig = layoutConfig;
        this.screenRectangles = new ArrayList<>();
        
        // 设置面板属性
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 400));
        
        // 添加鼠标监听器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });
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
        
        // 计算所有屏幕的边界
        for (DeviceScreen screen : screens) {
            // 确保屏幕有有效的尺寸
            int width = screen.getWidth() > 0 ? screen.getWidth() : 1920;
            int height = screen.getHeight() > 0 ? screen.getHeight() : 1080;
            
            minX = Math.min(minX, screen.getX());
            minY = Math.min(minY, screen.getY());
            maxX = Math.max(maxX, screen.getX() + width);
            maxY = Math.max(maxY, screen.getY() + height);
        }
        
        // 计算偏移量以居中显示
        int layoutWidth = maxX - minX;
        int layoutHeight = maxY - minY;
        
        int panelWidth = getWidth() - 100;
        int panelHeight = getHeight() - 100;
        
        // 计算缩放比例
        scale = Math.min((double) panelWidth / layoutWidth, (double) panelHeight / layoutHeight);
        scale = Math.min(scale, 0.5); // 最大缩放比例为0.5
        scale = Math.max(scale, 0.1); // 最小缩放比例为0.1
        
        // 计算偏移量
        offsetX = (getWidth() - (int) (layoutWidth * scale)) / 2 - (int) (minX * scale);
        offsetY = (getHeight() - (int) (layoutHeight * scale)) / 2 - (int) (minY * scale);
    }
    
    /**
     * 绘制单个屏幕
     * @param g2d 图形上下文
     * @param screen 屏幕信息
     */
    private void drawScreen(Graphics2D g2d, DeviceScreen screen) {
        // 确保屏幕有有效的尺寸
        int width = screen.getWidth() > 0 ? screen.getWidth() : 1920;
        int height = screen.getHeight() > 0 ? screen.getHeight() : 1080;
        
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
        
        // 绘制屏幕背景
        g2d.setColor(screenColor);
        g2d.fillRect(x, y, scaledWidth, scaledHeight);
        
        // 绘制屏幕边框
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, scaledWidth, scaledHeight);
        
        // 绘制屏幕名称
        g2d.setColor(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, Math.max(10, (int) (12 * scale)));
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        String name = screen.getDeviceName();
        if (name == null || name.isEmpty()) {
            name = "Unknown Device";
        }
        int textWidth = fm.stringWidth(name);
        int textHeight = fm.getHeight();
        
        // 确保文本在屏幕矩形内
        int textX = x + Math.max(5, (scaledWidth - textWidth) / 2);
        int textY = y + Math.max(textHeight, (scaledHeight + fm.getAscent()) / 2);
        
        g2d.drawString(name, textX, textY);
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
        int width1 = screen1.getWidth() > 0 ? screen1.getWidth() : 1920;
        int height1 = screen1.getHeight() > 0 ? screen1.getHeight() : 1080;
        int width2 = screen2.getWidth() > 0 ? screen2.getWidth() : 1920;
        int height2 = screen2.getHeight() > 0 ? screen2.getHeight() : 1080;
        
        // 检查水平相邻
        boolean horizontalAdjacent = 
            (screen1.getX() + width1 == screen2.getX() || 
             screen2.getX() + width2 == screen1.getX()) &&
            (Math.max(screen1.getY(), screen2.getY()) < 
             Math.min(screen1.getY() + height1, screen2.getY() + height2));
        
        // 检查垂直相邻
        boolean verticalAdjacent = 
            (screen1.getY() + height1 == screen2.getY() || 
             screen2.getY() + height2 == screen1.getY()) &&
            (Math.max(screen1.getX(), screen2.getX()) < 
             Math.min(screen1.getX() + width1, screen2.getX() + width2));
        
        return horizontalAdjacent || verticalAdjacent;
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
        
        // 根据相邻屏幕的位置调整连接点
        int width = screen.getWidth() > 0 ? screen.getWidth() : 1920;
        int height = screen.getHeight() > 0 ? screen.getHeight() : 1080;
        
        if (adjacentScreen.getX() + (adjacentScreen.getWidth() > 0 ? adjacentScreen.getWidth() : 1920) == screen.getX()) {
            // 相邻屏幕在左侧
            x = rect.x;
            y = rect.y + rect.height / 2;
        } else if (adjacentScreen.getX() == screen.getX() + width) {
            // 相邻屏幕在右侧
            x = rect.x + rect.width;
            y = rect.y + rect.height / 2;
        } else if (adjacentScreen.getY() + (adjacentScreen.getHeight() > 0 ? adjacentScreen.getHeight() : 1080) == screen.getY()) {
            // 相邻屏幕在上方
            x = rect.x + rect.width / 2;
            y = rect.y;
        } else if (adjacentScreen.getY() == screen.getY() + height) {
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
}