package io.hhplus.tdd.database;

import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Autowired
    private UserPointTable userPointTable;


    @Test
    @DisplayName("[GET: /point/{id}] 음수 userId 파라미터로 요청시 에외 응답 반환한다.")
    void 음수인_userId을_파라미터로_요청시_예외메시지_응답() throws Exception {
        // given
        long inValidUserId = -1L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                .get("/point/"+inValidUserId)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("[유효한 userId 가 아닙니다.]"));

    }

    @Test
    @DisplayName("[GET: /point/{id}] 저장되지 않은 유저의 포인트를 조회시 예외 응답 반환한다.")
    void 저장되지_않은_유저의_포인트_조회시_예외_응답() throws Exception {
        // given
        long notSavedUserId = 1L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/point/"+notSavedUserId)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("사용자 포인트 정보가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("[GET: /point/{id}] 특정 유저의 포인트를 조회시 성공 응답 반환한다.")
    void 포인트_조회시_성공_응답() throws Exception {
        // given
        long validUserId = 1L;
        long amount = 100L;
        // 사용자의 포인트 정보가 존재하는 전제
        userPointTable.insertOrUpdate(validUserId, amount);
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/point/"+validUserId)
        );
        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(validUserId))
                .andExpect(jsonPath("$.point").value(amount))
        ;
    }
}