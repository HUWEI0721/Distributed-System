package groupservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * @Description TODO: 每有一个新节点请求加入时，就开启一个此线程给新节点发memberList的内容
 * @Author Junwei Hu
 * @Date 2024/12/11 16:37
 * @Version 1.0
 */

public class JoinRequestHandler extends Thread{
    private Daemon daemon;
    private Socket socket;
    /**
     * @Description TODO: 构造函数
     * @param socket 新节点的Socket连接
     * @param daemonTmp Daemon实例
     * @Author Junwei Hu
     * @Date 2024/12/12 16:47
     * @Version 1.0
     */
    public JoinRequestHandler(Socket socket, Daemon daemonTmp){
        this.daemon=daemonTmp;
        this.socket=socket;
    }
    public void run(){
        // 获取输入流
        try {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            // 接收新节点发送的protobuf信息
            System.out.println("[RecieveJoin]:接收到新成员信息");
            // 读取服务端发送的 Protobuf 消息字节数组
            byte[] buf = new byte[1024];
            int len = inputStream.read(buf);
            byte[] receivedData = Arrays.copyOfRange(buf, 0, len);
            //System.out.println("此member字节大小："+len);
            // 将字节数组反序列化为 Protobuf 消息对象
            HeartbeatProto.Member receivedMember = HeartbeatProto.Member.parseFrom(receivedData);
            System.out.println("[RecieveJoin]:"+receivedMember.getName()+receivedMember.getIp()+(receivedMember.getPort()+""));
            boolean existed=false;
            // 处理已存在memberList情况
            for(Member member:daemon.memberList){
                if(member.getName().equals(receivedMember.getName())){
                    if(receivedMember.getSendingTimestamp()>member.getTimeStamp()){
                        member.setTimeStamp(receivedMember.getSendingTimestamp());
                    }
                    existed=true;
                }
            }
            // 当前memberlist中不存在该节点
            if(existed==false){
                Member joinMember=new Member(receivedMember.getName(),receivedMember.getIp(),receivedMember.getPort(),receivedMember.getSendingTimestamp());
                daemon.memberList.add(joinMember);
                daemon.memberList.sort(null);
                // 开启一次写Introduce日志的线程:此处的对象是，加入memberList的member
                new logWriteThread(daemon.getDaemonPort(),"introduce",System.currentTimeMillis(),joinMember.getName(),joinMember.getAddress(),joinMember.getPort(),true,daemon.memberList).start();
            }
            // 将MemberList封装为protobuf形式
            GossipProto.MemberList.Builder memListBuilder= GossipProto.MemberList.newBuilder();
            for (Member member:daemon.memberList) {
                memListBuilder.addMemberList(GossipProto.Member.newBuilder()
                        .setIp(member.getAddress())
                        .setName(member.getName())
                        .setPort(member.getPort())
                        .setTimestamp(member.getTimeStamp())
                        .build());
            }
            GossipProto.MemberList memberList=memListBuilder.build();
            byte[] outData = memberList.toByteArray();
            outputStream.write(outData);
            System.out.println("[RecieveJoin]:已向新节点发送现有memberList");
            outputStream.flush();
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
