# Ultary API Contract

## Source of truth

Spring `/api/v1/**` 경로·Method의 **원본 정의**는 FE(Next.js) 레포를 따른다.

- `ultary-web/docs/api-memo.md`
- `ultary-web/src/lib/api/endpoints.ts` 의 `springEndpoints`

이 BE 레포의 Controller 매핑은 위 문서와 동일해야 하며, 경로를 바꿀 때는 FE 메모/`springEndpoints`를 먼저 수정한 뒤 BE를 맞춘다.

## 응답

- 성공: `ApiResponse<T>`
- 실패: RFC 7807 `ProblemDetail` + `code` extension

## 스켈레톤

아직 로직·DB가 없는 API는 `ErrorCode.NOT_IMPLEMENTED` (HTTP 501) 을 반환한다.
로그인/refresh/logout/me, health 등 이미 구현된 API는 예외.
