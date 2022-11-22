package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItems;
import com.backbase.dbs.transaction.mgmt.presentation.domain.TransactionGetResponseBody;
import com.backbase.presentation.transaction.rest.spec.v2.transactions.TransactionItem;
import com.backbase.dbs.transaction.api.client.v2.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.api.client.v2.model.Currency;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Bai2ExportServiceTest {
    @Mock
    ArrangementsApi arrangementsApi;

    Bai2ExportService service;

    @BeforeEach
    void setUp() {
        reset(arrangementsApi);
        Bai2ExportService.FILE_ID.set(1234);
        service = new Bai2ExportService(arrangementsApi);
    }
    
    @Test
    void test_HappyPath_One_Account() throws IOException {
        var tgrb = getFirstX(5);

        programArrangementApi(tgrb.getTransactionItemList());

        String bai2 = ((Bai2Resource)service.generateBai2(tgrb)).getAsString();
        String[] lines = bai2.split("\n");
        System.out.println(bai2);
        assertEquals(17, lines.length);
        // Check recordsize of every line
        checkRecordLength(lines);
        // Now that recordsize has been checked, perform the rest of our tests with padding removed.
        lines = getLinesWithPaddingRemoved(lines);
        assertTrue(lines[0].matches("01,987654321,987654321,\\d{6},\\d{4},1234,\\d{2},\\d{2},2/"));
        assertTrue(lines[1].matches("02,BANK_NAME_NOT_CONFIGURED,987654321,1,\\d{6},\\d{4},,2/"));
        assertEquals("03,46240613071-1,USD,,,,,/", lines[2]);        

        checkTransactionLine(lines[3], "409", "1000", "Note increase - transfer");
        //checkTransactionLine(lines[4], "409", "1200", "Note increase - transfer");
        assertEquals("88,Note increase - transfer/", lines[4]);
        checkTransactionLine(lines[5], "409", "1200", "");
        assertEquals("88,Note increase - transfer with a really really long note that is definitely go", lines[6]);
        assertEquals("88,ing to cause things to break into multiple lines for unit test purposes/", lines[7]);
        checkTransactionLine(lines[8], "108", "2200", "");
        assertEquals("88,Special payment/", lines[9]);
        checkTransactionLine(lines[10], "409", "100000", "");
        assertEquals("88,Note increase - transfer/", lines[11]);
        checkTransactionLine(lines[12], "409", "10000", "");
        assertEquals("88,Note increase - transfer/", lines[13]);
        assertEquals("49,-110000,13/", lines[14]);
        assertEquals("98,-110000,1,15/", lines[15]);
        assertEquals("99,-110000,1,17/", lines[16]);
    }

    @Test
    void test_zero_transactions() throws IOException {
        var tgrb = getFirstX(0);

        programArrangementApi(tgrb.getTransactionItemList());

        String bai2 = ((Bai2Resource)service.generateBai2(tgrb)).getAsString();
        String[] lines = bai2.split("\n");
        System.out.println(bai2);
        assertEquals(4, lines.length);
        // Check recordsize of every line
        checkRecordLength(lines);
        // Now that recordsize has been checked, perform the rest of our tests with padding removed.
        lines = getLinesWithPaddingRemoved(lines);
        assertTrue(lines[0].matches("01,987654321,987654321,\\d{6},\\d{4},1234,\\d{2},\\d{2},2/"), lines[0]);
        assertTrue(lines[1].matches("02,BANK_NAME_NOT_CONFIGURED,987654321,1,\\d{6},\\d{4},,2/"), lines[1]);
        assertEquals("98,0,0,2/", lines[2], lines[2]);
        assertEquals("99,0,1,4/", lines[3], lines[3]);
    }

    @Test
    void test_multiline_text() throws IOException {
        var tgrb = getFirstX(2);

        programArrangementApi(tgrb.getTransactionItemList());
        service = new MultiLineExport(arrangementsApi);

        // We will need better test data here, so that the tx note actually spans multiple lines
        String bai2 = ((Bai2Resource)service.generateBai2(tgrb)).getAsString();

        String[] lines = bai2.split("\n");

        System.out.println(bai2);

        assertEquals(11, lines.length);
        // Check recordsize of every line
        checkRecordLength(lines);
        // Now that recordsize has been checked, perform the rest of our tests with padding removed.
        lines = getLinesWithPaddingRemoved(lines);

        assertTrue(lines[0].matches("01,987654321,987654321,\\d{6},\\d{4},1234,\\d{2},\\d{2},2/"));
        assertTrue(lines[1].matches("02,BANK_NAME_NOT_CONFIGURED,987654321,1,\\d{6},\\d{4},,2/"));
        assertEquals("03,46240613071-1,USD,,,,,/", lines[2]);        

        checkTransactionLine(lines[3], "999", "1000", "Note increase - transfer");
        assertEquals("88,Note increase - transfer/", lines[4]);

        checkTransactionLine(lines[5], "999", "1200", "Note increase - transfer");
        assertEquals("88,Note increase - transfer with a really really long note that is definitely go", lines[6]);
        assertEquals("88,ing to cause things to break into multiple lines for unit test purposes/", lines[7]);

        assertEquals("49,-2200,7/", lines[8]);
        assertEquals("98,-2200,1,9/", lines[9]);
        assertEquals("99,-2200,1,11/", lines[10]);
    }

    @Test
    void test_no_text() throws IOException {
        var tgrb = getFirstX(5);

        programArrangementApi(tgrb.getTransactionItemList());
        service = new NoTextExport(arrangementsApi);

        String bai2 = ((Bai2Resource)service.generateBai2(tgrb)).getAsString();
        String[] lines = bai2.split("\n");
        System.out.println(bai2);
        assertEquals(11, lines.length);
        // Check recordsize of every line
        checkRecordLength(lines);
        // Now that recordsize has been checked, perform the rest of our tests with padding removed.
        lines = getLinesWithPaddingRemoved(lines);
        assertTrue(lines[0].matches("01,987654321,987654321,\\d{6},\\d{4},1234,\\d{2},\\d{2},2/"));
        assertTrue(lines[1].matches("02,BANK_NAME_NOT_CONFIGURED,987654321,1,\\d{6},\\d{4},,2/"));
        assertEquals("03,46240613071-1,USD,,,,,/", lines[2]);        

        checkTransactionLine(lines[3], "777", "1000", "/");
        checkTransactionLine(lines[4], "777", "1200", "/");
        checkTransactionLine(lines[5], "222", "2200", "/");
        checkTransactionLine(lines[6], "777", "100000", "/");
        checkTransactionLine(lines[7], "777", "10000", "/");

        assertEquals("49,-110000,7/", lines[8]);
        assertEquals("98,-110000,1,9/", lines[9]);
        assertEquals("99,-110000,1,11/", lines[10]);
    }

    private void checkTransactionLine(String line, String code, String amount, String note) {
        assertTrue(line.startsWith("16," + code + "," + amount + ",0"));
        assertTrue(line.endsWith(",") || line.endsWith(",/"));
    }
    private TransactionGetResponseBody getFirstX(int x) throws IOException {
        final List<TransactionItem> xaction;
        try(Stream<String> lines = Files.lines(getCsvPath())) {
            xaction = lines
                .skip(1)
                .limit(x)
                .map(s -> s.replace("\"", ""))
                .map(this::csvToTransaction)
                .collect(Collectors.toList());
        }
        var tgrb = new TransactionGetResponseBody();
        tgrb.setTransactionItemList(xaction);
        tgrb.setTotalElements((long)xaction.size());

        return tgrb;
    }

    private void checkRecordLength(String[] lines) {
        for (String thisLine: lines) { // Check recordsize of every line
            assertEquals(service.getRecordSize(), thisLine.length());
        }
    }

    private String[] getLinesWithPaddingRemoved(String[] paddedLines) {
        return Arrays.stream(paddedLines)
            .map(line -> StringUtils.stripEnd(line, String.valueOf(Bai2ExportService.PADDING_CHAR)))
            .toArray(String[]::new);
    }

    private Path getCsvPath() {
        return Paths.get("src/test/resources/test_xaction_data.csv");
    }

    private TransactionItem csvToTransaction(String csv) {
        String[] part = csv.split(",");
        return new TransactionItem()
        .id(part[0])
        .arrangementId(part[1])
        .reference(part[2])
        .notes(part[3].substring(0, part[3].length()-3)) // This is a hack to preserve BBAN
        .description(part[5])
        .typeGroup(part[6])
        .type(part[7])
        .transactionAmountCurrency(new Currency().amount(part[11]).currencyCode(part[12]))
        .creditDebitIndicator(CreditDebitIndicator.valueOf(part[13]));
    }

    private void programArrangementApi(List<TransactionItem> xaction) {
        var arIds = xaction.stream()
            .map(TransactionItem::getArrangementId)
            .distinct()
            .collect(Collectors.toList());

        if (arIds.isEmpty()) return; // no need to stub

        Map<String, AccountArrangementItem> bbanMap = arIds.stream()
            .map(arId -> mapArrangementId2Item(arId, xaction))
            .reduce(new HashMap<String, AccountArrangementItem>(), 
                (m, a) -> {m.put(a.getId(), a); return m;}, 
                (m1, m2) -> {m1.putAll(m2); return m1;});

        doAnswer(call -> {
            List<String> ids = (List<String>)call.getArgument(1);
            return new AccountArrangementItems()
                .arrangementElements(ids.stream()
                    .map(bbanMap::get)
                    .collect(Collectors.toList()));
        }).when(arrangementsApi).getArrangements(isNull(), anyList(), isNull());
    }

    private AccountArrangementItem mapArrangementId2Item(String arId, List<TransactionItem> xactionList) {
        return xactionList.stream()
            .filter(t -> t.getArrangementId().equals(arId))
            .findFirst()
            .map(t -> {
                var account = new AccountArrangementItem();
                account.id(t.getArrangementId()).BBAN(t.getNotes()).currency("USD");
                return account;
            })
            .orElseThrow();
    }
}

class MultiLineExport extends Bai2ExportService {

    public MultiLineExport(ArrangementsApi arrangementsApi) {
        super(arrangementsApi);
    }

// Method has complex logic now, and can't be overridden for testing anymore
//    @Override
//    protected Optional<List<String>> getTransactionText(TransactionItem t, AccountArrangementItem account,
//            boolean isCredit) {
//        return Optional.of(List.of(t.getDescription(), t.getType(), t.getTypeGroup()));
//    }

    @Override
    protected int getTransactionType(TransactionItem xaction, boolean isCredit, AccountArrangementItem account) {
        return isCredit ? 111 : 999;
    } 
}

class NoTextExport extends Bai2ExportService {

    public NoTextExport(ArrangementsApi arrangementsApi) {
        super(arrangementsApi);
    }

    @Override
    protected Optional<List<String>> getTransactionText(TransactionItem t, AccountArrangementItem account,
            boolean isCredit) {
        return Optional.empty();
    }

    @Override
    protected int getTransactionType(TransactionItem xaction, boolean isCredit, AccountArrangementItem account) {
        return isCredit ? 222 : 777;
    } 
}