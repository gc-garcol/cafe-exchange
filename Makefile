.PHONY: help

help: ## Show all commands
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

setup-dev: ## Setup development environment
	docker-compose -f docker-compose.yaml up -d

down-dev: ## Down development environment
	docker-compose -f docker-compose.yaml down -v

build: ## Build jar
	./gradlew clean build

PORT ?= 8080
GRPC_PORT ?= 9500
run-cluster: ## Run cluster
	./gradlew :exchange-cluster:run-cluster -Pport=$(PORT) -Pgrpcport=${GRPC_PORT}
