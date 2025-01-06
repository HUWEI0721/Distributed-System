package Class;

import mypackage.HuffmanNode;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client3 {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private static final String FILE_NAME = "2153393-hw2-q1.dat";
    private static final String HUFFMAN_TREE_FILE = "huffman_tree.dat";
    private static final int MAX_VALUE = 1024 * 128; // 131,072

    public static void main(String[] args) {
        try {
            // 创建与服务器的连接
            Socket socket = new Socket(SERVER_ADDRESS, PORT);
            System.out.println("客户端3已连接到服务器。");

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush(); // 确保对象输出流的头信息发送
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // 请求读取权限
            // System.out.println("客户端3请求读取权限。");
            oos.writeObject("read");
            String response = (String) ois.readObject();
            if ("read_granted".equals(response)) {
                // System.out.println("客户端3获得读取权限，开始查找数据...");
                // 执行查找任务
                searchAndDelete();
            }

            // 请求写入权限
            // System.out.println("客户端3请求写入权限。");
            oos.writeObject("write");
            response = (String) ois.readObject();
            if ("write_granted".equals(response)) {
                // System.out.println("客户端3获得写入权限，开始删除和更新数据...");
                // 执行删除和写入操作
                performDeletionAndUpdate();

                // 通知服务器写入完成
                oos.writeObject("write_complete");
                // System.out.println("客户端3已完成写入操作，通知服务器。");
            }

            // 请求退出前，再次进行查找操作
            // System.out.println("客户端3请求再次读取权限以进行查找操作。");
            oos.writeObject("read");
            response = (String) ois.readObject();
            if ("read_granted".equals(response)) {
                // System.out.println("客户端3获得读取权限，开始再次查找数据...");
                // 再次执行查找任务
                searchAfterDeletion();
            }

            // 请求退出
            oos.writeObject("exit");
            // System.out.println("客户端3发送退出请求。");

            // 关闭连接
            ois.close();
            oos.close();
            socket.close();
            System.out.println("客户端3已关闭连接。");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 第一次查找并准备删除
    private static void searchAndDelete() {
        try (RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r")) {
            long[] header = readHeader(raf);
            long startPos = header[4];
            long length = header[5];

            System.out.println("客户端3读取的文件头信息：");
            System.out.println("Part C 起始位置：" + startPos + "，长度：" + length);

            // 检查是否越界
            long fileLength = raf.length();
            if (startPos + length > fileLength) {
                System.out.println("错误：要读取的数据超过文件长度。文件长度：" + fileLength);
                return;
            }

            // 加载霍夫曼树
            HuffmanNode root = loadHuffmanTree();
            if (root == null) {
                System.out.println("无法加载霍夫曼树，无法解压缩数据。");
                return;
            }

            raf.seek(startPos);

            long startTime = System.currentTimeMillis(); // 开始计时

            // 将压缩的数据读取到字节数组
            byte[] compressedData = new byte[(int) length];
            raf.readFully(compressedData);

            // System.out.println("客户端3开始解码压缩数据...");

            // 使用BitInputStream逐位读取数据
            BitInputStream bis = new BitInputStream(compressedData);

            int targetValue = 1024 * 64; // 65,536
            int closestValue = Integer.MAX_VALUE;
            List<Long> positions = new ArrayList<>(); // 存储指针位置（文件偏移量）

            HuffmanNode currentNode = root;
            long bitPosition = 0; // 当前位位置

            int bit;
            long decodedIntegers = 0;

            // 统计频率
            Map<Integer, Integer> frequencies = new HashMap<>();

            while ((bit = bis.readBit()) != -1) {
                bitPosition = bis.getBitPosition() - 1; // 获取当前位的位置

                if (bit == 0) {
                    currentNode = currentNode.left;
                } else {
                    currentNode = currentNode.right;
                }

                // 如果到达叶子节点，表示一个整数解码完成
                if (currentNode.left == null && currentNode.right == null) {
                    int value = currentNode.value;

                    frequencies.put(value, frequencies.getOrDefault(value, 0) + 1);

                    if (value >= targetValue) {
                        if (value < closestValue) {
                            closestValue = value;
                            positions.clear();
                            positions.add(startPos + bitPosition / 8);
                        } else if (value == closestValue) {
                            positions.add(startPos + bitPosition / 8);
                        }
                    }
                    currentNode = root;
                    decodedIntegers++;

//                    // 调试信息：已解码的整数数量
//                    if (decodedIntegers % 10_000_000 == 0) {
//                        System.out.println("已解码整数数量：" + decodedIntegers);
//                    }
                }
            }

            long endTime = System.currentTimeMillis(); // 结束计时

            // 输出结果
            if (closestValue != Integer.MAX_VALUE) {
                System.out.println("查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
                System.out.println("找到的整数值：" + closestValue + "共" + positions.size() + "个");
                System.out.println("对应的指针位置（近似）：");
                for (long pos : positions) {
                    System.out.println("    位置：" + pos);
                }
            } else {
                System.out.println("未找到大于等于目标值的整数。");
            }

            // 保存查找结果，供后续删除使用
            ClientData.setClosestValue(closestValue);
            ClientData.setPositions(positions);
            ClientData.setFrequencies(frequencies);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 执行删除和更新操作
    private static void performDeletionAndUpdate() {
        try {
            long startTime = System.currentTimeMillis(); // 开始计时

            // 获取频率表
            Map<Integer, Integer> frequencies = ClientData.getFrequencies();
            int valueToDelete = ClientData.getClosestValue();

            if (valueToDelete == Integer.MAX_VALUE) {
                System.out.println("没有需要删除的整数。");
                return;
            }

            int deletedCount = frequencies.remove(valueToDelete);
            System.out.println("客户端3共删除整数数量：" + deletedCount);

            // 重新构建霍夫曼树和编码
            HuffmanNode newRoot = buildHuffmanTreeFromFrequencies(frequencies);
            Map<Integer, HuffmanCode> huffmanCodes = new HashMap<>();
            generateCodes(newRoot, 0L, 0, huffmanCodes);

            // 保存新的霍夫曼树
            saveHuffmanTree(newRoot);

            // 编码数据并写入文件
            // System.out.println("客户端3开始重新编码并写入数据...");

            // 读取文件头信息
            RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "rw");
            long[] header = readHeader(raf);
            long startPos = header[4];
            long length = header[5];

            // 读取原始压缩数据
            raf.seek(startPos);
            byte[] compressedData = new byte[(int) length];
            raf.readFully(compressedData);

            // 准备解码和重新编码
            BitInputStream bis = new BitInputStream(compressedData);
            raf.setLength(startPos); // 截断文件，准备写入新的压缩数据
            raf.seek(startPos);

            BufferedOutputStream bos = new BufferedOutputStream(new RandomAccessFileOutputStream(raf));

            HuffmanNode oldRoot = loadOldHuffmanTree(); // 加载旧的霍夫曼树，用于解码
            if (oldRoot == null) {
                System.out.println("无法加载旧的霍夫曼树，无法重新编码。");
                return;
            }

            HuffmanNode currentNode = oldRoot;

            long bitBuffer = 0L;
            int bitBufferLength = 0;

            int bufferSize = 8192; // 输出缓冲区大小
            byte[] outputBuffer = new byte[bufferSize];
            int outputBufferPos = 0;

            int bit;
            long decodedIntegers = 0;
            long totalEncodedValues = 0;

            long deletedIntegers = 0; // 实际删除的整数数量

            // 添加调试信息，定期输出进度
            long progressInterval = 10_000_000; // 每1000万整数输出一次进度
            long nextProgress = progressInterval;

            while ((bit = bis.readBit()) != -1) {
                if (bit == 0) {
                    currentNode = currentNode.left;
                } else {
                    currentNode = currentNode.right;
                }

                if (currentNode.left == null && currentNode.right == null) {
                    int value = currentNode.value;

                    if (value != valueToDelete) {
                        HuffmanCode code = huffmanCodes.get(value);
                        if (code == null) {
                            System.out.println("警告：找不到整数 " + value + " 的霍夫曼编码！");
                            continue;
                        }

                        // 将编码加入到位缓冲区中
                        bitBuffer = (bitBuffer << code.codeLength) | code.codeBits;
                        bitBufferLength += code.codeLength;

                        // 当位缓冲区中有8位或以上时，提取字节写入输出缓冲区
                        while (bitBufferLength >= 8) {
                            bitBufferLength -= 8;
                            int byteToWrite = (int) ((bitBuffer >> bitBufferLength) & 0xFF);
                            outputBuffer[outputBufferPos++] = (byte) byteToWrite;

                            // 如果输出缓冲区已满，写入到文件
                            if (outputBufferPos == bufferSize) {
                                bos.write(outputBuffer, 0, outputBufferPos);
                                outputBufferPos = 0;
                            }
                        }

                        totalEncodedValues++;
                    } else {
                        deletedIntegers++;
                    }

                    currentNode = oldRoot; // 重置为旧的霍夫曼树根节点

                    decodedIntegers++;

                    // 输出进度信息
                    if (decodedIntegers >= nextProgress) {
                        // System.out.println("已处理整数数量：" + decodedIntegers);
                        nextProgress += progressInterval;
                    }
                }
            }

            // 处理剩余的位
            if (bitBufferLength > 0) {
                int byteToWrite = (int) ((bitBuffer << (8 - bitBufferLength)) & 0xFF);
                outputBuffer[outputBufferPos++] = (byte) byteToWrite;
            }

            // 将剩余的输出缓冲区写入文件
            if (outputBufferPos > 0) {
                bos.write(outputBuffer, 0, outputBufferPos);
            }

            bos.flush();
            bos.close(); // 不会关闭 raf

            long newCompressedLength = raf.getFilePointer() - startPos;
            raf.setLength(raf.getFilePointer()); // 设置文件长度

            // 更新文件头信息
            header[5] = newCompressedLength;
            writeHeader(raf, header);

            // 打印新的文件头信息
            System.out.println("更新后的文件头信息：");
            System.out.println("Part A 起始位置：" + header[0] + "，长度：" + header[1]);
            System.out.println("Part B 起始位置：" + header[2] + "，长度：" + header[3]);
            System.out.println("Part C 起始位置：" + header[4] + "，长度：" + header[5]);

            long endTime = System.currentTimeMillis(); // 结束计时
            System.out.println("重新编码并写入数据完成，耗时：" + (endTime - startTime) + " 毫秒。");
//            System.out.println("总共重新编码整数数量：" + totalEncodedValues);
//            System.out.println("总共解码整数数量：" + decodedIntegers);
//            System.out.println("总共删除整数数量：" + deletedIntegers);

            // 验证整数数量是否一致
            if (decodedIntegers != totalEncodedValues + deletedIntegers) {
                System.out.println("警告：解码的整数数量不等于重新编码的整数数量加上删除的整数数量！");
            } else {
                System.out.println("整数数量验证通过。");
            }

            raf.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 删除后再次查找下一个整数（无需删除）
    private static void searchAfterDeletion() {
        try (RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r")) {
            long[] header = readHeader(raf);
            long startPos = header[4];
            long length = header[5];

//            System.out.println("客户端3读取的文件头信息（删除后）：");
//            System.out.println("Part C 起始位置：" + startPos + "，长度：" + length);

            // 检查是否越界
            long fileLength = raf.length();
            if (startPos + length > fileLength) {
                System.out.println("错误：要读取的数据超过文件长度。文件长度：" + fileLength);
                return;
            }

            // 加载新的霍夫曼树
            HuffmanNode root = loadHuffmanTree();
            if (root == null) {
                System.out.println("无法加载新的霍夫曼树，无法解压缩数据。");
                return;
            }

            raf.seek(startPos);

            long startTime = System.currentTimeMillis(); // 开始计时

            // 将压缩的数据读取到字节数组
            byte[] compressedData = new byte[(int) length];
            raf.readFully(compressedData);

            // System.out.println("客户端3开始解码新的压缩数据...");

            // 使用BitInputStream逐位读取数据
            BitInputStream bis = new BitInputStream(compressedData);

            int targetValue = 1024 * 64; // 65,536
            int closestValue = Integer.MAX_VALUE;
            List<Long> positions = new ArrayList<>(); // 存储指针位置（文件偏移量）

            HuffmanNode currentNode = root;
            long bitPosition = 0; // 当前位位置

            int bit;
            long decodedIntegers = 0;

            Map<Integer, Integer> frequencies = new HashMap<>();

            while ((bit = bis.readBit()) != -1) {
                bitPosition = bis.getBitPosition() - 1; // 获取当前位的位置

                if (bit == 0) {
                    currentNode = currentNode.left;
                } else {
                    currentNode = currentNode.right;
                }

                // 如果到达叶子节点，表示一个整数解码完成
                if (currentNode.left == null && currentNode.right == null) {
                    int value = currentNode.value;

                    frequencies.put(value, frequencies.getOrDefault(value, 0) + 1);

                    if (value >= targetValue) {
                        if (value < closestValue) {
                            closestValue = value;
                            positions.clear();
                            positions.add(startPos + bitPosition / 8);
                        } else if (value == closestValue) {
                            positions.add(startPos + bitPosition / 8);
                        }
                    }
                    currentNode = root;
                    decodedIntegers++;

//                    // 调试信息：已解码的整数数量
//                    if (decodedIntegers % 10_000_000 == 0) {
//                        System.out.println("已解码整数数量：" + decodedIntegers);
//                    }
                }
            }

            long endTime = System.currentTimeMillis(); // 结束计时

            // 输出结果
            if (closestValue != Integer.MAX_VALUE) {
                System.out.println("查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
                System.out.println("找到的整数值：" + closestValue + " 共 " + positions.size() + " 个");
                System.out.println("对应的指针位置（近似）：");
                for (long pos : positions) {
                    System.out.println("    位置：" + pos);
                }
            } else {
                System.out.println("未找到大于等于目标值的整数。");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 构建霍夫曼树（从频率表）
    private static HuffmanNode buildHuffmanTreeFromFrequencies(Map<Integer, Integer> frequencies) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            HuffmanNode parent = new HuffmanNode(-1, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            pq.add(parent);
        }
        return pq.poll();
    }

    // 读取文件头信息
    private static long[] readHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        long[] header = new long[6];
        for (int i = 0; i < 6; i++) {
            header[i] = raf.readLong();
        }
        return header;
    }

    // 写入文件头信息
    private static void writeHeader(RandomAccessFile raf, long[] header) throws IOException {
        raf.seek(0);
        for (long value : header) {
            raf.writeLong(value);
        }
    }

    // 加载新的霍夫曼树
    private static HuffmanNode loadHuffmanTree() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HUFFMAN_TREE_FILE));
            HuffmanNode root = (HuffmanNode) ois.readObject();
            ois.close();
            return root;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 加载旧的霍夫曼树
    private static HuffmanNode loadOldHuffmanTree() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("old_" + HUFFMAN_TREE_FILE));
            HuffmanNode root = (HuffmanNode) ois.readObject();
            ois.close();
            return root;
        } catch (IOException | ClassNotFoundException e) {
            // 如果旧的霍夫曼树不存在，使用当前的霍夫曼树
            return loadHuffmanTree();
        }
    }

    // 保存霍夫曼树
    private static void saveHuffmanTree(HuffmanNode root) {
        try {
            // 备份旧的霍夫曼树
            File oldFile = new File(HUFFMAN_TREE_FILE);
            if (oldFile.exists()) {
                oldFile.renameTo(new File("old_" + HUFFMAN_TREE_FILE));
            }

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HUFFMAN_TREE_FILE));
            oos.writeObject(root);
            oos.close();
            System.out.println("客户端3已保存新的霍夫曼树。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 生成霍夫曼编码
    private static void generateCodes(HuffmanNode node, long codeBits, int codeLength, Map<Integer, HuffmanCode> huffmanCodes) {
        if (node != null) {
            if (node.value != -1) {
                huffmanCodes.put(node.value, new HuffmanCode(codeBits, codeLength));
            } else {
                generateCodes(node.left, (codeBits << 1), codeLength + 1, huffmanCodes);
                generateCodes(node.right, (codeBits << 1) | 1, codeLength + 1, huffmanCodes);
            }
        }
    }

    // 自定义的 OutputStream，不会在关闭时关闭 RandomAccessFile
    static class RandomAccessFileOutputStream extends OutputStream {
        private RandomAccessFile raf;

        public RandomAccessFileOutputStream(RandomAccessFile raf) {
            this.raf = raf;
        }

        @Override
        public void write(int b) throws IOException {
            raf.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            raf.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            raf.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            // RandomAccessFile 没有 flush 方法
        }

        @Override
        public void close() throws IOException {
            // 不关闭 RandomAccessFile
        }
    }

    // 辅助类
    static class HuffmanCode {
        long codeBits;
        int codeLength;

        HuffmanCode(long codeBits, int codeLength) {
            this.codeBits = codeBits;
            this.codeLength = codeLength;
        }
    }

    static class BitInputStream {
        private byte[] data;
        private int bytePos;
        private int bitPos;

        public BitInputStream(byte[] data) {
            this.data = data;
            this.bytePos = 0;
            this.bitPos = 0;
        }

        public int readBit() {
            if (bytePos >= data.length) {
                return -1; // 已到达数据末尾
            }
            int bit = (data[bytePos] >> (7 - bitPos)) & 1;
            bitPos++;
            if (bitPos == 8) {
                bitPos = 0;
                bytePos++;
            }
            return bit;
        }

        public long getBitPosition() {
            return (long) bytePos * 8 + bitPos;
        }

        public int getBytePosition() {
            return bytePos;
        }
    }

    // 辅助类，用于保存客户端的数据
    static class ClientData {
        private static int closestValue;
        private static List<Long> positions;
        private static Map<Integer, Integer> frequencies;

        public static int getClosestValue() {
            return closestValue;
        }

        public static void setClosestValue(int value) {
            closestValue = value;
        }

        public static List<Long> getPositions() {
            return positions;
        }

        public static void setPositions(List<Long> pos) {
            positions = pos;
        }

        public static Map<Integer, Integer> getFrequencies() {
            return frequencies;
        }

        public static void setFrequencies(Map<Integer, Integer> freq) {
            frequencies = freq;
        }
    }
}
