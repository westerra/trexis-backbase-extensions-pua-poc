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

import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItem;
import com.backbase.presentation.transaction.rest.spec.v2.transactions.TransactionItem;
import com.backbase.dbs.transaction.api.client.v2.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.mgmt.presentation.domain.TransactionGetResponseBody;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import lombok.SneakyThrows;
import org.apache.camel.Body;
import org.apache.camel.Consume;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.HttpResource;

@Component
@RequiredArgsConstructor
public class Bai2ExportService {
    public static final String DIRECT_EXPORT_TRANSACTIONS_BAI2 = "direct:transactions.export.bai2";

    public static final AtomicInteger FILE_ID = new AtomicInteger();
    
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int TT_MISC_CREDIT = 108;
    private static final int TT_MISC_DEBIT = 409;

    protected static final char PADDING_CHAR = ' ';

    private final ArrangementsApi arrangementsApi;

    @Value("${backbase.transaction.ofx.export.bankRoutingNumber:123456789}")
    private String routingNumber = "987654321"; // Maybe removed these assignments?

    @Value("${backbase.transaction.ofx.export.bai2BankName:BANK}")
    private String bai2BankName = "bank_name_not_configured";

    @Value("${backbase.transaction.ofx.export.bai2BlockSize:80}")
    private Integer bai2BlockSize = 80; // change to int

    @Value("${backbase.transaction.ofx.export.bai2RecordSize:80}")
    private Integer bai2RecordSize = 80;

    @Consume(DIRECT_EXPORT_TRANSACTIONS_BAI2)
    public HttpResource generateBai2(@Body TransactionGetResponseBody request) {
        StringWriter report = new StringWriter();

        outputAllTheThings(new Formatter(report), 
                           request.getTransactionItemList());

        return new Bai2Resource(report.toString());
    }

    private void outputAllTheThings(Formatter out, List<TransactionItem> transactions) {
        outputAllTheThings(out, 
                           transactions, 
                           transactions.stream()
                                .map(TransactionItem::getArrangementId)
                                .distinct()
                                .collect(Collectors.toSet()));
    }

