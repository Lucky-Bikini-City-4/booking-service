for i=1,#ARGV do
    local seatStatus = redis.call("HGET", KEYS[1], ARGV[i])
    if seatStatus == "locked" then
        redis.call("HSET", KEYS[1], ARGV[i], "available")
    end
end

return 1
