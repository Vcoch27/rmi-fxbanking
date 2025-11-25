package vn.vku.rmi.client;

import java.time.LocalDateTime;

/**
 * Class đại diện cho một sự kiện giao dịch
 * Dùng để hiển thị trong bảng lịch sử và ghi log CSV
 */
public class TransactionEvent {
    public final LocalDateTime at;   // Thời điểm xảy ra giao dịch
    public final String type;        // Loại: BALANCE|DEPOSIT|WITHDRAW|TRANSFER_OUT|TRANSFER_IN|ERROR|LOGIN
    public final String detail;      // Mô tả chi tiết (vd: "Nạp tiền", "Chuyển cho 1002")
    public final long amount;        // Số tiền giao dịch (0 nếu chỉ vấn tin)
    public final long balance;       // Số dư sau khi thực hiện giao dịchiao dịch

    /**
     * Constructor tạo sự kiện giao dịch mới
     * @param type Loại giao dịch (LOGIN, DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, ERROR, BALANCE)
     * @param detail Mô tả chi tiết
     * @param amount Số tiền (0 nếu không áp dụng)
     * @param balance Số dư sau giao dịch
     */
    public TransactionEvent(String type, String detail, long amount, long balance) {
        this.at = LocalDateTime.now();
        this.type = type;
        this.detail = detail;
        this.amount = amount;
        this.balance = balance;
    }
}