    private void outputAllTheThings(Formatter out, List<TransactionItem> transactions, Set<String> arrangementIds) {
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
                                accountMap.get(arId)
                            )
                        )
            );

        outputGroupFooter(out, arrangementIds.size(), fileTotals.withHeaders());
        
        outputFileFooter(out, fileTotals.withHeaders());
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

    @SneakyThrows
    private void outputTransaction(Formatter out, 
                                   TransactionItem t, 
                                   Totals accountTotals, 
                                   boolean isCredit,
                                   AccountArrangementItem account) {
        int amount = t.getTransactionAmountCurrency().getAmount()
            .multiply(HUNDRED)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

        accountTotals.addAmount(isCredit ? amount : -amount);

        // Line type (16 is transaction), transaction type, amount, funds type (0=immediately available), bank reference number, customer reference number
        String transactionText = String.format("16,%03d,%d,0,%s,%s,",
                getTransactionType(t, isCredit, account),
                Math.abs(amount),
                getBankTransactionReferenceNumber(t),
                getCustomerTransactionReferenceNumber(t));
            
        var optionalText = getTransactionText(t, account, isCredit);
        if (optionalText.isPresent()) {
            // If text is present, pad all the way to the end of the line
            transactionText = StringUtils.rightPad(transactionText, bai2RecordSize, PADDING_CHAR);
            out.out().append(transactionText).append("\n");
            optionalText.get().stream()
                .forEach(s -> {out.format("88,%s%n", s); accountTotals.incrementCount();});
        } else {
            // If text is not present, terminate and then pad
            transactionText = StringUtils.rightPad(transactionText + "/", bai2RecordSize, PADDING_CHAR);
            out.out().append(transactionText).append("\n");
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
        // Will need to call a service to get full tx details
        assert account != null; // Mostly to make SonarQube happy, but we expect this 
        assert xaction != null; // Mostly to make SonarQube happy, but we expect this
        return isCredit ? TT_MISC_CREDIT : TT_MISC_DEBIT;
    }


    /**
     * This method returns the bank transaction reference number.
     * By default, it will return the transaction id. It may be changed for
     * customer-specific applications.
     *
     * @param xaction the <code>TransactionItem</code> being described by the text
     *
     * @return bank transaction reference number
     */
    private String getBankTransactionReferenceNumber(TransactionItem xaction) {
        assert xaction != null; // Mostly to make SonarQube happy, but we expect this
        return "0";
    }

    /**
     * This method returns the customer transaction reference number.
     * By default, it will return the transaction id. It may be changed for
     * customer-specific applications.
     *
     * @param xaction the <code>TransactionItem</code> being described by the text
     *
     * @return customer transaction reference number
     */
    private String getCustomerTransactionReferenceNumber(TransactionItem xaction) {
        return (null != xaction.getCheckSerialNumber()) ? xaction.getCheckSerialNumber().toString() : "";
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

        String remainingDescription = t.getDescription();
        int maxNoteSize = bai2RecordSize - 3; // Account for the "88," that prepends all note lines
        ArrayList<String> allTheLines = new ArrayList<>();

        // Respect record and block sizes
        for (int i = 1; i < bai2BlockSize; i++) { // deliberately start at 1, since 1 is the tx (type 16) record
            if (remainingDescription.length() > maxNoteSize) {
                allTheLines.add(remainingDescription.substring(0, maxNoteSize)); // Add this part of the note in its entirety
                remainingDescription = remainingDescription.substring(maxNoteSize); // Remove the already-added part from the remaining note
            } else {
                allTheLines.add(StringUtils.rightPad(remainingDescription + "/", maxNoteSize, PADDING_CHAR)); // Add everything that remains, and terminate
                break;
            }
        }

        return Optional.of(allTheLines);
    }

    private void outputAccountFooter(Formatter out, Totals accountTotals) {
        var formattedString = String.format("49,%d,%d/", accountTotals.getAmount(), accountTotals.getCount());
        padAndOutput(out, formattedString);
    }

    private void outputAccountHeader(Formatter out, String bBan, String currency) {
        var formattedString = String.format("03,%s,%s,,,,,/", bBan, currency);
        padAndOutput(out, formattedString);
    }

    private void outputGroupFooter(Formatter out, int numAccounts, Totals fileTotals) {
        var formattedString = String.format("98,%d,%d,%d/", fileTotals.getAmount(), numAccounts, fileTotals.getCount());
        padAndOutput(out, formattedString);
    }

    private void outputGroupHeader(Formatter out) {
        var formattedString = String.format("02,%S,%s,1,%3$ty%3$tm%3$td,%3$tH%3$tM,,2/", bai2BankName, routingNumber, new Date());
        padAndOutput(out, formattedString);
    }

    private void outputFileFooter(Formatter out, Totals fileTotals) {
        var formattedString = String.format("99,%d,1,%d/", fileTotals.getAmount(), fileTotals.getCount());
        padAndOutput(out, formattedString);
    }

    private void outputFileHeader(Formatter out) {
        var formattedString = String.format("01,%1$s,%1$s,%2$ty%2$tm%2$td,%2$tH%2$tM,%3$d,%4$d,%5$d,2/",
                routingNumber,
                new Date(),
                FILE_ID.getAndIncrement(),
                bai2RecordSize,
                bai2BlockSize);
        padAndOutput(out, formattedString);
    }

    private List<AccountArrangementItem> getArrangementsByIds(List<String> arrangementIds) {
        if (arrangementIds.isEmpty()) {
            return List.of();
        }
        return arrangementsApi.getArrangements(null, arrangementIds, null).getArrangementElements();
    }

    @SneakyThrows
    private void padAndOutput(Formatter out, String formattedString) {
        formattedString = StringUtils.rightPad(formattedString, bai2RecordSize, PADDING_CHAR);
        out.out().append(formattedString).append("\n");
    }

    // For use by unit tests.
    protected int getRecordSize() {
        return this.bai2RecordSize;
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