## 실행 방법

Spring Boot DevTools를 정상적으로 사용하려면 두 개의 터미널을 실행합니다.

### 터미널 1

```bash
.\gradlew.bat -t classes
```

### 터미널 2

```bash
.\gradlew.bat bootRun
```

코드/리소스 저장 → 터미널1이 `classes` 재컴파일 → DevTools가 감지해 서버 재시작.

### 재시작이 깨졌을 때

`Could not load [mappers/]` / `web application instance has been stopped already` 가 뜨면
DevTools 재시작 중 MyBatis 로딩이 꼬인 상태입니다. **터미널2 `bootRun`을 끊고 다시 실행**하면 됩니다.

(mapper 경로는 `classpath*:mappers/**/*.xml` + `META-INF/spring-devtools.properties` 로 완화해 둠)

## Clean

```bash
./gradlew clean
```

## 배포 방법

WAR 파일을 생성합니다.

```bash
.\gradlew.bat clean bootWar
```

빌드 결과물은 아래 경로에 생성됩니다.

```bash
build/libs/ROOT.war
```

생성된 ROOT.war 파일을 Cafe24 Tomcat 서버에 배포합니다.
