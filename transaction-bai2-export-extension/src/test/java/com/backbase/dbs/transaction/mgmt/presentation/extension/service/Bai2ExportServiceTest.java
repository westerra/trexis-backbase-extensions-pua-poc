package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.backbase.dbs.transaction.mgmt.presentation.domain.TransactionGetResponseBody;
import com.backbase.presentation.transaction.rest.spec.v2.transactions.TransactionItem;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItems;
import com.backbase.dbs.transaction.api.client.v2.model.CreditDebitIndicator;
import com.backbase.dbs.transaction.api.client.v2.model.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Bai2ExportServiceTest {
    @Mock
    ArrangementsApi arrangementsApi;

    Bai2ExportService service;

    @BeforeEach
    void setUp() {
        reset(arrangementsApi);
        service = new Bai2ExportService(arrangementsApi);
    }
    
    @Test
    void test_HappyPath_One_Account() throws IOException {
        var tgrb = getFirstX(5);

        programArrangementApi(tgrb.getTransactionItemList());

        String bai2 = service.generateBai2(tgrb);
        String[] lines = bai2.split("\n");
        assertEquals(11, lines.length);
        assertTrue(lines[0].matches("01,081000210,081000210,\\d{6},\\d{4},0,,,2/"));
        assertTrue(lines[1].matches("02,FIRSTBANK,081000210,1,\\d{6},\\d{4},,2/"));
        assertEquals("03,46240613071-1,USD,,,,,/", lines[2]);        

        checkTransactionLine(lines[3], "409", "1000", "Note increase - transfer");
        checkTransactionLine(lines[4], "409", "1200", "Note increase - transfer");
        checkTransactionLine(lines[5], "108", "2200", "Special payment");
        checkTransactionLine(lines[6], "409", "100000", "Note increase - transfer");
        checkTransactionLine(lines[7], "409", "10000", "Note increase - transfer");

        assertEquals("49,-110000,7/", lines[8]);
        assertEquals("98,-110000,1,9/", lines[9]);
        assertEquals("99,-110000,1,11/", lines[10]);
    }

    private void checkTransactionLine(String line, String code, String amount, String note) {
        assertTrue(line.startsWith("16," + code + "," + amount + ",0"));
        assertTrue(line.endsWith("," + note));
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