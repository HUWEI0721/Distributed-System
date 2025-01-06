package groupservice;

/**
 * @description: 离线检查类，如果检查到有邻居节点离线，就更新自己的邻居列表
 * @author Junwei Hu
 * @date 2024.12.12 14:19 PM
 * @version 1.0
 */
public class OfflineCheckThread extends Thread {
    private Daemon daemon;

    public OfflineCheckThread(Daemon daemon) {
        this.daemon = daemon;
    }

    public void run() {
        while (daemon.isRunning) {
            try {
                // 获取当前时间
                long currentTime = System.currentTimeMillis();

                // 遍历组成员列表
                for (int i = 0; i < daemon.memberList.size(); i++) {
                    // 获取当前成员
                    Member member = daemon.memberList.get(i);

                    // 如果成员是邻居且存在于 LastHeartbeatMap 中
                    if (daemon.getNeighbors().contains(member.getName()) && daemon.getLastHeartbeatMap().get(member.getName()) != null) {
                        System.out.println("[OfflineCheck]: 正在检查邻居节点 " + member.getName() + " 是否离线");

                        // 获取节点的最后心跳时间
                        long lastHeartbeatTime = daemon.getLastHeartbeatMap().get(member.getName());

                        // 如果节点超过离线超时时间未发送心跳，判断为离线
                        if (currentTime - lastHeartbeatTime > daemon.OFFLINE_TIMEOUT) {
                            // 从组成员列表中移除节点
                            daemon.memberList.remove(i);
                            System.out.println("[OfflineCheck]: " + member.getName() + " 节点离线");

                            // 更新拓扑结构
                            daemon.findNeighbors();

                            // 移除心跳记录
                            daemon.getLastHeartbeatMap().remove(member.getName());

                            // 更新后的邻居列表
                            System.out.println("[OfflineCheck]: 邻居更新为: " + daemon.getNeighbors());

                            // 输出当前成员列表信息
                            System.out.println("[OfflineCheck]: 当前 MemberList 成员: ");
                            for (int j = 0; j < daemon.memberList.size(); j++) {
                                Member currentMember = daemon.memberList.get(j);
                                String members = currentMember.getName() + " " +
                                        currentMember.getAddress() + " " +
                                        currentMember.getPort() + " " +
                                        currentMember.getTimeStamp();
                                System.out.println(members);
                            }

                            // 创建一个离线日志记录线程
                            new logWriteThread(
                                    daemon.getDaemonPort(),
                                    "offline",
                                    System.currentTimeMillis(),
                                    member.getName(),
                                    member.getAddress(),
                                    member.getPort(),
                                    true,
                                    daemon.memberList
                            ).start();
                        }
                    }
                }
                // 等待指定的检查间隔时间
                Thread.sleep(daemon.OFFLINE_CHECK_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}