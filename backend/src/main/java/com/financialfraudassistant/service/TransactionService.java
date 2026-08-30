package com.financialfraudassistant.service;
import com.financialfraudassistant.dto.CsvImportResponse;
import com.financialfraudassistant.dto.TransactionRequest;
import com.financialfraudassistant.dto.TransactionResponse;
import com.financialfraudassistant.model.FinancialTransaction;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionService {
    private static final int MAX_IMPORT_ROWS = 1000;
    private final FinancialTransactionRepository repository;
    private final FraudDetectionService fraudDetectionService;
    public TransactionService(FinancialTransactionRepository repository, FraudDetectionService fraudDetectionService) { this.repository = repository; this.fraudDetectionService = fraudDetectionService; }
    public List<TransactionResponse> list(User user) {
        return repository.findByUserIdOrderByTransactionDateDescIdDesc(user.getId()).stream().map(TransactionResponse::from).toList();
    }
    public TransactionResponse create(User user, TransactionRequest request) {
        FinancialTransaction transaction = repository.save(new FinancialTransaction(user, request.transactionDate(), request.merchant().trim(),
                request.amount(), request.transactionType(), request.category().trim(), FinancialTransaction.Source.MANUAL, clean(request.notes())));
        fraudDetectionService.analyseTransaction(transaction);
        return TransactionResponse.from(transaction);
    }
    public TransactionResponse update(User user, Integer id, TransactionRequest request) {
        FinancialTransaction transaction = repository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        transaction.update(request.transactionDate(), request.merchant().trim(), request.amount(), request.transactionType(),
                request.category().trim(), clean(request.notes()));
        repository.save(transaction);
        fraudDetectionService.analyseTransaction(transaction);
        return TransactionResponse.from(transaction);
    }

    public void delete(User user, Integer id) {
        FinancialTransaction transaction = repository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        repository.delete(transaction);
    }

    public CsvImportResponse importCsv(User user, MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a non-empty CSV file");
        }
        try {
            List<String> lines = Arrays.asList(new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "").split("\\R"));
            if (lines.size() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV file has no transaction rows");
            List<String> headers = parseRow(lines.get(0));
            Map<ColumnKind, Integer> column = detectColumns(headers);
            if (!column.containsKey(ColumnKind.DATE) || !hasAmountColumn(column)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "CSV must include a date column and an amount column. Detected columns: " +
                                (headers.isEmpty() ? "none" : String.join(", ", headers)) +
                                ". Amount columns can be named amount, transaction amount, credit, debit, withdrawal, deposit, value etc.");
            }
            List<FinancialTransaction> transactions = new ArrayList<>();
            List<String> needsAttention = new ArrayList<>();
            for (int row = 1; row < lines.size() && row <= MAX_IMPORT_ROWS; row++) {
                if (lines.get(row).isBlank()) continue;
                try {
                    List<String> values = parseRow(lines.get(row));
                    transactions.add(buildTransaction(user, column, values));
                } catch (Exception exception) {
                    needsAttention.add("Row " + (row + 1) + ": " + exception.getMessage());
                }
            }
            if (lines.size() - 1 > MAX_IMPORT_ROWS) needsAttention.add("Only the first " + MAX_IMPORT_ROWS + " rows were imported");
            List<FinancialTransaction> savedTransactions = repository.saveAll(transactions);
            savedTransactions.forEach(fraudDetectionService::analyseTransaction);
            return new CsvImportResponse(transactions.size(), needsAttention);
        } catch (IOException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV file"); }
    }

    private enum ColumnKind { DATE, MERCHANT, AMOUNT, CREDIT, DEBIT, TYPE, CATEGORY, NOTES, IGNORE }

    private Map<ColumnKind, Integer> detectColumns(List<String> headers) {
        Map<ColumnKind, Integer> column = new java.util.EnumMap<>(ColumnKind.class);
        for (int i = 0; i < headers.size(); i++) {
            ColumnKind kind = kindOf(normalize(headers.get(i)));
            if (kind != ColumnKind.IGNORE) column.putIfAbsent(kind, i);
        }
        return column;
    }

    private boolean hasAmountColumn(Map<ColumnKind, Integer> column) {
        return column.containsKey(ColumnKind.AMOUNT) || column.containsKey(ColumnKind.CREDIT) || column.containsKey(ColumnKind.DEBIT);
    }

    private String normalize(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private ColumnKind kindOf(String n) {
        if (n.isEmpty()) return ColumnKind.IGNORE;
        if (n.contains("date")) return ColumnKind.DATE;
        if (startsAny(n, "credit", "cr") || n.equals("cr")) return ColumnKind.CREDIT;
        if (startsAny(n, "debit", "withdrawalamt", "withdrawalamount", "withdrawal", "withdrawn", "withdr") || n.equals("dr")) return ColumnKind.DEBIT;
        if (startsAny(n, "depositamt", "depositamount", "deposit")) return ColumnKind.CREDIT;
        if (startsAny(n, "txnamount", "transactionamount", "transactionamt", "amount", "amt", "balance", "value")) return ColumnKind.AMOUNT;
        if (n.contains("type")) return ColumnKind.TYPE;
        if (n.contains("category") || n.contains("head")) return ColumnKind.CATEGORY;
        if (containsAny(n, "note", "remark")) return ColumnKind.NOTES;
        if (containsAny(n, "merchant", "narration", "particular", "payee", "description", "details", "beneficiary", "counterparty", "party", "name")) return ColumnKind.MERCHANT;
        return ColumnKind.IGNORE;
    }

    private boolean startsAny(String value, String... prefixes) { for (String p : prefixes) if (value.startsWith(p)) return true; return false; }
    private boolean containsAny(String value, String... tokens) { for (String t : tokens) if (value.contains(t)) return true; return false; }

    private FinancialTransaction buildTransaction(User user, Map<ColumnKind, Integer> column, List<String> values) {
        String dateRaw = cell(column, values, ColumnKind.DATE);
        if (dateRaw.isBlank()) throw new IllegalArgumentException("date is required");
        LocalDate date = parseDate(dateRaw);
        if (date == null) throw new IllegalArgumentException("cannot parse date '" + dateRaw + "'");
        String merchant = cell(column, values, ColumnKind.MERCHANT).trim();
        if (merchant.isBlank()) merchant = "Unknown";
        String category = cell(column, values, ColumnKind.CATEGORY).trim();
        if (category.isBlank()) category = "Uncategorised";
        BigDecimal amount; FinancialTransaction.Type type;
        if (column.containsKey(ColumnKind.AMOUNT)) {
            BigDecimal raw = parseAmount(cell(column, values, ColumnKind.AMOUNT));
            FinancialTransaction.Type hinted = typeFromWord(cell(column, values, ColumnKind.TYPE));
            type = hinted != null ? hinted : (raw.signum() < 0 ? FinancialTransaction.Type.EXPENSE : FinancialTransaction.Type.INCOME);
            amount = raw.abs();
        } else if (column.containsKey(ColumnKind.CREDIT)) {
            BigDecimal creditRaw = parseAmount(cell(column, values, ColumnKind.CREDIT));
            BigDecimal debitRaw = parseAmount(cell(column, values, ColumnKind.DEBIT));
            boolean hasCredit = creditRaw != null && creditRaw.signum() != 0;
            boolean hasDebit = debitRaw != null && debitRaw.signum() != 0;
            if (hasCredit && hasDebit) {
                amount = creditRaw.abs().subtract(debitRaw.abs());
                type = amount.signum() < 0 ? FinancialTransaction.Type.EXPENSE : FinancialTransaction.Type.INCOME;
                amount = amount.abs();
            } else if (hasCredit) { amount = creditRaw.abs(); type = FinancialTransaction.Type.INCOME; }
            else if (hasDebit) { amount = debitRaw.abs(); type = FinancialTransaction.Type.EXPENSE; }
            else throw new IllegalArgumentException("no credit or debit amount");
        } else if (column.containsKey(ColumnKind.DEBIT)) {
            BigDecimal debitRaw = parseAmount(cell(column, values, ColumnKind.DEBIT));
            if (debitRaw == null || debitRaw.signum() == 0) throw new IllegalArgumentException("debit amount is empty");
            amount = debitRaw.abs(); type = FinancialTransaction.Type.EXPENSE;
        } else {
            throw new IllegalArgumentException("no amount column value");
        }
        if (amount == null) throw new IllegalArgumentException("cannot parse amount");
        return new FinancialTransaction(user, date, merchant, amount, type, category,
                FinancialTransaction.Source.CSV, clean(cell(column, values, ColumnKind.NOTES)));
    }

    private String cell(Map<ColumnKind, Integer> column, List<String> values, ColumnKind kind) {
        Integer index = column.get(kind);
        return index == null || index >= values.size() ? "" : values.get(index);
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MMM/yyyy", Locale.ENGLISH)
    );

    private LocalDate parseDate(String raw) {
        String candidate = raw.trim();
        if (candidate.contains(" ")) {
            String[] parts = candidate.split("\\s+");
            LocalDate fromFirstToken = tryFormats(parts[0].trim(), parts.length > 1 ? parts[1].trim() : null);
            if (fromFirstToken != null) return fromFirstToken;
        }
        return tryFormats(candidate, null);
    }

    private LocalDate tryFormats(String candidate, String secondToken) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try { return LocalDate.parse(candidate, format); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private BigDecimal parseAmount(String cell) {
        if (cell == null || cell.isBlank()) return null;
        String s = cell.trim();
        boolean negative = (s.startsWith("(") && s.endsWith(")")) || s.startsWith("-") || s.startsWith("\u2212");
        String core = s.replaceAll("[^0-9.,]", "");
        if (core.isEmpty()) return null;
        boolean hasDot = core.indexOf('.') >= 0;
        boolean hasComma = core.indexOf(',') >= 0;
        String normalized;
        if (hasDot && hasComma) {
            char decimal = core.lastIndexOf('.') > core.lastIndexOf(',') ? '.' : ',';
            char thousands = decimal == '.' ? ',' : '.';
            core = core.replace(String.valueOf(thousands), "");
            if (decimal == ',') core = core.replace(",", ".");
            normalized = core;
        } else if (hasComma) {
            normalized = core.replace(",", "");
        } else if (hasDot) {
            int dots = 0;
            for (int i = 0; i < core.length(); i++) if (core.charAt(i) == '.') dots++;
            normalized = dots > 1 ? core.replace(".", "") : core;
        } else {
            normalized = core;
        }
        if (negative) normalized = "-" + normalized;
        try {
            BigDecimal value = new BigDecimal(normalized.isEmpty() ? "0" : normalized);
            return negative ? value.abs().negate() : value;
        } catch (NumberFormatException exception) { return null; }
    }

    private FinancialTransaction.Type typeFromWord(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String word = raw.toLowerCase(Locale.ROOT);
        if (containsAny(word, "expense", "debit", "withdrawal", "withdraw", "paid", "dr")) return FinancialTransaction.Type.EXPENSE;
        if (containsAny(word, "income", "credit", "deposit", "received", "cr", "salary")) return FinancialTransaction.Type.INCOME;
        return null;
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<String> parseRow(String row) { List<String> values = new ArrayList<>(); StringBuilder current = new StringBuilder(); boolean quoted = false; for (int i = 0; i < row.length(); i++) { char character = row.charAt(i); if (character == '"' && i + 1 < row.length() && row.charAt(i + 1) == '"') { current.append(character); i++; } else if (character == '"') quoted = !quoted; else if (character == ',' && !quoted) { values.add(current.toString()); current.setLength(0); } else current.append(character); } values.add(current.toString()); return values; }
}
