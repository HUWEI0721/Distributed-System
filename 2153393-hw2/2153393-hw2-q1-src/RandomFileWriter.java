package Class;

import mypackage.HuffmanNode;

import java.io.*;
import java.util.*;

public class RandomFileWriter {
    private static final int NUM_INTEGERS = 1024 * 1024 * 128; // 134,217,728 个整数
    private static final int MAX_VALUE = 1024 * 128; // 131,072
    private static final int HEADER_SIZE = 48; // 6 个 long 类型，每个 8 字节
    private static final String FILE_NAME = "2153393-hw2-q1.dat";
    private static final String HUFFMAN_TREE_FILE = "huffman_tree.dat";

    public static void main(String[] args) {
        try {
            long totalStartTime = System.currentTimeMillis(); // 开始计时

            // 任务1：生成随机整数并写入文件
            long unsortedStartPos = HEADER_SIZE;
            long unsortedLength = writeRandomIntegers(unsortedStartPos);

            // 任务2：使用归并排序对整数排序并写入文件
            long sortedStartPos = unsortedStartPos + unsortedLength;
            long sortedLength = sortAndWriteIntegers(unsortedStartPos, unsortedLength, sortedStartPos);

            // 任务3：使用霍夫曼编码压缩排序后的整数并写入文件
            long compressedStartPos = sortedStartPos + sortedLength;
            long compressedLength = compressAndWriteIntegers(sortedStartPos, sortedLength, compressedStartPos);

            // 写入头信息
            writeHeader(unsortedStartPos, unsortedLength, sortedStartPos, sortedLength, compressedStartPos, compressedLength);
            // 验证头信息
            RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");
            raf.seek(0);
            for (int i = 0; i < 6; i++) {
                long headerValue = raf.readLong();
                System.out.println("验证头信息第 " + (i + 1) + " 项：" + headerValue);
            }
            raf.close();

            long totalEndTime = System.currentTimeMillis(); // 结束计时
            System.out.println("文件“" + FILE_NAME + "”生成完毕，耗时：" + (totalEndTime - totalStartTime) + " 毫秒。");

            // 打印文件头信息
            printHeader();

            // 执行任务2
            sequentialSearchInPartA(unsortedStartPos, unsortedLength);

            // 执行任务3
            randomAccessSearchInPartB(sortedStartPos, sortedLength);

            // 执行任务4
            randomAccessSearchInPartC(compressedStartPos, compressedLength);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 任务1：生成随机整数并写入文件
    private static long writeRandomIntegers(long startPos) throws IOException {
        Random random = new Random(237);

        FileOutputStream fos = new FileOutputStream(FILE_NAME);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        // 写入头信息的占位符
        bos.write(new byte[HEADER_SIZE]);

        int batchSize = 4_000_000; // 增加批处理大小，以减少I/O操作次数
        byte[] buffer = new byte[batchSize * 4];

        long totalIntegersWritten = 0;
        long totalBatches = NUM_INTEGERS / batchSize;
        int remainingIntegers = NUM_INTEGERS % batchSize;

        // 写入完整批次
        for (int i = 0; i < totalBatches; i++) {
            int offset = 0;
            for (int j = 0; j < batchSize; j++) {
                int value = random.nextInt(MAX_VALUE) + 1;
                buffer[offset++] = (byte) (value >>> 24);
                buffer[offset++] = (byte) (value >>> 16);
                buffer[offset++] = (byte) (value >>> 8);
                buffer[offset++] = (byte) value;
            }
            bos.write(buffer, 0, buffer.length);
            totalIntegersWritten += batchSize;
        }

        // 写入剩余的整数
        if (remainingIntegers > 0) {
            buffer = new byte[remainingIntegers * 4];
            int offset = 0;
            for (int j = 0; j < remainingIntegers; j++) {
                int value = random.nextInt(MAX_VALUE) + 1;
                buffer[offset++] = (byte) (value >>> 24);
                buffer[offset++] = (byte) (value >>> 16);
                buffer[offset++] = (byte) (value >>> 8);
                buffer[offset++] = (byte) value;
            }
            bos.write(buffer, 0, buffer.length);
            totalIntegersWritten += remainingIntegers;
        }

        bos.flush();
        bos.close();

        return totalIntegersWritten * 4;
    }

    // 任务2：使用归并排序对整数排序并写入文件
    private static long sortAndWriteIntegers(long readStartPos, long readLength, long writeStartPos) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "rw");

        // 读取所有整数到内存
        System.out.println("任务2：开始读取未排序的整数。");
        raf.seek(readStartPos);
        int numIntegers = (int) (readLength / 4);
        int[] integers = new int[numIntegers];
        byte[] buffer = new byte[numIntegers * 4];
        raf.readFully(buffer);

        for (int i = 0, offset = 0; i < numIntegers; i++, offset += 4) {
            integers[i] = ((buffer[offset] & 0xFF) << 24) |
                    ((buffer[offset + 1] & 0xFF) << 16) |
                    ((buffer[offset + 2] & 0xFF) << 8) |
                    (buffer[offset + 3] & 0xFF);
        }

        // 使用归并排序
        System.out.println("任务2：开始排序。");
        mergeSort(integers, 0, integers.length - 1);
        System.out.println("任务2：排序完成。");

        // 写入排序后的整数到文件
        System.out.println("任务2：开始写入排序后的整数。");
        raf.seek(writeStartPos);
        byte[] sortedBuffer = new byte[numIntegers * 4];
        for (int i = 0, offset = 0; i < numIntegers; i++, offset += 4) {
            int value = integers[i];
            sortedBuffer[offset] = (byte) (value >>> 24);
            sortedBuffer[offset + 1] = (byte) (value >>> 16);
            sortedBuffer[offset + 2] = (byte) (value >>> 8);
            sortedBuffer[offset + 3] = (byte) value;
        }
        raf.write(sortedBuffer);
        raf.close();
        return readLength; // 长度保持不变
    }

