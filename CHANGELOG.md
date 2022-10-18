##Change Log for extensions

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
