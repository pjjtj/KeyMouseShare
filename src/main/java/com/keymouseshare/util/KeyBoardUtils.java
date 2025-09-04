package com.keymouseshare.util;

import java.awt.event.InputEvent;

public class KeyBoardUtils {
    /**
     * 获取鼠标按钮对应的掩码
     *
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
     *
     * @param button  按钮编号
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
     *
     * @param button  按钮编号
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
     *
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
    
}
