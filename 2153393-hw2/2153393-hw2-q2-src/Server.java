package Class;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 12345; // 服务器端口号
    private static AtomicInteger clientCounter = new AtomicInteger(0); // 用于标识客户端顺序

    public static void main(String[] args) {
        try {
            // 创建服务器套接字
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("服务器已启动，等待客户端连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();
                System.out.println("客户端已连接：" + clientSocket.getInetAddress() + "，客户端编号：" + clientId);

                // 为每个客户端创建一个线程
                ClientHandler handler = new ClientHandler(clientSocket, clientId);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 客户端处理类
    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private int clientId;

        public ClientHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.oos.flush(); // 确保对象输出流的头信息发送
            this.ois = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                boolean running = true;
                while (running) {
                    // 接收客户端的请求类型（"read"、"write" 或 "exit"）
                    String requestType = (String) ois.readObject();
                    System.out.println("服务器接收到客户端 " + clientId + " 的请求：" + requestType);

                    if ("read".equals(requestType)) {
                        // 读取请求，直接允许
                        oos.writeObject("read_granted");
                        System.out.println("服务器授予客户端 " + clientId + " 读取权限。");
                    } else if ("write".equals(requestType)) {
                        // 写入请求，根据客户端编号协调写入顺序
                        System.out.println("客户端 " + clientId + " 请求写入权限。");
                        grantWritePermission();
                    } else if ("exit".equals(requestType)) {
                        // 客户端请求断开连接
                        running = false;
                        System.out.println("客户端 " + clientId + " 断开连接。");
                    } else {
                        System.out.println("收到未知请求：" + requestType);
                    }
                }

                // 关闭资源
                ois.close();
                oos.close();
                socket.close();
                System.out.println("已关闭与客户端 " + clientId + " 的连接。");

            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void grantWritePermission() throws IOException, InterruptedException, ClassNotFoundException {
            // 假设客户端编号即为写入顺序，客户端1先写，客户端2其次，客户端3最后
            while (clientId != ServerManager.getCurrentWriter()) {
                // 不满足写入条件，等待
                Thread.sleep(100);
            }
            // 允许写入
            oos.writeObject("write_granted");
            System.out.println("已授予客户端 " + clientId + " 写入权限。");

            // 等待客户端完成写入操作的确认
            String confirm = (String) ois.readObject();
            if ("write_complete".equals(confirm)) {
                System.out.println("客户端 " + clientId + " 已完成写入操作。");
                // 更新下一个可以写入的客户端编号
                ServerManager.moveToNextWriter();
            }
        }
    }
}

// 辅助类，用于管理当前允许写入的客户端编号
class ServerManager {
    private static int currentWriter = 1;

    public static synchronized int getCurrentWriter() {
        return currentWriter;
    }

    public static synchronized void moveToNextWriter() {
        currentWriter++;
        if (currentWriter > 3) {
            currentWriter = 1;
        }
    }
}
