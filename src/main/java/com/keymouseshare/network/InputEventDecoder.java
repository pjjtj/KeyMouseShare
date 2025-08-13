package com.keymouseshare.network;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import com.keymouseshare.input.*;

import java.util.List;

/**
 * 输入事件解码器，将网络接收的字节流解码为InputEvent对象
 */
public class InputEventDecoder extends ByteToMessageDecoder {
    private static final Gson gson = new Gson();
    private static final int LENGTH_FIELD_LENGTH = 4; // 长度字段的字节数
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的数据读取长度字段
        if (in.readableBytes() < LENGTH_FIELD_LENGTH) {
            return;
        }
        
        // 标记当前读位置，以便后续重置
        in.markReaderIndex();
        
        // 读取数据长度
        int dataLength = in.readInt();
        
        // 检查是否有足够的数据读取完整的消息
        if (in.readableBytes() < dataLength) {
            // 数据不足，重置读位置，等待更多数据
            in.resetReaderIndex();
            return;
        }
        
        // 读取数据
        byte[] bytes = new byte[dataLength];
        in.readBytes(bytes);
        
        // 将JSON字符串转换为对象
        String json = new String(bytes, "UTF-8");
        
        // 解析JSON为InputEvent对象
        InputEvent event = gson.fromJson(json, InputEvent.class);
        
        // 根据事件类型转换为具体的事件对象
        InputEvent concreteEvent = convertToConcreteEvent(event, json);
        
        // 添加到输出列表
        out.add(concreteEvent);
    }
    
    /**
     * 将通用InputEvent转换为具体类型的事件对象
     * @param event 通用事件对象
     * @param json JSON字符串
     * @return 具体类型的事件对象
     */
    private InputEvent convertToConcreteEvent(InputEvent event, String json) {
        switch (event.getType()) {
            case MOUSE_MOVE:
            case MOUSE_PRESS:
            case MOUSE_RELEASE:
            case MOUSE_WHEEL:
                return gson.fromJson(json, MouseEvent.class);
            case KEY_PRESS:
            case KEY_RELEASE:
                return gson.fromJson(json, KeyEvent.class);
            case FILE_DRAG_START:
            case FILE_DRAG_END:
            case FILE_TRANSFER:
                return gson.fromJson(json, FileDragEvent.class);
            default:
                return event;
        }
    }
}