package groupservice;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * @Description TODO: 向邻居节点发送心跳
 * @Author Junwei Hu
 * @Date 2024/12/11 14:27
 * @Version 1.0
 */
public class HeartbeatThread extends Thread {
    // 后台进程本身
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param daemon Daemon实例
     * @Author Junwei Hu
     * @Date 2024/12/12 16:41
     * @Version 1.0
     */
    public HeartbeatThread(Daemon daemon) {
        this.daemon = daemon;
    }

    /**
     * @Description TODO: 启动发送心跳线程
     * @Author Junwei Hu
     * @Date 2024/12/13 14:19
     * @Version 1.0
     */
    @Override
    public void run() {
        // 连接到目标主机
        try {
            while (daemon.isRunning) {
                for (Member member : daemon.memberList) {
                    if (daemon.getNeighbors().contains(member.getName())) {
                        if (daemon.isRunning) {
                            new TransportHeartbeat(member.getAddress(), member.getPort(), daemon).start();
                        }
                    }
                }
                // 等待一段时间
                Thread.sleep(Daemon.HEARTBEAT_INTERVAL);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/**
 * @Description TODO: 发送心跳的线程，每确定一个接收方，就启动一个此线程用来向其发送心跳，
 * 这样即使接收方已离线，也不会导致上面的主线程异常
 * @Author Junwei Hu
 * @Date 2024/12/12 16:43
 * @Version 1.0
 */
class TransportHeartbeat extends Thread {

    private String receiverIp;
    private int receiverPort;
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param ip Daemon的IP地址
     * @param port Daemon的端口号
     * @param daemon Daemon实例
     * @Author Junwei Hu
     * @Date 2024/12/12 16:43
     * @Version 1.0
     */
    public TransportHeartbeat(String ip, int port, Daemon daemon) {
        this.receiverIp = ip;
        this.receiverPort = port;
        this.daemon = daemon;
    }

    /**
     * @Description TODO: 发送心跳信息给指定的Daemon
     * @Author Junwei Hu
     * @Date 2024/12/13 14:19
     * @Version 1.0
     */
    @Override
    public void run() {
        try {
            System.out.println("[SendHeartbeat]: 向 " + receiverIp + ": " + receiverPort + " 节点发送心跳");
            Socket socket = new Socket(receiverIp, receiverPort);
            OutputStream os = socket.getOutputStream();
            // 向Server传递心跳信息
            // 封装待发送信息：与平台无关的protobuf格式
            HeartbeatProto.Member sentMessage = HeartbeatProto.Member.newBuilder()
                    .setIp(daemon.getDaemonAddress())
                    .setPort(daemon.getDaemonPort())
                    .setName(daemon.getDaemonName())
                    .setSendingTimestamp(System.currentTimeMillis())
                    .build();
            byte[] data = sentMessage.toByteArray();

            // 发送protobuf
            os.write(data);
            os.flush();
            os.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}