import javax.xml.stream.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO: 初始化DBLP数据，包括切分XML文件和传输至服务器
 * @Author Junwei Hu
 * @Date 2024/12/07 10:15
 * @Version 1.0
 **/
public class Initialize {
    /**
     * 可访问的服务器IP地址列表
     * @Author Junwei Hu
     * @Date 2024/12/08 09:45
     */
    private final static String[] ipList = new String[]
            { "124.220.24.233", "110.40.167.8","1.116.120.226"};

    /**
     * 可访问的服务器端口列表
     * @Author Junwei Hu
     * @Date 2024/12/08 09:50
     */
    private final static int[] portList = new int[]
            {8820, 8821, 8822};

    /**
     * @Description TODO: 切分 DBLP.xml 文件为多个小文件
     * @throws Exception 如果文件操作或XML解析失败
     * @Author Junwei Hu
     * @Date 2024/12/07 10:20
     * @Version 1.0
     **/
    public static void SplitXml() throws Exception {
        // 定义需要切分的XML大类标签
        Set<String> set = new HashSet<>(Arrays.asList(
                "article", "book", "inproceedings", "proceedings",
                "incollection", "phdthesis", "mastersthesis", "www", "data"
        ));

        // 输入输出路径，可根据需要修改
        String inputFile = "/Users/hjw/Desktop/distributed-system/hw3/mnt/dblp.xml";
        String outputDir = "/Users/hjw/Desktop/distributed-system/hw3/mnt";

        // 检查并创建输出目录
        File dir = new File(outputDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("已创建输出目录: " + outputDir);
            } else {
                throw new IOException("无法创建输出目录: " + outputDir);
            }
        }

