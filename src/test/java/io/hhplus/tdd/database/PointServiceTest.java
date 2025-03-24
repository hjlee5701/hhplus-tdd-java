package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

}
