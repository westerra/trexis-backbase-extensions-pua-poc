package com.backbase.dbs.transaction.mgmt.presentation.extension.advanced;

import com.backbase.buildingblocks.backend.internalrequest.InternalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.Consume;
import org.springframework.stereotype.Component;

/**
 * The custom camel Endpoints to be used in the behavior extension.
 */
@Component
public class CustomEndpoints {

    private static final Logger log = LoggerFactory.getLogger(CustomEndpoints.class);

    public static final String CUSTOM_ENDPOINT = "direct:mycustomendpoint";
    public static final String CUSTOM_PRE_HOOK_END_POINT = "direct:preHook";
    public static final String CUSTOM_POST_HOOK_END_POINT = "direct:postHook";

    // The actual endpoint method for the custom exchange.
    // It do not necessarily need to be here, and could be defined in another component, as long as they're annotated and 
    // configured appropriately. Also, it does not have to be just one exchange.

    @Consume(uri = CUSTOM_ENDPOINT)
    public void customExchange(InternalRequest internalRequest) {
        log.info("======== In custom endpoint ========");
        // TODO.... implement your custom logic
    }

    @Consume(uri = CUSTOM_PRE_HOOK_END_POINT)
    public void createPaymentCard(InternalRequest internalRequest) {
        log.info("======== In pre hook example method ========");
        // TODO.... implement your custom pre hook
    }

    @Consume(uri = CUSTOM_POST_HOOK_END_POINT)
    public void postHookExample(InternalRequest internalRequest) {
        log.info("======== In post hook example method ========");
        // TODO.... implement your custom post hook
    }

}