        // 创建XML输入工厂和流读取器
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        try (FileReader fileReader = new FileReader(inputFile)) {
            XMLStreamReader reader = inputFactory.createXMLStreamReader(fileReader);

            // 当前使用的XMLStreamWriter
            XMLStreamWriter currentWriter = null;
            // 存储24个XMLStreamWriter
            List<XMLStreamWriter> writerList = new ArrayList<>();
            // 用于随机分配大类标签到不同文件
            Random random = new Random();

            // 开始读取XML文档
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();
                        if ("dblp".equals(localName)) {
                            System.out.println("正在切分DBLP.xml文件");
                            // 创建24个切分文件的XMLStreamWriter
                            for(int i = 0; i < 24; i++) {
                                String currentFile = outputDir + "/dblp" + i + ".xml";
                                File file = new File(currentFile);

                                // 检查并创建文件
                                if (!file.exists()) {
                                    if (file.createNewFile()) {
                                        System.out.println("已创建文件: " + currentFile);
                                    } else {
                                        throw new IOException("无法创建文件: " + currentFile);
                                    }
                                }

                                // 创建XMLStreamWriter
                                XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
                                XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileWriter(file));
                                writerList.add(writer);
                                // 写入XML文档的开始部分
                                writer.writeStartDocument();
                                writer.writeStartElement("dblp");
                            }
                            // 设置当前Writer为第一个
                            currentWriter = writerList.get(0);
                        }
                        else if(set.contains(localName)) {
                            // 随机选择一个writer来写入大类标签
                            currentWriter = writerList.get(random.nextInt(writerList.size()));
                            currentWriter.writeStartElement(localName);
                        }
                        else {
                            // 写入其他元素的开始标签及属性
                            if (currentWriter != null) {
                                currentWriter.writeStartElement(localName);
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    currentWriter.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                                }
                            }
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        // 写入文本内容
                        if (currentWriter != null) {
                            currentWriter.writeCharacters(reader.getText());
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        String endLocalName = reader.getLocalName();
                        if ("dblp".equals(endLocalName)) {
                            // 关闭所有writer
                            for(XMLStreamWriter writer : writerList) {
                                writer.writeEndElement();
                                writer.writeEndDocument();
                                writer.close();
                            }
                            writerList.clear();
                            currentWriter = null;
                        }
                        else {
                            // 关闭当前元素的结束标签
                            if (currentWriter != null) {
                                currentWriter.writeEndElement();
                            }
                        }
                        break;
                }
            }
            // 关闭XMLStreamReader
            reader.close();
            System.out.println("DBLP.xml文件切分完成");
        }
    }

    /**
     * @Description TODO: 将切分好的DBLP.xml文件传输给指定服务器
     * @param fileName 文件名
     * @param ipSelected 目标服务器的IP地址
     * @param portSelected 目标服务器的端口号
     * @param isBackup 是否为备份文件
     * @throws Exception 如果文件传输失败
     * @Author Junwei Hu
     * @Date 2024/12/07 11:30
     * @Version 1.0
     **/
    public static void sendXml(String fileName, String ipSelected, int portSelected, boolean isBackup) throws Exception {
        // 文件路径，可根据需要修改
        String filePath = "/Users/hjw/Desktop/distributed-system/hw3/mnt/" + fileName;

        // 使用try-with-resources确保资源被正确关闭
        try (Socket socket = new Socket(ipSelected, portSelected);
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {

            // 向Server传递文件名称
            outputStream.writeUTF(fileName);
            outputStream.flush();
            System.out.println("发送文件名: " + fileName);

            // 向Server传递是否为备份文件的信息
            String backupTag = isBackup ? "isBackup" : "notBackup";
            outputStream.writeUTF(backupTag);
            outputStream.flush();
            System.out.println("发送备份标签: " + backupTag);

            // 读取文件并将文件内容写入输出流
            byte[] buffer = new byte[1024];
            int bytesRead;
            System.out.println("开始发送文件内容...");
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            System.out.println("文件发送完成.");

        } catch (IOException e) {
            System.err.println("传输文件失败: " + fileName + " 到 " + ipSelected + ":" + portSelected);
            throw e; // 重新抛出异常以便上层处理
        }
    }

    /**
     * @Description TODO: 执行初始化流程，包括切分XML文件和将XML发送给各个服务器
     * @Author Junwei Hu
     * @Date 2024/12/09 10:00
     * @Version 1.0
     **/
    public static void initDBLP() {
        try {
            // 切分XML文件
            SplitXml();

            // 使用线程池管理发送任务，固定线程池大小为6
            ExecutorService executorService = Executors.newFixedThreadPool(6);

            for (int i = 0; i < 24; i++) {
                String fileName = "dblp" + i + ".xml";
                System.out.println("准备发送文件: " + fileName);

                // 计算主服务器的IP和端口
                int ipSelect = (i / 2) % ipList.length;
                int portSelect = i % portList.length;

                // 计算备份服务器的IP和端口
                int ipBackupSelect = (portSelect == 0) ? ipSelect : (ipSelect + 1) % ipList.length;
                int portBackupSelect = (portSelect == 0) ? 1 : 0;

                // 获取主服务器的IP和端口
                String primaryIP = ipList[ipSelect];
                int primaryPort = portList[portSelect];

                // 获取备份服务器的IP和端口
                String backupIP = ipList[ipBackupSelect];
                int backupPort = portList[portBackupSelect];

                // 提交主服务器发送任务
                executorService.submit(() -> {
                    try {
                        sendXml(fileName, primaryIP, primaryPort, false);
                        System.out.println(fileName + " 已发送至主服务器：" + primaryIP + ":" + primaryPort);
                    } catch (Exception e) {
                        System.err.println("发送文件失败: " + fileName + " 到 主服务器 " + primaryIP + ":" + primaryPort);
                        e.printStackTrace();
                    }
                });

                // 提交备份服务器发送任务
                executorService.submit(() -> {
                    try {
                        sendXml(fileName, backupIP, backupPort, true);
                        System.out.println(fileName + " 已发送至备份服务器：" + backupIP + ":" + backupPort);
                    } catch (Exception e) {
                        System.err.println("发送备份文件失败: " + fileName + " 到 备份服务器 " + backupIP + ":" + backupPort);
                        e.printStackTrace();
                    }
                });
            }

            // 关闭线程池，等待所有任务完成
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                System.err.println("发送任务超时！");
                executorService.shutdownNow(); // 强制关闭
            } else {
                System.out.println("所有文件发送完成");
            }
        } catch (Exception e) {
            System.err.println("初始化过程发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}