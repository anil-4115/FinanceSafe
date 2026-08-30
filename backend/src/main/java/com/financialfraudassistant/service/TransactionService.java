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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            List<String> headers = parseRow(lines.get(0)).stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
            Map<String, Integer> column = java.util.stream.IntStream.range(0, headers.size()).boxed().collect(Collectors.toMap(headers::get, Function.identity(), (first, ignored) -> first));
            requireColumn(column, "date", "transaction_date");
            requireAmountColumn(column);
            List<FinancialTransaction> transactions = new ArrayList<>(); List<String> errors = new ArrayList<>();
            for (int row = 1; row < lines.size() && row <= MAX_IMPORT_ROWS; row++) {
                if (lines.get(row).isBlank()) continue;
                try {
                    List<String> values = parseRow(lines.get(row));
                    LocalDate date = LocalDate.parse(value(column, values, "date", "transaction_date"));
                    String merchant = optional(column, values, "merchant", "description", "narration", "payee", "merchant_name", "transaction description").trim();
                    if (merchant.isBlank()) merchant = "Unknown";
                    AmountValue amountValue = resolveAmount(column, values);
                    String rawType = optional(column, values, "type", "transaction_type");
                    FinancialTransaction.Type type = amountValue.type() != null ? amountValue.type() : resolveType(rawType, amountValue.rawSign());
                    String category = optional(column, values, "category");
                    transactions.add(new FinancialTransaction(user, date, merchant, amountValue.amount(), type,
                            category.isBlank() ? "Uncategorised" : category.trim(), FinancialTransaction.Source.CSV, optional(column, values, "notes")));
                } catch (Exception exception) { errors.add("Row " + (row + 1) + ": " + exception.getMessage()); }
            }
            List<FinancialTransaction> savedTransactions = repository.saveAll(transactions);
            savedTransactions.forEach(fraudDetectionService::analyseTransaction);
            if (lines.size() - 1 > MAX_IMPORT_ROWS) errors.add("Only the first " + MAX_IMPORT_ROWS + " rows were imported");
            return new CsvImportResponse(transactions.size(), errors);
        } catch (IOException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV file"); }
    }
    private void requireColumn(Map<String, Integer> columns, String... names) { if (findColumn(columns, names) == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV must include a " + names[0] + " column"); }
    private void requireAmountColumn(Map<String, Integer> columns) {
        if (findColumn(columns, "amount", "transaction amount", "transaction_amount", "credit", "debit", "deposit", "withdrawal", "value", "credit amount", "debit amount") == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV must include an amount column. Use one of: amount, transaction amount, credit, debit, deposit, withdrawal or value");
    }
    private record AmountValue(BigDecimal amount, FinancialTransaction.Type type, BigDecimal rawSign) { }
    private AmountValue resolveAmount(Map<String, Integer> columns, List<String> values) {
        BigDecimal straight = parseAmountValue(optional(columns, values, "amount", "transaction amount", "transaction_amount", "value"));
        if (straight != null) {
            return new AmountValue(straight.abs(), null, straight);
        }
        String credit = val(columns, values, "credit", "credit amount");
        String debit = val(columns, values, "debit", "debit amount");
        String deposit = val(columns, values, "deposit");
        String withdrawal = val(columns, values, "withdrawal");
        if (!credit.isBlank()) return new AmountValue(parse(credit).abs(), FinancialTransaction.Type.INCOME, null);
        if (!deposit.isBlank()) return new AmountValue(parse(deposit).abs(), FinancialTransaction.Type.INCOME, null);
        if (!debit.isBlank()) return new AmountValue(parse(debit).abs(), FinancialTransaction.Type.EXPENSE, null);
        if (!withdrawal.isBlank()) return new AmountValue(parse(withdrawal).abs(), FinancialTransaction.Type.EXPENSE, null);
        throw new IllegalArgumentException("amount is required");
    }
    private BigDecimal parseAmountValue(String raw) { if (raw == null || raw.isBlank()) return null; String cleaned = raw.replace(",", "").replace("₹", "").replace("Rs", "").trim(); if (cleaned.isEmpty()) return null; return new BigDecimal(cleaned); }
    private BigDecimal parse(String raw) { return parseAmountValue(raw); }
    private String val(Map<String, Integer> columns, List<String> values, String... names) { Integer index = findColumn(columns, names); return index == null || index >= values.size() ? "" : values.get(index).replace(",", "").replace("₹", "").trim(); }
    private String value(Map<String, Integer> columns, List<String> values, String... names) { Integer index = findColumn(columns, names); if (index == null || index >= values.size() || values.get(index).isBlank()) throw new IllegalArgumentException(names[0] + " is required"); return values.get(index); }
    private String optional(Map<String, Integer> columns, List<String> values, String... names) { Integer index = findColumn(columns, names); return index == null || index >= values.size() ? "" : values.get(index); }
    private Integer findColumn(Map<String, Integer> columns, String... names) { for (String name : names) if (columns.containsKey(name)) return columns.get(name); return null; }
    private FinancialTransaction.Type resolveType(String rawType, BigDecimal amount) { if (amount.signum() < 0 || rawType.equalsIgnoreCase("expense") || rawType.equalsIgnoreCase("debit")) return FinancialTransaction.Type.EXPENSE; return FinancialTransaction.Type.INCOME; }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<String> parseRow(String row) { List<String> values = new ArrayList<>(); StringBuilder current = new StringBuilder(); boolean quoted = false; for (int i = 0; i < row.length(); i++) { char character = row.charAt(i); if (character == '"' && i + 1 < row.length() && row.charAt(i + 1) == '"') { current.append(character); i++; } else if (character == '"') quoted = !quoted; else if (character == ',' && !quoted) { values.add(current.toString()); current.setLength(0); } else current.append(character); } values.add(current.toString()); return values; }
}
