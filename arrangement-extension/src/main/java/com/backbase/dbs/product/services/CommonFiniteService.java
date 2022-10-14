package com.backbase.dbs.product.services;

import com.finite.api.AccountsApi;
import com.finite.api.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.finite.FiniteConfiguration;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonFiniteService {

    private final FiniteConfiguration finiteConfiguration;
    private final AccountsApi accountsApi;

    //For nicknames we set the title in finite.  The name represent the original name of the account
    //The title field maps to bankAliasName during ingestion
    public void updateAccountTitle(String accountId, String title, String traceId){
        Account account = new Account();
        account.setId(accountId);
        account.setTitle(title);
        try {
            accountsApi.putAccount(account.getId(), account, traceId + "_PREFERENCE_UPDATE", null, false, finiteConfiguration.getEnhance().isAccounts());
        } catch(Exception ex){
            log.error("Unable to update account title: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
