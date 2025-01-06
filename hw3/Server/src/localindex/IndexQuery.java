package localindex;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;

/**
 * @Description TODO: 用以根据索引文件查询论文频次
 * @Author Junwei Hu
 * @Date 2024/12/07 15:30
 * @Version 1.0
 **/
public class IndexQuery {
    /**
     * 正式DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/07 15:35
     */
    private static final String DBLP_Path = "/mnt/dblpXmls";

    /**
     * 备份DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/07 15:37
     */
    private static final String DBLP_Backup_Path = "/mnt/dblpBackupXmls";

    /**
     * 对LocalIndex类中的queryByIndex函数进行了封装
     * 自动根据传入的参数生成索引文件所在目录，并调用LocalIndex类中的queryByIndex函数得到结果
     *
     * @param is_copy       要查询的xml片段文件是否为备份
     * @param vMachinePort  虚拟机对应端口
     * @param xmlFileName   要查询的xml片段文件的文件名
     * @param author        要查询的author
     * @param beginYear     查询限定的起始年份
     * @param endYear       查询限定的终止年份
     * @return 计数
     * @Author Junwei Hu
     * @Date 2024/12/07 15:40
     * @Version 1.0
     **/
    public static int queryByOneBlockIndex(boolean is_copy, int vMachinePort, String xmlFileName, String author, String beginYear, String endYear) {
        String indexDir = is_copy ? DBLP_Backup_Path : DBLP_Path;
        indexDir += "/" + vMachinePort + "/localIndex";

        // 提取文件名前缀（去除扩展名）
        int dotIndex = xmlFileName.lastIndexOf('.');
        if (dotIndex == -1) {
            System.err.println("文件名不包含扩展名: " + xmlFileName);
            return 0;
        }
        String prefix = xmlFileName.substring(0, dotIndex);
        indexDir += "/" + prefix;

        try {
            int result = LocalIndex.queryByIndex(indexDir, author, beginYear, endYear);
            return result;
        } catch (Exception e) {
            System.err.println("查询索引时发生错误，索引目录: " + indexDir + ", 错误信息: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 对LocalIndex类中的queryByIndex函数进行了封装
     * 对于本虚拟机对应文件夹（备份或非备份），调用本类中的queryByOneBlockIndex函数得到结果
     *
     * @param is_copy       要查询的xml片段文件是否为备份
     * @param vMachinePort  虚拟机对应端口, 示例：8820
     * @param author        要查询的author
     * @param beginYear     查询限定的起始年份
     * @param endYear       查询限定的终止年份
     * @return 计数
     * @Author Junwei Hu
     * @Date 2024/12/07 15:45
     * @Version 1.0
     **/
    public static String queryByIndex(boolean is_copy, int vMachinePort, String author, String beginYear, String endYear) {
        String xmlDir = is_copy ? DBLP_Backup_Path : DBLP_Path;
        xmlDir += "/" + vMachinePort;

        File dir = new File(xmlDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在或不是一个目录: " + xmlDir);
            return "0";
        }

        File[] xmlFiles = dir.listFiles((File d, String name) -> name.endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            System.out.println("目录中没有找到XML文件: " + xmlDir);
            return "0";
        }

        int num = 0;
        for (File xmlFile : xmlFiles) {
            int result = queryByOneBlockIndex(is_copy, vMachinePort, xmlFile.getName(), author, beginYear, endYear);
            num += result;
        }
        return String.valueOf(num);
    }

    /**
     * queryByIndex函数使用示例代码
     *
     * @param args 命令行参数
     * @Author Junwei Hu
     * @Date 2024/12/07 15:50
     * @Version 1.0
     **/
    public static void main(String[] args) {
        String result = queryByIndex(false, 8820, "Yuval Yarom", "2010", "2022");
        System.out.println(result);
    }
}