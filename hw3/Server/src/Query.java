import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import java.io.File;

/**
 * @Description TODO: 查询功能类，包含：1. 按姓名查询 2. 按姓名和年份查询 两种查询功能。
 * @Author Junwei Hu
 * @Date 2024/12/07 14:23
 * @Version 1.0
 **/
public class Query {
    /**
     * 当前虚拟机端口
     * @Author Junwei Hu
     * @Date 2024/12/07 14:25
     */
    private static int port;

    /**
     * dblp.xml正式块路径
     * @Author Junwei Hu
     * @Date 2024/12/07 14:30
     */
    private static String DBLP_Path;

    /**
     * dblp.xml备份块路径
     * @Author Junwei Hu
     * @Date 2024/12/07 14:35
     */
    private static String DBLP_Backup_Path;

    /**
     * 当前虚拟机下存储的正式文件块名称列表
     * @Author Junwei Hu
     * @Date 2024/12/07 14:40
     */
    private static ArrayList<String> dblpNames = new ArrayList<>();

    /**
     * 当前虚拟机下存储的备份文件块名称列表
     * @Author Junwei Hu
     * @Date 2024/12/07 14:45
     */
    private static ArrayList<String> dblpBackupNames = new ArrayList<>();

    /**
     * @Description TODO: Query的构造函数，初始化文件路径和文件名称列表
     * @param portSelected 选择的端口号
     * @Author Junwei Hu
     * @Date 2024/12/07 14:50
     * @Version 1.0
     **/
    public Query(int portSelected) {
        port = portSelected - 100;
        DBLP_Path = "/mnt/dblpXmls/" + port;
        DBLP_Backup_Path = "/mnt/dblpBackupXmls/" + port;

        dblpNames.clear();
        dblpBackupNames.clear();

        // 获取正式dblp文件块的名称
        File dir = new File(DBLP_Path);
        File[] xmlFiles = dir.listFiles((dir1, name) -> name.endsWith(".xml"));
        if (xmlFiles != null) {
            for (File xmlFile : xmlFiles) {
                dblpNames.add(xmlFile.getName());
            }
        }

        // 获取备份dblp文件块的名称
        dir = new File(DBLP_Backup_Path);
        File[] xmlFilesBackup = dir.listFiles((dir1, name) -> name.endsWith(".xml"));
        if (xmlFilesBackup != null) {
            for (File xmlFile : xmlFilesBackup) {
                dblpBackupNames.add(xmlFile.getName());
            }
        }
    }

