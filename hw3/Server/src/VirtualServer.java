import localindex.IndexQuery;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description TODO: 虚拟服务器类，负责接收和处理客户端的查询请求
 * @Author Junwei Hu
 * @Date 2024/12/07 16:10
 * @Version 1.0
 **/
public class VirtualServer {
    /**
     * 当前服务器监听的端口号
     * @Author Junwei Hu
     * @Date 2024/12/07 16:15
     */
    private int port;

    /**
     * 每台服务器应存放的XML文件数量
     * @Author Junwei Hu
     * @Date 2024/12/07 16:17
     */
    private final int xmlProperNum = 4;

    /**
     * @Description TODO: 虚拟服务器的构造函数，初始化端口号
     * @param portID 虚拟服务器的端口号
     * @Author Junwei Hu
     * @Date 2024/12/07 16:20
     * @Version 1.0
     **/
    public VirtualServer(int portID) {
        this.port = portID;
        // 在这里顺便初始化组服务（如果有需要，可以添加相关初始化代码）
    }

    /**
     * @Description TODO: 接收并处理客户端的查询请求
     * @Author Junwei Hu
     * @Date 2024/12/07 16:25
     * @Version 1.0
     **/
    public void receiveQuery() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("虚拟服务器已启动，监听端口：" + port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("客户端已连接：" + socket.getInetAddress());

                // 使用独立线程处理每个客户端连接，避免阻塞
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器运行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @Description TODO: 处理单个客户端的查询请求
     * @param socket 客户端Socket连接
     * @Author Junwei Hu
     * @Date 2024/12/07 16:30
     * @Version 1.0
     **/
    private void handleClient(Socket socket) {
        try (
                DataInputStream is = new DataInputStream(socket.getInputStream());
                DataOutputStream os = new DataOutputStream(socket.getOutputStream())
        ) {
            // 接收来自客户端的是否查询备份的信息
            String isBackup = is.readUTF();
            System.out.println("接收的备份标识: " + isBackup);

            // 接收来自客户端的name信息
            String name = is.readUTF();
            System.out.println("接收的作者姓名: " + name);

            // 接收来自客户端的beginYear信息
            String beginYear = is.readUTF();
            System.out.println("接收的起始年份: " + beginYear);

            // 接收来自客户端的endYear信息
            String endYear = is.readUTF();
            System.out.println("接收的结束年份: " + endYear);

            // 接收来自客户端的useIndex信息
            String useIndex = is.readUTF();
            System.out.println("接收的是否使用索引: " + useIndex);

            // 创建Query实例
            Query query = new Query(port);

            // 确定接收到了来自客户端的信息
            if (!name.isEmpty()) {
                String queryResult;
                if ("true".equalsIgnoreCase(useIndex)) {
                    boolean isCopy = !"false".equalsIgnoreCase(isBackup);
                    queryResult = IndexQuery.queryByIndex(isCopy, port - 100, name, beginYear, endYear);
                    System.out.println("使用索引查询的最终结果: " + queryResult);
                } else {
                    queryResult = query.queryByNameAndYear(name, beginYear, endYear, isBackup);
                    System.out.println("按姓名和年份查询的最终结果: " + queryResult);
                }
                // 向客户端发送查询结果
                os.writeUTF(queryResult);
                os.flush();
            }

            // 关闭Socket连接
            socket.close();
            System.out.println("关闭客户端连接");
        } catch (IOException e) {
            System.err.println("处理客户端数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}