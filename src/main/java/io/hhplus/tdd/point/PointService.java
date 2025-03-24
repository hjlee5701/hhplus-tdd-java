package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PointService {
    public static final long MAXIMUM_POINT = 1_000_000L;
    public static final long ZERO_POINT = 0L;
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

    // 특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long userId, long amount) {
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

        if (pointHistories.isEmpty()) {
            throw new RuntimeException("사용자 포인트 정보가 존재하지 않습니다.");
        }

        UserPoint userPoint = userPointTable.selectById(userId);

        long totalPoint = amount + userPoint.point();
        if (totalPoint > MAXIMUM_POINT) {
            throw new RuntimeException("충전 시 최대 보유 포인트를 초과합니다.");
        }
        // 포인트 충전
        UserPoint chargedUserPoint = userPointTable.insertOrUpdate(userId, totalPoint);

        // 충전 history
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return chargedUserPoint;
    }

    // 특정 유저의 포인트를 사용하는 기능
    public UserPoint usePoint(long userId, long amount) {
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

        if (pointHistories.isEmpty()) {
            throw new RuntimeException("사용자 포인트 정보가 존재하지 않습니다.");
        }

        UserPoint userPoint = userPointTable.selectById(userId);

        long totalPoint = userPoint.point() - amount;
        if (totalPoint < ZERO_POINT) {
            throw new RuntimeException("보유 포인트를 초과하여 사용할 수 없습니다.");
        }
        // 포인트 사용
        UserPoint usedUserPoint = userPointTable.insertOrUpdate(userId, totalPoint);

        // 사용 history
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

        return usedUserPoint;
    }

}
