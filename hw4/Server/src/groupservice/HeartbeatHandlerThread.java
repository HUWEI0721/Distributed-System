package groupservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * @Description TODO: 处理心跳的线程，接到心跳后，如果来源并不是自己的neighbor就将新的来源设为自己的neighbor，更改拓扑结构
 * @Author Junwei Hu
 * @Date 2024/12/11 14:37
 * @Version 1.0
 */
public class HeartbeatHandlerThread extends Thread {
    private Socket socket;
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param socket
     * @param daemon
     * @Author Junwei Hu
     * @Date 2024/12/12 16:41
     * @Version 1.0
     */
    public HeartbeatHandlerThread(Socket socket, Daemon daemon) {
        this.socket = socket;
        this.daemon = daemon;
    }

    /**
     * @Description TODO: 更新拓扑结构
     * @param receivedMember 接收到的Member信息
     * @Author Junwei Hu
     * @Date 2024/12/12 16:43
     * @Version 1.0
     */
    private void updateTopology(HeartbeatProto.Member receivedMember) {
        if (daemon.getNeighbors().size() <= 1) {
            daemon.getNeighbors().add(receivedMember.getName());
            System.out.println("[ReceiveHeartbeat]: Neighbors now: " + daemon.getNeighbors());
            return;
        }

        int daemonId = Integer.parseInt(daemon.getDaemonName());
        int neighbor1Id = Integer.parseInt(daemon.getNeighbors().get(0));
        int neighbor2Id = Integer.parseInt(daemon.getNeighbors().get(1));
        int leftNeighbor, rightNeighbor;

        if (((daemonId > neighbor1Id) && (daemonId > neighbor2Id)) ||
                ((daemonId < neighbor1Id) && (daemonId < neighbor2Id))) {
            if (neighbor1Id < neighbor2Id) {
                leftNeighbor = neighbor2Id;
                rightNeighbor = neighbor1Id;
            } else {
                leftNeighbor = neighbor1Id;
                rightNeighbor = neighbor2Id;
            }
        } else {
            if (neighbor1Id < neighbor2Id) {
                leftNeighbor = neighbor1Id;
                rightNeighbor = neighbor2Id;
            } else {
                leftNeighbor = neighbor2Id;
                rightNeighbor = neighbor1Id;
            }
        }

        int newNeighborId = Integer.parseInt(receivedMember.getName());

        if (leftNeighbor < newNeighborId && newNeighborId < daemonId) {
            daemon.getNeighbors().remove(Integer.toString(leftNeighbor));
            daemon.getLastHeartbeatMap().remove(Integer.toString(leftNeighbor));
            daemon.getNeighbors().add(Integer.toString(newNeighborId));
            System.out.println("[ReceiveHeartbeat]: Neighbors now: " + daemon.getNeighbors());
        } else if (daemonId < newNeighborId && newNeighborId < rightNeighbor) {
            daemon.getNeighbors().remove(Integer.toString(rightNeighbor));
            daemon.getLastHeartbeatMap().remove(Integer.toString(rightNeighbor));
            daemon.getNeighbors().add(Integer.toString(newNeighborId));
            System.out.println("[ReceiveHeartbeat]: Neighbors now: " + daemon.getNeighbors());
        } else if (rightNeighbor > daemonId) {
            daemon.getNeighbors().remove(Integer.toString(leftNeighbor));
            daemon.getLastHeartbeatMap().remove(Integer.toString(leftNeighbor));
            daemon.getNeighbors().add(Integer.toString(newNeighborId));
            System.out.println("[ReceiveHeartbeat]: Neighbors now: " + daemon.getNeighbors());
        } else if (daemonId > leftNeighbor) {
            daemon.getNeighbors().remove(Integer.toString(rightNeighbor));
            daemon.getLastHeartbeatMap().remove(Integer.toString(rightNeighbor));
            daemon.getNeighbors().add(Integer.toString(newNeighborId));
            System.out.println("[ReceiveHeartbeat]: Neighbors now: " + daemon.getNeighbors());
        } else {
            System.out.println("[ReceiveHeartbeat]: Neighbors Not Changed!");
        }
    }

    @Override
    public void run() {
        try {
            // 获取输入流
            InputStream inputStream = socket.getInputStream();

            // 读取消息
            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            byte[] receivedData = Arrays.copyOfRange(buffer, 0, length);

            HeartbeatProto.Member receivedMember = HeartbeatProto.Member.parseFrom(receivedData);

            // 当前心跳消息对应的name不存在于Neighbors中
            if (!daemon.getNeighbors().contains(receivedMember.getName())) {
                updateTopology(receivedMember);
            }

            // 收到心跳消息,更新心跳时间映射
            daemon.getLastHeartbeatMap().put(receivedMember.getName(), System.currentTimeMillis());
            System.out.println("[ReceiveHeartbeat]: " + receivedMember.getName() + " 心跳时间更新");

            inputStream.close();
            socket.close();
            // 开启一次写心跳日志的线程
            new logWriteThread(
                    daemon.getDaemonPort(),
                    "heartbeat",
                    System.currentTimeMillis(),
                    receivedMember.getName(),
                    receivedMember.getIp(),
                    receivedMember.getPort(),
                    false,
                    daemon.memberList
            ).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}