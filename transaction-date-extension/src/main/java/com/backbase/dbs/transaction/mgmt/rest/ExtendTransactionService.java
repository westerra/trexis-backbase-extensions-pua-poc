package com.backbase.dbs.transaction.mgmt.rest;

import com.backbase.dbs.transaction.mgmt.presentation.business.Entitlements;
import com.backbase.dbs.transaction.mgmt.presentation.business.GetEnumValues;
import com.backbase.dbs.transaction.mgmt.presentation.business.ReadTransactions;
import com.backbase.dbs.transaction.mgmt.presentation.business.TransactionsCheckImages;
import com.backbase.dbs.transaction.mgmt.presentation.domain.QueryParameters;
import com.backbase.dbs.transaction.mgmt.presentation.domain.TransactionGetResponseBody;
import com.backbase.dbs.transaction.mgmt.presentation.service.impl.TransactionLiveService;
import com.backbase.dbs.transaction.mgmt.presentation.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Primary
@Service
@Slf4j
public class ExtendTransactionService extends TransactionLiveService {

    public ExtendTransactionService(ApplicationContext applicationContext, GetEnumValues getEnumValues, ValidationUtil validationUtil, ObjectMapper objectMapper, ReadTransactions readTransactions, TransactionsCheckImages transactionsCheckImages, Entitlements entitlements) {
        super(applicationContext, getEnumValues, validationUtil, objectMapper, readTransactions, transactionsCheckImages, entitlements);
    }

    @Override
    public TransactionGetResponseBody getTransactions(QueryParameters params, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        var response = super.getTransactions(params, httpServletRequest, httpServletResponse);

        // Swap booking date and value date
        response.getTransactionItemList().stream()
                .forEach(tx -> {
                    var originalBookingDate = tx.getBookingDate();
                    tx.setBookingDate(tx.getValueDate());
                    tx.setValueDate(originalBookingDate);
                });

        //System.out.println("Returning extended tx");
        return response;
    }
}
