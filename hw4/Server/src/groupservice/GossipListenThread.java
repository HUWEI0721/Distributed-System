package groupservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GossipListenThread extends Thread {
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param daemon
     * @Author Junwei Hu
     * @Date 2024/12/11 14:37
     * @Version 1.0
     **/
    public GossipListenThread(Daemon daemon) {
        this.daemon = daemon;
    }

    @Override
    public void run() {
        // 创建Gossip ServerSocket实例
        ServerSocket gossipServerSocket = null;
        try {
            gossipServerSocket = new ServerSocket(daemon.getPortGossip());
            while (daemon.isRunning) {
                // 创建接收Gossip连接请求的Socket
                Socket socketGossip = gossipServerSocket.accept();
                if (daemon.isRunning) {
                    // 启动Gossip连接处理线程
                    new GossipHandlerThread(socketGossip, daemon).start();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}