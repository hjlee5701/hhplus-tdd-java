package io.hhplus.tdd.database;

import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
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

    @Test
    @DisplayName("[GET: /point/{id}/histories] 특정 유저의 포인트 충전/이용 내역을 조회시 성공 응답 반환한다.")
    void 포인트_충전_이용_내역_조회시_성공_응답() throws Exception {
        // given
        long validUserId = 1L;
        long amount = 100L;
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(validUserId, amount, TransactionType.USE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/point/"+validUserId+"/histories")
        );
        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.[0].userId").value(validUserId))
                .andExpect(jsonPath("$.[0].amount").value(amount))
                .andExpect(jsonPath("$.[0].type").value("CHARGE"))
                .andExpect(jsonPath("$.[1].userId").value(validUserId))
                .andExpect(jsonPath("$.[1].amount").value(amount))
                .andExpect(jsonPath("$.[1].type").value("USE"))
        ;
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] : 음수 amount로 충전 요청시 에외 응답 반환한다.")
    void 음수인_amount로_충전_요청시_예외_응답() throws Exception {

        // given
        long validUserId = 1L;
        long chargeAmount = -1L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/charge")
                        .content(String.valueOf(chargeAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("[최소 0포인트 이상 충전 가능합니다.]"));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] : 충전 요청 시 보유 포인트가 최대값을 초과될 시 예외 응답 반환한다.")
    void 보유_포인트에서_초과_충전시_예외_응답() throws Exception {

        // given
        long validUserId = 1L;
        long amount = 1_000_000L;
        long chargeAmount = 100L;
        userPointTable.insertOrUpdate(validUserId, amount);
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/charge")
                        .content(String.valueOf(chargeAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("충전 시 최대 보유 포인트를 초과합니다."));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] : 특정 유저의 포인트 충전시 성공 응답 반환한다.")
    void 포인트_충전시_성공_응답() throws Exception {
        // given
        long validUserId = 1L;
        long amount = 100L;
        long chargeAmount = 100;
        userPointTable.insertOrUpdate(validUserId, amount);
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/charge")
                        .content(String.valueOf(chargeAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(validUserId))
                .andExpect(jsonPath("$.point").value(amount+chargeAmount))
        ;
    }


    @Test
    @DisplayName("[PATCH /point/{id}/use] : 음수 amount로 포인트 사용 요청시 에외 응답 반환한다.")
    void 음수인_amount로_사용_요청시_예외_응답() throws Exception {

        // given
        long validUserId = 1L;
        long useAmount = -1L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/use")
                        .content(String.valueOf(useAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("[최소 0포인트 이상 사용 가능합니다.]"));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/use] : 보유 포인트가 초과 사용 요청시 예외 응답 반환한다.")
    void 보유_포인트_초과_사용시_예외_응답() throws Exception {

        // given
        long validUserId = 1L;
        long amount = 100L;
        long useAmount = 200L;
        userPointTable.insertOrUpdate(validUserId, amount);
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/use")
                        .content(String.valueOf(useAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("보유 포인트를 초과하여 사용할 수 없습니다."));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/use] : 특정 유저의 포인트 충전시 성공 응답 반환한다.")
    void 보유_포인트_사용시_성공_응답() throws Exception {

        // given
        long validUserId = 1L;
        long amount = 200L;
        long useAmount = 100L;
        userPointTable.insertOrUpdate(validUserId, amount);
        pointHistoryTable.insert(validUserId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .patch("/point/"+validUserId+"/use")
                        .content(String.valueOf(useAmount))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(validUserId))
                .andExpect(jsonPath("$.point").value(amount-useAmount));
    }
}