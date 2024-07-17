package com.vt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.redis.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleRedisServer {
    private final static int PORT = 6379;
    private final ConcurrentMap<String, String> dataStore = new ConcurrentHashMap<>();

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new RedisDecoder());
                            p.addLast(new RedisBulkStringAggregator());
                            p.addLast(new RedisArrayAggregator());
                            p.addLast(new RedisEncoder());
                            p.addLast(new RedisCommandHandler());
                        }
                    });

            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("해당 포트에서 레디스 서버가 시작됐습니다. 포트 번호 :  " + PORT);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class RedisCommandHandler extends SimpleChannelInboundHandler<RedisMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RedisMessage msg) throws Exception {
            if (msg instanceof ArrayRedisMessage) {
                List<RedisMessage> commands = ((ArrayRedisMessage) msg).children();
                String command = ((FullBulkStringRedisMessage) commands.get(0)).content().toString(io.netty.util.CharsetUtil.UTF_8).toUpperCase();

                switch (command) {
                    case "SET":
                        handleSetCommand(ctx, commands);
                        break;
                    case "GET":
                        handleGetCommand(ctx, commands);
                        break;
                    default:
                        ctx.writeAndFlush(new ErrorRedisMessage("ERR 잘못된 REDIS 명령어 현재 입력된 명령어 : '" + command + "'"));
                }
            }
        }

        private void handleSetCommand(ChannelHandlerContext ctx, List<RedisMessage> commands) {
            if (commands.size() != 3) { // SET {key} {value}
                ctx.writeAndFlush(new ErrorRedisMessage("ERR COMMAND_SET : 인자 개수가 잘못되었습니다."));
                return;
            }
            String key = ((FullBulkStringRedisMessage) commands.get(1)).content().toString(io.netty.util.CharsetUtil.UTF_8);
            String value = ((FullBulkStringRedisMessage) commands.get(2)).content().toString(io.netty.util.CharsetUtil.UTF_8);
            dataStore.put(key, value);
            ctx.writeAndFlush(new SimpleStringRedisMessage("OK"));
        }

        private void handleGetCommand(ChannelHandlerContext ctx, List<RedisMessage> commands) {
            if (commands.size() != 2) { // GET {key}
                ctx.writeAndFlush(new ErrorRedisMessage("ERR COMMAND_GET : 인자 개수가 잘못되었습니다."));
                return;
            }
            String key = ((FullBulkStringRedisMessage) commands.get(1)).content().toString(io.netty.util.CharsetUtil.UTF_8);
            String value = dataStore.get(key);
            if (value != null) {
                ctx.writeAndFlush(new FullBulkStringRedisMessage(io.netty.buffer.Unpooled.copiedBuffer(value, io.netty.util.CharsetUtil.UTF_8)));
            } else {
                ctx.writeAndFlush(FullBulkStringRedisMessage.NULL_INSTANCE);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new SimpleRedisServer().run();
    }
}