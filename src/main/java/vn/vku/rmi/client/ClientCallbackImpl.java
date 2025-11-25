package vn.vku.rmi.client;

import vn.vku.rmi.common.ClientCallback;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * Triển khai callback để nhận thông báo realtime từ server
 * Khi có tiền chuyển đến, server sẽ gọi notifyTransfer() của object này
 * Extends UnicastRemoteObject để trở thành remote object
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    // Consumer để xử lý sự kiện khi nhận được thông báo
    private final Consumer<TransactionEvent> onEvent;

    /**
     * Constructor
     * @param onEvent Hàm callback xử lý khi có sự kiện (truyền từ ClientApp)
     */
    public ClientCallbackImpl(Consumer<TransactionEvent> onEvent) throws RemoteException {
        super();
        this.onEvent = onEvent;
    }

    /**
     * Method được server gọi khi có tiền chuyển đến tài khoản này
     * @param fromId ID tài khoản người gửi
     * @param amount Số tiền nhận được
     * @param newBalance Số dư mới sau khi nhận tiền
     */
    @Override
    public void notifyTransfer(String fromId, long amount, long newBalance) throws RemoteException {
        // Tạo sự kiện TRANSFER_IN và gọi callback để cập nhật UI
        onEvent.accept(new TransactionEvent(
                "TRANSFER_IN", "Nhận từ " + fromId, amount, newBalance
        ));
    }
}
