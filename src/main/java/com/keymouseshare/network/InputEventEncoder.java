package com.keymouseshare.network;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.keymouseshare.input.InputEvent;

/**
 * 输入事件编码器，将InputEvent对象编码为字节流用于网络传输
 */
public class InputEventEncoder extends MessageToByteEncoder<InputEvent> {
    private static final Gson gson = new Gson();
    
    @Override
    protected void encode(ChannelHandlerContext ctx, InputEvent event, ByteBuf out) throws Exception {
        // 将事件对象转换为JSON字符串
        String json = gson.toJson(event);
        
        // 获取JSON字符串的字节数组
        byte[] bytes = json.getBytes("UTF-8");
        
        // 先写入长度字段
        out.writeInt(bytes.length);
        
        // 再写入数据
        out.writeBytes(bytes);
    }
}