package com.keymouseshare.network;

import com.google.gson.Gson;
import com.keymouseshare.bean.ControlEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * 控制消息编码器
 */
public class ControlMessageEncoder extends MessageToByteEncoder<ControlEvent> {
    private final Gson gson = new Gson();

    @Override
    protected void encode(ChannelHandlerContext ctx, ControlEvent event, ByteBuf out) {
        // 将ControlEvent对象转换为JSON字符串
        String json = gson.toJson(event);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        
        // 写入数据长度和数据
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}