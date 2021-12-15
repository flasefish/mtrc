package thunder.mrtc.common;

public enum ByeReason {

    INITIATIVE(0), // 主动
    TIMEOUT(1);    // 超时

    private int value;

    private ByeReason(int value) {
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
