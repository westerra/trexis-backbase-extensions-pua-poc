package com.backbase.dbs.transaction.mgmt.presentation.extension.advanced;

import com.backbase.buildingblocks.backend.communication.extension.ExtensibleRouteBuilder;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * An example of entirely replacing the out-of-the-box Camel RouteBuilder to provide completely different behavior.
 */
@Component
@Primary
public class CustomReplacingRouteBuilder {//extends ExtensibleRouteBuilder {

    // public CustomReplacingRouteBuilder() {
    //     // Use the same route ID as the RouteBuilder you wish to replace, for example:
    //     //super(com.backbase.dbs.transaction.mgmt.presentation.routes.ExportTransactionsRoute.ROUTE_ID);
    // }

    // @Override
    // public void configure() throws Exception {
    // 	// TODO Redefine the route, changing the order, adding transformations, replacing it completely, etc
    //     // from(originalFromUri).to(CustomEndpoints.CUSTOM_ENDPOINT)
    // }

}
