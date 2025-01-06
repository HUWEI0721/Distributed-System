package groupservice;

/**
 * @description: Member 类用来表示组成员，包含组成员的名称、IP 地址、端口号、时间戳等信息
 * @author Junwei Hu
 * @date 2024.12.13 10:47 AM
 * @version 1.0
 **/
public class Member implements Comparable<Member> {
    private String name;
    private String address;
    private int port;
    private int portGossip;
    private long timestamp;

    public Member(String name, String address, int port, long timestamp) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.portGossip = port + 100;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getPortGossip() {
        return portGossip;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int compareTo(Member other) {
        return Integer.parseInt(this.name) - Integer.parseInt(other.name);
    }

    /**
     * @description: 判断两个 Member 的名称、端口号、IP 地址是否相等
     * @param member 目标成员对象
     * @return boolean 返回是否相等
     * @author Junwei Hu
     * @date 2024.12.13 10:47 AM
     **/
    public boolean exist(Member member) {
        if (!this.name.equals(member.getName())) {
            return false;
        }
        if (!this.address.equals(member.getAddress())) {
            return false;
        }
        if (this.port != member.getPort()) {
            return false;
        }
        return true;
    }
}