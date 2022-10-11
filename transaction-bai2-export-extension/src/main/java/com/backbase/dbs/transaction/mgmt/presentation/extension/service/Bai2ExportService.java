package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import com.backbase.presentation.transaction.rest.spec.v2.transactions.TransactionItem;
import com.backbase.dbs.transaction.api.client.v2.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.mgmt.presentation.domain.TransactionGetResponseBody;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.apache.camel.Body;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Bai2ExportService {
    public static final String DIRECT_EXPORT_TRANSACTIONS_BAI2 = "direct:transactions.export.bai2";

    private static final AtomicInteger FILE_ID = new AtomicInteger();
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int TT_MISC_CREDIT = 108;
    private static final int TT_MISC_DEBIT = 409;

    private final ArrangementsApi arrangementsApi;

    @Value("${backbase.transaction.ofx.export.bankRoutingNumber:123456789}")
    private String routingNumber = "081000210"; // Maybe removed these assignments?

    @Value("${backbase.transaction.ofx.export.bai2BankName:BANK}")
    private String bai2BankName = "firstbank";
    
    public String generateBai2(@Body TransactionGetResponseBody request) {
        StringWriter report = new StringWriter();
        Formatter out = new Formatter(report);
        List<TransactionItem> transactions = request.getTransactionItemList();

        Set<String> arrangementIds = transactions.stream()
             .map(TransactionItem::getArrangementId)
             .distinct()
             .collect(Collectors.toSet());

        Map<String, AccountArrangementItem> accountMap = getAccountMap(arrangementIds);
        Totals fileTotals = new Totals();

        outputFileHeader(out);

        outputGroupHeader(out);

        arrangementIds.stream()
            .forEach(arId -> 
                        fileTotals.add(
                            outputAccountRecord(out, 
                            transactions, 
                            arId,
                            accountMap.get(arId)))
            );

        outputGroupFooter(out, arrangementIds.size(), fileTotals.withHeaders());
        
        outputFileFooter(out, fileTotals.withHeaders());

        return report.toString();
    }

    private Map<String, AccountArrangementItem> getAccountMap(final Set<String> arrangementIds) {
        List<AccountArrangementItem> accountList = getArrangementsByIds(new ArrayList<>(arrangementIds));

        return arrangementIds.stream()
            .map(arId -> getAccountById(arId, accountList))
             .reduce(new HashMap<String, AccountArrangementItem>(), 
                (m, t) -> {m.put(t.getId(),t); return m;}, 
                (m1, m2) -> {m1.putAll(m2); return m1;});
    }

    private AccountArrangementItem getAccountById(String arrangementId, List<AccountArrangementItem> accountList) {
        return accountList.stream()
            .filter(a -> a.getId().equals(arrangementId))
            .findFirst()
            .orElseThrow();
    }

    private Totals outputAccountRecord(Formatter out, 
                List<TransactionItem> transactions, 
                String arrangementId, 
                AccountArrangementItem account) 
    {
        Totals accountTotals = new Totals();

        outputAccountHeader(out, account.getBBAN(), account.getCurrency());

        transactions.stream()
             .filter(t -> t.getArrangementId().equals(arrangementId))
             .forEach(t -> outputTransaction(out, t, accountTotals, account));

        outputAccountFooter(out, accountTotals.withHeaders());

        return accountTotals;
    }
    
    private void outputTransaction(Formatter out, TransactionItem t, Totals accountTotals, AccountArrangementItem account) {
        outputTransaction(out, t, accountTotals, t.getCreditDebitIndicator() == CreditDebitIndicator.CRDT, account);
    }

    private void outputTransaction(Formatter out, 
                                   TransactionItem t, 
                                   Totals accountTotals, 
                                   boolean isCredit,
                                   AccountArrangementItem account) {
        int amount = new BigDecimal(t.getTransactionAmountCurrency().getAmount())
            .multiply(HUNDRED)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

        accountTotals.addAmount(isCredit ? amount : -amount);

        out.format("16,%03d,%d,0,%s,,",
            getTransactionType(t, isCredit, account),
            Math.abs(amount),
            t.getId());
            
        var optionalText = getTransactionText(t, account, isCredit);
        if (optionalText.isPresent()) {
            out.format("%s%n", optionalText.get().get(0));
            optionalText.get().stream()
            .skip(1)
            .forEach(s -> {out.format("88,%s%n", s); accountTotals.incrementCount();});
        } else {
            out.format("/%n");
        }
    }

    /*
     * This method returns the BAI2 transaction type. The default implementation will
     * return 108 (Any Credit) or 409 (Any Debit). The method can be overridden in a
     * subclass to provide more detailed codes.
     * 
     * @param t the <code>TransactionItem</code> being described by the text
     * @param account the <code>AccountItem</code> to which the <code>TransactionItem</code> belongs
     * @param isCredit <code>true</code> if the transaction credits the amount to the account
     *  
     * @return BAI2 transaction type value
     */
    protected int getTransactionType(TransactionItem xaction, boolean isCredit, AccountArrangementItem account) {
        assert account != null; // Mostly to make SonarQube happy, but we expect this 
        assert xaction != null; // Mostly to make SonarQube happy, but we expect this
        return isCredit ? TT_MISC_CREDIT : TT_MISC_DEBIT;
    }

    /*
     * This method can be overridden in a subclass to allow for more complex
     * multiline text. It should return <code>Optional.empty()</code> if there
     * is <em>no</em> text to accompany the transaction. If it returns a 
     * <code>List</code>, it is expected to have at least one element.
     * 
     * @param t the <code>TransactionItem</code> being described by the text
     * @param account the <code>AccountItem</code> to which the <code>TransactionItem</code> belongs
     * @param isCredit <code>true</code> if the transaction credits the amount to the account
     * 
     * @return an optional list of strings. If the <code>Optional</code> is not empty, 
     *         the <code>List</code> <em>must</em> contain at least one item.
     */
    protected Optional<List<String>> getTransactionText(TransactionItem t, AccountArrangementItem account, boolean isCredit) {
        assert account != null; // Mostly to make SonarQube happy, but we expect this
        assert isCredit != !isCredit; // Entirely to make SonarQube happy
        assert t.getDescription() != null : "TransactionItem::getDescription has a validator of not null";
        return Optional.of(List.of(t.getDescription()));
    }

    private void outputAccountFooter(Formatter out, Totals accountTotals) {
        out.format("49,%d,%d/%n", accountTotals.getAmount(), accountTotals.getCount());
    }

    private void outputAccountHeader(Formatter out, String bBan, String currency) {
        out.format("03,%s,%s,,,,,/%n", bBan, currency);
    }

    private void outputGroupFooter(Formatter out, int numAccounts, Totals fileTotals) {
        out.format("98,%d,%d,%d/%n", fileTotals.getAmount(), numAccounts, fileTotals.getCount());
    }

    private void outputGroupHeader(Formatter out) {
        out.format("02,%S,%s,1,%3$ty%3$tm%3$td,%3$tH%3$tM,,2/%n",
            bai2BankName,
            routingNumber,
            new Date());
    }

    private void outputFileFooter(Formatter out, Totals fileTotals) {
        out.format("99,%d,1,%d/%n", fileTotals.getAmount(), fileTotals.getCount());
    }

    private void outputFileHeader(Formatter out) {
        out.format("01,%1$s,%1$s,%2$ty%2$tm%2$td,%2$tH%2$tM,%3$d,,,2/%n",
            routingNumber,
            new Date(),
            FILE_ID.getAndIncrement());
    }

    private List<AccountArrangementItem> getArrangementsByIds(List<String> arrangementIds) {
        if (arrangementIds.isEmpty()) {
            return List.of();
        }
        return arrangementsApi.getArrangements(null, arrangementIds, null).getArrangementElements();
    }

    @Data
    private static class Totals {
        private long amount;
        private int count;

        public Totals incrementCount() {
            count += 1;
            return this;
        }

        public Totals addAmount(int increment) {
            amount += increment;
            return incrementCount();
        }

        public Totals add(Totals subTotal) {
            this.amount += subTotal.amount;
            this.count += subTotal.count;
            return this;
        }

        public Totals withHeaders() {
            count += 2;
            return this;
        }
    }
}