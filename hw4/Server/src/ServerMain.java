import java.util.Scanner;

public class ServerMain {
    private final static String[] ipList = new String[]
            {"124.220.24.233", "110.40.167.8", "1.116.120.226"};
    private final static int[] portList = new int[]
            {8820, 8821, 8822};

    /**
     * @description: 执行初始化流程，包括切分xml和将xml发送给各个虚拟机
     * @author Junwei Hu
     * @date 2024.12.13 16:35 PM
     * @version 1.0
     **/
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String name = null;
        int portSelected = -1;
        int deathTime = -1;
        boolean is_introducer = false;

        // 输入虚拟机名称
        while (name == null) {
            System.out.println("请输入虚拟机name: 采用数字0、1、2、3...");
            name = sc.nextLine();
            if (!(name.matches("[0-8]"))) {
                name = null;
                System.out.println("请重新输入");
            }
        }

        // 输入虚拟机端口选择
        while (portSelected == -1) {
            System.out.println("请输入0/1/2选择虚拟机端口：0--8820, 1--8821, 2--8822");
            try {
                String portStr = sc.nextLine();
                portSelected = Integer.parseInt(portStr);
                if (portSelected < 0 || portSelected > 2) {
                    portSelected = -1;
                    System.out.println("请重新输入");
                }
            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字");
            }
        }

        // 是否将虚拟机设为 introducer
        while (true) {
            System.out.println("是否将该虚拟机设为 introducer?  yes：是；no：否");
            String input = sc.nextLine();
            if ("yes".equalsIgnoreCase(input)) {
                is_introducer = true;
                break;
            } else if ("no".equalsIgnoreCase(input)) {
                break;
            } else {
                System.out.println("请重新输入");
            }
        }

        // 输入 Death 时间
        while (deathTime == -1) {
            System.out.println("请输入 Death 时间 (正整数):");
            try {
                String inputStr = sc.nextLine();
                deathTime = Integer.parseInt(inputStr);
                if (deathTime < 0) {
                    deathTime = -1;
                    System.out.println("请重新输入");
                }
            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字");
            }
        }

        // 创建查询虚拟机线程
        int port = portList[portSelected];
        VirtualServer vs = new VirtualServer(name, port + 100, is_introducer, deathTime);

        // 查询虚拟机线程
        Thread queryThread = new Thread(() -> vs.receiveQuery());
        queryThread.start();
        System.out.println("虚拟机已启动");

        // 创建并启动组服务后台线程
        if (deathTime != 0) {
            Thread daemon = new Thread(() -> vs.groupDaemon.startDaemon());
            daemon.start();
            System.out.println("组服务后台已启动");
        }
    }
}