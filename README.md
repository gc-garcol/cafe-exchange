# Cafe Exchange

## Overview

## Quick start

- Setup dev

```shell
make setup-dev
```

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

Client Swagger: http://localhost:8090/swagger-ui/index.html

## Schema reference

- https://developers.binance.com/docs/derivatives/option/general-info

