package org.yaccc.xRPC.transport.netty;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.yaccc.xRPC.transport.Server;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Created by zhaodong/yaccc(github.com/yaccc)|xRPC on 16/2/5
 */
public class NettyServer implements Server {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(NettyServer.class);
    private final SocketAddress socketAddress;
    //nio bio channel
    private final Class<? extends ServerChannel> channelType;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    @java.beans.ConstructorProperties({"socketAddress", "channelType", "bossGroup", "workerGroup"})
    NettyServer(SocketAddress socketAddress, Class<? extends ServerChannel> channelType, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.socketAddress = socketAddress;
        this.channelType =Preconditions.checkNotNull(channelType,"channel Type must not be null");
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
    }

    public static creater creater() {
        return new creater();
    }


    @Override
    public void start() throws Exception{
        ServerBootstrap sb=new ServerBootstrap().group(bossGroup,workerGroup)
                .channel(channelType)
                .option(ChannelOption.SO_BACKLOG,128)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast("nettyHander",new NettyServerHander());
                    }
                });
        ChannelFuture future = sb.bind(socketAddress);

        try {
            future.await();
            log.info("future await connection");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted waiting for bind");
            throw new RuntimeException("Interrupted waiting for bind");
        }
        if (!future.isSuccess()) {
            log.error("failed to bind");
            throw new IOException("Failed to bind", future.cause());
        }
        channel = future.channel();

    }

    @Override
    public void shutdown() {
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }

    }

    public static class creater {
        private SocketAddress socketAddress;
        private Class<? extends ServerChannel> channelType;
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;

        creater() {
        }

        public NettyServer.creater socketAddress(SocketAddress socketAddress) {
            this.socketAddress = socketAddress;
            return this;
        }

        public NettyServer.creater channelType( Class<? extends ServerChannel> channelType) {
            this.channelType = channelType;
            return this;
        }

        public NettyServer.creater bossGroup(EventLoopGroup bossGroup) {
            this.bossGroup = bossGroup;
            return this;
        }

        public NettyServer.creater workerGroup(EventLoopGroup workerGroup) {
            this.workerGroup = workerGroup;
            return this;
        }

        public NettyServer creat() {
            return new NettyServer(socketAddress, channelType, bossGroup, workerGroup);
        }

        public String toString() {
            return "org.yaccc.xRPC.transport.netty.NettyServer.creater(socketAddress=" + this.socketAddress + ", channelType=" + this.channelType + ", bossGroup=" + this.bossGroup + ", workerGroup=" + this.workerGroup + ")";
        }
    }
}
