package com.keymouseshare.network;

import com.keymouseshare.core.Controller;
import com.keymouseshare.screen.ScreenInfo;
import com.keymouseshare.screen.ScreenLayoutManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;

/**
 * 输入事件处理器
 * 捕获本地输入事件并转发到远程设备
 */
public class InputEventHandler implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private Controller controller;
    private String activeRemoteDeviceId;
    private boolean isLocalInputEnabled = true;
    
    public InputEventHandler(Controller controller) {
        this.controller = controller;
    }
    
    /**
     * 启用本地输入
     */
    public void enableLocalInput() {
        this.isLocalInputEnabled = true;
    }
    
    /**
     * 禁用本地输入（转发到远程设备）
     */
    public void disableLocalInput(String remoteDeviceId) {
        this.isLocalInputEnabled = false;
        this.activeRemoteDeviceId = remoteDeviceId;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseEvent(DataPacket.TYPE_MOUSE_PRESS, e);
            sendMouseEvent(DataPacket.TYPE_MOUSE_RELEASE, e);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseEvent(DataPacket.TYPE_MOUSE_PRESS, e);
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseEvent(DataPacket.TYPE_MOUSE_RELEASE, e);
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // 不处理
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // 不处理
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseEvent(DataPacket.TYPE_MOUSE_MOVE, e);
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseEvent(DataPacket.TYPE_MOUSE_MOVE, e);
        }
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!isLocalInputEnabled) {
            sendMouseWheelEvent(e);
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // KeyListener要求实现，但通常不使用
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (!isLocalInputEnabled) {
            sendKeyEvent(DataPacket.TYPE_KEY_PRESS, e);
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (!isLocalInputEnabled) {
            sendKeyEvent(DataPacket.TYPE_KEY_RELEASE, e);
        }
    }
    
    /**
     * 发送鼠标事件到远程设备
     */
    private void sendMouseEvent(String eventType, MouseEvent e) {
        try {
            // 创建数据包
            DataPacket packet = new DataPacket();
            packet.setType(eventType);
            packet.setDeviceId(activeRemoteDeviceId);
            
            // 格式化鼠标事件数据
            String mouseData = e.getX() + "," + e.getY() + "," + e.getButton();
            packet.setData(mouseData);
            
            // 发送到网络管理器
            controller.getNetworkManager().sendDataPacket(packet);
            
            System.out.println("Sent mouse event: " + eventType + " at (" + e.getX() + ", " + e.getY() + ")");
        } catch (Exception ex) {
            System.err.println("Error sending mouse event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 发送鼠标滚轮事件到远程设备
     */
    private void sendMouseWheelEvent(MouseWheelEvent e) {
        try {
            // 创建数据包
            DataPacket packet = new DataPacket();
            packet.setType(DataPacket.TYPE_MOUSE_WHEEL);
            packet.setDeviceId(activeRemoteDeviceId);
            
            // 格式化滚轮事件数据
            String wheelData = e.getX() + "," + e.getY() + "," + e.getWheelRotation();
            packet.setData(wheelData);
            
            // 发送到网络管理器
            controller.getNetworkManager().sendDataPacket(packet);
            
            System.out.println("Sent mouse wheel event: rotation=" + e.getWheelRotation());
        } catch (Exception ex) {
            System.err.println("Error sending mouse wheel event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 发送键盘事件到远程设备
     */
    private void sendKeyEvent(String eventType, KeyEvent e) {
        try {
            // 创建数据包
            DataPacket packet = new DataPacket();
            packet.setType(eventType);
            packet.setDeviceId(activeRemoteDeviceId);
            
            // 格式化键盘事件数据
            String keyData = String.valueOf(e.getKeyCode());
            packet.setData(keyData);
            
            // 发送到网络管理器
            controller.getNetworkManager().sendDataPacket(packet);
            
            System.out.println("Sent key event: " + eventType + " key=" + e.getKeyCode());
        } catch (Exception ex) {
            System.err.println("Error sending key event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 处理从远程设备接收到的鼠标事件
     */
    public void handleRemoteMouseEvent(DataPacket packet) {
        try {
            String[] data = packet.getData().split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            int button = data.length > 2 ? Integer.parseInt(data[2]) : 0;
            
            // 使用本地输入管理器执行鼠标操作
            switch (packet.getType()) {
                case DataPacket.TYPE_MOUSE_MOVE:
                    controller.getMouseMovementManager().setLocalActive(true);
                    // 实际应用中应该移动鼠标到指定位置
                    System.out.println("Moving mouse to: (" + x + ", " + y + ")");
                    break;
                case DataPacket.TYPE_MOUSE_PRESS:
                    controller.getMouseMovementManager().setLocalActive(true);
                    // 实际应用中应该按下鼠标按钮
                    System.out.println("Mouse pressed at: (" + x + ", " + y + ") button: " + button);
                    break;
                case DataPacket.TYPE_MOUSE_RELEASE:
                    controller.getMouseMovementManager().setLocalActive(true);
                    // 实际应用中应该释放鼠标按钮
                    System.out.println("Mouse released at: (" + x + ", " + y + ") button: " + button);
                    break;
            }
        } catch (Exception ex) {
            System.err.println("Error handling remote mouse event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 处理从远程设备接收到的鼠标滚轮事件
     */
    public void handleRemoteMouseWheelEvent(DataPacket packet) {
        try {
            String[] data = packet.getData().split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            int wheelRotation = Integer.parseInt(data[2]);
            
            controller.getMouseMovementManager().setLocalActive(true);
            // 实际应用中应该处理鼠标滚轮事件
            System.out.println("Mouse wheel rotated: " + wheelRotation + " at (" + x + ", " + y + ")");
        } catch (Exception ex) {
            System.err.println("Error handling remote mouse wheel event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 处理从远程设备接收到的键盘事件
     */
    public void handleRemoteKeyEvent(DataPacket packet) {
        try {
            int keyCode = Integer.parseInt(packet.getData());
            
            controller.getMouseMovementManager().setLocalActive(true);
            // 实际应用中应该处理键盘事件
            switch (packet.getType()) {
                case DataPacket.TYPE_KEY_PRESS:
                    System.out.println("Key pressed: " + keyCode);
                    break;
                case DataPacket.TYPE_KEY_RELEASE:
                    System.out.println("Key released: " + keyCode);
                    break;
            }
        } catch (Exception ex) {
            System.err.println("Error handling remote key event: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}