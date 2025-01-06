import groupservice.Daemon;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 虚拟服务器类，负责处理查询请求并与组服务交互
 * @author Junwei Hu
 * @date 2024.12.13 17:05 PM
 * @version 1.0
 */
public class VirtualServer {
    private int port;
    public Daemon groupDaemon;
    private final int xmlProperNum = 4; // 每台服务器应该存放的XML文件数量

    /**
     * @description: 构造函数，初始化虚拟服务器
     * @param name 虚拟机名称
     * @param portID 端口ID
     * @param is_introducer 是否为 introducer 节点
     * @param deathTime 死亡时间
     */
    public VirtualServer(String name, int portID, boolean is_introducer, int deathTime) {
        this.port = portID;
        // 初始化组服务
        groupDaemon = new Daemon(name, port + 100, is_introducer, deathTime);
    }

    /**
     * @description: 接收并处理查询请求
     */
    public void receiveQuery() {
        try {
            ServerSocket server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                DataInputStream is = new DataInputStream(socket.getInputStream());
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                // 接收来自客户端的type信息
                String type = is.readUTF();

                // 创建 Query 实例
                Query query = new Query(port);

                // 确认接收到来自客户端的信息
                if (type.length() > 0) {
                    if (type.equals("time")) {
                        // 发送 joinTime
                        String joinTime = query.getJoinTime();
                        System.out.println("[Query] :" + type + " joinTime " + joinTime);
                        os.writeUTF(joinTime);
                        os.flush();

                        // 发送 crashTime
                        String crashTime = query.getCrashTime();
                        System.out.println("[Query] :" + type + " crashTime " + crashTime);
                        os.writeUTF(crashTime);
                        os.flush();
                    } else if (type.equals("rate")) {
                        List<Long> changedTimestamps = new ArrayList<>();
                        // 获取所有 changedTimestamp
                        query.getChangedTimestamps(changedTimestamps);

                        // 发送改变次数
                        os.writeUTF(Integer.toString(changedTimestamps.size()));
                        os.flush();
                        System.out.println("[Query Rate] : changeNum 改变次数 " + changedTimestamps.size());

                        // 循环发送 changedTimestamp
                        for (Long timestamp : changedTimestamps) {
                            os.writeUTF(Long.toString(timestamp));
                            os.flush();
                        }
                    } else {
                        // 向客户端发送查询结果信息
                        String queryResult = query.queryByType(type);
                        System.out.println("[Query] :" + type + " Result " + queryResult);
                        os.writeUTF(queryResult);
                        os.flush();
                    }
                }

                // 关闭 Socket 链接
                is.close();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}