package io.hhplus.tdd.point;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/point")
@Validated
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
            @Min(value = 1, message = "유효한 userId 가 아닙니다.")
            @PathVariable("id") long userId
    ) {
        return pointService.findUserPointById(userId);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @Min(value = 1, message = "유효한 userId 가 아닙니다.")
            @PathVariable("id") long userId
    ) {
        return pointService.findHistories(userId);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @Min(value = 1, message = "유효한 userId 가 아닙니다.")
            @PathVariable("id") long userId,
            
            @Min(value = 0, message = "최소 0포인트 이상 충전 가능합니다.")
            @RequestBody long amount
    ) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @Min(value = 1, message = "유효한 userId 가 아닙니다.")
            @PathVariable("id") long userId,

            @Min(value = 0, message = "최소 0포인트 이상 사용 가능합니다.")
            @RequestBody long amount
    ) {
        return new UserPoint(0, 0, 0);
    }
}
