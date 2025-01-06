import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description TODO: 初始化接收服务器，负责接收切分后的DBLP XML文件
 * @Author Junwei Hu
 * @Date 2024/12/07 14:23
 * @Version 1.0
 **/
public class Initialize {
    /**
     * 正式DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/08 09:45
     */
    private static String DBLP_Path = "/mnt/dblpXmls";

    /**
     * 备份DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/08 09:50
     */
    private static String DBLP_Backup_Path = "/mnt/dblpBackupXmls";

    /**
     * @Description TODO: 启动接收服务器，监听指定端口接收文件
     * @param portSelected 监听的端口号
     * @throws Exception 如果服务器启动失败
     * @Author Junwei Hu
     * @Date 2024/12/08 10:15
     * @Version 1.0
     **/
    public static void receiveXml(int portSelected) throws Exception {
        // 使用try-with-resources确保ServerSocket被正确关闭
        try (ServerSocket serverSocket = new ServerSocket(portSelected)) {
            System.out.println("服务器已启动，监听端口：" + portSelected);

            while (true) {
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                System.out.println("客户端已连接：" + socket.getInetAddress());

                // 使用独立线程处理每个客户端连接，避免阻塞
                new Thread(new ClientHandler(socket, portSelected)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器运行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 内部类用于处理客户端连接
     * @Author Junwei Hu
     * @Date 2024/12/08 10:20
     * @Version 1.0
     **/
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private int portSelected;

        /**
         * 构造函数
         * @param socket 客户端Socket连接
         * @param portSelected 监听的端口号
         * @Author Junwei Hu
         * @Date 2024/12/08 10:25
         * @Version 1.0
         **/
        public ClientHandler(Socket socket, int portSelected) {
            this.socket = socket;
            this.portSelected = portSelected;
        }

        /**
         * 运行方法，处理客户端发送的文件
         * @Author Junwei Hu
         * @Date 2024/12/08 10:30
         * @Version 1.0
         **/
        @Override
        public void run() {
            // 使用try-with-resources确保流和Socket被正确关闭
            try (DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {
                // 接收文件名信息
                String fileName = inputStream.readUTF();
                System.out.println("接收的文件名: " + fileName);

                // 接收备份标识
                String backupTag = inputStream.readUTF();
                System.out.println("接收的备份标识: " + backupTag);

                // 确定文件写入路径
                String filePath;
                if ("isBackup".equalsIgnoreCase(backupTag)) {
                    filePath = DBLP_Backup_Path + "/" + portSelected + "/" + fileName;
                } else {
                    filePath = DBLP_Path + "/" + portSelected + "/" + fileName;
                }
                System.out.println("写入路径: " + filePath);

                // 创建文件对象
                File file = new File(filePath);

                // 确保父目录存在，如果不存在则创建
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (dirsCreated) {
                        System.out.println("创建目录: " + parentDir.getAbsolutePath());
                    } else {
                        System.err.println("无法创建目录: " + parentDir.getAbsolutePath());
                        throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                    }
                }

                // 创建文件，如果文件不存在
                if (!file.exists()) {
                    boolean fileCreated = file.createNewFile();
                    if (fileCreated) {
                        System.out.println("创建文件: " + file.getAbsolutePath());
                    } else {
                        System.err.println("无法创建文件: " + file.getAbsolutePath());
                        throw new IOException("无法创建文件: " + file.getAbsolutePath());
                    }
                }

                // 创建文件输出流
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    // 读取输入流中的数据并写入文件
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    System.out.println("开始接收文件内容...");
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                    System.out.println("文件写入完成: " + filePath);
                }

            } catch (IOException e) {
                System.err.println("处理客户端数据时发生错误: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 关闭Socket连接
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        System.out.println("关闭客户端连接");
                    }
                } catch (IOException ex) {
                    System.err.println("关闭Socket时发生错误: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }
}