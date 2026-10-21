# 설정 예시 (config-samples)

실제 실행 파일은 **상위** `src/main/resources/` 에 둔다 (gitignore):

- `application.yml`
- `application-local.yml`
- `application-prod.yml`

## 다른 PC 세팅

1. 이 폴더의 세 yml을 상위(`resources/`)로 복사
2. DB·JWT·업로드 경로 등 **공유하면 안 되는 값**만 채움
3. `bootRun` (`spring.profiles.active=local` 기본)

BFF 서버 인증은 local/prod 모두 필수다. `BFF_INTERNAL_KEY`에 Next 서버와 동일한 32자 이상 난수를 설정한다. 환경별 키는 다르게 사용한다. 예제에는 실제 키가 없다. prod는 HTTPS를 요구하고, TLS 종료 프록시가 있으면 `BFF_TRUSTED_PROXIES`에 그 프록시의 실제 IP/CIDR만 지정한다. 브라우저 IP나 `0.0.0.0/0`을 넣지 않는다.

FE는 공통 `.env`, 로컬 `.env.local`, 운영 `.env.production`을 사용하며 각 `.example`만 Git으로 공유한다. BE 로컬 키는 FE `.env.local`, 운영 키는 FE `.env.production`의 `BFF_INTERNAL_KEY`와 각각 맞춘다. Spring은 FE 환경 파일을 자동으로 읽지 않으므로 BE 환경변수에도 별도로 설정한다.

세 샘플은 대응하는 실제 yml과 설정 항목을 동일하게 유지한다. DB 연결 정보·JWT·AI/BFF 키·개인 업로드 경로는 환경변수 참조 또는 예시로 대체한다. `resources/` 바로 아래의 실제 `application*.yml`은 Git에서 제외하며 이 폴더의 샘플만 공유한다.

Spring Boot는 classpath **루트**의 `application*.yml`만 설정으로 읽는다.  
이 하위 폴더 파일은 **자동 로드되지 않는다** (이름과 무관).
