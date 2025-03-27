package io.hhplus.tdd.point;

import io.hhplus.tdd.concurrent.LockExecutor;
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
    private final LockExecutor lockExecutor;
    private final PointValidator pointValidator;


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

        // 포인트 최대값 초과 유효성 검증
        pointValidator.validatedChargePoint(amount);

        return lockExecutor.executeWithUserLock(userId, () -> {

            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

            if (pointHistories.isEmpty()) {
                throw new RuntimeException("사용자 포인트 정보가 존재하지 않습니다.");
            }

            UserPoint userPoint = userPointTable.selectById(userId);

            // 포인트 최대값 초과 유효성 검증
            long totalPoint = amount + userPoint.point();
            pointValidator.validatedChargePoint(totalPoint);
            
            // 포인트 충전
            UserPoint chargedUserPoint = userPointTable.insertOrUpdate(userId, totalPoint);

            // 충전 history
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return chargedUserPoint;
        });

    }

    // 특정 유저의 포인트를 사용하는 기능
    public UserPoint usePoint(long userId, long amount) {

        pointValidator.validatedUsePoint(amount);

        return lockExecutor.executeWithUserLock(userId, () -> {
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

            if (pointHistories.isEmpty()) {
                throw new RuntimeException("사용자 포인트 정보가 존재하지 않습니다.");
            }

            UserPoint userPoint = userPointTable.selectById(userId);

            long totalPoint = userPoint.point() - amount;
            pointValidator.validatedUsePoint(totalPoint);

            // 포인트 사용
            UserPoint usedUserPoint = userPointTable.insertOrUpdate(userId, totalPoint);

            // 사용 history
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return usedUserPoint;

        });
    }

}
