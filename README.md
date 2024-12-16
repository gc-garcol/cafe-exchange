# Cafe Exchange

Note: This project is still under development.

## Overview

## Stacks
- [X] `Cafe ringbuffer`
- [X] `Agrona real-logic`

## Benchmark

Compared to [Binance](https://www.binance.com/en/blog/tech/how-binance-ledger-powers-your-binance-experience-5409682424466769892#:~:text=Fault%20tolerance-,Performance,-The%20following%20table), this project demonstrates superior performance in both throughput and latency.

![img.png](benchmark-deposit.png)

## Quick start

- Setup dev

```shell
make setup-dev
```

- add `--add-opens java.base/java.nio=ALL-UNNAMED` to the JVM arguments

- Run cluster

```shell
make run-cluster PORT=8080 GRPC_PORT=9500
make run-cluster PORT=8081 GRPC_PORT=9500
make run-cluster PORT=8082 GRPC_PORT=9500
```

- Run client

```shell
./gradlew :exchange-client:run-client -Pport=8090
```

- Run benchmark

```shell
./gradlew :benchmark-cluster:run-benchmark -Pport=8095
```

Client Swagger: http://localhost:8090/swagger-ui/index.html

## Schema reference

- https://developers.binance.com/docs/derivatives/option/general-info

