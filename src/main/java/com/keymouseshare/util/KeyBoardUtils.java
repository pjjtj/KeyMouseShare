package com.keymouseshare.util;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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

    // JNativeHook 键码 => AWT KeyEvent 映射
    private static int mapToAwtKeyCode(int nativeKeyCode) {
        switch (nativeKeyCode) {
            case NativeKeyEvent.VC_A:
                return KeyEvent.VK_A;
            case NativeKeyEvent.VC_B:
                return KeyEvent.VK_B;
            case NativeKeyEvent.VC_C:
                return KeyEvent.VK_C;
            case NativeKeyEvent.VC_D:
                return KeyEvent.VK_D;
            case NativeKeyEvent.VC_E:
                return KeyEvent.VK_E;
            case NativeKeyEvent.VC_F:
                return KeyEvent.VK_F;
            case NativeKeyEvent.VC_G:
                return KeyEvent.VK_G;
            case NativeKeyEvent.VC_H:
                return KeyEvent.VK_H;
            case NativeKeyEvent.VC_I:
                return KeyEvent.VK_I;
            case NativeKeyEvent.VC_J:
                return KeyEvent.VK_J;
            case NativeKeyEvent.VC_K:
                return KeyEvent.VK_K;
            case NativeKeyEvent.VC_L:
                return KeyEvent.VK_L;
            case NativeKeyEvent.VC_M:
                return KeyEvent.VK_M;
            case NativeKeyEvent.VC_N:
                return KeyEvent.VK_N;
            case NativeKeyEvent.VC_O:
                return KeyEvent.VK_O;
            case NativeKeyEvent.VC_P:
                return KeyEvent.VK_P;
            case NativeKeyEvent.VC_Q:
                return KeyEvent.VK_Q;
            case NativeKeyEvent.VC_R:
                return KeyEvent.VK_R;
            case NativeKeyEvent.VC_S:
                return KeyEvent.VK_S;
            case NativeKeyEvent.VC_T:
                return KeyEvent.VK_T;
            case NativeKeyEvent.VC_U:
                return KeyEvent.VK_U;
            case NativeKeyEvent.VC_V:
                return KeyEvent.VK_V;
            case NativeKeyEvent.VC_W:
                return KeyEvent.VK_W;
            case NativeKeyEvent.VC_X:
                return KeyEvent.VK_X;
            case NativeKeyEvent.VC_Y:
                return KeyEvent.VK_Y;
            case NativeKeyEvent.VC_Z:
                return KeyEvent.VK_Z;

            case NativeKeyEvent.VC_1:
                return KeyEvent.VK_1;
            case NativeKeyEvent.VC_2:
                return KeyEvent.VK_2;
            case NativeKeyEvent.VC_3:
                return KeyEvent.VK_3;
            case NativeKeyEvent.VC_4:
                return KeyEvent.VK_4;
            case NativeKeyEvent.VC_5:
                return KeyEvent.VK_5;
            case NativeKeyEvent.VC_6:
                return KeyEvent.VK_6;
            case NativeKeyEvent.VC_7:
                return KeyEvent.VK_7;
            case NativeKeyEvent.VC_8:
                return KeyEvent.VK_8;
            case NativeKeyEvent.VC_9:
                return KeyEvent.VK_9;
            case NativeKeyEvent.VC_0:
                return KeyEvent.VK_0;

            case NativeKeyEvent.VC_ENTER:
                return KeyEvent.VK_ENTER;
            case NativeKeyEvent.VC_SPACE:
                return KeyEvent.VK_SPACE;
            case NativeKeyEvent.VC_TAB:
                return KeyEvent.VK_TAB;
            case NativeKeyEvent.VC_BACKSPACE:
                return KeyEvent.VK_BACK_SPACE;
            case NativeKeyEvent.VC_ESCAPE:
                return KeyEvent.VK_ESCAPE;

            case NativeKeyEvent.VC_COMMA:
                return KeyEvent.VK_COMMA;
            case NativeKeyEvent.VC_PERIOD:
                return KeyEvent.VK_PERIOD;
            case NativeKeyEvent.VC_SLASH:
                return KeyEvent.VK_SLASH;
            case NativeKeyEvent.VC_SEMICOLON:
                return KeyEvent.VK_SEMICOLON;
            case NativeKeyEvent.VC_QUOTE:
                return KeyEvent.VK_QUOTE;
            case NativeKeyEvent.VC_OPEN_BRACKET:
                return KeyEvent.VK_OPEN_BRACKET;
            case NativeKeyEvent.VC_CLOSE_BRACKET:
                return KeyEvent.VK_CLOSE_BRACKET;
            case NativeKeyEvent.VC_BACK_SLASH:
                return KeyEvent.VK_BACK_SLASH;
            case NativeKeyEvent.VC_MINUS:
                return KeyEvent.VK_MINUS;
            case NativeKeyEvent.VC_EQUALS:
                return KeyEvent.VK_EQUALS;

            case NativeKeyEvent.VC_SHIFT:
                return KeyEvent.VK_SHIFT;
            case NativeKeyEvent.VC_CONTROL:
                return KeyEvent.VK_CONTROL;
            case NativeKeyEvent.VC_ALT:
                return KeyEvent.VK_ALT;
            case NativeKeyEvent.VC_META:
                return KeyEvent.VK_META;

            default:
                return KeyEvent.VK_UNDEFINED;
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
