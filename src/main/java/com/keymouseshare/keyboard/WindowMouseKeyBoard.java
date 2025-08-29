package com.keymouseshare.keyboard;

import com.keymouseshare.bean.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.bean.VirtualDesktopStorage;
import com.keymouseshare.util.MouseEdgeDetector;
import com.sun.jna.*;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.keymouseshare.util.KeyBoardUtils.getButtonMask;
import static com.keymouseshare.util.KeyBoardUtils.getMouseEventFlags;

public class WindowMouseKeyBoard implements MouseKeyBoard {

    private static final Logger logger = Logger.getLogger(WindowMouseKeyBoard.class.getName());


    private static final WindowMouseKeyBoard INSTANCE = new WindowMouseKeyBoard();

    public static WindowMouseKeyBoard getInstance() {
        return INSTANCE;
    }

    private final DeviceStorage deviceStorage = DeviceStorage.getInstance();
    private final VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();
    private ScheduledExecutorService edgeWatcherExecutor;


    private Robot robot;

    private static final int WM_INPUT = 0x00FF;
    private static final int RID_INPUT = 0x10000003;
    private static final int WM_HOTKEY = 0x0312;
    private static final int RIM_TYPEMOUSE = 0;
    private static final int RIDEV_INPUTSINK = 0x00000100;
    private static final int HOTKEY_ID = 1;
    private static final int MOD_CONTROL = 0x0002;
    private static final int MOD_ALT = 0x0001;                         // [14]
    private static final int VK_ESCAPE = 0x1B;

    private static volatile boolean edgeMode = false;
    private static HWND hWnd;
    private static WNDCLASSEX wclx;

    // 扩展User32接口以包含缺失的方法
    public interface User32Ex extends User32 {
        WindowMouseKeyBoard.User32Ex INSTANCE = Native.load("user32", WindowMouseKeyBoard.User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

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
            public User32Ex.RAWINPUTHEADER header;
            public User32Ex.RAWMOUSE mouse; // 注意：这里使用mouse而不是data

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
        boolean RegisterRawInputDevices(User32Ex.RAWINPUTDEVICE[] pRawInputDevices, int uiNumDevices, int cbSize);
        int GetRawInputData(User32Ex.HRAWINPUT hRawInput, int uiCommand, Pointer pData, IntByReference pcbSize, int cbSizeHeader);
        void ClipCursor(RECT lpRect);
        int ShowCursor(boolean bShow);
        boolean RegisterHotKey(HWND hWnd, int id, int fsModifiers, int vk);
        void UnregisterHotKey(HWND hWnd, int id);

        /**
         * 注入鼠标事件
         *
         * @param dwFlags     事件标志
         * @param dx          X坐标
         * @param dy          Y坐标
         * @param dwData      数据
         * @param dwExtraInfo 额外信息
         */
        void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
        /**
         * 注入键盘事件
         *
         * @param bVk         虚拟键码
         * @param bScan       扫描码
         * @param dwFlags     事件标志
         * @param dwExtraInfo 额外信息
         */
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);

        // 设置光标位置
        boolean SetCursorPos(int x, int y);

        // 获取光标位置
        boolean GetCursorPos(User32.POINT lpPoint);

    }

