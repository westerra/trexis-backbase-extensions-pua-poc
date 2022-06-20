package com.backbase.dbs.actions.extended.handler;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContext;
import com.backbase.buildingblocks.backend.communication.event.EnvelopedEvent;
import com.backbase.buildingblocks.backend.communication.event.handler.EventHandler;
import com.backbase.dbs.actions.business.core.model.ActionRecipe;
import com.backbase.dbs.actions.eventhandling.event.InterpretedEvent;
import com.backbase.transaction.persistence.event.spec.v1.Transaction_;
import com.backbase.transaction.persistence.event.spec.v1.TransactionsAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.backbase.dbs.actions.business.core.persistence.specification.ActionRecipeJpaQueries.amountLessThanOrEqual;
import static com.backbase.dbs.actions.business.core.persistence.specification.ActionRecipeJpaQueries.eqArrangement;

@RequiredArgsConstructor
@Slf4j
public class TransactionOverThresholdHandler implements EventHandler<TransactionsAddedEvent> {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(EnvelopedEvent<TransactionsAddedEvent> envelopedEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Invoked handler {}", this.getClass().getName());
        }

        TransactionsAddedEvent transactionAddedEvent = envelopedEvent.getEvent();
        if (transactionAddedEvent == null
                || transactionAddedEvent.getTransactions() == null
                || transactionAddedEvent.getTransactions().isEmpty()
        ) {
            log.info("Event is invalid {}. Skipped handling", transactionAddedEvent);
            return;
        }

        transactionAddedEvent.getTransactions().stream()
                .peek(txn -> {
                    if ("pending".equalsIgnoreCase(txn.getBillingStatus())) {
                        log.info("transaction added event for pending transaction, skipping!");
                    }
                    log.info("transactionAmountCurrency: {}", txn.getTransactionAmountCurrency());
                    if (txn.getTransactionAmountCurrency() != null) {
                        log.info("transactionAmountCurrency.amount: {}", txn.getTransactionAmountCurrency().getAmount());
                    }
                    log.info("arrangementId: {}", txn.getArrangementId());
                    log.info("billingStatus: {}", txn.getBillingStatus());
                }).filter(txn -> txn.getTransactionAmountCurrency() != null && txn.getTransactionAmountCurrency().getAmount() != null && !StringUtils.isEmpty(txn.getArrangementId()) &&
                !"pending".equalsIgnoreCase(txn.getBillingStatus()))
                .forEach(validTxn -> publishEvent(validTxn, envelopedEvent.getOriginatorContext()));
    }

    private void publishEvent(Transaction_ validTx, OriginatorContext context) {
        String arrangement = validTx.getArrangementId();
        BigDecimal balance = validTx.getRunningBalance();

        // If you publish an event with a list of N transactions, BackBase will publish the first transaction event N times.
        // Thus, publish a list of only the one transaction we want to publish.
        TransactionsAddedEvent event = new TransactionsAddedEvent();
        event.setTransactions(List.of(validTx));

        InterpretedEvent interpretedEvent = InterpretedEvent.builder()
                .userId(null)
                .serviceAgreementId(null)
                .arrangementClause(eqArrangement(arrangement))
                .amountClause(amountLessThanOrEqual(balance))
                .recipeSpecificationFilter(recipeSpecificationFilter -> true)
                .recipeFilter(actionRecipe -> transactionPropertiesFilter(validTx, actionRecipe))
                .data(event)
                .originatorContext(context)
                .build();

        eventPublisher.publishEvent(interpretedEvent);

        if (log.isDebugEnabled()) {
            log.debug("Handler {} published interpreted event for further processing", this.getClass().getName());
        }
    }

    private boolean transactionPropertiesFilter(Transaction_ transaction, ActionRecipe recipe) {
        Map<String, String> additions = recipe.getAdditions();
        if (additions.isEmpty()) {
            return true;
        }

        return hasValidType(transaction, additions)
                && hasValidCreditDebitIndicator(transaction, additions);
    }

    private boolean hasValidType(Transaction_ transaction, Map<String, String> additions) {
        if (additions.containsKey("type")) {
            String type = additions.get("type");
            return type.equals(transaction.getType());
        }
        return true;
    }

    private boolean hasValidCreditDebitIndicator(Transaction_ transaction, Map<String, String> additions) {
        if (additions.containsKey("creditDebitIndicator")) {
            String creditDebitIndicator = additions.get("creditDebitIndicator");
            return creditDebitIndicator.equals(transaction.getCreditDebitIndicator().toString());
        }
        return true;
    }
}