-- KEYS[1] = seat hash key (ex: seat:200:100:10)
-- ARGV = 좌석 번호 리스트

for i=1,#ARGV do
    local seatStatus = redis.call("HGET", KEYS[1], ARGV[i])
    if seatStatus ~= "available" then
        return 0 -- 실패 (이미 locked 또는 booked)
    end
end

-- 전부 available → locked 처리
for i=1,#ARGV do
    redis.call("HSET", KEYS[1], ARGV[i], "locked")
end

return 1
