package thunder.mrtc.callback;

public interface AcceptEventExecutor {

    /**
     * 接受操作成功
     */
    void onSuccess();

    /**
     * 接受操作失败
     */
    void onFail(int code, String errorMessage);
}
