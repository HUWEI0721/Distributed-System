package localindex;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @Description TODO: 在本机上初始化XML片段文件对应的索引
 * @Author Junwei Hu
 * @Date 2024/12/07 16:45
 * @Version 1.0
 **/
public class IndexInitializer {
    /**
     * 正式DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/07 16:50
     */
    private static final String DBLP_Path = "/mnt/dblpXmls";

    /**
     * 备份DBLP XML文件存储路径
     * @Author Junwei Hu
     * @Date 2024/12/07 16:52
     */
    private static final String DBLP_Backup_Path = "/mnt/dblpBackupXmls";

    /**
     * @Description TODO: 初始化索引目录并为所有XML文件生成索引
     * @param args 命令行参数
     * @Author Junwei Hu
     * @Date 2024/12/07 16:55
     * @Version 1.0
     **/
    public static void main(String[] args) {
        // 建立一个字符串数组用来存四个XML文件的存放路径
        String[] xmlDirs = {
                DBLP_Path + "/8820",
                DBLP_Backup_Path + "/8820",
                DBLP_Path + "/8821",
                DBLP_Backup_Path + "/8821"
        };

        /* 针对两台虚拟机的各自的两个文件夹（正式和备份），共计四个位置创建本地索引文件夹 */
        for (String xmlDir : xmlDirs) {
            File outDir = new File(xmlDir + "/localIndex");
            if (!outDir.exists()) {
                boolean dirsCreated = outDir.mkdirs();
                if (dirsCreated) {
                    System.out.println("已创建索引目录: " + outDir.getAbsolutePath());
                } else {
                    System.err.println("无法创建索引目录: " + outDir.getAbsolutePath());
                    continue; // 继续处理下一个目录
                }
            } else {
                System.out.println("索引目录已存在: " + outDir.getAbsolutePath());
            }
        }

        /* 扫描四个XML文件存放目录下的所有XML文件，为每个XML文件建立对应的索引存放在刚刚创建的localIndex目录下 */
        for (String xmlDir : xmlDirs) {
            File dir = new File(xmlDir);
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("目录不存在或不是一个目录: " + xmlDir);
                continue; // 继续处理下一个目录
            }

            File[] xmlFiles = dir.listFiles((File d, String name) -> name.endsWith(".xml"));
            if (xmlFiles == null || xmlFiles.length == 0) {
                System.out.println("目录中没有找到XML文件: " + xmlDir);
                continue; // 继续处理下一个目录
            }

            for (File xmlFile : xmlFiles) {
                String indexOutputPath = xmlDir + "/localIndex";
                try {
                    LocalIndex.generateIndex(xmlFile.getAbsolutePath(), indexOutputPath);
                    System.out.println("文件: \"" + xmlFile.getAbsolutePath() + "\" 对应索引已建立...");
                } catch (Exception e) {
                    System.err.println("为文件 \"" + xmlFile.getAbsolutePath() + "\" 建立索引时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("目录: \"" + xmlDir + "\" 内的所有XML文件索引已建立完成\n");
        }
    }
}