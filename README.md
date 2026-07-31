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