package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContextUtil;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.transaction.mgmt.persistence.domain.ParameterHolder;
import com.backbase.dbs.transaction.mgmt.persistence.domain.Transaction;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.patch.TransactionPatchRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.post.TransacionPostRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.repository.SpecificationBuilder;
import com.backbase.dbs.transaction.mgmt.persistence.repository.TransactionRepository;
import com.backbase.dbs.transaction.mgmt.persistence.service.TransactionService;
import com.backbase.dbs.transaction.mgmt.presentation.extension.config.TransactionManagerConfig;
import com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsGetResponseBody;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import javax.validation.Validator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExtendTransactionServiceTest {

    private static String ARR_1_ID = "ARR_1";
    private static String ARR_1_EXT_ID = "ARR_EXT_1";
    private static String ARR_2_ID = "ARR_2";
    private static String ARR_2_EXT_ID = "ARR_EXT_2";


    @Test
    void getTransactions_NoSubToken() {
        CursorApi cursorApi = mock(CursorApi.class);
        SecurityContextUtil securityContextUtil = mock(SecurityContextUtil.class);
        ArrangementsApi arrangementsApi = mock(ArrangementsApi.class);

        //ToDo: figure out how to bypass super calls
        ExtendTransactionService extendTransactionService = getService(cursorApi, securityContextUtil, arrangementsApi);

        ParameterHolder parameterHolder = new ParameterHolder();
        //TransactionsGetResponseBody transactions = extendTransactionService.getTransactions(parameterHolder);

        //No requests should go to arrangement api, as there was no subject in the token
        //verify(arrangementsApi, times(0)).getArrangements(any(), any(), any());
    }

    @Test
    void getTransactions_happyPath() {
        CursorApi cursorApi = mock(CursorApi.class);
        SecurityContextUtil securityContextUtil = mock(SecurityContextUtil.class);
        ArrangementsApi arrangementsApi = mock(ArrangementsApi.class);

        //ToDo: figure out how to bypass super calls
        ExtendTransactionService extendTransactionService = getService(cursorApi, securityContextUtil, arrangementsApi);

        ParameterHolder parameterHolder = new ParameterHolder();
        parameterHolder.setArrangementIds(List.of(ARR_1_ID, ARR_2_ID));

        when(securityContextUtil.getUserTokenClaim(any(), any()))
                .thenReturn(Optional.of("mock_sub"));

        //TransactionsGetResponseBody transactions = spyExtendTransactionService.getTransactions(parameterHolder);

    }

    private ExtendTransactionService getService(CursorApi cursorApi,
                                                SecurityContextUtil securityContextUtil,
                                                ArrangementsApi arrangementsApi){

        //This is not working... fix it
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        Page<Transaction> transactionPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll((Example) any(), (Sort) any()))
                .thenReturn(Collections.singletonList(transactionPage));

        return new ExtendTransactionService(
                mock(ApplicationContext.class),
                transactionRepository,
                mock(EventBus.class),
                mock(OriginatorContextUtil.class),
                mock(Validator.class),
                "",
                Transaction.AdditionsMode.EMBEDDED_ADDITIONS,
                mock(TransacionPostRequestMapper.class),
                mock(TransactionPatchRequestMapper.class),
                mock(SpecificationBuilder.class),
                mock(TransactionManagerConfig.class),
                cursorApi,
                securityContextUtil,
                arrangementsApi
        );

    }
}