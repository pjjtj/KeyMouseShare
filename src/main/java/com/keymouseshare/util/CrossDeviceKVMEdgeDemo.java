package com.keymouseshare.util;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import com.sun.jna.ptr.IntByReference;                 // 添加IntByReference导入
import com.sun.jna.win32.W32APIOptions;

public class CrossDeviceKVMEdgeDemo {

    // --- 常量定义 ---
    private static final int SM_CXSCREEN = 0;
    private static final int SM_CYSCREEN = 1;
    private static final int RIDEV_INPUTSINK = 0x00000100;
    private static final int RID_INPUT = 0x10000003;
    private static final int RIM_TYPEMOUSE = 0;
    private static final int WM_INPUT = 0x00FF;
    private static final int WM_HOTKEY = 0x0312;
    private static final int HOTKEY_ID = 1;
    private static final int MOD_ALT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;
    private static final int VK_ESCAPE = 0x1B;

    private static volatile boolean edgeMode = false;
    private static HWND hWnd;
    
    // 扩展User32接口以包含缺失的方法
    public interface User32Ex extends User32 {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);
        
        // RAWINPUTDEVICE 结构体
        class RAWINPUTDEVICE extends Structure {
            public short usUsagePage;
            public short usUsage;
            public int dwFlags;
            public HWND hwndTarget;

            @Override
            protected java.util.List<String> getFieldOrder() {
                return java.util.Arrays.asList("usUsagePage", "usUsage", "dwFlags", "hwndTarget");
            }
        }
        
        // RAWINPUTHEADER
        class RAWINPUTHEADER extends Structure {
            public int dwType;
            public int dwSize;
            public HANDLE hDevice;
            public WPARAM wParam;

            @Override
            protected java.util.List<String> getFieldOrder() {
                return java.util.Arrays.asList("dwType", "dwSize", "hDevice", "wParam");
            }
        }
        
        // RAWMOUSE 结构体
        class RAWMOUSE extends Structure {
            public short usFlags;
            public short usButtonFlags;
            public short usButtonData;
            public int ulRawButtons;
            public int lLastX;
            public int lLastY;
            public int ulExtraInformation;

            @Override
            protected java.util.List<String> getFieldOrder() {
                return java.util.Arrays.asList("usFlags", "usButtonFlags", "usButtonData", "ulRawButtons", 
                                   "lLastX", "lLastY", "ulExtraInformation");
            }
        }
        
        // RAWINPUT (只演示鼠标)
        class RAWINPUT extends Structure {
            public RAWINPUTHEADER header;
            public RAWMOUSE mouse; // 注意：这里使用mouse而不是data

            @Override
            protected java.util.List<String> getFieldOrder() {
                return java.util.Arrays.asList("header", "mouse");
            }
            
            public RAWINPUT() {
                super();
            }
            
            public RAWINPUT(Pointer pointer) {
                super(pointer);
                read();
            }
        }
        
        // HRAWINPUT 定义
        class HRAWINPUT extends HANDLE {
            public HRAWINPUT() {}
            public HRAWINPUT(long value) { super(new Pointer(value)); }
            public HRAWINPUT(Pointer p) { super(p); }
        }
        
