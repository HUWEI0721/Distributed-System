package localindex;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @Description TODO: 本地索引相关类，用于生成索引以及利用索引进行查询
 * @Author Junwei Hu
 * @Date 2024/12/07 16:30
 * @Version 1.0
 */
public class LocalIndex {

    /**
     * 哈希桶的数量
     * @Author Junwei Hu
     * @Date 2024/12/07 16:35
     */
    public static final int LIST_NUM = 101;

    /**
     * 根据两个字符串计算其对应的范围在[0]-[range-1]的哈希值
     *
     * @param A     第一个字符串
     * @param B     第二个字符串
     * @param range 哈希值结果范围
     * @return 哈希值
     * @Author Junwei Hu
     * @Date 2024/12/07 16:40
     * @Version 1.0
     */
    public static int hashByStrings(String A, String B, int range) {
        StringBuilder sb = new StringBuilder();
        sb.append(A).append(B);
        return Math.abs(sb.toString().hashCode()) % range;
    }

    /**
     * 根据一个字符串计算其对应的范围在[0]-[range-1]的哈希值
     *
     * @param author 作者名
     * @param range  哈希值结果范围
     * @return 哈希值
     * @Author Junwei Hu
     * @Date 2024/12/07 16:45
     * @Version 1.0
     */
    public static int hashByAuthor(String author, int range) {
        return Math.abs(author.hashCode()) % range;
    }

