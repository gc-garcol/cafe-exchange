-- wrk -t12 -c100 -d30s -s create-balance.lua http://localhost:8090/v1/balance
-- wrk -t32 -c128 -d10s -s benchmark/create-balance.lua http://localhost:8090/v1/balance

math.randomseed(os.time())  -- Seed the random number generator with the current time

request = function()
  local ownerId = math.random(1, 1000)  -- Generate a random ownerId between 1 and 1000
  wrk.method = "POST"
  wrk.body = string.format('{"ownerId": %d}', ownerId)
  wrk.headers["Content-Type"] = "application/json"
  return wrk.format(nil, nil, nil, wrk.body)
end