        // 扩展方法
        boolean RegisterRawInputDevices(RAWINPUTDEVICE[] pRawInputDevices, int uiNumDevices, int cbSize);
        int GetRawInputData(HRAWINPUT hRawInput, int uiCommand, Pointer pData, IntByReference pcbSize, int cbSizeHeader);
        void ClipCursor(RECT lpRect);
        int ShowCursor(boolean bShow);
        boolean RegisterHotKey(HWND hWnd, int id, int fsModifiers, int vk);
        void UnregisterHotKey(HWND hWnd, int id);
    }

    // 确保隐藏/显示光标的辅助：ShowCursor 是引用计数型 API
    private static void hideCursorEnsured() {
        int count;
        do { count = User32Ex.INSTANCE.ShowCursor(false); } while (count >= 0);
    }
    private static void showCursorEnsured() {
        int count;
        do { count = User32Ex.INSTANCE.ShowCursor(true); } while (count < 0);
    }

    public static void main(String[] args) {
        // 1) 注册窗口类并创建一个**隐藏消息窗口**，用于接收 WM_INPUT 与热键消息
        HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle(null);

        String clsName = "RawInputMsgWindow";
        WNDCLASSEX wclx = new WNDCLASSEX();
        wclx.cbSize = wclx.size();
        wclx.style = 0;
        wclx.lpfnWndProc = new WindowProc() {
            @Override
            public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
                if (uMsg == WM_INPUT) {
                    handleRawInput(lParam);
                    return new LRESULT(0);
                } else if (uMsg == WM_HOTKEY) {
                    // Ctrl+Alt+Esc：强制退出边界模式并释放资源
                    System.out.println("[HOTKEY] Ctrl+Alt+Esc pressed → exit edge mode");
                    exitEdgeMode();
                    User32.INSTANCE.PostQuitMessage(0);
                    return new LRESULT(0);
                }
                return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
        };
        wclx.hInstance = hInst;
        wclx.lpszClassName = clsName;
        if (User32.INSTANCE.RegisterClassEx(wclx).intValue() == 0) {
            throw new RuntimeException("RegisterClassEx failed");
        }

        hWnd = User32.INSTANCE.CreateWindowEx(
                0, clsName, "RawInputHiddenWindow",
                0, 0, 0, 0, 0,                                           // style/x/y/w/h （隐藏）
                null, null, hInst, null
        );
        if (hWnd == null) throw new RuntimeException("CreateWindowEx failed");

        // 2) 注册全局 Raw Input 鼠标监听（INPUTSINK 确保即便窗口不聚焦也能收到）
        User32Ex.RAWINPUTDEVICE rid = new User32Ex.RAWINPUTDEVICE();
        rid.usUsagePage = (short) 0x01;  // Generic Desktop Controls
        rid.usUsage = (short) 0x02;      // Mouse
        rid.dwFlags = RIDEV_INPUTSINK;   // 后台也能收到
        rid.hwndTarget = hWnd;           // 目标窗口
        if (!User32Ex.INSTANCE.RegisterRawInputDevices(
                new User32Ex.RAWINPUTDEVICE[]{rid}, 1, rid.size())) {
            throw new RuntimeException("RegisterRawInputDevices failed");
        }

        // 3) 注册热键：Ctrl+Alt+Esc → 退出
        if (!User32Ex.INSTANCE.RegisterHotKey(hWnd, HOTKEY_ID, MOD_CONTROL | MOD_ALT, VK_ESCAPE)) {
            System.err.println("RegisterHotKey failed (Ctrl+Alt+Esc)");
        }

        // 4) 启动"右边界监视"线程：当光标到达右边界 → 进入边界模式（隐藏+锁定）
        Thread edgeWatcher = new Thread(() -> watchRightEdgeAndLock());
        edgeWatcher.setDaemon(true);
        edgeWatcher.start();

        System.out.println("[READY] Move cursor to the RIGHT edge to enter edge-mode. Press Ctrl+Alt+Esc to quit.");

        // 5) 消息循环：处理 WM_INPUT / WM_HOTKEY 等
        MSG msg = new MSG();
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        // 6) 退出清理
        cleanup();
    }

    private static void watchRightEdgeAndLock() {
        int screenW = User32.INSTANCE.GetSystemMetrics(SM_CXSCREEN);
        int screenH = User32.INSTANCE.GetSystemMetrics(SM_CYSCREEN);
        POINT pt = new POINT();
        while (true) {
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}
            System.out.println(pt.x+","+pt.y);
            if (!User32.INSTANCE.GetCursorPos(pt)) continue;
            if (!edgeMode && pt.x >= screenW - 1) {
                enterEdgeMode(screenW, screenH);
            }
        }
    }

    private static void enterEdgeMode(int screenW, int screenH) {
        edgeMode = true;
        System.out.println("[EDGE] Enter edge-mode: hide cursor + clip to right edge");

        hideCursorEnsured();

        // 将可用区域限制为屏幕最右侧 1px 列：x ∈ [screenW-1, screenW-1]
        RECT r = new RECT();
        r.left = screenW - 1; r.right = screenW - 1; r.top = 0; r.bottom = 0;
        User32Ex.INSTANCE.ClipCursor(r);
        User32Ex.INSTANCE.ShowCursor(false);

        // 可选：把系统光标定位到右边界（避免视觉误差）
        User32.INSTANCE.SetCursorPos(screenW - 1,0);
    }

    private static void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        System.out.println("[EDGE] Exit edge-mode: unclip + show cursor");
        User32Ex.INSTANCE.ClipCursor(null);
        showCursorEnsured();
    }

    // 处理 WM_INPUT：解析 RAWINPUT，取鼠标相对位移 dx/dy
    private static void handleRawInput(LPARAM lParam) {
        // 1) 先询问所需缓冲区大小
        IntByReference pcbSize = new IntByReference();
        int headerSize = new User32Ex.RAWINPUTHEADER().size();
        User32Ex.INSTANCE.GetRawInputData(new User32Ex.HRAWINPUT(lParam.longValue()),
                RID_INPUT, null, pcbSize, headerSize);
        int size = pcbSize.getValue();
        if (size <= 0) return;

        // 2) 按大小申请缓冲并真正读取
        Memory buffer = new Memory(size);
        int read = User32Ex.INSTANCE.GetRawInputData(new User32Ex.HRAWINPUT(lParam.longValue()),
                RID_INPUT, buffer, pcbSize, headerSize);
        if (read != size) return;

        // 3) 解析 RAWINPUT 结构（JNA 已有定义）
        User32Ex.RAWINPUT raw = new User32Ex.RAWINPUT(buffer);
        raw.read();

        if (raw.header.dwType == RIM_TYPEMOUSE) {
            User32Ex.RAWMOUSE m = raw.mouse;
            int dx = m.lLastX;
            int dy = m.lLastY;

            if (dx != 0 || dy != 0) {
                // ★ 关键：即便 ClipCursor 锁住了系统光标，Raw Input 仍提供硬件相对位移
                System.out.printf("[RAW] dx=%d, dy=%d\n", dx, dy);

                // TODO: 在这里把 (dx, dy) 通过网络发送到目标设备（例如 UDP/WebSocket）
                // sendToPeer(dx, dy);
            }
        }
    }

    private static int getCursorY() {
        POINT pt = new POINT();
        if (User32.INSTANCE.GetCursorPos(pt)) return pt.y;
        return 0;
    }

    private static void cleanup() {
        try { User32Ex.INSTANCE.ClipCursor(null); } catch (Throwable ignored) {}
        try { showCursorEnsured(); } catch (Throwable ignored) {}
        try { User32Ex.INSTANCE.UnregisterHotKey(hWnd, HOTKEY_ID); } catch (Throwable ignored) {}
        try { User32.INSTANCE.DestroyWindow(hWnd); } catch (Throwable ignored) {}
        System.out.println("[CLEAN] resources released.");
    }
}