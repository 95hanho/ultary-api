# MariaDB MCP (Cafe24 / HeidiSQL)

에이전트가 HeidiSQL과 같은 DB에 붙어 스키마·조회를 할 수 있게 한다.

## Codex 전용 연결

Cursor의 `.cursor/mcp.json`과 `mcp/mariadb/.env`는 그대로 사용한다.
Codex는 별도 `ultary_mariadb` 서버와 `mcp/mariadb/.env.codex`를 사용한다.

1. `.env.codex.example`을 `.env.codex`로 복사한다. 이미 있으면 덮어쓰지 않는다.
2. `.env.codex`에 `MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE`를 채운다.
   포트 기본값은 3306이다. 비밀번호에 `#`나 공백이 있으면 따옴표로 감싼다.
3. 형식 확인: `node mcp/mariadb/codex-start.mjs --check` (DB 접속은 하지 않음).
4. Codex 사용자 설정에 다음 서버를 등록한다. 실제 Node.js 경로와 저장소 경로를 사용한다.

```toml
[mcp_servers.ultary_mariadb]
command = 'C:\Program Files\nodejs\node.exe'
args = ['D:\workspace\java\ultary-api\mcp\mariadb\codex-start.mjs']
startup_timeout_sec = 60
tool_timeout_sec = 60
```

5. 계정 입력 후 Codex의 MCP 서버를 재시작/다시 로드한다.
6. `SELECT 1`, 현재 DB, 테이블 목록을 조회하여 실제 연결을 확인한다.

Node.js 24 이상과 npm이 필요하다. 실행 파일은 고정 버전
`@ashenud/mcp-mysql-server@1.0.0`을 npx로 실행한다. 첫 실행은 패키지 다운로드가 필요할 수 있다.
계정정보는 Codex 설정이나 Git에 저장하지 않는다. `.env.codex`는 Git 제외 대상이다.
두 프로젝트의 공통 DB라면 이 연결 하나를 사용한다. 조회/수정/DDL 가능 여부는 DB 계정 권한에 따른다.
MCP 연결 설정은 Cursor와 분리되지만, 같은 DB의 스키마·데이터 변경은 양쪽에 적용된다.

## 1. env 채우기

```text
mcp/mariadb/.env.example  →  mcp/mariadb/.env  (이미 .env 있으면 그 파일만 수정)
```

`MYSQL_HOST` / `USER` / `PASSWORD` / `DATABASE` 만 HeidiSQL과 동일하게 입력.

## 2. Cursor에서 할 일 (Figma와 비교)

| | Figma MCP | MariaDB MCP |
|--|-----------|-------------|
| 로그인(OAuth) | 있음 (Connect / 권한 허용) | **없음** |
| 설정 | MCP 켜기 | 동일: **Settings → MCP** 에서 `mariadb` **Enable** |
| 도구 실행 | 승인 팝업 | 동일: 에이전트가 도구 쓸 때 **Allow** (또는 Auto-review) |

Figma처럼 웹 계정 연동 버튼은 없고, **MCP 서버 ON + (필요할 때) 도구 승인**이면 된다.

## 3. 확인

1. Cursor 재시작 또는 MCP 패널에서 `mariadb` 서버 새로고침
2. 상태가 초록/연결됨인지 확인
3. 채팅에서 «DB 테이블 목록 보여줘» 등으로 테스트

## 주의

- 운영 DB면 **조회 전용 계정** 권장
- `.env` 는 git 제외 (비밀번호)
- Node.js / `npx` 필요
