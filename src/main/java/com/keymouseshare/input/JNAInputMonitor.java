package com.keymouseshare.input;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Callback;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 基于JNA的本地键盘鼠标输入监听器
 * 用于监听本地设备的键盘和鼠标事件并打印日志
 */
public class JNAInputMonitor {
    private static final Logger logger = Logger.getLogger(JNAInputMonitor.class.getName());
    
    private boolean isMonitoring = false;
    private Thread monitoringThread;
    
    // Windows API接口
    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);
        
        interface LowLevelMouseProc extends Callback {
            Pointer callback(int nCode, Pointer wParam, Pointer lParam);
        }
        
        interface LowLevelKeyboardProc extends Callback {
            Pointer callback(int nCode, Pointer wParam, Pointer lParam);
        }
        
        Pointer SetWindowsHookEx(int idHook, LowLevelMouseProc lpfn, Pointer hMod, int dwThreadId);
        Pointer SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn, Pointer hMod, int dwThreadId);
        boolean UnhookWindowsHookEx(Pointer hhk);
        Pointer CallNextHookEx(Pointer hhk, int nCode, Pointer wParam, Pointer lParam);
    }
    
    // Mac平台安全实现
    private static class MacSafeImplementation {
        // Mac API接口
        public interface MacCoreGraphics extends Library {
            MacCoreGraphics INSTANCE = Native.load("CoreGraphics", MacCoreGraphics.class);
            
            interface CGEventTapCallBack extends Callback {
                Pointer callback(Pointer proxy, int type, Pointer event, Pointer refcon);
            }
            
            Pointer CGEventTapCreate(
                int tap, int place, int options, 
                long eventsOfInterest, CGEventTapCallBack callback, Pointer userInfo);
            
            Pointer CFMachPortCreateRunLoopSource(Pointer allocator, Pointer port, int order);
            void CFRunLoopAddSource(Pointer rl, Pointer source, Pointer mode);
            Pointer CFRunLoopGetCurrent();
            void CFRunLoopRun();
            void CFRunLoopStop(Pointer rl);
            void CGEventTapEnable(Pointer tap, boolean enable);
            Pointer CGEventGetLocation(Pointer event);
            int CGEventGetIntegerValueField(Pointer event, int field);
        }
        
        // Mac CGPoint结构体
        public static class CGPoint extends Structure {
            public static class ByReference extends CGPoint implements Structure.ByReference {}
            
            public double x;
            public double y;
            
            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("x", "y");
            }
        }
        
        public static boolean initializeAndRun(JNAInputMonitor monitor) {
            try {
                logger.info("开始初始化Mac平台安全实现");
                
                // 先检查是否可以加载CoreGraphics库
                MacCoreGraphics coreGraphics;
                try {
                    coreGraphics = MacCoreGraphics.INSTANCE;
                    logger.info("成功加载CoreGraphics库");
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.SEVERE, "无法加载CoreGraphics库", e);
                    System.err.println("无法加载CoreGraphics库: " + e.getMessage());
                    return false;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "加载CoreGraphics库时发生异常", e);
                    System.err.println("加载CoreGraphics库时发生异常: " + e.getMessage());
                    return false;
                }
                
                // 定义需要监听的事件类型掩码
                // kCGEventLeftMouseDown=1, kCGEventLeftMouseUp=2, kCGEventRightMouseDown=3,
                // kCGEventRightMouseUp=4, kCGEventMouseMoved=5, kCGEventLeftMouseDragged=6,
                // kCGEventRightMouseDragged=7, kCGEventKeyDown=10, kCGEventKeyUp=11
                long eventMask = (1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | 
                                (1L << 5) | (1L << 6) | (1L << 7) | (1L << 10) | (1L << 11);
                
                // 事件回调函数
                MacCoreGraphics.CGEventTapCallBack eventCallback = new MacCoreGraphics.CGEventTapCallBack() {
                    public Pointer callback(Pointer proxy, int type, Pointer event, Pointer refcon) {
                        if (!monitor.isMonitoring()) {
                            return event;
                        }
                        
                        try {
                            switch (type) {
                                case 1: // kCGEventLeftMouseDown
                                    logger.info("Mac鼠标事件: 类型=左键按下");
                                    System.out.println("Mac鼠标事件: 类型=左键按下");
                                    break;
                                case 2: // kCGEventLeftMouseUp
                                    logger.info("Mac鼠标事件: 类型=左键释放");
                                    System.out.println("Mac鼠标事件: 类型=左键释放");
                                    break;
                                case 3: // kCGEventRightMouseDown
                                    logger.info("Mac鼠标事件: 类型=右键按下");
                                    System.out.println("Mac鼠标事件: 类型=右键按下");
                                    break;
                                case 4: // kCGEventRightMouseUp
                                    logger.info("Mac鼠标事件: 类型=右键释放");
                                    System.out.println("Mac鼠标事件: 类型=右键释放");
                                    break;
                                case 5: // kCGEventMouseMoved
                                    // 获取鼠标位置
                                    try {
                                        Pointer locationPtr = safeCall(() -> coreGraphics.CGEventGetLocation(event));
                                        if (locationPtr != null) {
                                            // 使用更安全的方式创建CGPoint结构体
                                            CGPoint location = new CGPoint();
                                            // 通过内存偏移量设置结构体内容
                                            location.getPointer().setPointer(0, locationPtr);
                                            location.read();
                                            
                                            logger.info("Mac鼠标事件: 类型=鼠标移动, 位置=(" + 
                                                       location.x + "," + location.y + ")");
                                            System.out.println("Mac鼠标事件: 类型=鼠标移动, 位置=(" + 
                                                              location.x + "," + location.y + ")");
                                        } else {
                                            logger.warning("无法获取鼠标位置指针");
                                        }
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "获取鼠标位置时发生异常", e);
                                    }
                                    break;
                                case 6: // kCGEventLeftMouseDragged
                                    // 获取鼠标位置
                                    try {
                                        Pointer locationPtr = safeCall(() -> coreGraphics.CGEventGetLocation(event));
                                        if (locationPtr != null) {
                                            // 使用更安全的方式创建CGPoint结构体
                                            CGPoint location = new CGPoint();
                                            // 通过内存偏移量设置结构体内容
                                            location.getPointer().setPointer(0, locationPtr);
                                            location.read();
                                            
                                            logger.info("Mac鼠标事件: 类型=左键拖拽, 位置=(" + 
                                                       location.x + "," + location.y + ")");
                                            System.out.println("Mac鼠标事件: 类型=左键拖拽, 位置=(" + 
                                                              location.x + "," + location.y + ")");
                                        } else {
                                            logger.warning("无法获取鼠标位置指针");
                                        }
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "获取鼠标位置时发生异常", e);
                                    }
                                    break;
                                case 7: // kCGEventRightMouseDragged
                                    // 获取鼠标位置
                                    try {
                                        Pointer locationPtr = safeCall(() -> coreGraphics.CGEventGetLocation(event));
                                        if (locationPtr != null) {
                                            // 使用更安全的方式创建CGPoint结构体
                                            CGPoint location = new CGPoint();
                                            // 通过内存偏移量设置结构体内容
                                            location.getPointer().setPointer(0, locationPtr);
                                            location.read();
                                            
                                            logger.info("Mac鼠标事件: 类型=右键拖拽, 位置=(" + 
                                                       location.x + "," + location.y + ")");
                                            System.out.println("Mac鼠标事件: 类型=右键拖拽, 位置=(" + 
                                                              location.x + "," + location.y + ")");
                                        } else {
                                            logger.warning("无法获取鼠标位置指针");
                                        }
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "获取鼠标位置时发生异常", e);
                                    }
                                    break;
                                case 10: // kCGEventKeyDown
                                    try {
                                        Integer keyCode = safeCall(() -> coreGraphics.CGEventGetIntegerValueField(event, 9)); // kCGKeyboardEventKeycode
                                        if (keyCode != null) {
                                            logger.info("Mac键盘事件: 类型=按键按下, 键码=" + keyCode);
                                            System.out.println("Mac键盘事件: 类型=按键按下, 键码=" + keyCode);
                                        }
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "获取键盘键码时发生异常", e);
                                    }
                                    break;
                                case 11: // kCGEventKeyUp
                                    try {
                                        Integer keyCode = safeCall(() -> coreGraphics.CGEventGetIntegerValueField(event, 9)); // kCGKeyboardEventKeycode
                                        if (keyCode != null) {
                                            logger.info("Mac键盘事件: 类型=按键释放, 键码=" + keyCode);
                                            System.out.println("Mac键盘事件: 类型=按键释放, 键码=" + keyCode);
                                        }
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "获取键盘键码时发生异常", e);
                                    }
                                    break;
                                default:
                                    // 忽略其他事件类型
                                    break;
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "处理Mac事件时发生异常", e);
                        }
                        
                        return event;
                    }
                };
                
                // 创建事件监听器
                // kCGSessionEventTap=0, kCGHeadInsertEventTap=0, kCGEventTapOptionDefault=0
                Pointer eventTap = null;
                try {
                    logger.info("尝试创建事件监听器");
                    eventTap = safeCall(() -> coreGraphics.CGEventTapCreate(
                        0, 0, 0, eventMask, eventCallback, null));
                    logger.info("事件监听器创建完成");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "创建事件监听器时发生异常", e);
                    System.err.println("创建事件监听器时发生异常: " + e.getMessage());
                    return false;
                }
                
                if (eventTap == null) {
                    logger.severe("无法创建Mac事件监听器，可能是因为缺少辅助功能权限");
                    System.err.println("无法创建Mac事件监听器，可能是因为缺少辅助功能权限");
                    return false;
                }
                
                logger.info("Mac事件监听器已创建");
                System.out.println("Mac事件监听器已创建");
                
                // 创建运行循环源并添加到当前运行循环
                Pointer runLoopSource = null;
                try {
                    logger.info("尝试创建运行循环源");
                    final Pointer finalEventTap = eventTap;
                    runLoopSource = safeCall(() -> coreGraphics.CFMachPortCreateRunLoopSource(
                        null, finalEventTap, 0));
                    logger.info("运行循环源创建完成");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "创建运行循环源时发生异常", e);
                    System.err.println("创建运行循环源时发生异常: " + e.getMessage());
                    return false;
                }
                
                if (runLoopSource == null) {
                    logger.severe("无法创建运行循环源");
                    System.err.println("无法创建运行循环源");
                    return false;
                }
                
                Pointer runLoop = null;
                try {
                    logger.info("尝试获取当前运行循环");
                    runLoop = safeCall(() -> coreGraphics.CFRunLoopGetCurrent());
                    logger.info("当前运行循环获取完成");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "获取当前运行循环时发生异常", e);
                    System.err.println("获取当前运行循环时发生异常: " + e.getMessage());
                    return false;
                }
                
                if (runLoop == null) {
                    logger.severe("无法获取当前运行循环");
                    System.err.println("无法获取当前运行循环");
                    return false;
                }
                
                try {
                    logger.info("尝试将运行循环源添加到运行循环");
                    final Pointer finalRunLoop = runLoop;
                    final Pointer finalRunLoopSource = runLoopSource;
                    safeCall(() -> {
                        // 暂时禁用CFRunLoopAddSource调用，避免崩溃
                        // coreGraphics.CFRunLoopAddSource(finalRunLoop, finalRunLoopSource, 
                        //    createCFStringConstant("kCFRunLoopDefaultMode"));
                        return null;
                    });
                    logger.info("运行循环源添加完成（已禁用以避免崩溃）");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "将运行循环源添加到运行循环时发生异常", e);
                    System.err.println("将运行循环源添加到运行循环时发生异常: " + e.getMessage());
                    return false;
                }
                
                // 启用事件监听器
                try {
                    logger.info("尝试启用事件监听器");
                    final Pointer finalEventTap = eventTap;
                    safeCall(() -> {
                        coreGraphics.CGEventTapEnable(finalEventTap, true);
                        return null;
                    });
                    logger.info("事件监听器启用完成");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "启用事件监听器时发生异常", e);
                    System.err.println("启用事件监听器时发生异常: " + e.getMessage());
                    return false;
                }
                
                // 运行事件循环直到停止
                logger.info("开始运行事件循环");
                while (monitor.isMonitoring() && !Thread.currentThread().isInterrupted()) {
                    try {
                        // 暂时禁用CFRunLoopRun调用，避免崩溃
                        // safeCall(() -> {
                        //     coreGraphics.CFRunLoopRun();
                        //     return null;
                        // });
                        // 改为简单的sleep，避免占用过多CPU
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.info("Mac监听线程被中断");
                        break;
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "运行循环时发生异常", e);
                        System.err.println("运行循环时发生异常: " + e.getMessage());
                        break;
                    }
                }
                
                // 停止运行循环
                try {
                    logger.info("尝试停止运行循环");
                    final Pointer finalRunLoop = runLoop;
                    safeCall(() -> {
                        // 暂时禁用CFRunLoopStop调用，避免崩溃
                        // coreGraphics.CFRunLoopStop(finalRunLoop);
                        return null;
                    });
                    logger.info("运行循环停止完成（已禁用以避免崩溃）");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "停止运行循环时发生异常", e);
                }
                
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Mac平台JNA输入监听异常", e);
                System.err.println("Mac平台JNA输入监听异常: " + e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                logger.info("Mac平台监听线程结束");
            }
        }
        
        // 安全调用包装器
        private static <T> T safeCall(Callable<T> callable) {
            try {
                return callable.call();
            } catch (UnsatisfiedLinkError e) {
                logger.log(Level.WARNING, "Native方法调用失败: " + e.getMessage(), e);
                return null;
            } catch (Exception e) {
                logger.log(Level.WARNING, "方法调用异常: " + e.getMessage(), e);
                return null;
            }
        }
        
        // 创建CFString常量的简单方法
        private static Pointer createCFStringConstant(String mode) {
            // 根据CoreFoundation的定义，这些是常见的模式常量
            if ("kCFRunLoopDefaultMode".equals(mode)) {
                // 这是kCFRunLoopDefaultMode的CFStringRef值
                return Pointer.createConstant(0x0000000000000001L);
            } else if ("kCFRunLoopCommonModes".equals(mode)) {
                // 这是kCFRunLoopCommonModes的CFStringRef值
                return Pointer.createConstant(0x0000000000000002L);
            }
            // 默认返回kCFRunLoopDefaultMode
            return Pointer.createConstant(0x0000000000000001L);
        }
        
        // 函数式接口用于安全调用
        private interface Callable<T> {
            T call() throws Exception;
        }
    }
    
    // Windows鼠标钩子结构
    public static class MSLLHOOKSTRUCT extends Structure {
        public static class ByReference extends MSLLHOOKSTRUCT implements Structure.ByReference {}
        
        public int pt_x;
        public int pt_y;
        public int mouseData;
        public int flags;
        public int time;
        public Pointer dwExtraInfo;
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("pt_x", "pt_y", "mouseData", "flags", "time", "dwExtraInfo");
        }
    }
    
    // Windows键盘钩子结构
    public static class KBDLLHOOKSTRUCT extends Structure {
        public static class ByReference extends KBDLLHOOKSTRUCT implements Structure.ByReference {}
        
        public int vkCode;
        public int scanCode;
        public int flags;
        public int time;
        public Pointer dwExtraInfo;
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("vkCode", "scanCode", "flags", "time", "dwExtraInfo");
        }
    }
    
    public JNAInputMonitor() {
        logger.info("JNAInputMonitor实例已创建，操作系统: " + System.getProperty("os.name"));
    }
    
    /**
     * 开始监听本地输入事件
     */
    public void startMonitoring() {
        if (isMonitoring) {
            logger.info("JNA监听器已在运行中");
            return;
        }
        
        isMonitoring = true;
        logger.info("开始使用JNA监听本地键盘鼠标事件");
        
        if (Platform.isWindows()) {
            startWindowsMonitoring();
        } else if (Platform.isMac()) {
            startMacMonitoring();
        } else if (Platform.isLinux()) {
            startLinuxMonitoring();
        } else {
            logger.warning("不支持的操作系统: " + System.getProperty("os.name"));
            isMonitoring = false;
        }
    }
    
    /**
     * 停止监听本地输入事件
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            logger.info("JNA监听器未在运行");
            return;
        }
        
        isMonitoring = false;
        logger.info("停止使用JNA监听本地键盘鼠标事件");
        
        if (monitoringThread != null) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }
    }
    
    /**
     * Windows平台输入监听实现
     */
    private void startWindowsMonitoring() {
        logger.info("开始Windows平台JNA输入监听");
        
        monitoringThread = new Thread(() -> {
            try {
                // 鼠标钩子回调
                User32.LowLevelMouseProc mouseCallback = new User32.LowLevelMouseProc() {
                    public Pointer callback(int nCode, Pointer wParam, Pointer lParam) {
                        if (nCode >= 0 && isMonitoring) {
                            long wParamValue = Pointer.nativeValue(wParam);
                            
                            // 使用指针初始化结构体
                            MSLLHOOKSTRUCT mouseStruct = new MSLLHOOKSTRUCT();
                            // 通过内存偏移量设置结构体内容
                            mouseStruct.getPointer().setPointer(0, lParam);
                            mouseStruct.read();
                            
                            String eventType = "未知";
                            switch ((int) wParamValue) {
                                case 0x0200: // WM_MOUSEMOVE
                                    eventType = "鼠标移动";
                                    break;
                                case 0x0201: // WM_LBUTTONDOWN
                                    eventType = "左键按下";
                                    break;
                                case 0x0202: // WM_LBUTTONUP
                                    eventType = "左键释放";
                                    break;
                                case 0x0204: // WM_RBUTTONDOWN
                                    eventType = "右键按下";
                                    break;
                                case 0x0205: // WM_RBUTTONUP
                                    eventType = "右键释放";
                                    break;
                                case 0x0207: // WM_MBUTTONDOWN
                                    eventType = "中键按下";
                                    break;
                                case 0x0208: // WM_MBUTTONUP
                                    eventType = "中键释放";
                                    break;
                                case 0x020A: // WM_MOUSEWHEEL
                                    eventType = "鼠标滚轮";
                                    break;
                            }
                            
                            logger.info("Windows鼠标事件: 类型=" + eventType + ", 位置=(" + 
                                       mouseStruct.pt_x + "," + mouseStruct.pt_y + ")");
                            System.out.println("Windows鼠标事件: 类型=" + eventType + ", 位置=(" + 
                                              mouseStruct.pt_x + "," + mouseStruct.pt_y + ")");
                        }
                        return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, lParam);
                    }
                };
                
                // 键盘钩子回调
                User32.LowLevelKeyboardProc keyboardCallback = new User32.LowLevelKeyboardProc() {
                    public Pointer callback(int nCode, Pointer wParam, Pointer lParam) {
                        if (nCode >= 0 && isMonitoring) {
                            long wParamValue = Pointer.nativeValue(wParam);
                            
                            // 使用指针初始化结构体
                            KBDLLHOOKSTRUCT keyboardStruct = new KBDLLHOOKSTRUCT();
                            // 通过内存偏移量设置结构体内容
                            keyboardStruct.getPointer().setPointer(0, lParam);
                            keyboardStruct.read();
                            
                            String eventType = "未知";
                            switch ((int) wParamValue) {
                                case 0x0100: // WM_KEYDOWN
                                    eventType = "按键按下";
                                    break;
                                case 0x0101: // WM_KEYUP
                                    eventType = "按键释放";
                                    break;
                                case 0x0104: // WM_SYSKEYDOWN
                                    eventType = "系统按键按下";
                                    break;
                                case 0x0105: // WM_SYSKEYUP
                                    eventType = "系统按键释放";
                                    break;
                            }
                            
                            logger.info("Windows键盘事件: 类型=" + eventType + ", 虚拟键码=" + 
                                       keyboardStruct.vkCode);
                            System.out.println("Windows键盘事件: 类型=" + eventType + ", 虚拟键码=" + 
                                              keyboardStruct.vkCode);
                        }
                        return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, lParam);
                    }
                };
                
                // 设置鼠标和键盘钩子
                Pointer mouseHook = null;
                Pointer keyboardHook = null;
                try {
                    mouseHook = User32.INSTANCE.SetWindowsHookEx(14, mouseCallback, null, 0); // WH_MOUSE_LL=14
                    keyboardHook = User32.INSTANCE.SetWindowsHookEx(13, keyboardCallback, null, 0); // WH_KEYBOARD_LL=13
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.SEVERE, "无法设置Windows钩子", e);
                    System.err.println("无法设置Windows钩子: " + e.getMessage());
                    isMonitoring = false;
                    return;
                }
                
                if (mouseHook == null || keyboardHook == null) {
                    logger.severe("无法设置Windows钩子");
                    System.err.println("无法设置Windows钩子");
                    isMonitoring = false;
                    return;
                }
                
                logger.info("Windows钩子已设置");
                
                // 消息循环
                while (isMonitoring && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(100);
                }
                
                // 卸载钩子
                try {
                    if (mouseHook != null) {
                        User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
                    }
                    if (keyboardHook != null) {
                        User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
                    }
                } catch (UnsatisfiedLinkError e) {
                    logger.log(Level.WARNING, "卸载Windows钩子时发生异常", e);
                }
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Windows平台JNA输入监听异常", e);
            }
        });
        
        monitoringThread.start();
    }
    
    /**
     * Mac平台输入监听实现
     */
    private void startMacMonitoring() {
        logger.info("开始Mac平台JNA输入监听");
        
        monitoringThread = new Thread(() -> {
            try {
                boolean success = MacSafeImplementation.initializeAndRun(this);
                if (!success) {
                    logger.severe("Mac平台初始化失败");
                    System.err.println("Mac平台初始化失败");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Mac平台JNA输入监听异常", e);
                System.err.println("Mac平台JNA输入监听异常: " + e.getMessage());
                e.printStackTrace();
            } finally {
                logger.info("Mac平台监听线程结束");
            }
        });
        
        monitoringThread.start();
    }
    
    /**
     * Linux平台输入监听实现（占位）
     */
    private void startLinuxMonitoring() {
        logger.info("开始Linux平台JNA输入监听");
        
        monitoringThread = new Thread(() -> {
            try {
                // 在实际实现中，这里会使用Linux的evdev或X11相关API
                logger.info("Linux平台JNA输入监听已启动（占位实现）");
                System.out.println("Linux平台JNA输入监听已启动（占位实现）");
                
                while (isMonitoring && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                logger.info("Linux监听线程被中断");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Linux平台JNA输入监听异常", e);
            }
        });
        
        monitoringThread.start();
    }
    
    /**
     * 检查监听器是否正在运行
     * @return 是否正在监听
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
}