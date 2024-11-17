-- wrk -t12 -c100 -d30s -s balance-balance.lua http://localhost:8090/v1/balance/deposit
-- wrk -t32 -c128 -d10s -s benchmark/deposit-balance.lua http://localhost:8090/v1/balance/deposit
--- wrk -t1 -c1 -d5s -s benchmark/deposit-balance.lua http://localhost:8090/v1/balance/deposit

request = function()
  wrk.method = "POST"
  wrk.body = string.format('{"ownerId": 1, "asset": "BTC", "amount": "1.0"}', ownerId)
  wrk.headers["Content-Type"] = "application/json"
  return wrk.format(nil, nil, nil, wrk.body)
end

