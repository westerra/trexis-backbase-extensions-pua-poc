package com.backbase.dbs.product.controller;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.ApiClient;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ProductSummaryApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.OrderByField;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.ProductSummaryItem;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.SortDirection;
import com.backbase.dbs.product.services.ProductSummaryServiceCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@RestController
@Primary
public class ProductSummaryController extends ProductSummaryApi {

    ProductSummaryServiceCustom productSummaryServiceCustom;

    public ProductSummaryController(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    public List<ProductSummaryItem> getArrangementsByBusinessFunction(String businessFunction, String resourceName, String privilege, Boolean withLatestBalances, Boolean maskIndicator, Boolean debitAccount, Boolean creditAccount, Boolean externalTransferAllowed, String productKindName, List<String> legalEntityIds, String sourceId, Boolean favorite, String searchTerm, Boolean customOrder, Boolean favoriteFirst, Integer from, String cursor, Integer size, List<OrderByField> orderBy, SortDirection direction, String contentLanguage) throws RestClientException {

        if ("getWesterraProductList".equalsIgnoreCase(businessFunction)) {
            return productSummaryServiceCustom.getEligibleProductList();
        }
        return super.getArrangementsByBusinessFunction(businessFunction, resourceName, privilege, withLatestBalances, maskIndicator, debitAccount, creditAccount, externalTransferAllowed, productKindName, legalEntityIds, sourceId, favorite, searchTerm, customOrder, favoriteFirst, from, cursor, size, orderBy, direction, contentLanguage);
    }
}
