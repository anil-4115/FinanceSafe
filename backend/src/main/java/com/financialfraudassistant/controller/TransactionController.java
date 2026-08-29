package com.financialfraudassistant.controller;
import com.financialfraudassistant.dto.CsvImportResponse;
import com.financialfraudassistant.dto.TransactionRequest;
import com.financialfraudassistant.dto.TransactionResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController @RequestMapping("/api/transactions")
public class TransactionController {
    private final CurrentUserService currentUserService; private final TransactionService transactionService;
    public TransactionController(CurrentUserService currentUserService, TransactionService transactionService) { this.currentUserService = currentUserService; this.transactionService = transactionService; }
    @GetMapping public List<TransactionResponse> list(Authentication authentication) { return transactionService.list(currentUserService.requireUser(authentication)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TransactionResponse create(Authentication authentication, @Valid @RequestBody TransactionRequest request) { return transactionService.create(currentUserService.requireUser(authentication), request); }
    @PutMapping("/{id}") public TransactionResponse update(Authentication authentication, @PathVariable Integer id, @Valid @RequestBody TransactionRequest request) { return transactionService.update(currentUserService.requireUser(authentication), id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Authentication authentication, @PathVariable Integer id) { transactionService.delete(currentUserService.requireUser(authentication), id); }
    @PostMapping("/import") public CsvImportResponse importCsv(Authentication authentication, @RequestParam("file") MultipartFile file) { return transactionService.importCsv(currentUserService.requireUser(authentication), file); }
}
