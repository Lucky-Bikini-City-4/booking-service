for i=1,#ARGV do
    local seatStatus = redis.call("HGET", KEYS[1], ARGV[i])
    if seatStatus ~= "locked" then
        return 0 -- 실패 (중간에 풀렸거나 이미 booked 됨)
    end
end

for i=1,#ARGV do
    redis.call("HSET", KEYS[1], ARGV[i], "booked")
end

return 1
