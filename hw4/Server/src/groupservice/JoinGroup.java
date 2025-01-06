package groupservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * @Description TODO: 新节点向introducer发送加入申请，接收introducer发过来的memberList
 * @Author Junwei Hu
 * @Date 2024/12/10 14:37
 * @Version 1.0
 */
public class JoinGroup extends Thread {
    private final String introducerName = "0";
    private final String introducerIp = "124.220.24.233";
    private final int introducerPort = 9220;
    private Daemon daemon;

    /**
     * @Description TODO: 构造函数
     * @param daemon Daemon实例
     * @Author Junwei Hu
     * @Date 2024/12/11 16:47
     * @Version 1.0
     */
    public JoinGroup(Daemon daemon) {
        this.daemon = daemon;
    }

    @Override
    public void run() {
        // 创建Socket连接
        Socket socket = null;
        try {
            socket = new Socket(introducerIp, introducerPort);
            OutputStream outputStream = socket.getOutputStream();
            // 将本机信息封装为protobuf
            HeartbeatProto.Member sentMessage = HeartbeatProto.Member.newBuilder()
                    .setIp(daemon.getDaemonAddress())
                    .setPort(daemon.getDaemonPort())
                    .setName(daemon.getDaemonName())
                    .setSendingTimestamp(System.currentTimeMillis())
                    .build();
            byte[] data = sentMessage.toByteArray();
            // 将protobuf信息发送给introducer
            outputStream.write(data);
            outputStream.flush();
            System.out.println("[SendJoin]: 将本机proto信息发送给Introducer");

            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            byte[] receivedData = Arrays.copyOfRange(buffer, 0, length);
            GossipProto.MemberList receivedMemberList = GossipProto.MemberList.parseFrom(receivedData);

            System.out.println("[SendJoin]: 接收到Introducer发来的MemberList");
            // 将接收到的MemberList信息加入到本机memberList
            for (int i = 0; i < receivedMemberList.getMemberListList().size(); i++) {
                String receivedName = receivedMemberList.getMemberList(i).getName();
                String receivedAddress = receivedMemberList.getMemberList(i).getIp();
                int receivedPort = receivedMemberList.getMemberList(i).getPort();
                long receivedTimestamp = receivedMemberList.getMemberList(i).getTimestamp();
                Member receivedMember = new Member(receivedName, receivedAddress, receivedPort, receivedTimestamp);
                daemon.memberList.add(receivedMember);
                daemon.memberList.sort(null);
            }
            // 输出当前MemberList信息
            System.out.println("[SendJoin]: 当前MemberList成员: ");
            for (int j = 0; j < daemon.memberList.size(); j++) {
                String memberInfo = daemon.memberList.get(j).getName() + " " +
                        daemon.memberList.get(j).getAddress() + " " +
                        daemon.memberList.get(j).getPort() + " " +
                        daemon.memberList.get(j).getTimeStamp();
                System.out.println(memberInfo);
            }
            // 找到neighbors
            System.out.println("[SendJoin]: 当前Neighbors:");
            daemon.findNeighbors();
            for (String neighbor : daemon.getNeighbors()) {
                System.out.println(neighbor);
            }
            outputStream.close();
            inputStream.close();
            socket.close();
            System.out.println("[SendJoin]: 成功加入组成员服务");
            // 开启一次写Join日志的线程
            new logWriteThread(
                    daemon.getDaemonPort(),
                    "join",
                    System.currentTimeMillis(),
                    introducerName,
                    introducerIp,
                    introducerPort,
                    true,
                    daemon.memberList
            ).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}