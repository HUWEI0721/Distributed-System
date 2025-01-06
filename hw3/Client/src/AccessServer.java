import java.io.*;
import java.net.Socket;

/**
 * @Description TODO: Client访问Server时使用此类
 * @Author Junwei Hu
 * @Date 2024/12/07 14:23
 * @Version 1.0
 **/
public class AccessServer {
    /**
     * ip列表，记录可以访问的Server的ip地址
     * @Author Junwei Hu
     * @Date 2024/12/08 09:15
     */
    private final static String[] ipList = new String[]
            { "124.220.24.233", "110.40.167.8","1.116.120.226"};

    /**
     * port列表，记录可以访问的Server的端口
     * @Author Junwei Hu
     * @Date 2024/12/09 18:47
     */
    private final static int[] portList = new int[]
            {8920, 8921, 8922};

    /**
     * @Description TODO: 向指定的Server发送查询信息（作者、年份），并获得查询结果
     * @param name 查询的名称
     * @param beginYear 起始年份
     * @param endYear 结束年份
     * @param ipSelected 选择的IP索引
     * @param portSelected 选择的端口索引
     * @param isBackup 是否查询备份文件块的信息
     * @param useIndex 是否使用索引查询
     * @return 查询到的次数，如果发生错误则返回-1
     * @Author Junwei Hu
     * @Date 2024/12/06 11:30
     * @Version 1.0
     **/
    public static int sendQuery(String name, String beginYear, String endYear, int ipSelected, int portSelected, boolean isBackup, boolean useIndex) {
        int num;
        try {
            // 创建Socket链接
            Socket socket = new Socket(ipList[ipSelected], portList[portSelected]);
            DataInputStream is = new DataInputStream(socket.getInputStream());
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());

            // 向Server传递isBackup是否要查询备份文件块的信息
            os.writeUTF(isBackup ? "true" : "false");
            os.flush();

            // 向Server传递name信息
            os.writeUTF(name);
            os.flush();

            // 向Server传递beginYear信息
            os.writeUTF(beginYear);
            os.flush();

            // 向Server传递endYear信息
            os.writeUTF(endYear);
            os.flush();

            // 向Server传递useIndex是否要使用索引查询
            os.writeUTF(useIndex ? "true" : "false");
            os.flush();

            // 接收服务端的查询信息
            String queryResult = is.readUTF();
            num = Integer.parseInt(queryResult); // 查询到的次数

            // System.out.println(ipSelected + " " + portSelected + " " + num);

            // 关闭Socket链接
            is.close();
            os.close();
            socket.close();

        } catch (IOException e) {
            // 发生IO异常，返回-1
            return -1;
        }
        return num;
    }
}