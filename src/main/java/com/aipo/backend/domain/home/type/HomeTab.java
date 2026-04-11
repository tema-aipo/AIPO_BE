package com.aipo.backend.domain.home.type;

public enum HomeTab {
    // 최근 성장순 탭
    RECENT_GROWTH("recentGrowth"),

    // 청약 예정순 탭
    SUBSCRIPTION_UPCOMING("subscriptionUpcoming"),

    // 관심 종목순 탭
    FAVORITE("favorite");

    // 클라이언트에서 전달하는 실제 문자열 값
    private final String value;

    HomeTab(String value) {
        this.value = value;
    }

    // 응답에 다시 문자열 값으로 내보낼 때 사용
    public String getValue() {
        return value;
    }

    // 요청으로 들어온 문자열을 enum으로 변환
    public static HomeTab from(String value) {
        // tab이 비어 있으면 기본값으로 최근 성장순 사용
        if (value == null || value.isBlank()) {
            return RECENT_GROWTH;
        }

        // enum에 정의된 값들과 비교해서 일치하는 탭 찾기
        for (HomeTab tab : values()) {
            if (tab.value.equalsIgnoreCase(value)) {
                return tab;
            }
        }

        // 허용되지 않은 값이면 예외 발생
        throw new IllegalArgumentException("지원하지 않는 홈 탭입니다: " + value);
    }
}
//탭 이름 프론트와 최종 확정 -> 문자열 수정 필요
//탭 종류가 늘어나면 추후 enum 추가 필요
