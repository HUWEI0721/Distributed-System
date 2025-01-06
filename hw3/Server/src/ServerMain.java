import java.util.Scanner;

/**
 * @Description TODO: 服务器主类，负责启动查询虚拟机和文件接收服务
 * @Author Junwei Hu
 * @Date 2024/12/07 16:45
 * @Version 1.0
 **/
public class ServerMain {
    /**
     * 可访问的服务器IP地址列表
     * @Author Junwei Hu
     * @Date 2024/12/07 16:50
     */
    private final static String[] ipList = new String[] {
            "124.220.24.233", "110.40.167.8", "1.116.120.226"
    };

    /**
     * 可访问的服务器端口列表
     * @Author Junwei Hu
     * @Date 2024/12/07 16:52
     */
    private final static int[] portList = new int[] {
            8820, 8821, 8822
    };

    /**
     * @Description TODO: 执行初始化流程，包括启动查询虚拟机和文件接收服务
     * @param args 命令行参数
     * @Author Junwei Hu
     * @Date 2024/12/07 16:55
     * @Version 1.0
     **/
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int portSelected = -1;

        // 用户选择虚拟机端口
        while (portSelected == -1) {
            System.out.println("请输入0/1/2选择虚拟机端口：0--8820, 1--8821, 2--8822");
            String portStr = sc.nextLine().trim();
            try {
                portSelected = Integer.parseInt(portStr);
                if (portSelected < 0 || portSelected > 2) {
                    portSelected = -1;
                    System.out.println("输入无效，请重新输入！");
                }
            } catch (NumberFormatException e) {
                portSelected = -1;
                System.out.println("输入格式错误，请输入数字0、1或2！");
            }
        }

        // 获取选择的端口号
        int port = portList[portSelected];

        // 启动查询虚拟机线程
        VirtualServer vs = new VirtualServer(port + 100);
        Thread queryThread = new Thread(() -> {
            vs.receiveQuery();
        }, "QueryThread");
        queryThread.start();
        System.out.println("查询虚拟机已启动，端口：" + (port + 100));

        // 启动文件接收线程
        Initialize in = new Initialize();
        Thread receiveThread = new Thread(() -> {
            try {
                in.receiveXml(port);
            } catch (Exception e) {
                System.err.println("文件接收服务启动失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, "ReceiveThread");
        receiveThread.start();
        System.out.println("文件接收服务已启动，端口：" + port);

        // 在这里创建daemon的组服务（如果有需要，可以在此处添加相关代码）
        // 示例：启动一个守护线程
        /*
        Thread daemonThread = new Thread(() -> {
            // 守护线程的任务
        }, "DaemonThread");
        daemonThread.setDaemon(true);
        daemonThread.start();
        */
    }
}