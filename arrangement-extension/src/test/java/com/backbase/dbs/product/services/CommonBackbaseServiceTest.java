package com.backbase.dbs.product.services;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonBackbaseServiceTest {

    private final ArrangementsApi arrangementsApi = mock(ArrangementsApi.class);

    @Test
    void getArrangementsById_happyPath() {
        String mockArrangementId = "mockId";
        when(arrangementsApi.getArrangementById(mockArrangementId, false))
                .thenReturn(new AccountArrangementItem().id(mockArrangementId));
        CommonBackbaseService commonBackbaseService = new CommonBackbaseService(arrangementsApi);
        AccountArrangementItem arrangementItem = commonBackbaseService.getArrangementsById("mockId");
        assertTrue(arrangementItem.getId().equals(mockArrangementId));
    }
}