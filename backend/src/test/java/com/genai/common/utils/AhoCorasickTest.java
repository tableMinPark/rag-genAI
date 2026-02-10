package com.genai.common.utils;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AhoCorasickTest {

    private static final Logger log = LoggerFactory.getLogger(AhoCorasickTest.class);

    @Test
    void ahoCorasick() {
        AhoCorasick ahoCorasick = new AhoCorasick();

        String content =  "참고 문서:\n\n9.3.2.3. filterQuery 기본구조 221\n9.3.2.4. 옵션설명 221\n9.3.3. MATCH 연산자 221\n9.3.4. NOT_MATCH 연산자 222\n9.3.5. IN 연산자 222\n9.3.6. NOT_IN 연산자 223\n9.3.7. START 연산자 224\n9.3.8. END 연산자 224\n9.3.9. SUBSTRING 연산자 225\n9.3.10. GT, GTE, LT, LTE 연산자 225\n9.3.11. RANGE 연산 226\n9.3.11.1. API 호출 226\n9.3.11.2. range 연산 기본구조 226\n9.3.11.3. 옵션설명 226\n9.3.11.4. 예시 227\n9.3.12. 날짜 제한 검색 쿼리 (Date Range Query) 227\n9.3.12.1. API 호출 228\n9.3.12.2. date_range 연산 기본구조 228\n9.3.12.3. 옵션설명 228\n9.3.12.4. 예시 229\n9.3.13. 카테고리 검색 쿼리 (Category Search Query) 229\n9.3.13.1. API 호출 230\n9.3.13.2. categorySearch 연산 기본구조 230\n9.3.13.3. 옵션설명 230\n10\n9.3.13.4. 예시 230\n9.4. 권한 쿼리 (Authority Query) 231\n9.4.1. 개요 231\n9.4.1.1. 권한 컬렉션 API 호출 231\n9.4.1.2. 권한 쿼리 API 호출 232\n9.4.1.3. 권한 조회 232\n9.4.2. 권한 컬렉션 (Authority Collection) 232\n9.4.2.1. 컬렉션 내부 옵션 232\n9.4.3. 권한 쿼리 (Authority Query) 233\n9.4.3.1. 권한 쿼리 기본 구조 233\n9.4.3.2. 쿼리 내부 옵션 234\n9.4.3.2.1. 예제 234\n9.4.3.2.2. 권한 컬렉션 설정 234\n9.4.3.2.3. 검색 234\n9.5. 부가 기능 235\n9.5.1. 소개 235\n9.5.2. 정렬 (Sorting) 235\n9.5.2.1. API 호출 235\n9.5.2.2. 정렬 기본 구조 235\n9.5.2.3. 쿼리 내부 옵션 236\n9.5.2.4. 단일 정렬 예제 236\n9.5.2.5. 멀티 정렬 예제 237\n9.5.3. 형태소 분석 조회 기능 (Morpheme Analysis) 238\n9.5.3.1. API 호출 238\n9.5.3.2. 쿼리 내부 옵션 239\n9.5.3.3. 예제 240\n9.5.4. 최근 검색어 조회 (Recent Query List) 240\n9.5.4.1. API 호출 240\n9.5.4.2. 기본 구조 241\n9.5.4.3. 쿼리 내부 옵션 241\n9.5.4.4. 예제 242\n9.5.5. 머지 컬렉션 (Merged Collection) 243\n11\n9.5.5.1. API 호출 244\n9.5.5.2. 머지 컬렉션 검색 기본 구조 244\n9.5.5.3. 쿼리 내부 옵션 245\n9.5.5.4. 예제 246\n9.5.6. 거리 제한 검색 (Geo Distance Search) 248\n9.5.6.1. API 호출 248\n9.5.6.2. 거리 검색 일반 검색 기본 구조 248\n9.5.6.3. 쿼리 내부 옵션 249\n9.5.6.4. 예제 249\n9.6. 랭킹 250\n9.6.1. 랭킹 요소 250\n9.6.2. 기본 구조 251\n9.6.3. 랭킹 내부 옵션 251\n9.6.4. 키워드 부스팅 쿼리 주의 사항 252\n9.6.5. 기본 랭킹 252\n9.6.6. 커스텀 랭킹 260\n10. 로그 관리 262\n10.1. 로그 설정 262\n10.1.1. 로그 파일 경로 262\n10.1.2. 로그 언어 지원 263\n10.2. 시스템 로그 263\n10.3. 색인 로그 264\n10.4. 검색 로그 265\n12\n1. 제품 개요\n본 제품은 데이터 빅뱅 시대에 초대용량 검색을 요구하는 고객 니즈가 점차 늘어남에 따른 선제적\n대응으로 개발된 Java 기반 클라우드 레벨 분산형 검색엔진이다. 1억건 이상의 초대용량 검색이\n가능하도록 개발되었다.\n1.1. 제품 특징";
//        String content =  "검색어 옵션";


        ahoCorasick.insert("검색어");
        ahoCorasick.insert("옵션");
        ahoCorasick.insert("상민");
        ahoCorasick.insert("연산자자");

        ahoCorasick.build();

        log.info("{}", ahoCorasick.search(content));
    }
}