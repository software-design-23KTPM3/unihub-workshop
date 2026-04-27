local redis = require "resty.redis"
local cjson = require "cjson"
local red = redis:new()

red:set_timeout(1000) -- 1s

local ok, err = red:connect("redis-rate", 6379)
if not ok then
    ngx.log(ngx.ERR, "failed to connect to redis: ", err)
    return
end

local user_id = ngx.var.jwt_payload_sub or ngx.var.remote_addr
local key = "rate:limit:" .. user_id

-- Token Bucket implementation
-- capacity: 10, refill_rate: 5/s
local capacity = 10
local refill_rate = 5
local now = ngx.now()

local res, err = red:get(key)
local bucket = {}

if res == ngx.null then
    bucket = { tokens = capacity, last_refill = now }
else
    bucket = cjson.decode(res)
end

-- Refill
local elapsed = now - bucket.last_refill
local tokens_to_add = elapsed * refill_rate
bucket.tokens = math.min(capacity, bucket.tokens + tokens_to_add)
bucket.last_refill = now

if bucket.tokens >= 1 then
    bucket.tokens = bucket.tokens - 1
    red:set(key, cjson.encode(bucket), "EX", 60)
else
    ngx.status = 429
    ngx.say("Too many requests")
    ngx.exit(429)
end
