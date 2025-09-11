package com.keymouseshare.util;

import com.keymouseshare.bean.MoveTargetScreenInfo;
import com.keymouseshare.storage.DeviceStorage;
import com.keymouseshare.bean.ScreenInfo;
import com.keymouseshare.storage.VirtualDesktopStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.keymouseshare.keyboard.MouseKeyBoard;
import com.keymouseshare.keyboard.MouseKeyBoardFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 鼠标边缘检测工具类
 * 实现动态阈值检测法来判断鼠标是否接近屏幕边缘
 */
public class MouseEdgeDetector {

    private static final Logger logger = LoggerFactory.getLogger(MouseEdgeDetector.class);

    // 鼠标移动速度计算相关
    private static final long VELOCITY_CALCULATION_INTERVAL = 100; // 速度计算间隔（毫秒）
    private static final int BASE_THRESHOLD = 5; // 基础阈值（像素）
    private static final double VELOCITY_FACTOR = 0.1; // 速度因子
    private static final long ANTI_FALSE_TRIGGER_TIME = 100; // 防误触时间（毫秒）
    private static final int IGNORE_BUFFER_ZONE = 165; // 忽略中间区域（像素）

    // 鼠标位置和时间记录
    private static double lastX = 0;
    private static double lastY = 0;
    private static long lastTime = 0;
    private static double lastVelocity = 0;

    // 防误触相关
    private static long edgeEntryTime = 0;
    private static String lastEdgeScreenId = null;
    private static String currentEdgeScreenId = null;
    private static EdgeDirection lastEdgeDirection = null;
    private static EdgeDirection currentEdgeDirection = null;

    // 记录每个屏幕的边缘状态
    private static Map<String, EdgeState> screenEdgeStates = new ConcurrentHashMap<>();

    private static VirtualDesktopStorage virtualDesktopStorage = VirtualDesktopStorage.getInstance();

    private static MouseKeyBoard mouseKeyBoard = MouseKeyBoardFactory.getFactory();

    private static DeviceStorage deviceStorage = DeviceStorage.getInstance();

    /**
     * 边缘方向枚举
     */
    public enum EdgeDirection {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    /**
     * 边缘状态类
     */
    private static class EdgeState {
        boolean isAtEdge;
        long entryTime;
        EdgeDirection direction;

        EdgeState(boolean isAtEdge, long entryTime, EdgeDirection direction) {
            this.isAtEdge = isAtEdge;
            this.entryTime = entryTime;
            this.direction = direction;
        }
    }

    /**
     * 虚拟屏幕边缘检测鼠标是否在边缘
     *
     * @return 如果鼠标在屏幕边缘且满足触发条件，返回将被唤醒鼠标的ScreenInfo对象，否则返回null
     */
    public static MoveTargetScreenInfo isAtScreenEdge() {
        int x = virtualDesktopStorage.getMouseLocation()[0];
        int y = virtualDesktopStorage.getMouseLocation()[1];
        logger.debug("检查鼠标边缘检测: 位置=({}, {})", x, y);
        // 计算鼠标速度
        long currentTime = System.currentTimeMillis();
        double velocity = calculateVelocity(x, y, currentTime);

        // 获取动态阈值
        double threshold = calculateDynamicThreshold(velocity);

        logger.debug("鼠标速度: {}, 动态阈值: {}", velocity, threshold);

        // 检查所有屏幕
        Map<String, ScreenInfo> screens = virtualDesktopStorage.getScreens();

        // 首先检查鼠标是否在某个屏幕内
        ScreenInfo currentScreen = null;
        for (ScreenInfo screen : screens.values()) {
            if (screen.virtualContains(x, y)) {
                currentScreen = screen;
                logger.debug("鼠标在屏幕内: {}:{}", screen.getDeviceIp(), screen.getScreenName());
                break;
            }
        }

        // 如果鼠标不在任何屏幕内，则不触发边缘检测
        if (currentScreen == null) {
            logger.debug("鼠标不在任何屏幕内，不触发边缘检测");
            // 更新最后位置和时间
            lastX = x;
            lastY = y;
            lastTime = currentTime;
            lastVelocity = velocity;
            return null;
        }

        // 检查相邻屏幕边缘
        ScreenInfo targetScreen = null;
        String targetScreenId = null;
        EdgeDirection targetDirection = null;

        for (Map.Entry<String, ScreenInfo> entry : screens.entrySet()) {
            ScreenInfo screen = entry.getValue();

            // 跳过当前鼠标所在的屏幕
            if (screen == currentScreen) {
                continue;
            }

            // 检查是否为相邻屏幕
            if (isAdjacentScreen(currentScreen, screen)) {
                logger.debug("发现相邻屏幕: {}:{}", screen.getDeviceIp(), screen.getScreenName());

                // 检查鼠标是否在当前屏幕的边缘，并且接近相邻屏幕
                EdgeDirection direction = getEdgeDirection(x, y, currentScreen, threshold);

                if (direction != EdgeDirection.NONE) {
                    // 检查是否接近相邻屏幕
                    if (isCloseToAdjacentScreen(x, y, currentScreen, screen, direction, threshold)) {
                        targetScreen = screen;
                        targetScreenId = entry.getKey();
                        targetDirection = direction;
                        break;
                    }
                }
            }
        }

        // 如果找到目标屏幕，则检查是否满足触发条件
        if (targetScreen != null) {

            logger.debug("屏幕targetScreen {}----屏幕Screen{}", targetScreen.getDeviceIp(), currentScreen.getDeviceIp());
            // 更新边缘状态
            updateEdgeState(targetScreenId, targetDirection, currentTime);

            // 检查是否满足触发条件（防误触）
            boolean shouldTrigger = shouldTriggerEdgeTransition(targetScreenId, currentTime);
            logger.debug("相邻屏幕 {}:{} 是否满足触发条件: {}", targetScreen.getDeviceIp(), targetScreen.getScreenName(), shouldTrigger);

            if (shouldTrigger) {
                logger.debug("当前鼠标位置:[{},{}],鼠标方向:{}-----触发边缘检测，将唤醒设备: {} 屏幕: {}", x, y, targetDirection, targetScreen.getDeviceIp(), targetScreen.getScreenName());
                // 更新最后位置和时间
                lastX = x;
                lastY = y;
                lastTime = currentTime;
                lastVelocity = velocity;
                return new MoveTargetScreenInfo(targetDirection.name(),targetScreen); // 返回将被唤醒鼠标的屏幕信息
            }
        }

        // 更新最后位置和时间
        lastX = x;
        lastY = y;
        lastTime = currentTime;
        lastVelocity = velocity;

        logger.debug("未触发任何屏幕的边缘检测");
        return null;
    }