    /**
     * @Description TODO: 开启终端，执行传入的命令行，获得执行结果
     * @param commandStr 要执行的命令行字符串
     * @return 命令执行的结果
     * @Author Junwei Hu
     * @Date 2024/12/07 15:00
     * @Version 1.0
     **/
    public static String exeCmd(String commandStr) {
        StringBuilder result = new StringBuilder();
        try {
            String[] cmd = {"/bin/sh", "-c", commandStr};
            Process ps = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            // 等待命令执行完成
            ps.waitFor();
        } catch (Exception e) {
            System.err.println("执行命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return result.toString().trim();
    }

    /**
     * @Description TODO: 仅按照作者名进行查询
     * @param name 要查询的作者姓名
     * @param isBackup 是否查询备份文件块
     * @return 作者发表论文的总次数
     * @Author Junwei Hu
     * @Date 2024/12/07 15:05
     * @Version 1.0
     **/
    public static String queryByName(String name, String isBackup) {
        // 记录频次
        int num = 0;
        ArrayList<String> targetList = "false".equals(isBackup) ? dblpNames : dblpBackupNames;
        String targetPath = "false".equals(isBackup) ? DBLP_Path : DBLP_Backup_Path;

        for (int i = 0; i < targetList.size(); i++) {
            // 根据姓名进行查询
            String command = String.format("grep -wo \"%s\" %s/%s | wc -l", name, targetPath, targetList.get(i));
            System.out.println("执行命令: " + command);

            String result = exeCmd(command);
            System.out.println("命令结果: " + result);

            try {
                int count = Integer.parseInt(result.trim());
                num += count;
                System.out.println("当前计数: " + count + ", 总计数: " + num);
            } catch (NumberFormatException e) {
                System.err.println("解析结果时发生错误: " + result);
            }
        }
        System.out.println("最终计数，作者 \"" + name + "\" 的论文数量: " + num);
        return String.valueOf(num);
    }

    /**
     * @Description TODO: 按照作者名和年份范围进行查询
     * @param name 要查询的作者姓名
     * @param beginYear 起始年份
     * @param endYear 截至年份
     * @param dblpBlockPath 要查询的DBLP文件块路径
     * @return 匹配的论文数量
     * @Author Junwei Hu
     * @Date 2024/12/07 15:10
     * @Version 1.0
     **/
    public static String queryBlockByNameAndYear(String name, String beginYear, String endYear, String dblpBlockPath) throws FileNotFoundException {
        int matchedCounter = 0;
        try {
            // 创建一个 XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // 创建一个 XMLStreamReader
            System.out.println("读取文件: " + dblpBlockPath);
            try (FileReader fileReader = new FileReader(dblpBlockPath)) {
                XMLStreamReader reader = inputFactory.createXMLStreamReader(fileReader);

                // 创建一个字符串集合，包含DBLP数据库中所有可能的文章类型
                Set<String> typeSet = new HashSet<>(Arrays.asList(
                        "article",
                        "inproceedings",
                        "proceedings",
                        "book",
                        "incollection",
                        "phdthesis",
                        "mastersthesis",
                        "www",
                        "person",
                        "data"
                ));

                // 用于记录当前读取的块的信息
                String currentAuthor = null;
                String currentYear = null;
                boolean hasAuthor = false;
                // 创建一个栈，用于记录当前读取到的所有元素的名称
                Stack<String> elementStack = new Stack<>();

                // 开始读取 XML 文档
                while (reader.hasNext()) {
                    int event = reader.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            String localName = reader.getLocalName();
                            if (typeSet.contains(localName)) {
                                currentAuthor = null;
                                currentYear = null;
                                hasAuthor = false;
                            }
                            elementStack.push(localName);
                            break;
                        case XMLStreamConstants.END_ELEMENT:
                            String endLocalName = reader.getLocalName();
                            if (typeSet.contains(endLocalName)) {
                                if (hasAuthor && checkYearInRange(currentYear, beginYear, endYear)) {
                                    matchedCounter++;
                                    System.out.println("匹配的块: 作者=\"" + currentAuthor + "\", 年份=" + currentYear);
                                }
                                currentAuthor = null;
                                currentYear = null;
                                hasAuthor = false;
                            }
                            elementStack.pop();
                            break;
                        case XMLStreamConstants.CHARACTERS:
                            if (!elementStack.isEmpty()) {
                                String currentElement = elementStack.peek();
                                if ("author".equals(currentElement)) {
                                    currentAuthor = reader.getText().trim();
                                    if (name.equals(currentAuthor)) {
                                        hasAuthor = true;
                                    }
                                } else if ("year".equals(currentElement)) {
                                    currentYear = reader.getText().trim();
                                }
                            }
                            break;
                    }
                }
                reader.close();
            }
            System.out.println("文件 " + dblpBlockPath + " 的匹配计数: " + matchedCounter);
        } catch (XMLStreamException | IOException ex) {
            System.err.println("处理文件时发生错误: " + dblpBlockPath + " - " + ex.getMessage());
            return "0";
        }
        return String.valueOf(matchedCounter);
    }

    /**
     * @Description TODO: 按照姓名、年份限制对本虚拟机下存储的所有dblp块进行查询
     * @param name 要查询的作者姓名
     * @param beginYear 起始年份
     * @param endYear 截至年份
     * @param isBackup 是否查询备份文件块
     * @return 作者在指定年份范围内发表论文的总次数
     * @Author Junwei Hu
     * @Date 2024/12/07 15:15
     * @Version 1.0
     **/
    public static String queryByNameAndYear(String name, String beginYear, String endYear, String isBackup) {
        // 记录频次
        int num = 0;
        ArrayList<String> targetList = "false".equals(isBackup) ? dblpNames : dblpBackupNames;
        String targetPath = "false".equals(isBackup) ? DBLP_Path : DBLP_Backup_Path;

        for (int i = 0; i < targetList.size(); i++) {
            String dblpBlockPath = targetPath + "/" + targetList.get(i);
            String result = null;
            try {
                result = queryBlockByNameAndYear(name, beginYear, endYear, dblpBlockPath);
                int count = Integer.parseInt(result.trim());
                num += count;
                System.out.println("中间总计，文件索引 " + i + ": " + num);
            } catch (FileNotFoundException | NumberFormatException e) {
                System.err.println("处理块时发生错误: " + e.getMessage());
            }
        }
        System.out.println("最终总计，作者 \"" + name + "\" 在年份范围 [" + beginYear + ", " + endYear + "] 内的论文数量: " + num);
        return String.valueOf(num);
    }

    /**
     * @Description TODO: 判断输入的年份是否在指定的范围内，并返回一个布尔值表示是否在指定范围内。
     * @param year 要判断的年份
     * @param beginYear 起始年份
     * @param endYear 结束年份
     * @return 是否在指定范围内
     * @Author Junwei Hu
     * @Date 2024/12/07 15:20
     * @Version 1.0
     **/
    public static boolean checkYearInRange(String year, String beginYear, String endYear) {
        if (year == null || year.isEmpty()) {
            return true;
        }
        try {
            int y = Integer.parseInt(year);
            if (beginYear.equals("*") && endYear.equals("*")) {
                return true;
            }
            if (beginYear.equals("*")) {
                return y <= Integer.parseInt(endYear);
            }
            if (endYear.equals("*")) {
                return y >= Integer.parseInt(beginYear);
            }
            return y >= Integer.parseInt(beginYear) && y <= Integer.parseInt(endYear);
        } catch (NumberFormatException e) {
            System.err.println("年份格式错误: " + year);
            return false;
        }
    }

    /**
     * @Description TODO: 返回本地存储的正式DBLP文件块的数量
     * @return 正式DBLP文件块的数量
     * @Author Junwei Hu
     * @Date 2024/12/07 15:25
     * @Version 1.0
     **/
    public static int getXmlNum() {
        return dblpNames.size();
    }
}