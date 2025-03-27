package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointPolicy;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private UserPointTable userPointTable;

    private final long MAXIMUM_POINT = PointPolicy.MAXIMUM_POINT.getAmount();

    static long testUserId = 1L;
    final long tenPoint = 100L;

    private static Long getTestUserId() {
        return ++testUserId;
    }
    @BeforeEach
    public void createPointHistory() {
        pointHistoryTable.insert(++testUserId, tenPoint, TransactionType.CHARGE, System.currentTimeMillis());
    }



    @Test
    @DisplayName("포인트 충전에 성공합니다.")
    void 포인트_충전에_성공() {
        // given
        long userId = testUserId;
        long amount = tenPoint;

        // when
        UserPoint userPoint = pointService.chargeUserPoint(userId, amount);

        // then
        assertThat(userId).isEqualTo(userPoint.id());
        assertThat(amount).isEqualTo(userPoint.point());
    }

    @Test
    @DisplayName("충전 후 보유 포인트가 최대값이 초과해 충전에 실패합니다.")
    void 최대값_초과로_포인트_충전_실패() {
        // given
        long userId = testUserId;
        long amount = PointPolicy.MAXIMUM_POINT.getAmount() + tenPoint;

        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.chargeUserPoint(userId, amount));

        // then
        assertThat(exception.getMessage())
                .isEqualTo("충전 시 최대 보유 포인트를 초과합니다.");
    }

    @Test
    @DisplayName("포인트 충전 이후 PointHistory 가 조회 가능합니다.")
    void 포인트를_충전한_히스토리_조회_가능() {
        // given
        long userId = testUserId;
        pointHistoryTable.insert(userId, tenPoint, TransactionType.CHARGE, System.currentTimeMillis());
        pointService.chargeUserPoint(userId, tenPoint);

        // when
        UserPoint findUserPoint = pointService.findUserPointById(userId);

        // then
        assertThat(userId).isEqualTo(findUserPoint.id());
        assertThat(tenPoint).isEqualTo(findUserPoint.point());
    }


    @Test
    @DisplayName("보유 포인트보다 초과 사용 요청시 사용에 실패합니다.")
    void 포인트_초과_사용으로_실패() {
        // given
        long userId = testUserId;
        long amount = tenPoint;

        pointService.chargeUserPoint(userId, amount);
        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pointService.usePoint(userId, amount + 100L));

        // then
        assertThat(exception.getMessage())
                .isEqualTo("보유 포인트를 초과하여 사용할 수 없습니다.");
    }


    @Test
    @DisplayName("N번 충전 요청시 포인트 충전에 성공합니다.")
    void 동일한_userId로_n번_충전시_포인트_충전에_성공() throws InterruptedException {
        // given
        long userId = getTestUserId();
        long amount = tenPoint;
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargeUserPoint(userId, amount);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        UserPoint userPoint = pointService.findUserPointById(userId);

        assertThat(userId).isEqualTo(userPoint.id());
        assertThat(amount * threadCount).isEqualTo(userPoint.point());
    }

    @Test
    @DisplayName("N번 충전 중 최대 포인트 초과로 이후 충전부터 실패합니다.")
    void 충전중_최대_포인트_초과로_실패() throws InterruptedException {
        // given
        long userId = testUserId;
        int threadCount = 20;
        int expectFailCount = 15;

        long amount = MAXIMUM_POINT / (threadCount - expectFailCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargeUserPoint(testUserId, amount);
                    successCount.getAndIncrement();
                } catch (Exception ex) {
                    failCount.getAndIncrement();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        UserPoint userPoint = pointService.findUserPointById(testUserId);

        assertThat(successCount.get()).isEqualTo(threadCount - expectFailCount);
        assertThat(failCount.get()).isEqualTo(expectFailCount);

        assertThat(userId).isEqualTo(userPoint.id());
        assertThat(amount * successCount.get()).isEqualTo(userPoint.point());
    }

    @Test
    @DisplayName("N번 사용 요청시 포인트 사용에 n번 성공합니다.")
    void 동일한_userId로_n번_포인트_사용시_포인트_사용을_n번_성공() throws InterruptedException {
        // given
        long userId = testUserId;

        long amount = 100L;
        int threadCount = 10;
        long existPoint = threadCount * amount;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, amount);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        UserPoint userPoint = pointService.findUserPointById(userId);

        assertThat(userId).isEqualTo(userPoint.id());
        assertThat(existPoint - amount * threadCount).isEqualTo(userPoint.point());
    }

    @Test
    @DisplayName("N번 사용 중 보유 포인트 초과로 이후 사용부터 실패합니다.")
    void 사용중_사용이_보유_포인트를_초과한_이후부터_포인트_사용_실패() throws InterruptedException {
        // given
        long userId = testUserId;
        int threadCount = 10;
        int expectFailCount = 3;
        long existPoint = 1000L;
        long amount = existPoint / (threadCount - expectFailCount);

        userPointTable.insertOrUpdate(userId, existPoint);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, amount);
                    successCount.getAndIncrement();
                } catch (Exception ex) {
                    failCount.getAndIncrement();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        UserPoint userPoint = pointService.findUserPointById(userId);

        assertThat(successCount.get()).isEqualTo(threadCount - expectFailCount);
        assertThat(failCount.get()).isEqualTo(expectFailCount);

        assertThat(userId).isEqualTo(userPoint.id());
        assertThat(existPoint - (amount * successCount.get())).isEqualTo(userPoint.point());
    }
}