    /**
     * 计算鼠标移动速度
     *
     * @param x           当前X坐标
     * @param y           当前Y坐标
     * @param currentTime 当前时间
     * @return 鼠标移动速度（像素/毫秒）
     */
    private static double calculateVelocity(int x, int y, long currentTime) {
        if (lastTime == 0) {
            return 0; // 第一次调用，无法计算速度
        }

        long timeDelta = currentTime - lastTime;
        if (timeDelta < VELOCITY_CALCULATION_INTERVAL) {
            return lastVelocity; // 时间间隔太短，使用上次速度
        }

        double distance = Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2));
        return distance / timeDelta;
    }

    /**
     * 计算动态阈值
     *
     * @param velocity 鼠标移动速度
     * @return 动态阈值
     */
    private static double calculateDynamicThreshold(double velocity) {
        // 基础阈值 + 速度相关阈值
        double threshold = Math.max(BASE_THRESHOLD, BASE_THRESHOLD + velocity * VELOCITY_FACTOR);
        logger.debug("计算动态阈值: 基础阈值=" + BASE_THRESHOLD + ", 速度={}, 速度因子=" + VELOCITY_FACTOR + ", 结果={}", velocity, threshold);
        return threshold;
    }

    /**
     * 检查两个屏幕是否相邻
     *
     * @param screen1 屏幕1
     * @param screen2 屏幕2
     * @return 如果相邻返回true，否则返回false
     */
    private static boolean isAdjacentScreen(ScreenInfo screen1, ScreenInfo screen2) {
        double left1 = screen1.getVx();
        double right1 = screen1.getVx() + screen1.getWidth();
        double top1 = screen1.getVy();
        double bottom1 = screen1.getVy() + screen1.getHeight();

        double left2 = screen2.getVx();
        double right2 = screen2.getVx() + screen2.getWidth();
        double top2 = screen2.getVy();
        double bottom2 = screen2.getVy() + screen2.getHeight();

        // 检查是否水平相邻（上下边缘对齐）
        boolean horizontalAdjacent =
                (Math.abs(right1 - left2) <= 1 || Math.abs(right2 - left1) <= 1) &&  // 共享垂直边缘
                        (Math.max(top1, top2) < Math.min(bottom1, bottom2)); // Y轴有重叠

        // 检查是否垂直相邻（左右边缘对齐）
        boolean verticalAdjacent =
                (Math.abs(bottom1 - top2) <= 1 || Math.abs(bottom2 - top1) <= 1) &&  // 共享水平边缘
                        (Math.max(left1, left2) < Math.min(right1, right2)); // X轴有重叠

        return horizontalAdjacent || verticalAdjacent;
    }

    /**
     * 获取鼠标在屏幕边缘的方向
     *
     * @param x         鼠标X坐标
     * @param y         鼠标Y坐标
     * @param screen    屏幕信息
     * @param threshold 阈值
     * @return 边缘方向
     */
    private static EdgeDirection getEdgeDirection(double x, double y, ScreenInfo screen, double threshold) {
        double left = screen.getVx();
        double right = screen.getVx() + screen.getWidth();
        double top = screen.getVy();
        double bottom = screen.getVy() + screen.getHeight();

        // 检查具体在哪个边缘
        if (x <= left + threshold) {
            logger.debug("左---边缘方向: x={}, y={}, 屏幕边界: left={}, right={}, top={}, bottom={}, 阈值={}", x, y, left, right, top, bottom, threshold);
            return EdgeDirection.LEFT;
        } else if (x >= right - threshold) {
            logger.debug("右---边缘方向: x={}, y={}, 屏幕边界: left={}, right={}, top={}, bottom={}, 阈值={}", x, y, left, right, top, bottom, threshold);
            return EdgeDirection.RIGHT;
        } else if (y <= top + threshold) {
            logger.debug("上---边缘方向: x={}, y={}, 屏幕边界: left={}, right={}, top={}, bottom={}, 阈值={}", x, y, left, right, top, bottom, threshold);
            return EdgeDirection.TOP;
        } else if (y >= bottom - threshold) {
            logger.debug("下---边缘方向: x={}, y={}, 屏幕边界: left={}, right={}, top={}, bottom={}, 阈值={}", x, y, left, right, top, bottom, threshold);
            return EdgeDirection.BOTTOM;
        }

        logger.debug("不在任何边缘");
        return EdgeDirection.NONE;
    }

    /**
     * 检查鼠标是否接近相邻屏幕
     *
     * @param x              鼠标X坐标
     * @param y              鼠标Y坐标
     * @param currentScreen  当前屏幕
     * @param adjacentScreen 相邻屏幕
     * @param direction      边缘方向
     * @param threshold      阈值
     * @return 如果接近相邻屏幕返回true，否则返回false
     */
    private static boolean isCloseToAdjacentScreen(double x, double y, ScreenInfo currentScreen,
                                                   ScreenInfo adjacentScreen, EdgeDirection direction,
                                                   double threshold) {
        double currentLeft = currentScreen.getVx();
        double currentRight = currentScreen.getVx() + currentScreen.getWidth();
        double currentTop = currentScreen.getVy();
        double currentBottom = currentScreen.getVy() + currentScreen.getHeight();

        double adjacentLeft = adjacentScreen.getVx();
        double adjacentRight = adjacentScreen.getVx() + adjacentScreen.getWidth();
        double adjacentTop = adjacentScreen.getVy();
        double adjacentBottom = adjacentScreen.getVy() + adjacentScreen.getHeight();

        logger.debug("检查是否接近相邻屏幕: 方向={}；\n当前屏幕边界: left={}, right={}, top={}, bottom={}；\n相邻屏幕边界: left={}, right={}, top={}, bottom={}", direction, currentLeft, currentRight, currentTop, currentBottom, adjacentLeft, adjacentRight, adjacentTop, adjacentBottom);

        boolean isClose = false;

        switch (direction) {
            case LEFT:
                // 鼠标在当前屏幕左边缘，检查是否接近左侧相邻屏幕
                // 鼠标应该在当前屏幕左侧，并且在相邻屏幕右侧边缘附近
                isClose = (x < currentLeft + threshold) &&
                        (Math.abs(x - adjacentRight) < threshold) &&
                        (y >= Math.max(currentTop, adjacentTop)) &&
                        (y <= Math.min(currentBottom, adjacentBottom));
                break;

            case RIGHT:
                // 鼠标在当前屏幕右边缘，检查是否接近右侧相邻屏幕
                // 鼠标应该在当前屏幕右侧，并且在相邻屏幕左侧边缘附近
                isClose = (x > currentRight - threshold) &&
                        (Math.abs(x - adjacentLeft) < threshold) &&
                        (y >= Math.max(currentTop, adjacentTop)) &&
                        (y <= Math.min(currentBottom, adjacentBottom));
                break;

            case TOP:
                // 鼠标在当前屏幕上边缘，检查是否接近上侧相邻屏幕
                // 鼠标应该在当前屏幕上侧，并且在相邻屏幕下侧边缘附近
                isClose = (y < currentTop + threshold) &&
                        (Math.abs(y - adjacentBottom) < threshold) &&
                        (x >= Math.max(currentLeft, adjacentLeft)) &&
                        (x <= Math.min(currentRight, adjacentRight));
                break;

            case BOTTOM:
                // 鼠标在当前屏幕下边缘，检查是否接近下侧相邻屏幕
                // 鼠标应该在当前屏幕下侧，并且在相邻屏幕上侧边缘附近
                isClose = (y > currentBottom - threshold) &&
                        (Math.abs(y - adjacentTop) < threshold) &&
                        (x >= Math.max(currentLeft, adjacentLeft)) &&
                        (x <= Math.min(currentRight, adjacentRight));
                break;
        }

        logger.debug("是否接近相邻屏幕: {}", isClose);

        if (!isClose) {
            return false;
        }

        // 检查是否在缓冲区（忽略中间区域）
        double bufferLeft = adjacentLeft + IGNORE_BUFFER_ZONE;
        double bufferRight = adjacentRight - IGNORE_BUFFER_ZONE;
        double bufferTop = adjacentTop + IGNORE_BUFFER_ZONE;
        double bufferBottom = adjacentBottom - IGNORE_BUFFER_ZONE;

        boolean inBufferZone = false;
        switch (direction) {
            case LEFT:
            case RIGHT:
                // 水平方向检查Y轴是否在缓冲区
                inBufferZone = (y >= bufferTop && y <= bufferBottom);
                break;

            case TOP:
            case BOTTOM:
                // 垂直方向检查X轴是否在缓冲区
                inBufferZone = (x >= bufferLeft && x <= bufferRight);
                break;
        }

        logger.debug("是否在缓冲区: {}, 缓冲区边界: left={}, right={}, top={}, bottom={}", inBufferZone, bufferLeft, bufferRight, bufferTop, bufferBottom);

        if (inBufferZone) {
            logger.debug("在缓冲区内，触发边缘检测");
            return true; // 在缓冲区内，不触发边缘检测
        }

        logger.debug("在相邻屏幕边缘且不在缓冲区，不触发边缘检测");
        return false;
    }

    /**
     * 更新边缘状态
     *
     * @param screenId    屏幕ID
     * @param direction   边缘方向
     * @param currentTime 当前时间
     */
    private static void updateEdgeState(String screenId, EdgeDirection direction, long currentTime) {
        logger.debug("更新边缘状态: screenId={}, direction={}", screenId, direction);

        // 检查是否是同一个屏幕和方向，如果是，则保持原来的entryTime
        EdgeState existingState = screenEdgeStates.get(screenId);
        long entryTime = currentTime;

        if (existingState != null &&
                existingState.isAtEdge &&
                existingState.direction == direction &&
                currentEdgeScreenId != null &&
                currentEdgeScreenId.equals(screenId)) {
            // 如果是同一个屏幕且方向相同，保持原来的entryTime
            entryTime = existingState.entryTime;
            logger.debug("保持原有进入时间: {}", entryTime);
        } else {
            // 否则使用当前时间作为新的entryTime
            entryTime = currentTime;
            logger.debug("设置新的进入时间: {}", entryTime);
        }

        // 更新当前边缘信息
        lastEdgeScreenId = currentEdgeScreenId;
        currentEdgeScreenId = screenId;
        lastEdgeDirection = currentEdgeDirection;
        currentEdgeDirection = direction;

        // 更新屏幕边缘状态
        screenEdgeStates.put(screenId, new EdgeState(true, entryTime, direction));
    }

    /**
     * 检查是否应该触发边缘过渡
     *
     * @param screenId    屏幕ID
     * @param currentTime 当前时间
     * @return 如果应该触发返回true，否则返回false
     */
    private static boolean shouldTriggerEdgeTransition(String screenId, long currentTime) {
        EdgeState edgeState = screenEdgeStates.get(screenId);
        if (edgeState == null || !edgeState.isAtEdge) {
            logger.debug("边缘状态为空或不在边缘");
            return false;
        }

        // 检查是否满足防误触时间
        long timeAtEdge = currentTime - edgeState.entryTime;
        boolean shouldTrigger = timeAtEdge >= ANTI_FALSE_TRIGGER_TIME;

        logger.debug("在边缘时间: {}ms, 防误触时间: " + ANTI_FALSE_TRIGGER_TIME + "ms, 是否触发: {}", timeAtEdge, shouldTrigger);

        // 如果不满足触发条件，但已经持续在边缘状态很长时间，也触发
        if (!shouldTrigger && timeAtEdge > ANTI_FALSE_TRIGGER_TIME * 3) {
            logger.debug("在边缘状态时间过长，强制触发");
            // 重置状态以避免重复触发
            screenEdgeStates.put(screenId, new EdgeState(false, 0, EdgeDirection.NONE));
            return true;
        }

        return shouldTrigger;
    }

    /**
     * 重置边缘检测状态
     */
    public static void reset() {
        logger.debug("重置边缘检测状态");
        lastX = 0;
        lastY = 0;
        lastTime = 0;
        lastVelocity = 0;
        edgeEntryTime = 0;
        lastEdgeScreenId = null;
        currentEdgeScreenId = null;
        lastEdgeDirection = null;
        currentEdgeDirection = null;
        screenEdgeStates.clear();
    }
}