package groupservice;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.LinkedList;
import java.util.Collections;

public class Daemon {
    //定时关闭线程的标签
    public volatile boolean isRunning = true;

    private final static String[] ipList = new String[]
            { "124.220.24.233", "110.40.167.8", "1.116.120.226"};
    // 定义心跳消息
    public static String HEARTBEAT_MESSAGE = "I'm still alive";
    // 定义心跳频率（每隔1秒发送一次心跳）
    public static int HEARTBEAT_INTERVAL = 1000;
    // 定义Gossip频率（每隔2秒发送一次Gossip）
    public static int GOSSIP_INTERVAL = 2000;
    // 定义组成员列表
    public List<Member> memberList = Collections.synchronizedList(new LinkedList<>());
    // 定义离线检查频率（每隔5秒检查一次）
    static final int OFFLINE_CHECK_INTERVAL = 500;
    // 定义离线超时时间（如果某个节点超过30秒没有发送心跳消息，则认为该节点已经离线）
    static final int OFFLINE_TIMEOUT = 1800;

    // Gossip触发所需，上一次Gossip时的数据字节流
    byte[] gossipBackup = {};

    // 本机名称
    private String name;
    // 本机公网IP
    private String address;

    // Daemon的1号端口: 用于心跳机制
    private int port;
    // Daemon的2号端口: 用于Gossip机制
    private int portGossip;
    // Daemon的3号端口: 用于Join机制
    private int portJoin;
    // 本机Daemon进程生命的时间长度
    private int deathTime;
    // 标识本机是否为introducer
    public boolean isIntroducer = false;
    // 定义节点的最后心跳时间映射
    private Map<String, Long> lastHeartbeatMap = new HashMap<>();
    // 定义节点的在拓扑结构上的neighbors:记录name
    private List<String> neighbors = new ArrayList<>();

