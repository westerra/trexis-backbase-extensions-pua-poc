# Change Log for trexis-backbase-extensions

### March 14, 2023

Added to the transaction-extension which extends the getTransactions method.
The new functionality includes ordering transactions by addition values for posting datetime (posted) and created datetime (pending).
This new functionality is disabled by default.

This sorting takes place after the check for the ingestion extension enabled, so it will not apply if that extension is not enabled.
Additionally, this extension only applies when an `orderBy` is sent with value `bookingDate`.

Relevant configurations include:

```yaml
transaction-extension.additionOrdering.enabled: true|false
transaction-extension.additionOrdering.postedAddition: "bookingDateTime"
transaction-extension.additionOrdering.pendingAddition: "createdDateTime"
```

Posted transactions (billing status `BILLED`) will be sorted by the addition with the key configured in `postedAddition` above.
Pending transactions (billing status `UNBILLED/PENDING`) will be sorted by the addition with the key configured in `pendingAddition` above.
In the case additions have equal values, the sequence number at the end of the transaction's externalId is used to determine order.

### January 19, 2023

Added (another) extension to transactions to flip the booking date and value date on transactions before returning them.
This feature may be turned on and off separately from the other transactions extension using the property below, which defaults to false.

`transaction-extension.ingestion.dateFlipEnabled: TRUE | FALSE`

### January 12, 2023

Added extension to transactions for delaying the response while the given arrangement is still being ingested
This feature can be enabled/disabled with the configuration, the default is true:  
``transaction-extension.ingestion.enabled: true|false``

Additional configurations include:
```yaml
transaction-extension.ingestion.timeWaitSeconds: 15
transaction-extension.ingestion.pollIntervalSeconds: 1
transaction-extension.ingestion.continueAfterFailedCursorCheck: true
```

### October 11, 2022

Introduce extension to sync arrangement aliases back to the core via Finite.  It is disabled by default.
```yml
arrangement-extension.product-arrangement.syncAliasToCore: false
```
Note that this requires Finite to be configured in the arrangement manager as well:
```yml
finite:
  hosturl: http://localhost:9090
  apiKeys:
    - name: core
      enabled: true
      apikey: demo-api-key-do-not-use-in-production
```
