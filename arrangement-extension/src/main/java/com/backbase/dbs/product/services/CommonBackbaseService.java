package com.backbase.dbs.product.services;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonBackbaseService {
    private final ArrangementsApi arrangementsApi;

    public AccountArrangementItem getArrangementsById(String arrangementId) {
        return arrangementsApi.getArrangementById(arrangementId, false);
    }
}
