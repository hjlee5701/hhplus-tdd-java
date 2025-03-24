package io.hhplus.tdd.database;

import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("PointHistory 에 존재하지 않은 userId 로 조회할 시 예외가 발생한다.")
    void 유효하지_않은_userId로_조회시_예외발생() {
        // given
        long invalidUserId = 99L;
        given(pointHistoryTable.selectAllByUserId(Mockito.anyLong()))
                .willReturn(List.of());
        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.findUserPointById(invalidUserId)
        );
        // then
        assertEquals("사용자 포인트 정보가 존재하지 않습니다.", exception.getMessage());
        verify(userPointTable, never()).selectById(invalidUserId);
    }

    public PointHistory createPointHistory() {
        return new PointHistory(1L, 1L, 0, TransactionType.CHARGE, System.currentTimeMillis());
    }

    @Test
    @DisplayName("충전 시 보유 포인트 + 충전 금액이 최대값을 초과하면 예외 발생한다.")
    void 충전_후_최대값_초과_예외() {
        // given
        long userId = 1L;
        long amount = 600_000L;
        long currentPoint = 9_500_000L; // currentPoint + amount
        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, currentPoint, System.currentTimeMillis()));

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.chargeUserPoint(userId, amount)
        );
        assertEquals("충전 시 최대 보유 포인트를 초과합니다.", exception.getMessage());
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
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

        // when
        UserPoint result = pointService.chargeUserPoint(userId, amount);

        // then
        assertEquals(1_000_000L, result.point());
        verify(userPointTable).insertOrUpdate(userId, 1_000_000L);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("충전 시 보유 포인트 + 충전 금액이 최대값 미만이면 충전 성공한다.")
    void 충전_후_최대값_미만_성공() {
        // given
        long userId = 1L;
        long amount = 10L;
        given(pointHistoryTable.selectAllByUserId(userId))
                .willReturn(List.of(createPointHistory()));

        given(userPointTable.selectById(userId))
                .willReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        given(userPointTable.insertOrUpdate(userId, 10L))
                .willReturn(new UserPoint(userId, 10L, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargeUserPoint(userId, amount);

        // then
        assertEquals(10L, result.point());
        verify(userPointTable).insertOrUpdate(userId, 10L);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }



}
