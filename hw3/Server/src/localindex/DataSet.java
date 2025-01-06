package localindex;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @Description TODO: 数据节点，存储[author] [year] [frequency]
 * @Author Junwei Hu
 * @Date 2024/12/07 10:15
 * @Version 1.0
 **/
class DataNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String author;
    private String year;
    private int frequency;

    /**
     * 构造方法
     *
     * @param author    作者姓名
     * @param year      发表年份
     * @param frequency 论文频次
     * @Author Junwei Hu
     * @Date 2024/12/07 10:20
     * @Version 1.0
     */
    public DataNode(String author, String year, int frequency) {
        this.author = author;
        this.year = year;
        this.frequency = frequency;
    }

    // Getter 和 Setter 方法

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * 重写equals方法，以便HashSet正确识别重复的DataNode
     *
     * @param o 比较的对象
     * @return 如果两个对象的author和year相同，则认为相等
     * @Author Junwei Hu
     * @Date 2024/12/07 10:25
     * @Version 1.0
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataNode dataNode = (DataNode) o;
        return Objects.equals(author, dataNode.author) &&
                Objects.equals(year, dataNode.year);
    }

    /**
     * 重写hashCode方法，与equals方法保持一致
     *
     * @return 哈希码
     * @Author Junwei Hu
     * @Date 2024/12/07 10:30
     * @Version 1.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(author, year);
    }
}

/**
 * @Description TODO: 数据节点的集合
 * @Author Junwei Hu
 * @Date 2024/12/07 10:35
 * @Version 1.0
 **/
public class DataSet implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<DataNode> set; // 使用HashSet来存储节点

    /**
     * 构造方法
     *
     * @Author Junwei Hu
     * @Date 2024/12/07 10:40
     * @Version 1.0
     */
    public DataSet() {
        set = new HashSet<>();
    }

    /**
     * 判断集合中是否存在某一节点满足author和year与传入的参数完全相同
     *
     * @param author 作者姓名
     * @param year   发表年份
     * @return 如果存在则返回true，否则返回false
     * @Author Junwei Hu
     * @Date 2024/12/07 10:45
     * @Version 1.0
     */
    public boolean contains(String author, String year) {
        return set.contains(new DataNode(author, year, 0));
    }

    /**
     * 根据传入的author和year信息，查找集合中是否存在，
     * 若存在则相应频次++，若不存在则新增该行数据
     *
     * @param author 作者姓名
     * @param year   发表年份
     * @Author Junwei Hu
     * @Date 2024/12/07 10:50
     * @Version 1.0
     */
    public void add(String author, String year) {
        DataNode tempNode = new DataNode(author, year, 1);
        if (!set.add(tempNode)) {
            // 如果添加失败，说明已存在，需更新频次
            for (DataNode node : set) {
                if (node.equals(tempNode)) {
                    node.setFrequency(node.getFrequency() + 1);
                    break;
                }
            }
        }
    }

    /**
     * 判断输入的年份是否在指定的范围内，并返回一个布尔值表示是否在指定范围内。
     *
     * @param year      要判断的年份
     * @param beginYear 起始年份
     * @param endYear   结束年份
     * @return 是否在指定范围内
     * @Author Junwei Hu
     * @Date 2024/12/07 10:55
     * @Version 1.0
     */
    private static boolean checkYearInRange(String year, String beginYear, String endYear) {
        if (year == null || year.equals("null")) {
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
     * 查找此数据集合内符合要求的author和year所对应的总的论文频次总数
     *
     * @param author    作者姓名
     * @param beginYear 起始年份
     * @param endYear   截至年份
     * @return 论文频次总数
     * @Author Junwei Hu
     * @Date 2024/12/07 11:00
     * @Version 1.0
     */
    public int countByAuthorAndYear(String author, String beginYear, String endYear) {
        int count = 0;
        for (DataNode node : set) {
            if (node.getAuthor().equals(author) && checkYearInRange(node.getYear(), beginYear, endYear)) {
                count += node.getFrequency();
            }
        }
        return count;
    }

    /**
     * 查找此数据集合内符合要求的author所对应的总的论文频次总数
     *
     * @param author 作者姓名
     * @return 论文频次总数
     * @Author Junwei Hu
     * @Date 2024/12/07 11:05
     * @Version 1.0
     */
    public int countByAuthor(String author) {
        int count = 0;
        for (DataNode node : set) {
            if (node.getAuthor().equals(author)) {
                count += node.getFrequency();
            }
        }
        return count;
    }

    /**
     * 获取数据集中的所有DataNode
     *
     * @return 数据集
     * @Author Junwei Hu
     * @Date 2024/12/07 11:10
     * @Version 1.0
     */
    public Set<DataNode> getSet() {
        return set;
    }
}