    /**
     * 将对象序列化并写入磁盘
     *
     * @param dataSet 需要序列化的DataSet对象
     * @param filePath 文件路径
     * @Author Junwei Hu
     * @Date 2024/12/07 16:50
     * @Version 1.0
     */
    public static void writeObjectToDisk(DataSet dataSet, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(dataSet);
        } catch (IOException e) {
            System.err.println("写入对象到磁盘时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从磁盘反序列化对象
     *
     * @param filePath 文件路径
     * @return 反序列化后的DataSet对象
     * @Author Junwei Hu
     * @Date 2024/12/07 16:55
     * @Version 1.0
     */
    public static DataSet readObjectFromDisk(String filePath) {
        DataSet dataSet = null;
        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            dataSet = (DataSet) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("从磁盘读取对象时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return dataSet;
    }

    /**
     * 更新磁盘上的对象
     *
     * @param dataSet 需要更新的DataSet对象
     * @param filePath 文件路径
     * @Author Junwei Hu
     * @Date 2024/12/07 17:00
     * @Version 1.0
     */
    public static void updateObjectOnDisk(DataSet dataSet, String filePath) {
        byte[] data = toByteArray(dataSet);
        try (FileChannel fileChannel = new RandomAccessFile(filePath, "rw").getChannel()) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
            mappedByteBuffer.put(data);
        } catch (IOException e) {
            System.err.println("更新磁盘上的对象时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将对象转换为字节数组
     *
     * @param object 需要转换的对象
     * @return 字节数组
     * @Author Junwei Hu
     * @Date 2024/12/07 17:05
     * @Version 1.0
     */
    public static byte[] toByteArray(Object object) {
        byte[] bytes = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            bytes = baos.toByteArray();
        } catch (IOException e) {
            System.err.println("将对象转换为字节数组时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * 用于生成索引文件
     *
     * @param inputXMLFilePath 需要建立索引文件的XML文件的完整路径，示例："C:\\Users\\Junwei Hu\\Documents\\output3.xml"
     * @param outputDirectory   索引文件的保存目录，程序会在此目录下根据xml文件名建立对应目录来存储一系列索引文件，示例："D:\\Idea_Project\\DBLPTest\\outxml"
     * @Author Junwei Hu
     * @Date 2024/12/07 17:10
     * @Version 1.0
     */
    public static void generateIndex(String inputXMLFilePath, String outputDirectory) {
        // 创建 XMLInputFactory 对象
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // 创建 XMLStreamReader 对象
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new FileInputStream(inputXMLFilePath));
        } catch (FileNotFoundException e) {
            System.err.println("找不到文件: " + inputXMLFilePath);
            e.printStackTrace();
            return;
        } catch (XMLStreamException e) {
            System.err.println("创建XMLStreamReader时发生错误: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 创建一个字符串集合，包含DBLP数据库中所有可能的文章类型
        Set<String> typeSet = new HashSet<>(Arrays.asList(
                "article",
                "inproceedings",
                "proceedings",
                "book",
                "incollection",
                "phdthesis",
                "mastersthesis",
                "person",
                "data",
                "www"
        ));

        // 索引信息相关变量定义
        List<String> authors = new ArrayList<>();
        String year = "null";

        // 存储哈希桶的所有链表
        DataSet[] dataSets = new DataSet[LIST_NUM];
        for (int i = 0; i < dataSets.length; i++) {
            dataSets[i] = new DataSet();
        }

        int processedCount = 0;
        long start = System.nanoTime();

        // 读取解析XML文件
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String elementName = reader.getLocalName();
                        if (typeSet.contains(elementName)) {
                            // 清空存储的信息
                            authors.clear();
                            year = "null";
                        } else if (elementName.equals("author")) {
                            // 处理 author 节点
                            authors.add(reader.getElementText().trim());
                        } else if (elementName.equals("year")) {
                            // 处理 year 节点
                            year = reader.getElementText().trim();
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        elementName = reader.getLocalName();
                        if (typeSet.contains(elementName)) {
                            processedCount++;
                            // 根据author和year信息计算哈希值
                            for (String author : authors) {
                                int hashIndex = hashByAuthor(author, LIST_NUM);
                                dataSets[hashIndex].add(author, year);
                            }
                            // 清空存储的信息
                            authors.clear();
                            year = "null";
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            System.err.println("解析XML文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭 XMLStreamReader 对象
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (XMLStreamException e) {
                System.err.println("关闭XMLStreamReader时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 获取输入XML文件的前缀并据此作为新建的目录名称
        File file = new File(inputXMLFilePath);
        String realIndexDirectory = outputDirectory + "/" + file.getName().replaceFirst("[.][^.]+$", "");
        // 建立索引文件夹
        File indexDir = new File(realIndexDirectory);
        if (!indexDir.exists()) {
            boolean dirsCreated = indexDir.mkdirs();
            if (dirsCreated) {
                System.out.println("已创建索引目录: " + indexDir.getAbsolutePath());
            } else {
                System.err.println("无法创建索引目录: " + indexDir.getAbsolutePath());
                return;
            }
        } else {
            System.out.println("索引目录已存在: " + indexDir.getAbsolutePath());
        }

        // 写入所有链表文件
        for (int i = 0; i < dataSets.length; i++) {
            String outputFilePath = realIndexDirectory + "/DataSet-" + i + ".ser";
            writeObjectToDisk(dataSets[i], outputFilePath);
        }

        long elapsed = System.nanoTime() - start;
        double elapsedSeconds = (double) elapsed / 1_000_000_000.0;
        System.out.println("索引生成完成，总处理块数: " + processedCount);
        System.out.printf("总耗时: %.2f 秒\n", elapsedSeconds);
    }

    /**
     * 传入索引文件所在的实际目录，根据author和year信息进行查询，
     * 若beginYear和endYear均为“*”则仅根据author进行查询
     *
     * @param indexDirectory 索引文件的保存目录（索引文件实际的保存目录），例如"D:\\Idea_Project\\DBLPTest\\outxml\\output3"
     * @param author         作者名
     * @param beginYear      起始年份，为“*”时表示无限制
     * @param endYear        结束年份，为“*”时表示无限制
     * @return 论文频次总数
     * @Author Junwei Hu
     * @Date 2024/12/07 17:15
     * @Version 1.0
     */
    public static int queryByIndex(String indexDirectory, String author, String beginYear, String endYear) {
        int count = 0;
        int hashIndex = hashByAuthor(author, LIST_NUM);
        String filePath = indexDirectory + "/DataSet-" + hashIndex + ".ser";

        // 从磁盘上加载数据集
        DataSet dataSet = readObjectFromDisk(filePath);
        if (dataSet == null) {
            System.err.println("未能加载数据集: " + filePath);
            return 0;
        }

        // 进行查询
        if ("*".equals(beginYear) && "*".equals(endYear)) {
            count = dataSet.countByAuthor(author);
        } else {
            count = dataSet.countByAuthorAndYear(author, beginYear, endYear);
        }

        return count;
    }

    /**
     * Main函数，示范了这个类的两个主要函数怎么使用
     *
     * @param args 命令行参数
     * @Author Junwei Hu
     * @Date 2024/12/07 17:20
     * @Version 1.0
     */
    public static void main(String[] args) {
        // 生成索引
        String inputXMLFilePath = "";
        String outputDirectory = "";
        generateIndex(inputXMLFilePath, outputDirectory);

        // 用索引查询
        String realIndexDirectory = "";
        int queryResult = queryByIndex(realIndexDirectory, "Yuval Yarom", "2017", "2022");
        System.out.println("查询结果: " + queryResult);
    }
}