    // 归并排序实现
    private static void mergeSort(int[] arr, int left, int right) {
        // 为了提高效率，设置一个小数组阈值，使用插入排序
        if (right - left <= 16) {
            insertionSort(arr, left, right);
            return;
        }
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);
            if (arr[mid] <= arr[mid + 1]) {
                // 如果已经有序，跳过合并
                return;
            }
            merge(arr, left, mid, right);
        }
    }

    // 插入排序实现（用于小数组优化）
    private static void insertionSort(int[] arr, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            int current = arr[i];
            int j = i - 1;
            while (j >= left && arr[j] > current) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = current;
        }
    }

    // 合并函数
    private static void merge(int[] arr, int left, int mid, int right) {
        // 复制左半部分到辅助数组
        int[] leftArray = Arrays.copyOfRange(arr, left, mid + 1);

        int l = 0; // 左半部分的指针
        int r = mid + 1; // 右半部分的指针
        int k = left;

        while (l < leftArray.length && r <= right) {
            if (leftArray[l] <= arr[r]) {
                arr[k++] = leftArray[l++];
            } else {
                arr[k++] = arr[r++];
            }
        }
        // 将剩余的左半部分元素复制回原数组
        while (l < leftArray.length) {
            arr[k++] = leftArray[l++];
        }
        // 右半部分的剩余元素已经在原数组中，无需处理
    }

    private static final int INDEX_INTERVAL = 1000000;

    // 任务3：压缩并写入文件
    private static long compressAndWriteIntegers(long readStartPos, long readLength, long writeStartPos) throws IOException {
        RandomAccessFile rafRead = new RandomAccessFile(FILE_NAME, "r");
        RandomAccessFile rafWrite = new RandomAccessFile(FILE_NAME, "rw");

        // 读取排序后的整数并构建频率表
        System.out.println("任务3：开始构建频率表。");
        int[] frequencies = new int[MAX_VALUE + 1]; // 索引从1到MAX_VALUE

        long totalIntegers = readLength / 4;
        long readIntegers = 0;
        int batchSize = 1_000_000;
        byte[] buffer = new byte[batchSize * 4];
        rafRead.seek(readStartPos);

        while (readIntegers < totalIntegers) {
            int integersToRead = (int) Math.min(batchSize, totalIntegers - readIntegers);
            rafRead.readFully(buffer, 0, integersToRead * 4);

            for (int i = 0, offset = 0; i < integersToRead; i++, offset += 4) {
                int value = ((buffer[offset] & 0xFF) << 24) |
                        ((buffer[offset + 1] & 0xFF) << 16) |
                        ((buffer[offset + 2] & 0xFF) << 8) |
                        (buffer[offset + 3] & 0xFF);
                frequencies[value]++;
            }
            readIntegers += integersToRead;
        }

        // 构建霍夫曼树
        System.out.println("任务3：开始构建霍夫曼树。");
        HuffmanNode root = buildHuffmanTree(frequencies);

        // 生成霍夫曼编码
        System.out.println("任务3：开始生成霍夫曼编码。");
        Map<Integer, HuffmanCode> huffmanCodes = new HashMap<>();
        generateCodes(root, 0L, 0, huffmanCodes);

        // 保存霍夫曼树到文件，以便解码
        saveHuffmanTree(root);

        // 编码数据并写入文件
        System.out.println("任务3：开始编码数据并写入文件。");
        rafRead.seek(readStartPos); // 重置读取位置
        rafWrite.seek(writeStartPos); // 设置写入位置

        BufferedOutputStream bos = new BufferedOutputStream(new RandomAccessFileOutputStream(rafWrite));

        readIntegers = 0;
        int totalProgress = (int) (totalIntegers / batchSize);
        int progressCounter = 0;

        int bufferSize = 8192; // 缓冲区大小，可以根据需要调整
        byte[] outputBuffer = new byte[bufferSize];
        int outputBufferPos = 0;

        long bitBuffer = 0L;
        int bitBufferLength = 0;

        while (readIntegers < totalIntegers) {
            int integersToRead = (int) Math.min(batchSize, totalIntegers - readIntegers);
            rafRead.readFully(buffer, 0, integersToRead * 4);

            for (int i = 0, offset = 0; i < integersToRead; i++, offset += 4) {
                int value = ((buffer[offset] & 0xFF) << 24) |
                        ((buffer[offset + 1] & 0xFF) << 16) |
                        ((buffer[offset + 2] & 0xFF) << 8) |
                        (buffer[offset + 3] & 0xFF);
                HuffmanCode code = huffmanCodes.get(value);

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
            }

            readIntegers += integersToRead;
            progressCounter++;
            if (progressCounter % 10 == 0 || readIntegers == totalIntegers) {
                System.out.println("任务3：已编码并写入 " + readIntegers + " / " + totalIntegers + " 个整数。");
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
        bos.close(); // 不会关闭 rafWrite

        long compressedLength = rafWrite.getFilePointer() - writeStartPos;

        rafRead.close();
        rafWrite.close();

        return compressedLength;
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

    // 新的HuffmanCode类
    static class HuffmanCode {
        long codeBits;
        int codeLength;

        HuffmanCode(long codeBits, int codeLength) {
            this.codeBits = codeBits;
            this.codeLength = codeLength;
        }
    }

    // 构建霍夫曼树
    private static HuffmanNode buildHuffmanTree(int[] frequencies) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (int i = 1; i <= MAX_VALUE; i++) {
            if (frequencies[i] > 0) {
                pq.add(new HuffmanNode(i, frequencies[i]));
            }
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

    // 修改后的generateCodes方法
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

    // 写入头信息
    private static void writeHeader(long unsortedStartPos, long unsortedLength,
                                    long sortedStartPos, long sortedLength,
                                    long compressedStartPos, long compressedLength) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "rw");
        raf.seek(0);
        raf.writeLong(unsortedStartPos);
        raf.writeLong(unsortedLength);
        raf.writeLong(sortedStartPos);
        raf.writeLong(sortedLength);
        raf.writeLong(compressedStartPos);
        raf.writeLong(compressedLength);
        raf.close();
        System.out.println("头信息写入完毕。");
    }

    // 打印文件头信息
    private static void printHeader() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");
        raf.seek(0);
        long unsortedStartPos = raf.readLong();
        long unsortedLength = raf.readLong();
        long sortedStartPos = raf.readLong();
        long sortedLength = raf.readLong();
        long compressedStartPos = raf.readLong();
        long compressedLength = raf.readLong();
        raf.close();

        System.out.println("文件头信息：");
        System.out.println("(a) 无排序随机整数的字节数组起始位置和长度：");
        System.out.println("    起始位置：" + unsortedStartPos + "，长度：" + unsortedLength);
        System.out.println("(b) 排序随机整数的字节数组起始位置和长度：");
        System.out.println("    起始位置：" + sortedStartPos + "，长度：" + sortedLength);
        System.out.println("(c) 压缩后的随机整数的字节数组起始位置和长度：");
        System.out.println("    起始位置：" + compressedStartPos + "，长度：" + compressedLength);
    }

    // 任务2：顺序访问查找(a)部分
    private static void sequentialSearchInPartA(long startPos, long length) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");
        raf.seek(startPos);

        long startTime = System.currentTimeMillis(); // 开始计时

        int batchSize = 1_000_000; // 每次读取100万整数
        byte[] buffer = new byte[batchSize * 4];
        int targetValue = 1024 * 64; // 65,536
        int closestValue = Integer.MAX_VALUE;
        List<Long> positions = new ArrayList<>();

        long totalIntegers = length / 4;
        long readIntegers = 0;
        long position = startPos;

        while (readIntegers < totalIntegers) {
            int integersToRead = (int) Math.min(batchSize, totalIntegers - readIntegers);
            raf.readFully(buffer, 0, integersToRead * 4);
            for (int i = 0; i < integersToRead; i++) {
                int offset = i * 4;
                int value = ((buffer[offset] & 0xFF) << 24) |
                        ((buffer[offset + 1] & 0xFF) << 16) |
                        ((buffer[offset + 2] & 0xFF) << 8) |
                        (buffer[offset + 3] & 0xFF);

                if (value >= targetValue) {
                    if (value < closestValue) {
                        closestValue = value;
                        positions.clear();
                        positions.add(position + (readIntegers + i) * 4);
                    } else if (value == closestValue) {
                        positions.add(position + (readIntegers + i) * 4);
                    }
                }
            }
            readIntegers += integersToRead;
        }
        long endTime = System.currentTimeMillis(); // 结束计时
        raf.close();

        System.out.println("任务2：顺序访问查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
        System.out.println("找到的整数值：" + closestValue);
        System.out.println("对应的指针位置：");
        for (long pos : positions) {
            System.out.println("    位置：" + pos);
        }
    }

    // 任务3：随机访问查找(b)部分
    private static void randomAccessSearchInPartB(long startPos, long length) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");

        long startTime = System.currentTimeMillis(); // 开始计时

        int targetValue = 1024 * 64; // 65,536
        long totalIntegers = length / 4;

        // 二分查找第一个大于等于目标值的位置
        long left = 0;
        long right = totalIntegers - 1;
        long foundIndex = -1;

        while (left <= right) {
            long mid = (left + right) / 2;
            raf.seek(startPos + mid * 4);
            int value = raf.readInt();
            if (value >= targetValue) {
                foundIndex = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        if (foundIndex != -1) {
            raf.seek(startPos + foundIndex * 4);
            int closestValue = raf.readInt();
            List<Long> positions = new ArrayList<>();
            positions.add(startPos + foundIndex * 4);

            // 向后查找相同的值
            long index = foundIndex + 1;
            while (index < totalIntegers) {
                raf.seek(startPos + index * 4);
                int value = raf.readInt();
                if (value == closestValue) {
                    positions.add(startPos + index * 4);
                    index++;
                } else {
                    break;
                }
            }

            long endTime = System.currentTimeMillis(); // 结束计时
            raf.close();

            System.out.println("任务3：随机访问查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
            System.out.println("找到的整数值：" + closestValue);
            System.out.println("对应的指针位置：");
            for (long pos : positions) {
                System.out.println("    位置：" + pos);
            }
        } else {
            long endTime = System.currentTimeMillis(); // 结束计时
            raf.close();
            System.out.println("任务3：未找到大于等于目标值的整数，耗时：" + (endTime - startTime) + " 毫秒。");
        }
    }


    // BitInputStream类，用于从字节数组中逐位读取数据
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
    }

    // 任务4：随机访问查找(c)部分
    private static void randomAccessSearchInPartC(long startPos, long length) throws IOException {
        // 加载霍夫曼树
        HuffmanNode root = loadHuffmanTree();
        if (root == null) {
            System.out.println("任务4：无法加载霍夫曼树，无法解压缩数据。");
            return;
        }

        RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r");
        raf.seek(startPos);

        long startTime = System.currentTimeMillis(); // 开始计时

        // 将压缩的数据读取到字节数组
        byte[] compressedData = new byte[(int) length];
        raf.readFully(compressedData);
        raf.close();

        // 使用BitInputStream逐位读取数据
        BitInputStream bis = new BitInputStream(compressedData);

        int targetValue = 1024 * 64; // 65,536
        int closestValue = Integer.MAX_VALUE;
        List<Long> positions = new ArrayList<>(); // 存储指针位置（文件偏移量）

        HuffmanNode currentNode = root;
        long bitPosition = 0; // 当前位位置

        int bit;
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
            }
        }

        long endTime = System.currentTimeMillis(); // 结束计时

        System.out.println("任务4：查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
        System.out.println("找到的整数值：" + closestValue);
        System.out.println("对应的指针位置（文件偏移量）：");
        for (long pos : positions) {
            System.out.println("    位置：" + pos);
        }
    }


    // 生成码字到值的映射表
    private static void generateCodeToValueMap(HuffmanNode node, String code, Map<String, Integer> codeToValueMap) {
        if (node != null) {
            if (node.value != -1) {
                codeToValueMap.put(code, node.value);
            }
            generateCodeToValueMap(node.left, code + '0', codeToValueMap);
            generateCodeToValueMap(node.right, code + '1', codeToValueMap);
        }
    }

    // 保存霍夫曼树到文件（为了任务4能够解码）
    // 保存霍夫曼树的方法
    private static void saveHuffmanTree(HuffmanNode root) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HUFFMAN_TREE_FILE));
        oos.writeObject(root);
        oos.close();
        System.out.println("霍夫曼树已保存。");
    }

    // 从文件中加载霍夫曼树
    private static HuffmanNode loadHuffmanTree() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HUFFMAN_TREE_FILE));
            HuffmanNode root = (HuffmanNode) ois.readObject();
            ois.close();
            System.out.println("霍夫曼树已加载。");
            return root;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