    public WindowMouseKeyBoard() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.log(Level.SEVERE, "无法创建Robot实例", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标移动事件
                    User32Ex.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            // 回退到Robot
            robot.mouseMove(x, y);
        }
    }

    @Override
    public void mousePress(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);

            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标按下事件
                    int flags = getMouseEventFlags(button, true);
                    User32Ex.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.mousePress(buttonMask);
        }
    }

    @Override
    public void mouseRelease(int button) {
        if (robot != null) {
            int buttonMask = getButtonMask(button);

            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入鼠标释放事件
                    int flags = getMouseEventFlags(button, false);
                    User32Ex.INSTANCE.mouse_event(flags, 0, 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.mouseRelease(buttonMask);
        }
    }

    @Override
    public void mouseClick(int x, int y) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 先移动鼠标到指定位置
                    User32Ex.INSTANCE.mouse_event(0x0001, x, y, 0, 0);
                    // 模拟鼠标左键点击（按下然后释放）
                    User32Ex.INSTANCE.mouse_event(0x0002, 0, 0, 0, 0); // 左键按下
                    User32Ex.INSTANCE.mouse_event(0x0004, 0, 0, 0, 0); // 左键释放
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入鼠标事件，回退到Robot", e);
                }
            }
            // 回退到Robot
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    @Override
    public void mouseDragged() {

    }

    @Override
    public void keyPress(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘按下事件
                    User32Ex.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.keyPress(keyCode);
        }
    }

    @Override
    public void keyRelease(int keyCode) {
        if (robot != null) {
            // 尝试使用JNA在Windows上注入事件
            if (Platform.isWindows()) {
                try {
                    // 在Windows上使用JNA注入键盘释放事件
                    User32Ex.INSTANCE.keybd_event((byte) keyCode, (byte) 0, 0x0002, 0);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "无法使用JNA注入键盘事件，回退到Robot", e);
                }
            }

            // 回退到Robot
            robot.keyRelease(keyCode);
        }
    }

    private void virtualScreenEdgeCheck() {
        if (virtualDesktopStorage.getActiveScreen() == null) {
            return;
        }
        double x = virtualDesktopStorage.getMouseLocation()[0];
        double y = virtualDesktopStorage.getMouseLocation()[1];
        ScreenInfo screenInfo = MouseEdgeDetector.isAtScreenEdge(x, y);
        if (screenInfo != null) {
            // 更新激活屏幕
            if(!(screenInfo.getDeviceIp()+screenInfo.getScreenName()).equals(virtualDesktopStorage.getActiveScreen().getDeviceIp()+virtualDesktopStorage.getActiveScreen().getScreenName())){
                System.out.println("激活设备："+screenInfo.getDeviceIp()+",屏幕："+screenInfo.getScreenName());
                virtualDesktopStorage.setActiveScreen(screenInfo);
            }
            System.out.println("检查鼠标边缘检测:  位置=(" + x + ", " + y + "),检测到边缘设备："+screenInfo.getDeviceIp()+",屏幕："+screenInfo.getScreenName());
            // 如果是当前设备进行鼠标控制
            if (screenInfo.getDeviceIp().equals(deviceStorage.getSeverDevice().getIpAddress())) {
                cleanup();
                exitEdgeMode();
            } else {
                if (!edgeMode) {
                    enterEdgeMode();
                }
            }
        }
    }

    @Override
    public void startMouseKeyController() {
        if (!Platform.isWindows()) {
            return; // 仅在Windows上实现
        }

        // 1) 注册窗口类并创建一个**隐藏消息窗口**，用于接收 WM_INPUT 与热键消息
        HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle(null);
        String clsName = "RawInputMsgWindow";
        wclx = new WNDCLASSEX();
        wclx.cbSize = wclx.size();
        wclx.style = 0;

        wclx.lpfnWndProc = new WinUser.WindowProc() {
            @Override
            public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
                if (uMsg == WM_INPUT) {
                    if (edgeMode) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ignored) {
                        }
                        handleRawInput(lParam);
                    }
                    return new LRESULT(0);
                } else if (uMsg == WM_HOTKEY) {
                    // Ctrl+Alt+Esc：强制退出边界模式并释放资源
                    System.out.println("[HOTKEY] Ctrl+Alt+Esc pressed → exit edge mode");
                    User32Ex.INSTANCE.PostQuitMessage(0);
                    cleanup();
                    return null;
                }
                return User32Ex.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
            }
        };

        wclx.hInstance = hInst;
        wclx.lpszClassName = clsName;
        if (User32Ex.INSTANCE.RegisterClassEx(wclx).intValue() == 0) {
            throw new RuntimeException("RegisterClassEx failed");
        }

        hWnd = User32Ex.INSTANCE.CreateWindowEx(
                0, clsName, "RawInputHiddenWindow",                     // [29]
                0, 0, 0, 0, 0,                                           // style/x/y/w/h （隐藏）
                null, null, hInst, null
        );

        if (hWnd == null) throw new RuntimeException("CreateWindowEx failed");

        // 2) 注册全局 Raw Input 鼠标监听（INPUTSINK 确保即便窗口不聚焦也能收到）
        User32Ex.RAWINPUTDEVICE rid = new User32Ex.RAWINPUTDEVICE();                      // [31]
        rid.usUsagePage = (short) 0x01;  // Generic Desktop Controls
        rid.usUsage = (short) 0x02;      // Mouse
        rid.dwFlags = RIDEV_INPUTSINK;   // 后台也能收到
        rid.hwndTarget = hWnd;           // 目标窗口
        if (!User32Ex.INSTANCE.RegisterRawInputDevices(
                new User32Ex.RAWINPUTDEVICE[]{rid}, 1, rid.size())) {            // [32]
            throw new RuntimeException("RegisterRawInputDevices failed");
        }

        // 3) 注册热键：Ctrl+Alt+Esc → 退出
        if (!User32Ex.INSTANCE.RegisterHotKey(hWnd, HOTKEY_ID, MOD_CONTROL | MOD_ALT, VK_ESCAPE)) { // [33]
            System.err.println("RegisterHotKey failed (Ctrl+Alt+Esc)");
        }

        // 4) 启动"右边界监视"线程：当光标到达右边界 → 进入边界模式（隐藏+锁定）
        if(edgeWatcherExecutor==null||edgeWatcherExecutor.isTerminated()){
            edgeWatcherExecutor = Executors.newScheduledThreadPool(1);
        }
        edgeWatcherExecutor.scheduleAtFixedRate(this::virtualScreenEdgeCheck, 0, 5, TimeUnit.MILLISECONDS);

        // 5) 消息循环：处理 WM_INPUT / WM_HOTKEY 等
        WinUser.MSG msg = new WinUser.MSG();                                            // [35]
        while (User32Ex.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
            User32Ex.INSTANCE.TranslateMessage(msg);
            User32Ex.INSTANCE.DispatchMessage(msg);
        }

    }

    @Override
    public void stopMouseKeyController() {
        cleanup();
    }

    // 确保隐藏/显示光标的辅助：ShowCursor 是引用计数型 API
    private static void hideCursorEnsured() {                           // [19]
        int count;
        do {
            count = User32Ex.INSTANCE.ShowCursor(false);
        } while (count >= 0);
    }

    private static void showCursorEnsured() {                           // [20]
        int count;
        do {
            count = User32Ex.INSTANCE.ShowCursor(true);
        } while (count < 0);
    }

    private void enterEdgeMode() {        // [40]
        edgeMode = true;
        System.out.println("[EDGE] Enter edge-mode: hide cursor + clip to right edge");

        hideCursorEnsured();                                             // [41]

        // 将可用区域限制为屏幕最右侧 1px 列：x ∈ [screenW-1, screenW-1]
        RECT r = new RECT();                                             // [42]
        r.left = 0;
        r.right = 1;
        r.top = 0;
        r.bottom = 1;
        User32Ex.INSTANCE.ClipCursor(r);
        User32Ex.INSTANCE.ShowCursor(false);

        // 可选：把系统光标定位到右边界（避免视觉误差）
        User32Ex.INSTANCE.SetCursorPos(-1, -1); // [43]

        System.out.println("[READY] Move cursor to the RIGHT edge to enter edge-mode. Press Ctrl+Alt+Esc to quit.");
    }

    private void exitEdgeMode() {
        if (!edgeMode) return;
        edgeMode = false;
        System.out.println("[EDGE] Exit edge-mode: unclip + show cursor");
        User32Ex.INSTANCE.ClipCursor(null);
        showCursorEnsured();
    }

    @Override
    public void stopEdgeDetection() {
        edgeWatcherExecutor.close();
    }

    private void cleanup() {
        try {
            User32Ex.INSTANCE.ClipCursor(null);
        } catch (Throwable ignored) {
        }
        try {
            showCursorEnsured();
        } catch (Throwable ignored) {
        }
        try {
            User32Ex.INSTANCE.UnregisterHotKey(hWnd, HOTKEY_ID);
        } catch (Throwable ignored) {
        }
        try {
            User32Ex.INSTANCE.DestroyWindow(hWnd);
        } catch (Throwable ignored) {
        }
        try {
            if(User32Ex.INSTANCE.UnregisterClass(wclx.lpszClassName, wclx.hInstance)){
                System.out.println("[UnregisterClass] success.");
            }else{
                System.out.println("[UnregisterClass] failed.");
            };
        }catch (Throwable ignored){

        }

        System.out.println("[CLEAN] resources released.");
    }

    @Override
    public void initVirtualMouseLocation() {
        if (virtualDesktopStorage.isApplyVirtualDesktopScreen()) {
            User32Ex.POINT pt = new User32Ex.POINT();
            // 这里返回的是设备的虚拟桌面绝对坐标？？？
            User32Ex.INSTANCE.GetCursorPos(pt);
            System.out.println(pt.x + "," + pt.y);// [39]
            // 获取本地设备屏幕坐标系中的鼠标相对位置
            ScreenInfo screenInfo = deviceStorage.getLocalDevice().getScreens().stream()
                    .filter(s -> s.localContains(pt.x, pt.y))
                    .findFirst()
                    .orElse(null);
            // 修改鼠标虚拟桌面所在坐标
            if (screenInfo != null) {
                ScreenInfo vScreenInfo = virtualDesktopStorage.getScreens().get(screenInfo.getDeviceIp() + screenInfo.getScreenName());
                // 控制器上更新当前鼠标所在屏幕
                virtualDesktopStorage.setActiveScreen(vScreenInfo);
                // 控制器上更新虚拟桌面鼠标坐标
                //  pt.x-screenInfo.getDx(),pt.y-screenInfo.getDy() 本地虚拟屏幕的相对坐标位置
                //  vScreenInfo.getVx()+ pt.x-screenInfo.getDx(),vScreenInfo.getVy()+pt.y-screenInfo.getDy() 控制器虚拟桌面的绝对坐标位置
                virtualDesktopStorage.setMouseLocation(vScreenInfo.getVx() + pt.x - screenInfo.getDx(), vScreenInfo.getVy() + pt.y - screenInfo.getDy());
            }
        }
    }

    // 处理 WM_INPUT：解析 RAWINPUT，取鼠标相对位移 dx/dy
    private void handleRawInput(LPARAM lParam) {
        // 1) 先询问所需缓冲区大小
        IntByReference pcbSize = new IntByReference();
        int headerSize = new User32Ex.RAWINPUTHEADER().size();                    // [48]
        User32Ex.INSTANCE.GetRawInputData(new User32Ex.HRAWINPUT(lParam.longValue()),
                RID_INPUT, null, pcbSize, headerSize);                   // [49]
        int size = pcbSize.getValue();
        if (size <= 0) return;

        // 2) 按大小申请缓冲并真正读取
        Memory buffer = new Memory(size);                                // [50]
        int read = User32Ex.INSTANCE.GetRawInputData(new User32Ex.HRAWINPUT(lParam.longValue()),
                RID_INPUT, buffer, pcbSize, headerSize);                 // [51]
        if (read != size) return;

        // 3) 解析 RAWINPUT 结构（JNA 已有定义）
        User32Ex.RAWINPUT raw = new User32Ex.RAWINPUT(buffer);                             // [52]
        raw.read();                                                      // [53]

        if (raw.header.dwType == RIM_TYPEMOUSE) {                        // [54]
            User32Ex.RAWMOUSE m = raw.mouse;                                 // [55]
            int dx = m.lLastX;                                // [56]
            int dy = m.lLastY;                                // [57]

            if (dx != 0 || dy != 0) {
                // ★ 关键：即便 ClipCursor 锁住了系统光标，Raw Input 仍提供硬件相对位移
//                System.out.print(System.currentTimeMillis() + "\t" + virtualDesktopStorage.getActiveScreen().getDeviceIp());
//                System.out.print("[" + virtualDesktopStorage.getMouseLocation()[0] + "," + virtualDesktopStorage.getMouseLocation()[1] + ']');
                virtualDesktopStorage.moveMouseLocation(dx, dy);
//                System.out.printf("\t[RAW] dx=%d, dy=%d", dx, dy);
//                System.out.println("\t[" + virtualDesktopStorage.getMouseLocation()[0] + "," + virtualDesktopStorage.getMouseLocation()[1] + ']');
            }
        }
    }
}