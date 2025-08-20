package com.keymouseshare.network;

import com.google.gson.Gson;
import com.keymouseshare.bean.ControlEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 控制消息解码器
 */
public class ControlMessageDecoder extends ByteToMessageDecoder {
    private final Gson gson = new Gson();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 确保有足够的数据可读
        if (in.readableBytes() < 4) {
            return;
        }

        // 标记当前读位置
        in.markReaderIndex();
        
        // 读取数据长度
        int dataLength = in.readInt();
        
        // 检查是否有足够的数据
        if (in.readableBytes() < dataLength) {
            // 重置读位置
            in.resetReaderIndex();
            return;
        }

        // 读取数据
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        
        // 将数据转换为ControlEvent对象
        String json = new String(data, StandardCharsets.UTF_8);
        ControlEvent event = gson.fromJson(json, ControlEvent.class);
        out.add(event);
    }
}