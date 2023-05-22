package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContextUtil;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItem;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItems;
import com.backbase.dbs.transaction.mgmt.persistence.domain.ParameterHolder;
import com.backbase.dbs.transaction.mgmt.persistence.domain.Transaction;
import com.backbase.dbs.transaction.mgmt.persistence.domain.TransactionEmbeddedAdditions;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.patch.TransactionPatchRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.post.TransacionPostRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.repository.SpecificationBuilder;
import com.backbase.dbs.transaction.mgmt.persistence.repository.TransactionRepository;
import com.backbase.dbs.transaction.mgmt.presentation.extension.config.TransactionManagerConfig;
import com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsGetResponseBody;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Validator;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtendTransactionServiceTest {

    private static String ARR_1_ID = "ARR_1";
    private static String ARR_2_ID = "ARR_2";

    @Mock
    CursorApi cursorApi;
    @Mock
    SecurityContextUtil securityContextUtil;
    @Mock
    ArrangementsApi arrangementsApi;
    @Mock
    ApplicationContext applicationContext;
    @Mock
    EventBus eventBus;
    @Mock
    OriginatorContextUtil originatorContextUtil;
    @Mock
    Validator validator;
    @Mock
    TransacionPostRequestMapper transacionPostRequestMapper;
    @Mock
    TransactionPatchRequestMapper transactionPatchRequestMapper;
    @Mock
    SpecificationBuilder specificationBuilder;
    @Mock
    TransactionRepository transactionRepository;

    TransactionManagerConfig.Ingestion transactionManagerIngestionConfig;

    TransactionManagerConfig transactionManagerConfig;

    Page<Transaction> dbTransactions;

    ExtendTransactionService extendTransactionService;

    @BeforeEach
    void setUp() {
        transactionManagerConfig = new TransactionManagerConfig();
        transactionManagerIngestionConfig = new TransactionManagerConfig.Ingestion();
        transactionManagerIngestionConfig.setDateFlipEnabled(false);
        transactionManagerIngestionConfig.setEnabled(false);
        transactionManagerConfig.setIngestion(transactionManagerIngestionConfig);

        extendTransactionService = new ExtendTransactionService(
                applicationContext,
                transactionRepository,
                eventBus,
                originatorContextUtil,
                validator,
                "",
                Transaction.AdditionsMode.EMBEDDED_ADDITIONS,
                transacionPostRequestMapper,
                transactionPatchRequestMapper,
                specificationBuilder,
                transactionManagerConfig,
                cursorApi,
                securityContextUtil,
                arrangementsApi
        );
        ReflectionTestUtils.setField(extendTransactionService, "secondarySort", "sequenceNumber");
    }

    @Test
    void getTransactions_happyPath() {
        // setup
        setupTransactions("BILLED");

        ParameterHolder parameterHolder = new ParameterHolder();
        parameterHolder.setArrangementIds(List.of(ARR_1_ID, ARR_2_ID));
        parameterHolder.setCheckSerialNumbers(List.of(1L));
        parameterHolder.setDirection("DESC");
        parameterHolder.setOrderBy("bookingDate");

        // test
        TransactionsGetResponseBody actual = extendTransactionService.getTransactions(parameterHolder);

        // verify
        // Nothing to verify, happy path
    }

    private void setupTransactions(String billingStatus) {
        var transaction1 = new TransactionEmbeddedAdditions();
        transaction1.setId("1");
        transaction1.setAdditions(
                Map.of("bookingDateTime", "2023-01-01T12:00:00", "createdDateTime", "2022-12-02T12:00:00"));
        transaction1.setArrangementId(ARR_1_ID);
        transaction1.setBillingStatus(billingStatus);

        var transaction2 = new TransactionEmbeddedAdditions();
        transaction2.setId("2");
        transaction2.setAdditions(
                Map.of("bookingDateTime", "2023-01-01T18:00:00", "createdDateTime", "2022-12-01T18:00:00"));
        transaction2.setArrangementId(ARR_1_ID);
        transaction2.setBillingStatus(billingStatus);

        var transaction3 = new TransactionEmbeddedAdditions();
        transaction3.setId("3");
        transaction3.setAdditions(
                Map.of("bookingDateTime", "2023-01-02T12:00:00", "createdDateTime", "2022-12-01T12:00:00"));
        transaction3.setArrangementId(ARR_1_ID);
        transaction3.setBillingStatus(billingStatus);

        dbTransactions = new PageImpl<>(List.of(transaction1, transaction2, transaction3));

        when(transactionRepository.findAll(nullable(Specification.class),
                any(PageRequest.class))).thenReturn(dbTransactions);
    }

}