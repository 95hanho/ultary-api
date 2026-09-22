# 설정 예시 (config-samples)

실제 실행 파일은 **상위** `src/main/resources/` 에 둔다 (gitignore):

- `application.yml`
- `application-local.yml`
- `application-prod.yml`

## 다른 PC 세팅

1. 이 폴더의 세 yml을 상위(`resources/`)로 복사
2. DB·JWT·업로드 경로 등 **공유하면 안 되는 값**만 채움
3. `bootRun` (`spring.profiles.active=local` 기본)

Spring Boot는 classpath **루트**의 `application*.yml`만 설정으로 읽는다.  
이 하위 폴더 파일은 **자동 로드되지 않는다** (이름과 무관).