    /**
     * @description: Daemon构造函数
     * @param: portId   Daemon的基础端口
     * inputIntroducer    是否为introducer
     * @return:
     * @Author Junwei Hu
     * @Date 2024/12/10 14:17
     */
    public Daemon(String name, int port, boolean isIntroducer, int deathTime) {
        this.name = name;
        this.port = port;
        this.portGossip = port + 100;
        this.portJoin = port + 200;
        this.isIntroducer = isIntroducer;
        this.deathTime = deathTime;
        // 获取本机公网IP
        String localIp = null;
        try {
            // localIp = getPublicIp();
            localIp = ipList[Integer.parseInt(name) / 2];
            this.address = localIp;
            if (this.isIntroducer) {
                // 将本机Member信息加入memberList
                long timestamp = System.currentTimeMillis();
                Member member = new Member(this.name, this.address, this.port, timestamp);
                // 加入组成员服务列表，并排序
                this.memberList.add(member);
                this.memberList.sort(null);
            }
            //            // 初始化本机的neighbors
            //            findNeighbors();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @description:  获取本机公网ip，目前是用来将自己的信息加入memberList中
     * @param:
     * @return: java.lang.String
     * @Author Junwei Hu
     * @Date 2024/12/10 16:29
     */
    public static String getPublicIp() throws Exception {
        Process process = Runtime.getRuntime().exec("dig +short myip.opendns.com @resolver1.opendns.com");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String publicIp = reader.readLine();
        reader.close();
        return publicIp;
    }

    /**
     * @Description TODO: 将本机的neighbors加入List
     * @Author Junwei Hu
     * @Date 2024/12/11 14:43
     * @Version 1.0
     **/
    public void findNeighbors() {
        // 组成员列表中只有本机
        if (memberList.size() == 1) {
            // 此时不存在neighbors
            neighbors.clear();
            return;
        }
        // 组成员列表中只有本机和另一台虚拟机
        else if (memberList.size() == 2) {
            for (Member member : memberList) {
                if (!member.getName().equals(name)) {
                    // 确定是另外一台虚拟机
                    neighbors.clear();
                    neighbors.add(member.getName());
                }
            }
        }
        // 组成员列表中除了本机以外，有2台以上虚拟机
        else {
            // 算法的思路是：将全部虚拟机按照id大小排列成一个圆形，如1-2-3-4-5-6-1
            // 分别找到一个节点的左相邻和右相邻

            // 首先，顺时针寻找右相邻
            int rightNeighborID = Integer.parseInt(name) + 1;
            boolean rightNeighborFound = false;
            while (true) {
                // 已到达最右侧
                if (rightNeighborID > Integer.parseInt(memberList.get(memberList.size() - 1).getName())) {
                    // 转一圈回到左侧
                    rightNeighborID = Integer.parseInt(memberList.get(0).getName());
                }
                // 遍历列表，确定是否存在当前rightNeighborID
                for (int i = 0; i < memberList.size(); i++) {
                    if (Integer.parseInt(memberList.get(i).getName()) == rightNeighborID) {
                        rightNeighborFound = true;
                        break;
                    }
                }
                // 确定右相邻
                if (rightNeighborFound == true) {
                    break;
                }
                rightNeighborID++;
            }
            // 然后，逆时针寻找左相邻
            int leftNeighborID = Integer.parseInt(name) - 1;
            boolean leftNeighborFound = false;
            while (true) {
                // 到达最左侧
                if (leftNeighborID < Integer.parseInt(memberList.get(0).getName())) {
                    // 转一圈回到右侧
                    leftNeighborID = Integer.parseInt(memberList.get(memberList.size() - 1).getName());
                }
                // 遍历列表，确定是否存在当前leftNeighborID
                for (int i = 0; i < memberList.size(); i++) {
                    if (Integer.parseInt(memberList.get(i).getName()) == leftNeighborID) {
                        leftNeighborFound = true;
                        break;
                    }
                }
                // 确定左相邻
                if (leftNeighborFound == true) {
                    break;
                }
                leftNeighborID--;
            }
            neighbors.clear();
            neighbors.add(String.valueOf(leftNeighborID));
            neighbors.add(String.valueOf(rightNeighborID));
        }
        return;
    }

    /**
     * @Description TODO: 开启Daemon后台进程
     * @Author Junwei Hu
     * @Date 2024/12/12 16:47
     * @Version 1.0
     **/
    public void startDaemon() {
        try {
            //启动introducer线程
            if (isIntroducer) {
                System.out.println("Server as Introducer!");
                ServerSocket introducerServerSocket = new ServerSocket(portJoin);
                new ListenJoinRequest(this, introducerServerSocket).start();
                new logWriteThread(this.getDaemonPort(), "join", System.currentTimeMillis(), this.name, this.address, this.port, true, this.memberList).start();
            } else {
                // 启动向introducer连接的请求
                new JoinGroup(this).start();
            }

            // 启动心跳发送线程
            new HeartbeatThread(this).start();

            // 启动离线检查线程
            new OfflineCheckThread(this).start();

            // 启动gossip发送线程
            new GossipThread(this).start();

            // 循环接收心跳信息
            new HeartbeatListenThread(this).start();

            // 循环接收gossip信息
            new GossipListenThread(this).start();

            // Stop the thread after deathTime milliseconds
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    // 开启一次写Crash日志的线程: 对象保存的是Crash机器本身
                    logWriteThread logThread = new logWriteThread(port, "crash", System.currentTimeMillis(), name, address, port, false, memberList);
                    logThread.start();
                    try {
                        logThread.join();
                    } catch (Exception e) {
                        System.out.println("logThread Error!");
                    }
                    stopThread();
                }
            }, deathTime);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDaemonAddress() {
        return address;
    }

    public String getDaemonName() {
        return name;
    }

    public int getDaemonPort() {
        return port;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public Map<String, Long> getLastHeartbeatMap() {
        return lastHeartbeatMap;
    }

    public int getPortGossip() {
        return portGossip;
    }

    public void stopThread() {
        this.isRunning = false;
    }
}