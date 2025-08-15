package com.keymouseshare.ui;

import com.keymouseshare.core.Controller;
import com.keymouseshare.screen.ScreenInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 屏幕布局配置对话框
 * 用于可视化配置多个设备屏幕的相对位置关系
 */
public class ScreenLayoutConfigDialog extends JDialog {
    private Controller controller;
    private JPanel layoutPanel;
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int SNAP_DISTANCE = 20; // 吸附距离
    
    // 拖拽相关变量
    private ScreenInfo draggingScreen = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    
    public ScreenLayoutConfigDialog(Frame parent, Controller controller) {
        super(parent, "屏幕布局配置", true);
        this.controller = controller;
        
        initializeComponents();
        setupLayout();
        
        setSize(900, 700);
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        layoutPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawScreenLayout(g);
            }
        };
        layoutPanel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        layoutPanel.setBackground(Color.WHITE);
        layoutPanel.setBorder(BorderFactory.createEtchedBorder());
        
        // 添加鼠标事件处理
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e.getX(), e.getY());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e.getX(), e.getY());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClicked(e.getX(), e.getY());
            }
        };
        
        layoutPanel.addMouseListener(mouseAdapter);
        layoutPanel.addMouseMotionListener(mouseAdapter);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JLabel infoLabel = new JLabel("拖拽屏幕排列位置，双击添加新屏幕");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(infoLabel, BorderLayout.NORTH);
        
        // 居中显示布局面板
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(layoutPanel);
        JScrollPane scrollPane = new JScrollPane(centerPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 添加按钮事件处理
        okButton.addActionListener(e -> dispose());
        cancelButton.addActionListener(e -> dispose());
    }
    
    /**
     * 绘制屏幕布局
     */
    private void drawScreenLayout(Graphics g) {
        List<ScreenInfo> screens = controller.getScreenLayoutManager().getAllScreens();
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 计算所有屏幕的边界以居中显示
        if (!screens.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            
            for (ScreenInfo screen : screens) {
                minX = Math.min(minX, screen.getX());
                minY = Math.min(minY, screen.getY());
                maxX = Math.max(maxX, screen.getX() + screen.getWidth());
                maxY = Math.max(maxY, screen.getY() + screen.getHeight());
            }
            
            // 计算缩放比例以适应面板
            int totalWidth = maxX - minX;
            int totalHeight = maxY - minY;
            double scaleX = (double) (PANEL_WIDTH - 100) / totalWidth;
            double scaleY = (double) (PANEL_HEIGHT - 100) / totalHeight;
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0); // 不放大，只缩小
            
            // 计算偏移量以居中显示
            int offsetX = (int) ((PANEL_WIDTH - totalWidth * scale) / 2 - minX * scale);
            int offsetY = (int) ((PANEL_HEIGHT - totalHeight * scale) / 2 - minY * scale);
            
            // 绘制每个屏幕
            for (ScreenInfo screen : screens) {
                // 计算屏幕在面板中的位置和大小（按真实比例缩放）
                int x = (int) (screen.getX() * scale) + offsetX;
                int y = (int) (screen.getY() * scale) + offsetY;
                int width = (int) (screen.getWidth() * scale);
                int height = (int) (screen.getHeight() * scale);
                
                // 确保最小尺寸
                width = Math.max(50, width);
                height = Math.max(30, height);
                
                // 绘制屏幕边框
                Color screenColor = getScreenColor(screen);
                if (screen == draggingScreen) {
                    // 正在拖拽的屏幕添加高亮效果
                    g2d.setColor(screenColor.brighter());
                } else {
                    g2d.setColor(screenColor);
                }
                g2d.fillRect(x, y, width, height);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, width, height);
                
                // 绘制屏幕信息（IP:屏幕ID）
                g2d.setColor(Color.BLACK);
                FontMetrics fm = g2d.getFontMetrics();
                
                // 提取IP和屏幕ID信息
                String displayText = getScreenDisplayText(screen);
                String[] lines = displayText.split("\n");
                
                int startY = y + (height - lines.length * fm.getHeight()) / 2 + fm.getAscent();
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    int textWidth = fm.stringWidth(line);
                    int textX = x + (width - textWidth) / 2;
                    int textY = startY + i * fm.getHeight();
                    g2d.drawString(line, textX, textY);
                }
            }
        }
        
        g2d.dispose();
    }
    
    /**
     * 获取屏幕显示文本（IP:屏幕ID）
     */
    private String getScreenDisplayText(ScreenInfo screen) {
        String id = screen.getId();
        if (id != null && id.contains(":")) {
            // 格式为"IP:屏幕ID"或"local:屏幕ID"
            String[] parts = id.split(":", 2);
            if (parts.length == 2) {
                return parts[0] + ":" + parts[1];
            }
        }
        // 默认显示ID和名称
        return screen.getId() + "\n" + screen.getName();
    }
    
    /**
     * 根据屏幕类型获取颜色
     */
    private Color getScreenColor(ScreenInfo screen) {
        switch (screen.getDeviceType()) {
            case LOCAL:
                return Color.BLUE;
            case SERVER:
                return Color.ORANGE;
            case CLIENT:
                return Color.GREEN;
            default:
                return Color.LIGHT_GRAY;
        }
    }
    
    /**
     * 处理鼠标按下事件
     */
    private void handleMousePressed(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
        
        // 查找点击的屏幕
        draggingScreen = findScreenAt(x, y);
        
        if (draggingScreen != null) {
            // 计算拖拽偏移量
            List<ScreenInfo> screens = controller.getScreenLayoutManager().getAllScreens();
            
            // 计算边界
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            
            for (ScreenInfo screen : screens) {
                minX = Math.min(minX, screen.getX());
                minY = Math.min(minY, screen.getY());
                maxX = Math.max(maxX, screen.getX() + screen.getWidth());
                maxY = Math.max(maxY, screen.getY() + screen.getHeight());
            }
            
            // 计算缩放比例
            int totalWidth = maxX - minX;
            int totalHeight = maxY - minY;
            double scaleX = (double) (PANEL_WIDTH - 100) / totalWidth;
            double scaleY = (double) (PANEL_HEIGHT - 100) / totalHeight;
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0);
            
            // 计算偏移量
            int offsetX = (int) ((PANEL_WIDTH - totalWidth * scale) / 2 - minX * scale);
            int offsetY = (int) ((PANEL_HEIGHT - totalHeight * scale) / 2 - minY * scale);
            
            // 计算屏幕在面板中的位置
            int screenX = (int) (draggingScreen.getX() * scale) + offsetX;
            int screenY = (int) (draggingScreen.getY() * scale) + offsetY;
            
            dragOffsetX = x - screenX;
            dragOffsetY = y - screenY;
            
            layoutPanel.repaint();
        }
    }
    
    /**
     * 处理鼠标拖拽事件
     */
    private void handleMouseDragged(int x, int y) {
        if (draggingScreen != null) {
            List<ScreenInfo> screens = controller.getScreenLayoutManager().getAllScreens();
            
            // 计算边界
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            
            for (ScreenInfo screen : screens) {
                minX = Math.min(minX, screen.getX());
                minY = Math.min(minY, screen.getY());
                maxX = Math.max(maxX, screen.getX() + screen.getWidth());
                maxY = Math.max(maxY, screen.getY() + screen.getHeight());
            }
            
            // 计算缩放比例
            int totalWidth = maxX - minX;
            int totalHeight = maxY - minY;
            double scaleX = (double) (PANEL_WIDTH - 100) / totalWidth;
            double scaleY = (double) (PANEL_HEIGHT - 100) / totalHeight;
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0);
            
            // 计算偏移量
            int offsetX = (int) ((PANEL_WIDTH - totalWidth * scale) / 2 - minX * scale);
            int offsetY = (int) ((PANEL_HEIGHT - totalHeight * scale) / 2 - minY * scale);
            
            // 计算新的屏幕位置（反向计算原始坐标）
            int newX = (int) ((x - dragOffsetX - offsetX) / scale);
            int newY = (int) ((y - dragOffsetY - offsetY) / scale);
            
            // 应用边缘吸附
            for (ScreenInfo screen : screens) {
                if (screen != draggingScreen) {
                    // 检查水平边缘吸附
                    if (Math.abs(newX - screen.getX()) < SNAP_DISTANCE) {
                        newX = screen.getX(); // 左边缘对齐
                    } else if (Math.abs(newX + draggingScreen.getWidth() - screen.getX()) < SNAP_DISTANCE) {
                        newX = screen.getX() - draggingScreen.getWidth(); // 右边缘对齐到左边缘
                    } else if (Math.abs(newX - (screen.getX() + screen.getWidth())) < SNAP_DISTANCE) {
                        newX = screen.getX() + screen.getWidth(); // 左边缘对齐到右边缘
                    }
                    
                    // 检查垂直边缘吸附
                    if (Math.abs(newY - screen.getY()) < SNAP_DISTANCE) {
                        newY = screen.getY(); // 上边缘对齐
                    } else if (Math.abs(newY + draggingScreen.getHeight() - screen.getY()) < SNAP_DISTANCE) {
                        newY = screen.getY() - draggingScreen.getHeight(); // 下边缘对齐到上边缘
                    } else if (Math.abs(newY - (screen.getY() + screen.getHeight())) < SNAP_DISTANCE) {
                        newY = screen.getY() + screen.getHeight(); // 上边缘对齐到下边缘
                    }
                }
            }
            
            // 更新屏幕位置
            draggingScreen.setX(newX);
            draggingScreen.setY(newY);
            
            layoutPanel.repaint();
        }
        
        lastMouseX = x;
        lastMouseY = y;
    }
    
    /**
     * 处理鼠标释放事件
     */
    private void handleMouseReleased() {
        if (draggingScreen != null) {
            // 保存屏幕位置到布局管理器
            controller.getScreenLayoutManager().updateScreen(draggingScreen);
            draggingScreen = null;
            layoutPanel.repaint();
        }
    }
    
    /**
     * 处理鼠标点击事件
     */
    private void handleMouseClicked(int x, int y) {
        // 只有在没有拖拽的情况下才处理点击事件
        if (Math.abs(x - lastMouseX) < 5 && Math.abs(y - lastMouseY) < 5) {
            // 查找点击的屏幕
            ScreenInfo clickedScreen = findScreenAt(x, y);
            
            if (clickedScreen != null) {
                // 选中屏幕
                controller.getScreenLayoutManager().setActiveScreen(clickedScreen);
                layoutPanel.repaint();
            } else if (lastMouseX == x && lastMouseY == y) {
                // 双击添加新屏幕
                if (SwingUtilities.isLeftMouseButton(SwingUtilities.convertMouseEvent(layoutPanel, 
                        new MouseEvent(layoutPanel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 2, false), layoutPanel))) {
                    addNewScreen(x, y);
                }
            }
        }
    }
    
    /**
     * 查找指定位置的屏幕
     */
    private ScreenInfo findScreenAt(int x, int y) {
        List<ScreenInfo> screens = controller.getScreenLayoutManager().getAllScreens();
        
        if (screens.isEmpty()) {
            return null;
        }
        
        // 计算边界
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        for (ScreenInfo screen : screens) {
            minX = Math.min(minX, screen.getX());
            minY = Math.min(minY, screen.getY());
            maxX = Math.max(maxX, screen.getX() + screen.getWidth());
            maxY = Math.max(maxY, screen.getY() + screen.getHeight());
        }
        
        // 计算缩放比例
        int totalWidth = maxX - minX;
        int totalHeight = maxY - minY;
        double scaleX = (double) (PANEL_WIDTH - 100) / totalWidth;
        double scaleY = (double) (PANEL_HEIGHT - 100) / totalHeight;
        double scale = Math.min(Math.min(scaleX, scaleY), 1.0);
        
        // 计算偏移量
        int offsetX = (int) ((PANEL_WIDTH - totalWidth * scale) / 2 - minX * scale);
        int offsetY = (int) ((PANEL_HEIGHT - totalHeight * scale) / 2 - minY * scale);
        
        // 逆序查找，确保上层的屏幕优先被选中
        for (int i = screens.size() - 1; i >= 0; i--) {
            ScreenInfo screen = screens.get(i);
            int screenX = (int) (screen.getX() * scale) + offsetX;
            int screenY = (int) (screen.getY() * scale) + offsetY;
            int screenWidth = Math.max(50, (int) (screen.getWidth() * scale));
            int screenHeight = Math.max(30, (int) (screen.getHeight() * scale));
            
            if (x >= screenX && x <= screenX + screenWidth &&
                y >= screenY && y <= screenY + screenHeight) {
                return screen;
            }
        }
        
        return null;
    }
    
    /**
     * 添加新屏幕
     */
    private void addNewScreen(int x, int y) {
        List<ScreenInfo> screens = controller.getScreenLayoutManager().getAllScreens();
        
        // 计算边界
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        if (!screens.isEmpty()) {
            for (ScreenInfo screen : screens) {
                minX = Math.min(minX, screen.getX());
                minY = Math.min(minY, screen.getY());
                maxX = Math.max(maxX, screen.getX() + screen.getWidth());
                maxY = Math.max(maxY, screen.getY() + screen.getHeight());
            }
        } else {
            // 默认位置
            minX = minY = 0;
            maxX = 1920;
            maxY = 1080;
        }
        
        // 计算缩放比例
        int totalWidth = maxX - minX;
        int totalHeight = maxY - minY;
        double scaleX = (double) (PANEL_WIDTH - 100) / totalWidth;
        double scaleY = (double) (PANEL_HEIGHT - 100) / totalHeight;
        double scale = Math.min(Math.min(scaleX, scaleY), 1.0);
        
        // 计算偏移量
        int offsetX = (int) ((PANEL_WIDTH - totalWidth * scale) / 2 - minX * scale);
        int offsetY = (int) ((PANEL_HEIGHT - totalHeight * scale) / 2 - minY * scale);
        
        // 计算新屏幕的原始坐标
        int originalX = (int) ((x - offsetX) / scale);
        int originalY = (int) ((y - offsetY) / scale);
        
        ScreenInfo newScreen = new ScreenInfo();
        newScreen.setId("192.168.1." + (100 + (int)(Math.random() * 100)) + ":screen_" + System.currentTimeMillis());
        newScreen.setName("新屏幕");
        newScreen.setWidth(1920);
        newScreen.setHeight(1080);
        newScreen.setX(originalX);
        newScreen.setY(originalY);
        newScreen.setDeviceType(ScreenInfo.DeviceType.CLIENT);
        newScreen.setConnectionStatus(ScreenInfo.ConnectionStatus.DISCONNECTED);
        
        controller.getScreenLayoutManager().addScreen(newScreen);
        layoutPanel.repaint();
    }
}