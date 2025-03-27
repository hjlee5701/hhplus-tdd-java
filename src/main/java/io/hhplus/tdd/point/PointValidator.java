package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import static io.hhplus.tdd.point.PointPolicy.*;

@Component
public class PointValidator {

    public void validatedChargePoint(long amount) {
        if (amount > MAXIMUM_POINT.getAmount()) {
            throw new RuntimeException("충전 시 최대 보유 포인트를 초과합니다.");
        }
    }


    public void validatedUsePoint(long amount) {
        if (amount < ZERO_POINT.getAmount() || amount > MAXIMUM_POINT.getAmount()) {
            throw new RuntimeException("보유 포인트를 초과하여 사용할 수 없습니다.");
        }

    }

}
