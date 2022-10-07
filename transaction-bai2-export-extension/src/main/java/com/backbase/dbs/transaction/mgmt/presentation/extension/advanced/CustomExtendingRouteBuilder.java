package com.backbase.dbs.transaction.mgmt.presentation.extension.advanced;

import org.apache.camel.model.RouteDefinition;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * An example of how to extend the out-of-the-box RouteBuilder implementation to add custom camel logic before and
 * after the standard behavior.
 */
@Component
@Primary
public class CustomExtendingRouteBuilder extends com.backbase.dbs.transaction.mgmt.presentation.routes.ExportTransactionsRoute {

    // @Override
    // protected void configurePreHook(RouteDefinition rd) throws Exception {
    //     super.configurePreHook(rd);
    //     rd.to(CustomEndpoints.CUSTOM_PRE_HOOK_END_POINT);
    // }

    // @Override
    // protected void configurePostHook(RouteDefinition rd) throws Exception {
    //     super.configurePostHook(rd);
    //     rd.to(CustomEndpoints.CUSTOM_POST_HOOK_END_POINT);
    // }

}
