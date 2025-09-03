package com.keymouseshare.util;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class KeyBoardUtils {
    /**
     * 获取鼠标按钮对应的掩码
     * @param button 按钮编号
     * @return 掩码
     */
    public static int getButtonMask(int button) {
        switch (button) {
            case 1: // 左键
                return InputEvent.BUTTON1_DOWN_MASK;
            case 2: // 中键
                return InputEvent.BUTTON2_DOWN_MASK;
            case 3: // 右键
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }

    /**
     * 获取鼠标事件对应的Windows标志
     * @param button 按钮编号
     * @param isPress 是否按下
     * @return 标志
     */
    public static int getMouseEventFlags(int button, boolean isPress) {
        if (isPress) {
            switch (button) {
                case 1: // 左键
                    return 0x0002; // MOUSEEVENTF_LEFTDOWN
                case 2: // 中键
                    return 0x0020; // MOUSEEVENTF_MIDDLEDOWN
                case 3: // 右键
                    return 0x0008; // MOUSEEVENTF_RIGHTDOWN
                default:
                    return 0;
            }
        } else {
            switch (button) {
                case 1: // 左键
                    return 0x0004; // MOUSEEVENTF_LEFTUP
                case 2: // 中键
                    return 0x0040; // MOUSEEVENTF_MIDDLEUP
                case 3: // 右键
                    return 0x0010; // MOUSEEVENTF_RIGHTUP
                default:
                    return 0;
            }
        }
    }

    /**
     * 获取macOS鼠标事件类型
     * @param button 按钮编号
     * @param isPress 是否按下
     * @return 事件类型
     */
    public static int getMacMouseEventType(int button, boolean isPress) {
        if (isPress) {
            switch (button) {
                case 1: // 左键
                    return 1; // kCGEventLeftMouseDown
                case 2: // 中键
                    return 3; // kCGEventOtherMouseDown
                case 3: // 右键
                    return 3; // kCGEventRightMouseDown
                default:
                    return 0;
            }
        } else {
            switch (button) {
                case 1: // 左键
                    return 2; // kCGEventLeftMouseUp
                case 2: // 中键
                    return 4; // kCGEventOtherMouseUp
                case 3: // 右键
                    return 4; // kCGEventRightMouseUp
                default:
                    return 0;
            }
        }
    }

    /**
     * 获取macOS鼠标按钮编号
     * @param button 按钮编号
     * @return macOS按钮编号
     */
    public static int getMacMouseButton(int button) {
        switch (button) {
            case 1: // 左键
                return 0; // kCGMouseButtonLeft
            case 2: // 中键
                return 2; // kCGMouseButtonCenter
            case 3: // 右键
                return 1; // kCGMouseButtonRight
            default:
                return 0;
        }
    }

    /**
     * 将Java虚拟键码转换为macOS键码
     * @param keyCode Java虚拟键码
     * @return macOS键码
     */
    public static short convertToMacKeyCode(int keyCode) {
        // 这里只是一个简单的映射示例，实际应用中需要更完整的映射表
        switch (keyCode) {
            case KeyEvent.VK_A:
                return 0x00; // kVK_ANSI_A
            case KeyEvent.VK_B:
                return 0x0B; // kVK_ANSI_B
            case KeyEvent.VK_C:
                return 0x08; // kVK_ANSI_C
            // 更多键码映射...
            default:
                return (short) keyCode;
        }
    }

    public static int keyCharToMacKeyCode(char ch) {
        int keyCode = KeyEvent.getExtendedKeyCodeForChar(ch);
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            System.err.println("无法映射字符: " + ch);
        }
        return keyCode;
    }
}
