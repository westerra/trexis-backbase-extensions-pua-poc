package com.backbase.dbs.product.services;

import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.AccountsApi;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.ProductItem;
import com.backbase.dbs.arrangement.arrangement_manager.v2.model.ProductSummaryItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSummaryServiceCustom {

    public List<ProductSummaryItem> getEligibleProductList() {

        List<ProductSummaryItem> productSummaryItemList = new ArrayList<>();

        // Prime Share
        ProductSummaryItem primeShare = new ProductSummaryItem();
        ProductItem prime = new ProductItem();
        prime.setExternalId("S-0");
        prime.setExternalTypeId("0");
        prime.setTypeName("Prime Share");
        primeShare.setName("Prime Share");

        primeShare.setAccountInterestRate(BigDecimal.valueOf(6));
        primeShare.setProduct(prime);
        productSummaryItemList.add(primeShare);

        // Secondary Share
        ProductSummaryItem secondaryShare = new ProductSummaryItem();
        ProductItem secondary = new ProductItem();
        secondary.setExternalId("S-1");
        secondary.setExternalTypeId("1");
        secondary.setTypeName("Secondary Share");
        secondaryShare.setName("Secondary Share");
        secondaryShare.setAccountInterestRate(BigDecimal.valueOf(5));
        secondaryShare.setProduct(secondary);
        productSummaryItemList.add(secondaryShare);

        // Free Checking
        ProductSummaryItem freeChecking = new ProductSummaryItem();
        ProductItem freeCheck = new ProductItem();
        freeCheck.setExternalId("S-3");
        freeCheck.setExternalTypeId("3");
        freeCheck.setTypeName("Free Checking");
        freeChecking.setName("Free Checking");
        freeChecking.setAccountInterestRate(BigDecimal.valueOf(0));
        freeChecking.setProduct(freeCheck);
        productSummaryItemList.add(freeChecking);

        // Interest Checking
        ProductSummaryItem interestChecking = new ProductSummaryItem();
        ProductItem interestCheck = new ProductItem();
        interestCheck.setExternalId("S-4");
        interestCheck.setExternalTypeId("4");
        interestCheck.setTypeName("Interest Checking");
        interestChecking.setName("Interest Checking");
        interestChecking.setAccountInterestRate(BigDecimal.valueOf(1.5));
        interestChecking.setProduct(interestCheck);
        productSummaryItemList.add(interestChecking);

        // Holiday Club
        ProductSummaryItem holidayClub = new ProductSummaryItem();
        ProductItem holiday = new ProductItem();
        holiday.setExternalId("S-5");
        holiday.setExternalTypeId("5");
        holiday.setTypeName("Holiday Club");
        holidayClub.setName("Holiday Club");
        holidayClub.setAccountInterestRate(BigDecimal.valueOf(4.5));
        holidayClub.setProduct(holiday);
        productSummaryItemList.add(holidayClub);

        return productSummaryItemList;

    }
}
