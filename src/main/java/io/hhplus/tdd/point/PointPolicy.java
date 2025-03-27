package io.hhplus.tdd.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointPolicy {
    MAXIMUM_POINT(1_000_000L),
    ZERO_POINT(0L),
    ;
    private final long amount;
}
