package Class;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client2 {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private static final String FILE_NAME = "2153393-hw2-q1.dat";

    public static void main(String[] args) {
        try {
            // 创建与服务器的连接
            Socket socket = new Socket(SERVER_ADDRESS, PORT);
            System.out.println("客户端2已连接到服务器。");

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // 请求读取权限
            // System.out.println("客户端2请求读取权限。");
            oos.writeObject("read");
            String response = (String) ois.readObject();
            if ("read_granted".equals(response)) {
                // System.out.println("客户端2获得读取权限，开始查找数据...");
                // 执行查找任务
                searchAndDelete();
            }

            // 请求写入权限
            // System.out.println("客户端2请求写入权限。");
            oos.writeObject("write");
            response = (String) ois.readObject();
            if ("write_granted".equals(response)) {
                // 在写入之前重新读取头信息
                // System.out.println("客户端2获得写入权限，重新读取文件头信息以确保写入位置正确...");
                performDeletionAndUpdate();

                // 通知服务器写入完成
                oos.writeObject("write_complete");
                // System.out.println("客户端2已完成写入操作，通知服务器。");
            }

            // 请求退出前，再次进行查找操作
            //System.out.println("客户端2请求再次读取权限以进行查找操作。");
            oos.writeObject("read");
            response = (String) ois.readObject();
            if ("read_granted".equals(response)) {
                // System.out.println("客户端2获得读取权限，开始再次查找数据...");
                // 清理之前的临时数据
                ClientData.setClosestValue(Integer.MAX_VALUE);
                ClientData.setPositions(new ArrayList<>());
                // 再次执行查找任务
                searchAndDelete();
            }

            // 请求退出
            oos.writeObject("exit");
            // System.out.println("客户端2发送退出请求。");

            // 关闭连接
            ois.close();
            oos.close();
            socket.close();
            System.out.println("客户端2已关闭连接。");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 查找不小于且最接近 1024 * 64 的整数
    private static void searchAndDelete() {
        try (RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "r")) {
            long[] header = readHeader(raf);
            long startPos = header[2];
            long length = header[3];

            System.out.println("客户端2读取的文件头信息：");
            System.out.println("Part B 起始位置：" + startPos + "，长度：" + length);

            long startTime = System.currentTimeMillis(); // 开始计时

            int targetValue = 1024 * 64; // 65,536
            long totalIntegers = length / 4;

            System.out.println("客户端2开始随机查找（使用二分查找）...");
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

            int closestValue;
            List<Long> positions = new ArrayList<>();

            if (foundIndex != -1) {
                raf.seek(startPos + foundIndex * 4);
                closestValue = raf.readInt();
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
            } else {
                closestValue = Integer.MAX_VALUE;
            }

            long endTime = System.currentTimeMillis(); // 结束计时

            // 输出结果
            if (closestValue != Integer.MAX_VALUE) {
                System.out.println("查找完成，耗时：" + (endTime - startTime) + " 毫秒。");
                System.out.println("找到的整数值：" + closestValue + " 共 " + positions.size() + " 个");
                System.out.println("对应的指针位置：");
                for (long pos : positions) {
                    System.out.println("    位置：" + pos);
                }
            } else {
                System.out.println("未找到大于等于目标值的整数。");
            }

            // 保存查找结果，供后续删除使用
            ClientData.setClosestValue(closestValue);
            ClientData.setPositions(positions);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 删除整数并更新文件
    private static void performDeletionAndUpdate() {
        try (RandomAccessFile raf = new RandomAccessFile(FILE_NAME, "rw")) {
            // 重新读取文件头信息，确保读取到最新的位置
            long[] header = readHeader(raf);
            long bStartPos = header[2];  // B数组的起始位置
            long bLength = header[3];    // B数组的长度
            long cStartPos = header[4];  // C数组的起始位置
            long cLength = header[5];    // C数组的长度

            int valueToDelete = ClientData.getClosestValue();
            List<Long> positions = ClientData.getPositions();

            if (valueToDelete == Integer.MAX_VALUE || positions.isEmpty()) {
                System.out.println("没有需要删除的整数。");
                return;
            }

            long startTime = System.currentTimeMillis(); // 开始计时

            // 删除B数组的整数并移动数据
            int deletedCount = deleteIntegers(raf, bStartPos, bLength, valueToDelete, positions);
            int deletedBytes = deletedCount * 4;

            // 计算C数组的新起点
            long newCStartPos = bStartPos + (bLength - deletedBytes); // B数组新结束点作为C的新起点

//            // 调试信息：打印移动前后 c 数组的起始位置数值
//            System.out.println("移动前 C 数组起始位置：" + cStartPos);
//            raf.seek(cStartPos);
//            System.out.println("移动前 C 数组起始位置的值：" + raf.readInt());
//
//            System.out.println("移动前 C 数组结束位置：" + (cStartPos + cLength));
//            raf.seek((cStartPos + cLength));
//            System.out.println("移动前 C 数组结束位置的值：" + raf.readInt());

            // 移动C数组
            moveData(raf, cStartPos, cLength, newCStartPos);

//            // 调试信息：打印移动后 c 数组的起始位置数值
//            System.out.println("移动后 C 数组起始位置：" + newCStartPos);
//            raf.seek(newCStartPos);
//            System.out.println("移动后 C 数组起始位置的值：" + raf.readInt());
//
//            System.out.println("移动前 C 数组结束位置：" + (newCStartPos + cLength));
//            raf.seek((newCStartPos + cLength));
//            System.out.println("移动前 C 数组结束位置的值：" + raf.readInt());

            // 更新文件头信息
            header[3] = bLength - deletedBytes; // 更新B数组长度
            header[4] = newCStartPos;  // 更新C数组的起始位置
            writeHeader(raf, header);

            // ** 在移动完数据后再截断文件，删除掉被移动部分的数据 **
            raf.setLength(newCStartPos + cLength); // 按照新位置截断文件

            long endTime = System.currentTimeMillis(); // 结束计时

            // 打印调试信息
            System.out.println("删除和更新操作完成，耗时：" + (endTime - startTime) + " 毫秒。");
            System.out.println("共删除整数数量：" + deletedCount);

            // 调试信息：打印已删除整数指针现在的值
            // printNewValuesAtDeletedPositions(raf, positions);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 删除整数，返回删除的整数数量
    private static int deleteIntegers(RandomAccessFile raf, long startPos, long length, int valueToDelete, List<Long> positions) throws IOException {
        raf.seek(startPos);
        long readPos = startPos;
        long writePos = startPos;

        long endPos = startPos + length;

        byte[] buffer = new byte[4 * 1024]; // 4KB缓冲区

        int deletedCount = 0;

        while (readPos < endPos) {
            int bytesToRead = (int) Math.min(buffer.length, endPos - readPos);
            raf.seek(readPos);
            raf.readFully(buffer, 0, bytesToRead);

            int validDataLength = 0;
            for (int i = 0; i < bytesToRead; i += 4) {
                int value = ((buffer[i] & 0xFF) << 24) |
                        ((buffer[i + 1] & 0xFF) << 16) |
                        ((buffer[i + 2] & 0xFF) << 8) |
                        (buffer[i + 3] & 0xFF);

                if (value != valueToDelete) {
                    // 将该整数写入到写入位置
                    raf.seek(writePos + validDataLength);
                    raf.writeInt(value);
                    validDataLength += 4;
                } else {
                    deletedCount++;
                    // 调试信息：输出删除整数的指针位置和值
                    // System.out.println("删除整数：" + value + "，位置：" + (readPos + i));
                }
            }
            readPos += bytesToRead;
            writePos += validDataLength;
        }

        return deletedCount;
    }

//    // 打印已删除指针位置现在的整数值
//    private static void printNewValuesAtDeletedPositions(RandomAccessFile raf, List<Long> positions) throws IOException {
//        System.out.println("已删除位置现在的整数值：");
//        for (long pos : positions) {
//            raf.seek(pos);
//            int newValue = raf.readInt();
//            System.out.println("位置：" + pos + "，现在的整数值：" + newValue);
//        }
//    }

    // 移动数据
    private static void moveData(RandomAccessFile raf, long srcPos, long length, long destPos) throws IOException {
        raf.seek(srcPos);
        byte[] buffer = new byte[1024 * 4];  // 4KB的缓冲区
        long bytesToMove = length;

        while (bytesToMove > 0) {
            int bytesRead = (int) Math.min(buffer.length, bytesToMove);
            raf.seek(srcPos);
            raf.read(buffer, 0, bytesRead);
            raf.seek(destPos);
            raf.write(buffer, 0, bytesRead);
            srcPos += bytesRead;
            destPos += bytesRead;
            bytesToMove -= bytesRead;
        }
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
}
