import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import java.io.File;

/**
 * @description: 查询功能类，包含按姓名查询和按姓名及年份查询两种功能
 * @author Junwei Hu
 * @date 2024.12.13 15:22 PM
 * @version 1.0
 **/
public class Query {
    // 当前虚拟机端口
    private static int port;
    // log日志文件路径
    private static String LOG_Path;
    // log日志文件名称
    private static ArrayList<String> logFileNames = new ArrayList<>();

    /**
     * @description: Query的构造函数
     * @param portSelected 选择的端口
     **/
    public Query(int portSelected) {
        port = portSelected + 100;
        LOG_Path = "/mnt/log/" + port;
        // 获取log文件的名称
        File dir = new File(LOG_Path);
        File[] xmlFiles = dir.listFiles((dir1, name) -> name.endsWith(".log"));
        if (xmlFiles != null) {
            for (File xmlFile : xmlFiles) {
                logFileNames.add(xmlFile.getName());
            }
        }
    }

    /**
     * @description: 开启终端，执行传入的命令行，获得执行结果
     * @param commandStr 终端命令
     * @return 执行结果
     **/
    public static String exeCmd(String commandStr) {
        String result = null;
        try {
            String[] cmd = new String[]{"/bin/sh", "-c", commandStr};
            Process ps = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @description: 按照Type名进行查询
     * @param type 查询的类型
     * @return 查询结果
     **/
    public static String queryByType(String type) {
        String command = "grep -wo \"" + type + "\" " + LOG_Path + "/" + type + ".log" + " |wc -l";
        return exeCmd(command);
    }

    /**
     * @description: 获取crashTime
     * @return crashTime
     **/
    public String getCrashTime() {
        String crashTime = "";
        String path = LOG_Path + "/crash.log";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            for (int i = 0; i < 5; i++) {
                reader.readLine();
            }
            crashTime = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crashTime;
    }

    /**
     * @description: 获取joinTime
     * @return joinTime
     **/
    public String getJoinTime() {
        String joinTime = "";
        String path = LOG_Path + "/join.log";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            for (int i = 0; i < 5; i++) {
                reader.readLine();
            }
            joinTime = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return joinTime;
    }

    /**
     * @description: 获取日志中MemberList改变的时间戳
     * @param path 日志路径
     * @param changedTimestamps 存储改变时间戳的列表
     **/
    public void getChangedTime(String path, List<Long> changedTimestamps) {
        try (Scanner scanner = new Scanner(new File(path))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                System.out.println("[Query]: 打印Log " + line);
                if (line.contains("true")) {
                    scanner.nextLine(); // 跳过两行无用信息
                    scanner.nextLine();
                    long changedTimestamp = Long.parseLong(scanner.nextLine());
                    changedTimestamps.add(changedTimestamp);
                    System.out.println("[Query]: 得到changedTimestamp " + changedTimestamp);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description: 获取所有MemberList改变的时间戳
     * @param changedTimestamps 存储改变时间戳的列表
     **/
    public void getChangedTimestamps(List<Long> changedTimestamps) {
        String gossipPath = LOG_Path + "/gossip.log";
        String offlinePath = LOG_Path + "/offline.log";
        String introducePath = LOG_Path + "/introduce.log";

        System.out.println("[Query]: LogFiles " + logFileNames);
        if (logFileNames.contains("gossip.log")) {
            getChangedTime(gossipPath, changedTimestamps);
        }
        if (logFileNames.contains("offline.log")) {
            getChangedTime(offlinePath, changedTimestamps);
        }
        if (logFileNames.contains("introduce.log")) {
            getChangedTime(introducePath, changedTimestamps);
        }
    }
}