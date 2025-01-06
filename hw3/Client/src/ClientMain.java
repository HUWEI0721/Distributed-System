import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @Description TODO: Client的主类
 * @Author Junwei Hu
 * @Date 2024/12/08 10:45
 * @Version 1.0
 **/
public class ClientMain {
    /**
     * @Description TODO: 向服务器发送query请求（非备份）
     * @param numWithYear 存储每台服务器的查询结果
     * @param name 作者姓名
     * @param beginYear 起始年份
     * @param endYear 截至年份
     * @param useIndex 是否使用索引查询
     * @Author Junwei Hu
     * @Date 2024/12/07 16:20
     * @Version 1.0
     **/
    public static void sendQuerys(int[] numWithYear, String name, String beginYear, String endYear, boolean useIndex) {
        // 定义服务器的ip和端口组合
        int[][] serverConfigs = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1},
                {2, 0},
                {2, 1}
        };

        List<Thread> threads = new ArrayList<>();

        // 创建并启动线程
        for (int i = 0; i < serverConfigs.length; i++) {
            int ipSelected = serverConfigs[i][0];
            int portSelected = serverConfigs[i][1];
            int index = ipSelected * 2 + portSelected; // 原有计算index的方式
            // 调用修改后的createThread，多传一个index参数
            Thread thread = createThread(numWithYear, name, beginYear, endYear, ipSelected, portSelected, false, useIndex, index);
            threads.add(thread);
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("线程执行被中断");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @Description TODO: 向服务器发送query请求（备份）
     * @param numWithYear 存储每台服务器的查询结果
     * @param name 作者姓名
     * @param beginYear 起始年份
     * @param endYear 截至年份
     * @param useIndex 是否使用索引查询
     * @Author Junwei Hu
     * @Date 2024/12/09 09:30
     * @Version 1.0
     **/
    public static void sendBackupQuerys(int[] numWithYear, String name, String beginYear, String endYear, boolean useIndex) {
        // 定义服务器的ip和端口组合，注意备份查询的顺序
        int[][] backupConfigs = {
                {0, 1},
                {1, 0},
                {1, 1},
                {2, 0},
                {2, 1},
                {0, 0}
        };

        List<Thread> threads = new ArrayList<>();

        // 检查并启动需要查询备份的线程
        for (int i = 0; i < numWithYear.length; i++) {
            if (numWithYear[i] == -1) {
                int ipSelected = backupConfigs[i][0];
                int portSelected = backupConfigs[i][1];
                System.out.println("====================================================");
                System.out.println("====================虚拟机" + (i + 1) + "故障=====================");
                System.out.println("====================正在查询备份====================");
                System.out.println("====================================================");
                // 在备份查询中，直接使用i作为numWithYear的索引，以防止覆盖其他值
                Thread thread = createThread(numWithYear, name, beginYear, endYear, ipSelected, portSelected, true, useIndex, i);
                threads.add(thread);
                thread.start();
            }
        }

        // 等待已启动的线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("备份线程执行被中断");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @Description 创建并返回一个新的查询线程
     * @param numWithYear 存储查询结果的数组
     * @param name 作者姓名
     * @param beginYear 起始年份
     * @param endYear 截至年份
     * @param ipSelected 选择的IP索引
     * @param portSelected 选择的端口索引
     * @param isBackup 是否查询备份
     * @param useIndex 是否使用索引查询
     * @param index 要写入numWithYear的目标索引（修改点）
     * @return 新创建的线程
     * @Author Junwei Hu
     * @Date 2024/12/08 11:00
     * @Version 1.0
     **/
    private static Thread createThread(int[] numWithYear, String name, String beginYear, String endYear,
                                       int ipSelected, int portSelected, boolean isBackup, boolean useIndex, int index) {
        return new Thread(() -> {
            numWithYear[index] = AccessServer.sendQuery(name, beginYear, endYear, ipSelected, portSelected, isBackup, useIndex);
        });
    }

    /**
     * @Description TODO: Client的主函数，调用其他类，运行Client的逻辑
     * @param args 命令行参数
     * @Author Junwei Hu
     * @Date 2024/12/06 14:50
     * @Version 1.0
     **/
    public static void main(String[] args) {
        // 初始化日志记录
        Logger logger = null;
        try {
            // 确保日志目录存在
            File logDir = new File("Logger");
            if (!logDir.exists()) {
                boolean dirsCreated = logDir.mkdirs();
                if (!dirsCreated) {
                    System.err.println("无法创建日志目录: " + logDir.getAbsolutePath());
                    return;
                }
            }

            logger = new Logger("Logger/2153393-hw2-q1.log");
        } catch (IOException e) {
            System.err.println("无法初始化日志记录: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Scanner sc = new Scanner(System.in);

        // 初始化DBLP分布式存储
        while (true) {
            System.out.println("====================================================");
            System.out.print("选择是/否要初始化DBLP文件的分布式存储(输入yes/no)：");
            String str = sc.nextLine().trim().toLowerCase();
            // 记录用户输入到日志
            logger.logInput("=========================");
            logger.logInput("选择初始化DBLP分布式存储: " + str);
            if (str.equals("yes")) {
                try {
                    // 执行初始化流程
                    Initialize.initDBLP();
                    System.out.println("DBLP分布式存储初始化完成");
                    logger.logMessage("DBLP分布式存储初始化完成");
                } catch (Exception e) {
                    System.out.println("初始化失败: " + e.getMessage());
                    logger.logMessage("初始化失败: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            } else if (str.equals("no")) {
                break;
            } else {
                System.out.println("输入不合法，请重新输入");
                logger.logMessage("用户输入不合法: " + str);
            }
        }

        System.out.println("====================================================");
        // 主查询循环
        while (true) {
            System.out.print("请输入作者姓名 (输入exit退出):");
            String name = sc.nextLine().trim();
            // 退出标识: exit
            if (name.equalsIgnoreCase("exit")) {
                System.out.println("客户端退出.");
                logger.logMessage("客户端退出.");
                break;
            }

            System.out.println("====================================================");
            // 输入年份区间
            System.out.print("请输入起始年份(输入'*'代表不限制): ");
            String beginYear = sc.nextLine().trim(); // 起始年份
            System.out.print("请输入截止年份(输入'*'代表不限制): ");
            String endYear = sc.nextLine().trim(); // 截止年份

            boolean hasYearLimit = !(beginYear.equals("*") && endYear.equals("*"));
            System.out.println("====================================================");

            // 查询（未使用本地索引）
            System.out.println("正在查询（未使用索引）.....");
            long startTime = System.currentTimeMillis();

            // 初始化查询结果数组
            int[] numWithYear = {-2, -2, -2, -2, -2, -2};
            int numAll = 0;

            // 向各个服务器发送查询请求
            sendQuerys(numWithYear, name, beginYear, endYear, false);

            // 检查是否有服务器宕机，若有则查询备份
            boolean hasFailure = false;
            for (int num : numWithYear) {
                if (num == -1) {
                    hasFailure = true;
                    break;
                }
            }
            if (hasFailure) {
                sendBackupQuerys(numWithYear, name, beginYear, endYear, false);
            }

            // 计算总的论文频次数
            numAll = 0;
            for (int num : numWithYear) {
                if (num > 0) {
                    numAll += num;
                }
                // System.out.println(num);
            }

            // 输出用时和查询结果
            long endTime = System.currentTimeMillis();
            double queryTimeSeconds = (endTime - startTime) / 1000.0;
            System.out.printf("查询成功， 用时：%.2fs%n", queryTimeSeconds);
            if (hasYearLimit) {
                System.out.println("从" + beginYear + "年到" + endYear + "，" + name + "发表DBLP论文总数为" + numAll);
            } else {
                System.out.println("在没有年份限制情况下，查询到" + name + "发表的DBLP论文总数为" + numAll);
            }
            System.out.println("====================================================");
            logger.logQuery(name, beginYear, endYear, "未使用索引", numAll, queryTimeSeconds);

            // 查询（使用本地索引）
            System.out.println("正在查询（使用索引）.....");
            startTime = System.currentTimeMillis();

            // 重置查询结果数组
            for (int i = 0; i < numWithYear.length; i++) {
                numWithYear[i] = -2;
            }
            numAll = 0;

            // 向各个服务器发送查询请求
            sendQuerys(numWithYear, name, beginYear, endYear, true);

            // 检查是否有服务器宕机，若有则查询备份
            hasFailure = false;
            for (int num : numWithYear) {
                if (num == -1) {
                    hasFailure = true;
                    break;
                }
            }
            if (hasFailure) {
                sendBackupQuerys(numWithYear, name, beginYear, endYear, true);
            }

            // 计算总的论文频次数
            numAll = 0;
            for (int num : numWithYear) {
                if (num > 0) {
                    numAll += num;
                }
            }

            // 输出用时和查询结果
            endTime = System.currentTimeMillis();
            queryTimeSeconds = (endTime - startTime) / 1000.0;
            System.out.printf("查询成功（使用本地索引）! 用时：%.2fs%n", queryTimeSeconds);
            if (hasYearLimit) {
                System.out.println("从" + beginYear + "年到" + endYear + "，" + name + "发表DBLP论文总数为" + numAll);
            } else {
                System.out.println("在没有年份限制情况下，查询到" + name + "发表的DBLP论文总数为" + numAll);
            }
            System.out.println("====================================================");
            logger.logQuery(name, beginYear, endYear, "使用索引", numAll, queryTimeSeconds);
        }

        sc.close();
        // 关闭日志记录器
        if (logger != null) {
            try {
                logger.close();
            } catch (IOException e) {
                System.err.println("关闭日志记录器时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * @Description TODO: 自定义Logger类，用于记录查询信息到日志文件
     * @Author Junwei Hu
     * @Date 2024/12/08 10:50
     * @Version 1.0
     **/
    public static class Logger {
        private BufferedWriter logWriter;

        /**
         * 构造函数，初始化日志文件
         *
         * @param logFilePath 日志文件路径
         * @throws IOException 如果无法创建或写入日志文件
         */
        public Logger(String logFilePath) throws IOException {
            // 创建日志文件的父目录（如果未创建）
            File logFile = new File(logFilePath);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirsCreated = parentDir.mkdirs();
                if (!dirsCreated) {
                    throw new IOException("无法创建日志文件的父目录: " + parentDir.getAbsolutePath());
                }
            }

            // 初始化BufferedWriter用于写入日志
            this.logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFilePath, true), "UTF-8"));
        }

        /**
         * 记录用户输入到日志文件
         *
         * @param input 用户输入内容
         */
        public void logInput(String input) {
            try {
                // 仅记录用户的输入，不包含提示信息
                logWriter.write(input);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("写入用户输入到日志文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 记录普通消息到日志文件
         *
         * @param message 消息内容
         */
        public void logMessage(String message) {
            try {
                // 记录程序内部的消息，例如初始化完成或错误信息
                logWriter.write(message);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("写入消息到日志文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 记录查询信息到日志文件
         *
         * @param name             作者姓名
         * @param beginYear        起始年份
         * @param endYear          截至年份
         * @param indexUsage       是否使用索引查询（"使用索引" 或 "未使用索引"）
         * @param numAll           查询到的论文总数
         * @param queryTimeSeconds 查询用时，单位秒
         */
        public void logQuery(String name, String beginYear, String endYear, String indexUsage, int numAll, double queryTimeSeconds) {
            try {
                logWriter.write(String.format("作者姓名：%s", name));
                logWriter.newLine();
                logWriter.write(String.format("起始年份：%s", beginYear));
                logWriter.newLine();
                logWriter.write(String.format("截止年份：%s", endYear));
                logWriter.newLine();
                logWriter.write(String.format("是否索引：%s", indexUsage));
                logWriter.newLine();
                logWriter.write(String.format("查询结果：%d", numAll));
                logWriter.newLine();
                logWriter.write(String.format("查询用时：%.2f s", queryTimeSeconds));
                logWriter.newLine();
                // 修改为equals而不是==判断
                if(indexUsage.equals("使用索引")){
                    logWriter.write("=========================");
                }
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("写入查询信息到日志文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * 关闭日志记录器，释放资源
         *
         * @throws IOException 如果发生I/O错误
         */
        public void close() throws IOException {
            if (this.logWriter != null) {
                this.logWriter.close();
            }
        }
    }
}