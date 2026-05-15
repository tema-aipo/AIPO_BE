package com.aipo.backend.domain.investmentprofile.service;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileAnswerRequest;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionsResponse;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileResultResponse;
import com.aipo.backend.domain.investmentprofile.dto.RetestInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.dto.SkipInvestmentProfileRequest;
import com.aipo.backend.domain.investmentprofile.dto.SubmitInvestmentProfileResultRequest;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileOption;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileQuestion;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileTestStatus;
import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileType;
import com.aipo.backend.domain.investmentprofile.entity.UserInvestmentProfileAnswer;
import com.aipo.backend.domain.investmentprofile.entity.UserInvestmentProfileResult;
import com.aipo.backend.domain.investmentprofile.repository.InvestmentProfileOptionRepository;
import com.aipo.backend.domain.investmentprofile.repository.InvestmentProfileQuestionRepository;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileAnswerRepository;
import com.aipo.backend.domain.investmentprofile.repository.UserInvestmentProfileResultRepository;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentProfileServiceTest {

    @Mock
    private InvestmentProfileQuestionRepository questionRepository;

    @Mock
    private InvestmentProfileOptionRepository optionRepository;

    @Mock
    private UserInvestmentProfileResultRepository resultRepository;

    @Mock
    private UserInvestmentProfileAnswerRepository answerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvestmentProfileService investmentProfileService;

    @Test
    @DisplayName("투자 성향 문항 조회 시 현재 버전 기준 문항과 선택지를 반환한다")
    void getQuestions_success() {
        List<InvestmentProfileQuestion> questions = questions();
        List<InvestmentProfileOption> options = options(questions);
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(questionRepository.findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(1)).thenReturn(questions);
        when(optionRepository.findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)))
                .thenReturn(options);

        InvestmentProfileQuestionsResponse response = investmentProfileService.getQuestions();

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.questions()).hasSize(8);
        assertThat(response.questions().get(0).options()).hasSize(4);
    }

    @Test
    @DisplayName("검사 제출 시 총점을 계산해 완료 결과를 저장한다")
    void submitResult_success() {
        List<InvestmentProfileQuestion> questions = questions();
        List<InvestmentProfileOption> options = options(questions);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(questionRepository.findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(1)).thenReturn(questions);
        when(optionRepository.findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(any())).thenReturn(options);
        when(resultRepository.save(any(UserInvestmentProfileResult.class))).thenAnswer(invocation -> {
            UserInvestmentProfileResult result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 10L);
            return result;
        });

        InvestmentProfileResultResponse response = investmentProfileService.submitResult(
                new SubmitInvestmentProfileResultRequest(1L, 1, List.of(
                        new InvestmentProfileAnswerRequest(1L, 104L),
                        new InvestmentProfileAnswerRequest(2L, 204L),
                        new InvestmentProfileAnswerRequest(3L, 304L),
                        new InvestmentProfileAnswerRequest(4L, 403L),
                        new InvestmentProfileAnswerRequest(5L, 503L),
                        new InvestmentProfileAnswerRequest(6L, 603L),
                        new InvestmentProfileAnswerRequest(7L, 704L),
                        new InvestmentProfileAnswerRequest(8L, 804L)
                ))
        );

        assertThat(response.testStatus()).isEqualTo(InvestmentProfileTestStatus.COMPLETED);
        assertThat(response.profileType()).isEqualTo(InvestmentProfileType.AGGRESSIVE);
        assertThat(response.totalScore()).isEqualTo(25);
        assertThat(response.profileLabel()).isEqualTo("공격형");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserInvestmentProfileAnswer>> answerCaptor = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).saveAll(answerCaptor.capture());
        assertThat(answerCaptor.getValue()).hasSize(8);
    }

    @Test
    @DisplayName("동일 문항을 중복 제출하면 예외가 발생한다")
    void submitResult_whenDuplicateQuestionSubmitted_throwsException() {
        List<InvestmentProfileQuestion> questions = questions();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(questionRepository.findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(1)).thenReturn(questions);

        assertThatThrownBy(() -> investmentProfileService.submitResult(
                new SubmitInvestmentProfileResultRequest(1L, 1, List.of(
                        new InvestmentProfileAnswerRequest(1L, 101L),
                        new InvestmentProfileAnswerRequest(1L, 102L),
                        new InvestmentProfileAnswerRequest(3L, 301L),
                        new InvestmentProfileAnswerRequest(4L, 401L),
                        new InvestmentProfileAnswerRequest(5L, 501L),
                        new InvestmentProfileAnswerRequest(6L, 601L),
                        new InvestmentProfileAnswerRequest(7L, 701L),
                        new InvestmentProfileAnswerRequest(8L, 801L)
                ))
        ))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
    }

    @Test
    @DisplayName("검사 스킵 시 SKIPPED 결과를 현재 결과로 저장한다")
    void skip_success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(resultRepository.save(any(UserInvestmentProfileResult.class))).thenAnswer(invocation -> {
            UserInvestmentProfileResult result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 20L);
            return result;
        });

        InvestmentProfileResultResponse response = investmentProfileService.skip(new SkipInvestmentProfileRequest(1L, 1));

        assertThat(response.testStatus()).isEqualTo(InvestmentProfileTestStatus.SKIPPED);
        assertThat(response.profileType()).isNull();
        assertThat(response.profileLabel()).isEqualTo("분석 대기중");
    }

    @Test
    @DisplayName("현재 결과가 없으면 NOT_TESTED 결과를 생성해 반환한다")
    void getCurrentResult_whenMissingCurrentResult_createsNotTestedResult() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(resultRepository.findByUserIdAndCurrentTrue(1L)).thenReturn(Optional.empty());
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(resultRepository.save(any(UserInvestmentProfileResult.class))).thenAnswer(invocation -> {
            UserInvestmentProfileResult result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 30L);
            return result;
        });

        InvestmentProfileResultResponse response = investmentProfileService.getCurrentResult(1L);

        assertThat(response.testStatus()).isEqualTo(InvestmentProfileTestStatus.NOT_TESTED);
        assertThat(response.profileType()).isNull();
        assertThat(response.profileLabel()).isEqualTo("분석 대기중");
        assertThat(response.title()).isEqualTo("분석 대기중");
    }

    @Test
    @DisplayName("문항 마스터가 없어도 NOT_TESTED fallback 응답을 반환한다")
    void getCurrentResult_whenQuestionMasterMissing_returnsFallback() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(resultRepository.findByUserIdAndCurrentTrue(1L)).thenReturn(Optional.empty());
        when(questionRepository.findCurrentVersion()).thenReturn(null);

        InvestmentProfileResultResponse response = investmentProfileService.getCurrentResult(1L);

        assertThat(response.testStatus()).isEqualTo(InvestmentProfileTestStatus.NOT_TESTED);
        assertThat(response.profileType()).isNull();
        assertThat(response.profileLabel()).isEqualTo("분석 대기중");
        assertThat(response.title()).isEqualTo("분석 대기중");
        assertThat(response.resultId()).isNull();
        verify(resultRepository, never()).save(any(UserInvestmentProfileResult.class));
    }

    @Test
    @DisplayName("재검사 제출도 동일한 계산 규칙으로 완료 결과를 저장한다")
    void retest_success() {
        List<InvestmentProfileQuestion> questions = questions();
        List<InvestmentProfileOption> options = options(questions);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findCurrentVersion()).thenReturn(1);
        when(questionRepository.findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(1)).thenReturn(questions);
        when(optionRepository.findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(any())).thenReturn(options);
        when(resultRepository.save(any(UserInvestmentProfileResult.class))).thenAnswer(invocation -> {
            UserInvestmentProfileResult result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 40L);
            return result;
        });

        InvestmentProfileResultResponse response = investmentProfileService.retest(
                1L,
                new RetestInvestmentProfileRequest(1, List.of(
                        new InvestmentProfileAnswerRequest(1L, 101L),
                        new InvestmentProfileAnswerRequest(2L, 202L),
                        new InvestmentProfileAnswerRequest(3L, 302L),
                        new InvestmentProfileAnswerRequest(4L, 402L),
                        new InvestmentProfileAnswerRequest(5L, 502L),
                        new InvestmentProfileAnswerRequest(6L, 602L),
                        new InvestmentProfileAnswerRequest(7L, 702L),
                        new InvestmentProfileAnswerRequest(8L, 802L)
                ))
        );

        assertThat(response.testStatus()).isEqualTo(InvestmentProfileTestStatus.COMPLETED);
        assertThat(response.profileType()).isEqualTo(InvestmentProfileType.STABLE);
        assertThat(response.totalScore()).isEqualTo(9);
        assertThat(response.profileLabel()).isEqualTo("안정형");
    }

    private List<InvestmentProfileQuestion> questions() {
        List<InvestmentProfileQuestion> questions = new ArrayList<>();
        for (long index = 1; index <= 8; index++) {
            InvestmentProfileQuestion question = instantiate(InvestmentProfileQuestion.class);
            ReflectionTestUtils.setField(question, "id", index);
            ReflectionTestUtils.setField(question, "version", 1);
            ReflectionTestUtils.setField(question, "questionOrder", (int) index);
            ReflectionTestUtils.setField(question, "questionText", "question-" + index);
            ReflectionTestUtils.setField(question, "active", true);
            ReflectionTestUtils.setField(question, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(question, "updatedAt", LocalDateTime.now());
            questions.add(question);
        }
        return questions;
    }

    private List<InvestmentProfileOption> options(List<InvestmentProfileQuestion> questions) {
        List<InvestmentProfileOption> options = new ArrayList<>();
        for (InvestmentProfileQuestion question : questions) {
            for (int order = 1; order <= 4; order++) {
                InvestmentProfileOption option = instantiate(InvestmentProfileOption.class);
                ReflectionTestUtils.setField(option, "id", question.getId() * 100 + order);
                ReflectionTestUtils.setField(option, "question", question);
                ReflectionTestUtils.setField(option, "optionOrder", order);
                ReflectionTestUtils.setField(option, "optionText", "option-" + question.getId() + "-" + order);
                ReflectionTestUtils.setField(option, "score", order - 1);
                ReflectionTestUtils.setField(option, "createdAt", LocalDateTime.now());
                ReflectionTestUtils.setField(option, "updatedAt", LocalDateTime.now());
                options.add(option);
            }
        }
        return options;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
