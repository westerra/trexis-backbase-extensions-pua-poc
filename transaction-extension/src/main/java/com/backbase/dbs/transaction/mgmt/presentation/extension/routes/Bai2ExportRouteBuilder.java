package com.backbase.dbs.transaction.mgmt.presentation.extension.routes;
 
import com.backbase.dbs.transaction.mgmt.presentation.routes.constants.RoutesEndpoints;
import com.backbase.dbs.transaction.mgmt.presentation.extension.service.Bai2ExportService;
import com.backbase.dbs.transaction.mgmt.presentation.routes.ExportTransactionsRoute;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Predicate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
 
/**
 * Extend the transaction-manager to allow BAI2 Export.
 */
@Component
@Primary
@Slf4j
public class Bai2ExportRouteBuilder extends ExportTransactionsRoute {
    private Predicate isBai2 = header("exportType").isEqualToIgnoreCase("BAI2");
 
    @Override
    public void configure() {
        interceptSendToEndpoint(RoutesEndpoints.DIRECT_EXPORT_TRANSACTIONS_CSV)
            .skipSendToOriginalEndpoint()
            .when(isBai2).to(Bai2ExportService.DIRECT_EXPORT_TRANSACTIONS_BAI2);
        super.configure();
        log.info("Created route with id: {}", Bai2ExportService.DIRECT_EXPORT_TRANSACTIONS_BAI2);
    }
}