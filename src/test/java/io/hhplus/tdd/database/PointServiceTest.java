package io.hhplus.tdd.database;

import io.hhplus.tdd.concurrent.LockExecutor;
import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private LockExecutor lockExecutor;

    @Mock
    private PointValidator pointValidator;

    @InjectMocks
    private PointService pointService;

    private PointHistory createPointHistory() {
        return new PointHistory(1L, 1L, 0, TransactionType.CHARGE, System.currentTimeMillis());
    }
    private void ignoringUserLock(long userId) {
        given(lockExecutor.executeWithUserLock(eq(userId), any()))
                .willAnswer(invocation -> {
                    Supplier<?> task = invocation.getArgument(1);
                    return task.get(); // 실제로 task 실행되도록
                });
    }

    @Test
    @DisplayName("PointHistory 에 존재하지 않은 userId 로 조회할 시 예외가 발생한다.")
    void 유효하지_않은_userId로_조회시_예외발생() {
        // given
        long invalidUserId = 99L;
        given(pointHistoryTable.selectAllByUserId(invalidUserId))
                .willReturn(List.of());

        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.findUserPointById(invalidUserId)
        );
        // then
        assertEquals("사용자 포인트 정보가 존재하지 않습니다.", exception.getMessage());
        verify(userPointTable, never()).selectById(anyLong());
    }


    @Test
    @DisplayName("충전 시 보유 포인트 + 충전 금액이 최대값과 동일하면 충전 성공한다.")
    void 충전_후_최대값_동일_성공() {
        // given
        long userId = 1L;
        long amount = 50_000L;
        long currentPoint = 950_000L; // currentPoint + amount = 1_000_000
        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, currentPoint, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, 1_000_000L))
                .willReturn(new UserPoint(userId, 1_000_000L, System.currentTimeMillis()));

        ignoringUserLock(userId);

        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, amount);

        // then
        assertEquals(userId, userPoint.id());
        assertEquals(1_000_000L, userPoint.point());
    }

    @Test
    @DisplayName("충전 시 보유 포인트 + 충전 금액이 최대값 미만이면 충전 성공한다.")
    void 충전_후_최대값_미만_성공() {
        // given
        long userId = 1L;
        long amount = 100L;
        long chargePoint = 200L;

        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, chargePoint, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, amount+chargePoint))
                .willReturn(new UserPoint(userId, amount+chargePoint, System.currentTimeMillis()));

        ignoringUserLock(userId);
        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, amount);

        // then
        assertEquals(userId, userPoint.id());
        assertEquals(300L, userPoint.point());
        verify(pointValidator, times(1)).validatedChargePoint(amount);
        verify(pointValidator, times(1)).validatedChargePoint(amount+chargePoint);
    }

    @DisplayName("충전 성공 시 PointHistory 에 사용자의 충전 기록이 저장된다.")
    @Test
    void 충전_성공시_PointHistory에_저장() {
        // given
        long userId = 1L;
        long amount = 10L;
        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, 10L))
                .willReturn(new UserPoint(userId, 10L, System.currentTimeMillis()));

        ignoringUserLock(userId);
        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, amount);

        // then
        assertEquals(10L, userPoint.point());
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }




    @Test
    @DisplayName("보유 포인트를 전부 사용 시 포인트 사용 성공한다.")
    void 보유_포인트를_전부_사용() {
        // given
        long userId = 1L;
        long amount = 300L;
        long usePoint = 100L;
        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, amount-usePoint))
                .willReturn(new UserPoint(userId, amount-usePoint, System.currentTimeMillis()));

        ignoringUserLock(userId);
        // when
        UserPoint userPoint = pointService.usePoint(userId, usePoint);

        // then
        assertEquals(userId, userPoint.id());
        assertEquals(amount-usePoint, userPoint.point());
        verify(pointValidator, times(1)).validatedUsePoint(usePoint);
        verify(pointValidator, times(1)).validatedUsePoint(amount-usePoint);
    }

    @Test
    @DisplayName("보유 포인트 미만을 사용 시 포인트 사용 성공한다.")
    void 보유_포인트_미만을_사용() {
        // given
        long userId = 1L;
        long amount = 100L;
        long usePoint = 70L;


        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, amount - usePoint))
                .willReturn(new UserPoint(userId, amount - usePoint, System.currentTimeMillis()));

        ignoringUserLock(userId);
        // when
        UserPoint userPoint = pointService.usePoint(userId, usePoint);

        // then
        assertEquals(userId, userPoint.id());
        assertEquals(amount-usePoint, userPoint.point());
        verify(pointValidator, times(1)).validatedUsePoint(usePoint);
        verify(pointValidator, times(1)).validatedUsePoint(amount - usePoint);
    }

}
