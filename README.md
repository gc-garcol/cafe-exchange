# Cafe Exchange

## Overview

- `Leader`: The leader node is responsible for writing the `command-log` into the into `command-log-storage`.
- `Learner`: The learner node is exclusively tasked with snapshotting the state machine.
- `Translator`: Publish `changed-event` to the `event-bus`.
- `MarketQuerier`

## Schema reference
- https://developers.binance.com/docs/derivatives/option/general-info

