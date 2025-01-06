package groupservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description TODO: 监听并处理心跳连接请求的线程
 * @Author Junwei Hu
 * @Date 2024/12/12 16:23
 * @Version 1.0
 */
public class HeartbeatListenThread extends Thread {
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param daemon Daemon实例
     * @Author Junwei Hu
     * @Date 2024/12/12 16:37
     * @Version 1.0
     */
    public HeartbeatListenThread(Daemon daemon) {
        this.daemon = daemon;
    }

    @Override
    public void run() {
        // 创建心跳ServerSocket实例
        ServerSocket heartbeatServerSocket = null;
        try {
            heartbeatServerSocket = new ServerSocket(daemon.getDaemonPort());
            while (daemon.isRunning) {
                // 创建接收心跳连接请求的Socket
                Socket heartbeatSocket = heartbeatServerSocket.accept();
                if (daemon.isRunning) {
                    // 启动心跳连接处理线程
                    new HeartbeatHandlerThread(heartbeatSocket, daemon).start();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}