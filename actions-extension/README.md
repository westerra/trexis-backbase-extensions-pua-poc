# Arrangement Extension

Extension for `arrangement-manager` that is intended to be overlaid on the BackBase build artifact.

## Features

Overrides the `getProductSummary` endpoint to make it aware of in progress entities being ingested.

If ingestion has already succeeded or failed, immediately returns the arrangements by calling `super.getProductSummary`.
If ingestion is still in progress for the entity, sleeps for a configured amount of seconds before checking again. After a configured amount of seconds since the start of ingestion, stops waiting and sends the user a notification, returning whatever existing arrangements may be in dbs with `super.getProductSummary`.

## Configuration

- `arrangement-extension.product-summary.in-progress-cursor.time-wait-seconds`: Total time to wait on an in progress ingestion before returning arrangements, **default 60**
- `arrangement-extension.product-summary.in-progress-cursor.poll-interval-seconds`: Time to `Thread.sleep` between each check on the cursor status, **default 2**