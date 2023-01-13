##Change Log for extensions

### January 12, 2023
Added extension to transactions for delaying the response while the given arrangement is still being ingested
This feature can be enabled/disabled with the configuration, the default is true:  
``transaction-extension.ingestion.enabled: true|false``
Additional configurations include:
```
transaction-extension.ingestion.timeWaitSeconds: 15
transaction-extension.ingestion.pollIntervalSeconds: 1
transaction-extension.ingestion.continueAfterFailedCursorCheck: true
```

### October 11, 2022
Introduce extension to sync arrangement aliases back to the core via Finite.  It is disabled by default.
```yml
arrangement-extension.product-arrangement.syncAliasToCore: false
```
Note that this require Finite to be configured in the arrangement manager as well:
```yml
finite:
  hosturl: http://localhost:9090
  apiKeys:
    -
      name: core
      enabled: true
      apikey: demo-api-key-do-not-use-in-production
```
