package mypackage;

import java.io.Serializable;

public class HuffmanNode implements Comparable<HuffmanNode>, Serializable {
    private static final long serialVersionUID = 1L;

    public int value; // 如果是叶子节点，存储整数值；如果是内部节点，值为-1
    public int frequency;
    public HuffmanNode left;
    public HuffmanNode right;

    public HuffmanNode(int value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return this.frequency - o.frequency;
    }

    @Override
    public String toString() {
        return "HuffmanNode{" +
                "value=" + value +
                ", frequency=" + frequency +
                '}';
    }
}

