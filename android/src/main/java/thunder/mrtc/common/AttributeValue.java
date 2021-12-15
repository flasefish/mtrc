package thunder.mrtc.common;


// 流媒体方向
public enum AttributeValue {
 
    SENDRECV(0),
    SENDONLY(1),
    RECVONLY(2);
 
    private int value;
 
    private AttributeValue(int value) {
        this.value = value;
    }
 
    public int getValue() {
        return value;
    }
 
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}