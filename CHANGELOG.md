# Change Log for trexis-backbase-extensions

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
