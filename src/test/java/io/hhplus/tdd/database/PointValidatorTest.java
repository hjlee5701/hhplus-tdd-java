package io.hhplus.tdd.database;


import io.hhplus.tdd.point.PointPolicy;
import io.hhplus.tdd.point.PointValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PointValidatorTest {

    private final PointValidator pointValidator = new PointValidator();

    private final long MAXIMUM_POINT = PointPolicy.MAXIMUM_POINT.getAmount();

    @Test
    @DisplayName("충전 시 보유 포인트 + 충전 금액이 최대값을 초과하면 예외 발생한다.")
    void 충전_후_최대값_초과_예외() {
        // given
        long amount = MAXIMUM_POINT + 100L;

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointValidator.validatedChargePoint(amount)
        );
        assertEquals("충전 시 최대 보유 포인트를 초과합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("보유 포인트를 초과하여 사용 시 예외 발생한다.")
    void 보유_포인트를_초과_사용() {
        // given
        long amount = MAXIMUM_POINT + 100L;

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointValidator.validatedUsePoint(amount)
        );
        assertEquals("보유 포인트를 초과하여 사용할 수 없습니다.", exception.getMessage());
    }
}
