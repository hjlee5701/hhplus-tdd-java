package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PointService {
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    // 특정 유저의 포인트를 조회하는 기능
    public UserPoint findUserPointById(long userId) {
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

        if (pointHistories.isEmpty()) {
            throw new RuntimeException("사용자 포인트 정보가 존재하지 않습니다.");
        }
        return userPointTable.selectById(userId);
    }

    // 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
    public List<PointHistory> findHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
