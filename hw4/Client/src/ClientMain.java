import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * @Description TODO: Client的主类
 * @Author Junwei Hu
 * @Date 2024/12/11 14:37
 * @Version 1.0
 **/
public class ClientMain {
    /**
     * @Description TODO: 向服务器发送query请求（非备份）
     * @Author Junwei Hu
     * @Date 2024/12/12 16:47
     * @Version 1.0
     **/
    public static void sendQuerys(int[] numResults, String type, long[] joinTime, long[] crashTime, LinkedList<Long>[] timeList){
        Thread[] threads = new Thread[6];
        for(int i = 0; i < 6; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    numResults[index] = AccessServer.sendQuery(type, index / 2, index % 2, index, joinTime, crashTime, timeList[index]);
                    // Debug information commented out
                    // System.out.println("Thread " + index + " 接收到的数据:");
                    // System.out.println("numResult: " + numResults[index]);
                    // System.out.println("joinTime: " + joinTime[index]);
                    // System.out.println("crashTime: " + crashTime[index]);
                    // System.out.println("timeList: " + timeList[index]);
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        try {
            for(Thread thread : threads){
                thread.join();
            }
        } catch (Exception e) {
            // Debug information commented out
            // System.out.println("线程错误: " + e.getMessage());
        }
    }

    /**
     * @Description TODO: 计算指定百分位数
     * @param sortedList 已排序的列表
     * @param percentile 百分位数（0-100）
     * @return 对应的阈值
     */
    private static long calculatePercentile(List<Long> sortedList, double percentile) {
        if (sortedList.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    /**
     * @Description TODO: 计算每台虚拟机的错误率
     * 当joinTime和crashTime为0时，将使用timeList的最小值作为joinTime，最大值作为crashTime进行推断。
     * 动态阈值选择方法：使用75th百分位数作为阈值，并设置一个最小阈值（如200ms）来过滤短错误间隔。
     * 处理极短生命周期的虚拟机（如liveTime < 2000ms），不计算其错误率。
     * @param timeList 每台虚拟机的错误时间戳列表
     * @return 错误率数组
     **/
    public static float[] calculateErrorRates(LinkedList<Long>[] timeList) {
        float[] rates = new float[6];
        // 定义最小错误持续时间阈值（单位：ms）
        final long MIN_THRESHOLD = 200;
        // 定义最小生命周期阈值（单位：ms）
        final long MIN_LIVETIME = 2000;

        for (int i = 0; i < 6; i++) {
            LinkedList<Long> errors = timeList[i];
            if (errors.isEmpty()) {
                rates[i] = 0.0f;
                // Debug information commented out
                // System.out.println(i + "号虚拟机没有错误记录。");
                continue;
            }

            // 对timeList进行排序
            Collections.sort(errors);

            // 如果joinTime和crashTime为0(服务器未返回)，则用timeList推断
            long join = errors.getFirst(); // 最早的时间戳为joinTime
            long crash = errors.getLast(); // 最晚的时间戳为crashTime

            // 收集所有有效的错误间隔
            List<Long> allIntervals = new LinkedList<>();
            boolean inError = false;
            long errorStart = 0;

            for (Long t : errors) {
                if (!inError) {
                    inError = true;
                    errorStart = t;
                } else {
                    inError = false;
                    long interval = t - errorStart;
                    if (interval > 0) {
                        allIntervals.add(interval);
                    } else {
                        // Debug information commented out
                        // System.out.println("警告: 虚拟机" + i + "的错误结束时间早于开始时间。");
                    }
                }
            }

            // 若最后一个错误没有结束，则持续到crash
            if (inError && crash > errorStart) {
                long interval = crash - errorStart;
                if (interval > 0) {
                    allIntervals.add(interval);
                }
            }

            // 计算动态阈值（75th百分位数）
            long dynamicThreshold = calculatePercentile(allIntervals, 75.0);
            // 应用最小阈值
            dynamicThreshold = Math.max(dynamicThreshold, MIN_THRESHOLD);
            // Debug information commented out
            // System.out.println(i + "号虚拟机的动态阈值为：" + dynamicThreshold + "ms");

            // 根据动态阈值计算errorTime
            long errorTime = 0;
            inError = false;
            errorStart = 0;
            for (Long t : errors) {
                if (!inError) {
                    inError = true;
                    errorStart = t;
                } else {
                    inError = false;
                    long interval = t - errorStart;
                    if (interval >= dynamicThreshold) {
                        errorTime += interval;
                    } else {
                        // Debug information commented out
                        // System.out.println("虚拟机" + i + "的错误间隔(" + interval + "ms)小于动态阈值(" + dynamicThreshold + "ms)，已忽略。");
                    }
                }
            }
            // 再次处理最后未结束的错误
            if (inError && crash > errorStart) {
                long interval = crash - errorStart;
                if (interval >= dynamicThreshold) {
                    errorTime += interval;
                } else {
                    // Debug information commented out
                    // System.out.println("虚拟机" + i + "的最后一次错误间隔(" + interval + "ms)小于动态阈值(" + dynamicThreshold + "ms)，已忽略。");
                }
            }

            long liveTime = crash - join;
            if (liveTime < MIN_LIVETIME) {
                rates[i] = 0.0f;
                // Debug information commented out
                // System.out.println("警告: 虚拟机" + i + "的liveTime为" + liveTime + "ms，低于最小生命周期阈值(" + MIN_LIVETIME + "ms)，错误率设为0。");
            } else if (liveTime > 0) {
                rates[i] = (float) errorTime / liveTime;
            } else {
                rates[i] = 0.0f;
                // Debug information commented out
                // System.out.println("警告: 虚拟机" + i + "的liveTime为零或负数。");
            }

            // Debug information commented out
            // System.out.println(i + "号虚拟机的错误持续时间: " + errorTime + "ms, 生命周期: " + liveTime + "ms, 错误率: " + (rates[i]*100) + "%");
        }
        return rates;
    }

    /**
     * @Description TODO: Client的主函数，调用其他类，运行Client的逻辑
     * @return
     * @param args
     * @Author Junwei Hu
     * @Date 2024/12/13 14:28
     * @Version 1.0
     **/
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in); // 将Scanner移到循环外，避免重复创建
        while(true) {
            System.out.println("请输入查询Type,可选：1-heartbeat,2-gossip,3-join,4-offline,5-crash,6-查询memberList错误时长占比");
            String input = sc.nextLine();
            // Type
            String type = "";
            // 退出标识:exit
            if (input.equalsIgnoreCase("exit"))
                break;
            else if(input.equals("1")){
                type = "heartbeat";
            }
            else if(input.equals("2")){
                type = "gossip";
            }
            else if(input.equals("3")){
                type = "join";
            }
            else if(input.equals("4")) {
                type = "offline";
            }
            else if(input.equals("5")) {
                type = "crash";
            }
            else if(input.equals("6")){
                type = "rate";
            }
            else{
                System.out.println("输入不合法，请重新输入");
                continue; // 继续下一次循环
            }
            System.out.println("正在查询.....");
            //创建计时
            long startTime = System.currentTimeMillis();

            if(type.equals("rate")){
                // 存储"rate"查询的数据
                int[] numResultsRate = {-2, -2, -2, -2, -2, -2};
                long[] joinTimeRate = {0, 0, 0, 0, 0, 0};
                long[] crashTimeRate = {0, 0, 0, 0, 0, 0};
                LinkedList<Long>[] timeListRate = new LinkedList[6];
                for(int i = 0; i < 6; i++){
                    timeListRate[i] = new LinkedList<>();
                }

                // 发送"rate"查询
                sendQuerys(numResultsRate, "rate", joinTimeRate, crashTimeRate, timeListRate);

                // 输出"rate"查询的结果
                // Debug information commented out
                /*
                System.out.println("=== 接收到的rate查询数据 ===");
                for(int i = 0; i < 6; i++){
                    System.out.println(i + "号虚拟机:");
                    System.out.println("  numResult: " + numResultsRate[i]);
                    System.out.println("  joinTime: " + joinTimeRate[i]);
                    System.out.println("  crashTime: " + crashTimeRate[i]);
                    System.out.println("  timeList: " + timeListRate[i]);
                }
                System.out.println("=======================");
                */

                // 计算MemberList错误率（使用动态阈值来过滤短间隔）
                float[] rates = calculateErrorRates(timeListRate);

                // 输出最终的错误率
                System.out.println("=== MemberList错误率 ===");
                for (int i = 0; i < rates.length; i++) {
                    System.out.println(i + "号虚拟机的MemberList错误率为：" + (rates[i] * 100) + "%");
                }
                System.out.println("=======================");
            }
            else{
                // 普通查询
                // 记录每台虚拟机的查询结果：-2为初始值，-1为连接失败
                int[] numResults = {-2, -2, -2, -2, -2, -2};
                long[] joinTime = {0, 0, 0, 0, 0, 0};
                long[] crashTime = {0, 0, 0, 0, 0, 0};
                // 存储每个Server组成员列表的Change时间
                LinkedList<Long>[] timeList = new LinkedList[6];
                for(int i = 0; i < 6; i++){
                    timeList[i] = new LinkedList<>();
                }
                // 向各个服务器发送查询请求
                sendQuerys(numResults, type, joinTime, crashTime, timeList);

                // 输出用时
                long endTime = System.currentTimeMillis();
                System.out.println("查询成功! 用时：" + (double) (endTime - startTime) / 1000 + "s");

                // 打印所有接收到的数据
                // Debug information commented out
                /*
                System.out.println("=== 接收到的所有数据 ===");
                for(int i = 0; i < 6; i++){
                    System.out.println(i + "号虚拟机:");
                    System.out.println("  numResult: " + numResults[i]);
                    System.out.println("  joinTime: " + joinTime[i]);
                    System.out.println("  crashTime: " + crashTime[i]);
                    System.out.println("  timeList: " + timeList[i]);
                }
                System.out.println("=======================");
                */

                // 输出查询结果
                for(int i = 0; i < numResults.length; i++){
                    // 由于有标签头和标签尾，结果除2
                    System.out.println(i + "号虚拟机日志中记录的" + type + "信息条数为：" + numResults[i] / 2);
                }
            }
            System.out.println();
        }
        sc.close();
    }
}