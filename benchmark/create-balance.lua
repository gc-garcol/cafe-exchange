-- wrk -t12 -c100 -d30s -s create-balance.lua http://localhost:8090/v1/balance
-- wrk -t1 -c1 -d5s -s create-balance.lua http://localhost:8090/v1/balance

wrk.method = "POST"
wrk.body = '{"ownerId": 123}'
wrk.headers["Content-Type"] = "application/json"
