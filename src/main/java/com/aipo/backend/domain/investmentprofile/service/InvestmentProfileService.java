package com.aipo.backend.domain.investmentprofile.service;

import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileAnswerRequest;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileOptionItem;
import com.aipo.backend.domain.investmentprofile.dto.InvestmentProfileQuestionItem;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InvestmentProfileService {

    private static final int REQUIRED_QUESTION_COUNT = 6;
    private static final String DEFAULT_START_BUTTON_LABEL = "AIPO 시작하기";
    private static final String DEFAULT_NEXT_ACTION = "LOGIN";
    private static final String NOT_TESTED_LABEL = "분석 대기중";
    private static final String NOT_TESTED_DESCRIPTION =
            "아직 투자 성향 검사를 진행하지 않았어요. 마이페이지에서 언제든 재검사할 수 있어요.";
    private static final List<String> NOT_TESTED_TAGS = List.of("미검사", "마이페이지에서 재검사 가능");

    private final InvestmentProfileQuestionRepository questionRepository;
    private final InvestmentProfileOptionRepository optionRepository;
    private final UserInvestmentProfileResultRepository resultRepository;
    private final UserInvestmentProfileAnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InvestmentProfileQuestionsResponse getQuestions() {
        Integer currentVersion = getCurrentVersion();
        List<InvestmentProfileQuestion> questions = questionRepository
                .findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(currentVersion);
        List<InvestmentProfileOption> options = optionRepository.findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(
                questions.stream().map(InvestmentProfileQuestion::getId).toList()
        );

        Map<Long, List<InvestmentProfileOptionItem>> optionItemsByQuestionId = new HashMap<>();
        for (InvestmentProfileOption option : options) {
            optionItemsByQuestionId.computeIfAbsent(option.getQuestion().getId(), ignored -> new ArrayList<>())
                    .add(new InvestmentProfileOptionItem(
                            option.getId(),
                            option.getOptionOrder(),
                            option.getOptionText(),
                            option.getScore()
                    ));
        }

        List<InvestmentProfileQuestionItem> questionItems = questions.stream()
                .map(question -> new InvestmentProfileQuestionItem(
                        question.getId(),
                        question.getQuestionOrder(),
                        question.getQuestionText(),
                        optionItemsByQuestionId.getOrDefault(question.getId(), List.of())
                ))
                .toList();

        return new InvestmentProfileQuestionsResponse(currentVersion, questionItems);
    }

    public void initializeNotTestedResult(Long userId) {
        validateUser(userId);
        if (resultRepository.existsByUserIdAndCurrentTrue(userId)) {
            return;
        }

        Integer currentVersion = findCurrentVersion();
        if (currentVersion == null) {
            return;
        }

        resultRepository.save(UserInvestmentProfileResult.createNotTested(userId, currentVersion));
    }

    public InvestmentProfileResultResponse submitResult(SubmitInvestmentProfileResultRequest request) {
        validateUser(request.userId());
        return saveCompletedResult(request.userId(), request.version(), request.answers());
    }

    public InvestmentProfileResultResponse skip(SkipInvestmentProfileRequest request) {
        validateUser(request.userId());
        validateVersion(request.version());
        resultRepository.clearCurrentResult(request.userId());
        UserInvestmentProfileResult result = resultRepository.save(
                UserInvestmentProfileResult.createSkipped(request.userId(), request.version())
        );
        return toResponse(result);
    }

    public InvestmentProfileResultResponse getCurrentResult(Long userId) {
        validateUser(userId);
        UserInvestmentProfileResult result = resultRepository.findByUserIdAndCurrentTrue(userId)
                .orElseGet(() -> {
                    Integer currentVersion = findCurrentVersion();
                    if (currentVersion == null) {
                        return null;
                    }
                    return resultRepository.save(UserInvestmentProfileResult.createNotTested(userId, currentVersion));
                });

        if (result == null) {
            return createNotTestedFallbackResponse();
        }

        return toResponse(result);
    }

    public InvestmentProfileResultResponse retest(Long userId, RetestInvestmentProfileRequest request) {
        validateUser(userId);
        return saveCompletedResult(userId, request.version(), request.answers());
    }

    private InvestmentProfileResultResponse saveCompletedResult(
            Long userId,
            Integer version,
            List<InvestmentProfileAnswerRequest> answers
    ) {
        List<InvestmentProfileQuestion> questions = questionRepository.findAllByVersionAndActiveTrueOrderByQuestionOrderAsc(version);
        validateAnswerRequests(version, questions, answers);

        Map<Long, InvestmentProfileQuestion> questionById = toQuestionMap(questions);
        Map<Long, InvestmentProfileOption> optionById = toOptionMap(questionById.keySet());

        int totalScore = 0;
        for (InvestmentProfileAnswerRequest answer : answers) {
            InvestmentProfileOption option = optionById.get(answer.optionId());
            if (option == null || !Objects.equals(option.getQuestion().getId(), answer.questionId())) {
                throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
            }
            totalScore += option.getScore();
        }

        resultRepository.clearCurrentResult(userId);
        UserInvestmentProfileResult result = resultRepository.save(
                UserInvestmentProfileResult.createCompleted(userId, version, determineProfileType(totalScore), totalScore)
        );

        answerRepository.saveAll(answers.stream()
                .map(answer -> UserInvestmentProfileAnswer.create(
                        result,
                        questionById.get(answer.questionId()),
                        optionById.get(answer.optionId())
                ))
                .toList());

        return toResponse(result);
    }

    private void validateAnswerRequests(
            Integer version,
            List<InvestmentProfileQuestion> questions,
            List<InvestmentProfileAnswerRequest> answers
    ) {
        validateVersion(version);
        if (questions.size() != REQUIRED_QUESTION_COUNT || answers.size() != REQUIRED_QUESTION_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
        }

        Set<Long> submittedQuestionIds = new HashSet<>();
        for (InvestmentProfileAnswerRequest answer : answers) {
            if (!submittedQuestionIds.add(answer.questionId())) {
                throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
            }
        }

        Set<Long> validQuestionIds = new HashSet<>();
        for (InvestmentProfileQuestion question : questions) {
            validQuestionIds.add(question.getId());
        }

        if (!validQuestionIds.equals(submittedQuestionIds)) {
            throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
        }
    }

    private Map<Long, InvestmentProfileQuestion> toQuestionMap(List<InvestmentProfileQuestion> questions) {
        Map<Long, InvestmentProfileQuestion> questionById = new HashMap<>();
        for (InvestmentProfileQuestion question : questions) {
            questionById.put(question.getId(), question);
        }
        return questionById;
    }

    private Map<Long, InvestmentProfileOption> toOptionMap(Collection<Long> questionIds) {
        Map<Long, InvestmentProfileOption> optionById = new HashMap<>();
        for (InvestmentProfileOption option : optionRepository
                .findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(questionIds)) {
            optionById.put(option.getId(), option);
        }
        return optionById;
    }

    private Integer getCurrentVersion() {
        Integer currentVersion = findCurrentVersion();
        if (currentVersion == null) {
            throw new CustomException(ErrorCode.INVESTMENT_PROFILE_QUESTION_NOT_FOUND);
        }
        return currentVersion;
    }

    private Integer findCurrentVersion() {
        return questionRepository.findCurrentVersion();
    }

    private void validateVersion(Integer version) {
        if (!Objects.equals(getCurrentVersion(), version)) {
            throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private InvestmentProfileType determineProfileType(int totalScore) {
        if (totalScore >= 15) {
            return InvestmentProfileType.AGGRESSIVE;
        }
        if (totalScore >= 7) {
            return InvestmentProfileType.NEUTRAL;
        }
        return InvestmentProfileType.STABLE;
    }

    private InvestmentProfileResultResponse toResponse(UserInvestmentProfileResult result) {
        UiContent uiContent;
        if (result.getTestStatus() == InvestmentProfileTestStatus.COMPLETED) {
            uiContent = completedUiContent(result.getProfileType());
        } else if (result.getTestStatus() == InvestmentProfileTestStatus.SKIPPED) {
            uiContent = new UiContent(
                    "분석 대기중",
                    "분석 대기중",
                    "기본 설정으로 먼저 시작할 수 있어요.",
                    List.of("기본 설정", "마이페이지에서 재검사 가능")
            );
        } else {
            uiContent = new UiContent(
                    NOT_TESTED_LABEL,
                    NOT_TESTED_LABEL,
                    NOT_TESTED_DESCRIPTION,
                    NOT_TESTED_TAGS
            );
        }

        return new InvestmentProfileResultResponse(
                result.getId(),
                result.getVersion(),
                result.getTestStatus(),
                result.getProfileType(),
                uiContent.profileLabel(),
                uiContent.title(),
                uiContent.description(),
                uiContent.tags(),
                DEFAULT_START_BUTTON_LABEL,
                DEFAULT_NEXT_ACTION,
                result.getTotalScore(),
                result.getCalculatedAt()
        );
    }

    private UiContent completedUiContent(InvestmentProfileType profileType) {
        if (profileType == null) {
            throw new CustomException(ErrorCode.INVALID_INVESTMENT_PROFILE_SUBMISSION);
        }

        return switch (profileType) {
            case STABLE -> new UiContent(
                    "안정형",
                    "안정형 투자 성향으로 분석되었어요",
                    "변동성보다 안정적인 흐름을 선호하는 성향이에요.",
                    List.of("안정 추구", "낮은 변동성 선호")
            );
            case NEUTRAL -> new UiContent(
                    "중립형",
                    "중립형 투자 성향으로 분석되었어요",
                    "위험과 수익 사이의 균형을 고려하는 성향이에요.",
                    List.of("위험 중립", "균형 추구")
            );
            case AGGRESSIVE -> new UiContent(
                    "공격형",
                    "공격형 투자 성향으로 분석되었어요",
                    "높은 수익 가능성을 위해 위험을 감수하는 성향이에요.",
                    List.of("공격 추구", "고위험 고수익 선호")
            );
        };
    }

    private InvestmentProfileResultResponse createNotTestedFallbackResponse() {
        return new InvestmentProfileResultResponse(
                null,
                null,
                InvestmentProfileTestStatus.NOT_TESTED,
                null,
                NOT_TESTED_LABEL,
                NOT_TESTED_LABEL,
                NOT_TESTED_DESCRIPTION,
                NOT_TESTED_TAGS,
                DEFAULT_START_BUTTON_LABEL,
                DEFAULT_NEXT_ACTION,
                null,
                null
        );
    }

    private record UiContent(
            String profileLabel,
            String title,
            String description,
            List<String> tags
    ) {
    }
}
