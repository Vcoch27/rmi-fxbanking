package vn.vku.rmi.client;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

public class CsvLogger {
    private final Path dir = Paths.get("client_logs");
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CsvLogger() {
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
    }

    public synchronized void append(String accountId, TransactionEvent e) {
        Path f = dir.resolve("history_" + accountId + ".csv");
        boolean header = !Files.exists(f);
        try (var out = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(f, StandardOpenOption.CREATE, StandardOpenOption.APPEND), "UTF-8"))) {
            if (header) out.write("time,type,detail,amount,balance\n");
            out.write(String.format("%s,%s,%s,%d,%d%n",
                    fmt.format(e.at), e.type, escape(e.detail), e.amount, e.balance));
        } catch (IOException ex) { /* ignore client-side log errors */ }
    }

    private String escape(String s){ return s.replace(",", " "); }
}
