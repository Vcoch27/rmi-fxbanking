package vn.vku.rmi.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import vn.vku.rmi.common.BankService;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;

/**
 * Ứng dụng Client JavaFX cho RMI eBanking
 * Kết nối tới server qua RMI, thực hiện đăng nhập và các giao dịch
 * Hiển thị lịch sử giao dịch realtime
 */
public class ClientApp extends Application {

    // Stub kết nối tới BankService trên server
    private BankService svc;

    // ID tài khoản đang đăng nhập
    private String accountId;

    // Logger để ghi lịch sử ra file CSV
    private final CsvLogger csv = new CsvLogger();

    // Label hiển thị tài khoản và số dư ở header
    private final Label lbUser = new Label("-");
    private final Label lbBalance = new Label("0");

    // TableView hiển thị lịch sử giao dịch
    private final TableView<TransactionEvent> table = new TableView<>();

    // Buffer lưu tất cả giao dịch trong session hiện tại
    private final List<TransactionEvent> buffer = new ArrayList<>();

    /**
     * Method khởi tạo UI và kết nối tới server
     * @param stage Stage chính của JavaFX
     */
    @Override
    public void start(Stage stage) throws Exception {
        // ========== KẾT NỐI TỚI SERVER ==========
        // Có thể override: java -Dbank.host=<IP> -jar client.jar
        String host = System.getProperty("bank.host", "192.168.1.80");
        svc = (BankService) Naming.lookup("rmi://" + host + ":1099/BankService");

        // ========== HEADER: HIỂN THỊ TÀI KHOẢN & SỐ DƯ ==========
        lbUser.getStyleClass().add("header");
        lbBalance.getStyleClass().add("balance");
        var headBox = new HBox(20,
            new VBox(new Label("Tài khoản"), lbUser),
            new VBox(new Label("Số dư"), lbBalance)
        );
        headBox.setAlignment(Pos.CENTER_LEFT);
        headBox.setPadding(new Insets(12));

        // ========== ĐĂNG NHẬP ==========
        TextField tfId = new TextField();
        tfId.setPromptText("Account ID (vd: 1001)");

        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Password (123456)");

        Button btnLogin = new Button("Đăng nhập & Đăng ký Callback");
        btnLogin.setOnAction(e -> safe(() -> {
            accountId = tfId.getText().trim();
            String password = tfPass.getText();

            // Bước 1: Gọi server để xác thực
            boolean success = svc.login(accountId, password);
            if (!success) {
                pushError("Đăng nhập thất bại - Sai ID hoặc mật khẩu");
                return;
            }

            // Bước 2: Cập nhật UI
            lbUser.setText(accountId);

            // Bước 3: Đăng ký callback để nhận thông báo realtime
            svc.registerCallback(accountId, new ClientCallbackImpl(this::onEvent));

            // Bước 4: Load số dư
            refreshBalance();

            // Bước 5: Ghi log đăng nhập
            pushInfo("LOGIN", "Đăng nhập " + accountId, 0, Long.parseLong(lbBalance.getText()));
        }));
        var loginBox = new HBox(10, tfId, tfPass, btnLogin);
        loginBox.setPadding(new Insets(10));
        loginBox.setPadding(new Insets(10));

        // ========== CÁC CHỨC NĂNG: VẤN TIN / NẠP / RÚT ==========
        TextField tfAmount = new TextField();
        tfAmount.setPromptText("Amount");

        // Nút Vấn tin: Kiểm tra số dư hiện tại
        Button btnBalance = new Button("Vấn tin");
        btnBalance.setOnAction(e -> safe(() -> {
            refreshBalance();
            pushInfo("BALANCE", "Vấn tin", 0, Long.parseLong(lbBalance.getText()));
        }));

        // Nút Nạp tiền
        Button btnDeposit = new Button("Nạp tiền");
        btnDeposit.setOnAction(e -> safe(() -> {
            long v = parse(tfAmount);
            svc.deposit(accountId, v);  // Gọi RMI
            refreshBalance();           // Cập nhật số dư
            pushInfo("DEPOSIT", "Nạp tiền", v, Long.parseLong(lbBalance.getText()));
        }));

        // Nút Rút tiền
        Button btnWithdraw = new Button("Rút tiền");
        btnWithdraw.setOnAction(e -> safe(() -> {
            long v = parse(tfAmount);
            svc.withdraw(accountId, v); // Gọi RMI
            refreshBalance();           // Cập nhật số dư
            pushInfo("WITHDRAW", "Rút tiền", v, Long.parseLong(lbBalance.getText()));
        }));

        var act1 = new HBox(10, btnBalance, tfAmount, btnDeposit, btnWithdraw);
        act1.setPadding(new Insets(10));

        // ========== CHUYỂN KHOẢN ==========
        TextField tfTo = new TextField();
        tfTo.setPromptText("To Account");

        TextField tfAmount2 = new TextField();
        tfAmount2.setPromptText("Amount");

        Button btnTransfer = new Button("Chuyển khoản");
        btnTransfer.setOnAction(e -> safe(() -> {
            long v = parse(tfAmount2);

            // Kiểm tra số tiền hợp lệ
            if (v <= 0) {
                pushError("Số tiền phải > 0");
                return;
            }

            // Refresh số dư để kiểm tra đủ tiền không
            refreshBalance();
            long current = Long.parseLong(lbBalance.getText());
            if (v > current) {
                pushError("Số tiền vượt quá số dư (" + current + ")");
                return;
            }

            // Kiểm tra tài khoản nhận
            String to = tfTo.getText().trim();
            if (to.isBlank()) {
                pushError("Nhập tài khoản nhận");
                return;
            }
            if (to.equals(accountId)) {
                pushError("Không thể tự chuyển cho chính mình");
                return;
            }

            // Thực hiện chuyển khoản
            svc.transfer(accountId, to, v);
            refreshBalance();
            pushInfo("TRANSFER_OUT", "Chuyển cho " + to, v, Long.parseLong(lbBalance.getText()));
        }));

        var act2 = new HBox(10, new Label("To:"), tfTo, tfAmount2, btnTransfer);
        act2.setPadding(new Insets(10));

        // --- TableView lịch sử ---
        TableColumn<TransactionEvent, String> c1 = new TableColumn<>("Thời gian");
        c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().at.toString()));
        c1.setPrefWidth(160);

        TableColumn<TransactionEvent, String> c2 = new TableColumn<>("Loại");
        c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        c2.setPrefWidth(120);

        TableColumn<TransactionEvent, String> c3 = new TableColumn<>("Chi tiết");
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().detail));
        c3.setPrefWidth(260);

        TableColumn<TransactionEvent, Number> c4 = new TableColumn<>("Số tiền");
        c4.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().amount));
        c4.setPrefWidth(100);

        TableColumn<TransactionEvent, Number> c5 = new TableColumn<>("Số dư sau");
        c5.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().balance));
        c5.setPrefWidth(120);

        table.getColumns().addAll(c1,c2,c3,c4,c5);
        table.setPrefHeight(240);

        // --- Layout tổng ---
        var root = new VBox(6, headBox, loginBox, new Separator(), act1, act2, new Separator(), table);
        root.setPadding(new Insets(12));
        var cssUrl = ClientApp.class.getResource("/vn/vku/rmi/client/style.css");
        if (cssUrl != null) {
            root.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("RMI eBanking – VKU (Client)");
        stage.setScene(new Scene(root, 800, 520));
        stage.show();
    }

    /**
     * Parse số tiền từ TextField
     */
    private long parse(TextField t){
        return Long.parseLong(t.getText().trim());
    }

    /**
     * Callback được gọi khi nhận thông báo từ server (qua ClientCallbackImpl)
     * Chạy trên JavaFX thread để cập nhật UI an toàn
     */
    private void onEvent(TransactionEvent e) {
        Platform.runLater(() -> {
            lbBalance.setText(Long.toString(e.balance)); // Cập nhật số dư realtime
            push(e);                                     // Thêm vào bảng + ghi CSV
        });
    }

    /**
     * Tạo và đẩy sự kiện thông tin vào lịch sử
     */
    private void pushInfo(String type, String detail, long amount, long balance) {
        push(new TransactionEvent(type, detail, amount, balance));
    }

    /**
     * Tạo và đẩy sự kiện lỗi vào lịch sử
     */
    private void pushError(String detail) {
        push(new TransactionEvent("ERROR", detail, 0, Long.parseLong(lbBalance.getText())));
    }

    /**
     * Thêm sự kiện vào buffer, cập nhật TableView và ghi CSV
     */
    private void push(TransactionEvent e){
        buffer.add(e);
        table.getItems().setAll(buffer);  // Cập nhật bảng
        if (accountId != null) csv.append(accountId, e); // Ghi log
    }

    /**
     * Gọi RMI để lấy số dư mới nhất từ server
     */
    private void refreshBalance() throws Exception {
        if (accountId == null || accountId.isBlank()) return;
        long bal = svc.getBalance(accountId);
        lbBalance.setText(Long.toString(bal));
    }

    /**
     * Wrapper để bắt exception từ RMI call và hiển thị lỗi
     */
    private void safe(RmiRunnable r){
        try {
            r.run();
        } catch (Exception ex) {
            pushInfo("ERROR", ex.getMessage(), 0, parseLongSafe(lbBalance.getText()));
        }
    }

    /**
     * Parse Long an toàn, trả về 0 nếu lỗi
     */
    private long parseLongSafe(String s){
        try { return Long.parseLong(s); }
        catch (Exception e){ return 0; }
    }

    /**
     * Functional interface cho RMI operations có thể ném exception
     */
    @FunctionalInterface interface RmiRunnable { void run() throws Exception; }

    public static void main(String[] args) { launch(args); }
}
