package com.backbase.dbs.transaction.mgmt.presentation.extension.simple;

import com.backbase.buildingblocks.backend.communication.extension.annotations.BehaviorExtension;
import com.backbase.buildingblocks.backend.communication.extension.annotations.PostHook;
import com.backbase.buildingblocks.backend.communication.extension.annotations.PreHook;
import org.apache.camel.Exchange;

/**
 * An example of how to provide a behavior extension using annotations.
 */
// @BehaviorExtension(
//     // The name parameter is used as a configuration key to enable/disable this specific extension.
//     // For example, "backbase.behavior-extensions.example-behavior.enabled=false".
//     // (Extensions are enabled by default.)
//     name = "example-behavior",
//     // The routeId parameter is the value returned by the getRouteId() method of the target SimpleExtensibleRouteBuilder
//     // and is typically exposed as a constant by that route builder.  E.g.:
//     routeId = com.backbase.dbs.transaction.mgmt.presentation.routes.ExportTransactionsRoute.ROUTE_ID
// )
// public class ExampleBehaviorExtension {

//     @PreHook
//     public void examplePreHook(Object body, Exchange exchange) {
//         // Custom pre-hook code here.
//         // Update the "body" parameter type according to the producer method signature (parameter type) of the route you extend.
//         // See the Camel documentation for details about how parameters are bound: http://camel.apache.org/bean-binding.html#BeanBinding-Parameterbinding
//         // If no pre-hook behavior is required, this method can be deleted.
//     }

//     @PostHook
//     public void examplePostHook(Object body, Exchange exchange) {
//         // Custom post-hook code here.
//         // Update the "body" parameter type according to the last consumer method signature (return type) of the route you extend.
//         // See the Camel documentation for details about how parameters are bound: http://camel.apache.org/bean-binding.html#BeanBinding-Parameterbinding
//         // If no post-hook behavior is required, this method can be deleted.
//     }

// }
