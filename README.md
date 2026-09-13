# 🎸 VibeBass (바이브베이스)

Kotlin Multiplatform (KMP) 기반의 차세대 악보 동기화 및 연습 보조 솔루션입니다. 유튜브 연주 영상의 재생 시간에 맞춰 세로 스크롤 PDF 악보가 부드럽게 자동 연동되며, 사용자가 직접 스페이스바를 누르며 간편하게 싱크를 생성하고 조율할 수 있습니다.

---

## 🛠️ 기술 스택 (Technology Stack)

*   **Frontend**: Kotlin 기반 **Compose Multiplatform (CMP)**
    *   *목표 플랫폼*: Web (WasmJs) 1단계 프로토타입 ➡️ Android & iOS 네이티브 확장
    *   *핵심 기능*: YouTube IFrame API 연동, 로컬 PDF 악보 렌더링, 선형 보간법(Linear Interpolation) 스무스 스크롤, Spacebar 이벤트 기반 싱크 매니저(Sync Manager)
*   **Backend**: Kotlin **Spring Boot 3.x**
*   **Database**: **PostgreSQL** (JSONB 형식을 통한 싱크 데이터 모델링)
    *   *싱크 데이터 규격*: `[{"time_sec": 0, "scroll_pixel": 0}, {"time_sec": 15, "scroll_pixel": 400}]`

---

## 📂 프로젝트 모듈 구조 (Project Structure)

*   [**`/shared`**](./shared/src): 여러 플랫폼 간에 공유되는 공통 비즈니스 로직 및 UI 코드
    *   [**`commonMain`**](./shared/src/commonMain/kotlin): 플랫폼 독립적인 메인 App 레이아웃, 선형 보간 계산 유틸리티, `expect` 컴포저블 선언
    *   [**`wasmJsMain`**](./shared/src/wasmJsMain/kotlin): Web 타겟을 위한 `actual` 구현 (유튜브 JS Bridge 바인딩, PDF 스크롤 렌더링, 애니메이션 스펙 연동)
    *   [**`androidMain`** / **`iosMain`**](./shared/src/androidMain/kotlin): 디바이스 플랫폼을 위한 `actual` 뼈대 정의
*   [**`/webApp`**](./webApp): Web (WasmJs) 실행 및 리소스를 포괄하는 웹 쉘 진입부
*   [**`/androidApp`**](./androidApp): Android 네이티브 앱 빌드 모듈
*   [**`/iosApp`**](./iosApp): iOS Xcode 프로젝트 실행 및 빌드 모듈

---

## 🚀 실행 가이드 (Running the Apps)

### 1. Web (WasmJs) 실행
빠르고 모던한 Kotlin/Wasm 개발 환경에서 웹 앱을 구동합니다.
```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```
*구동 후 터미널에 표시되는 웹 주소(기본 `http://localhost:8080`)로 접속합니다. 백엔드도 별도로 실행해야 합니다.*

웹의 API 호출은 현재 접속한 서버의 `/api`를 사용합니다. 개발 서버는 [api-proxy.js](webApp/webpack.config.d/api-proxy.js)를 통해 `/api` 요청을 `http://127.0.0.1:8082`로 전달합니다. 백엔드 주소가 다르면 개발 서버를 시작하는 셸의 `VIBEBASS_DEV_API_TARGET`에 지정합니다. 서버 배포 시에는 웹 리소스와 `/api`를 같은 출처에서 제공하세요. [Kotlin webpack 설정](https://kotlinlang.org/docs/js-project-setup.html#webpack-configuration-file), [webpack 개발 프록시](https://webpack.js.org/configuration/dev-server/#devserverproxy)

### 2. Android 실행
```bash
./gradlew :androidApp:assembleDebug
```

### 3. YouTube 자동 검색용 서버 API 키

현재 결정(2026-09-13): 서버 배포 전까지 `YOUTUBE_API_KEY`를 미설정하여 자동 검색을 비활성화합니다. 새 키 발급과 Docker 실행 환경 주입은 서버 배포 시 진행합니다. [배포 시 재개할 작업](PROJECT_BACKLOG.md#서버-배포-시-재개-youtube-자동-검색)에 기록했습니다. 아래 설정 절차는 자동 검색을 재개할 때 적용합니다.

백엔드 실행 환경에 `YOUTUBE_API_KEY`를 설정합니다. 키가 없거나 공백이면 자동 검색 API는 외부 요청 없이 **503**을 반환합니다. PDF 열기와 YouTube 링크 직접 입력은 계속 사용할 수 있습니다. 외부 검색 오류는 상세 내용을 노출하지 않는 **502**로 처리합니다.

IntelliJ의 백엔드 Run Configuration에서 환경변수를 지정하거나, PowerShell에서 아래처럼 입력한 후 **같은 셸에서** 백엔드를 실행하세요. 키를 소스, 공유 Run Configuration, 웹 리소스, 명령 기록에 저장하지 마세요. Spring Boot는 이 프로젝트의 `.env` 파일을 자동으로 읽지 않습니다.

```powershell
$youtubeKeyInput = Read-Host 'YouTube API key' -AsSecureString
$env:YOUTUBE_API_KEY = [System.Net.NetworkCredential]::new('', $youtubeKeyInput).Password
Remove-Variable youtubeKeyInput
```

기존 설정에 있던 키 형태의 값은 Git 이력에 남아 있습니다. **실제 발급된 키라면 Google Cloud Console에서 교체해야 합니다.** 새 키의 API 제한을 **YouTube Data API v3**로 설정하고, 운영 서버에 고정 송신 IP가 있다면 해당 IP만 허용하세요. 새 키를 서버 환경에 반영하고 검색을 확인한 뒤 기존 키를 삭제하세요. 사용하지 않는 키라면 바로 삭제하세요. 이번 소스 수정은 Google 계정의 키를 교체하거나 Git 이력을 삭제하지 않습니다. [Google API 키 관리 지침](https://docs.cloud.google.com/docs/authentication/api-keys-best-practices)

서버는 키를 URL 쿼리 대신 `X-Goog-Api-Key` 헤더로 전송합니다. 운영 HTTP 디버그·프록시·APM 설정에서도 이 헤더를 기록하지 않도록 마스킹해야 합니다. [Google 시스템 파라미터](https://docs.cloud.google.com/apis/docs/system-parameters)

---

## 🧪 테스트 가이드 (Running Tests)

악보 테스트는 사용자 지정 PDF 9개를 우선 사용합니다. 정확한 파일명은 [pdf-samples.json](webApp/src/webMain/tests/pdf-samples.json)에 기록했습니다. 기본 위치는 현재 사용자의 `Downloads`이며, 파일이 이동하면 `VIBEBASS_PDF_SAMPLE_DIR`로 폴더를 지정합니다. PDF 원본은 저장소에 복사하지 않습니다.

아래 명령으로 로컬 검사 서버를 실행하고, 출력된 주소에서 **PDF 9개 검사**를 누릅니다. 실제 파일 선택 처리·PDF.js 렌더링·마지막 페이지 위치·리사이즈·파일명/좌표 JSON 왕복을 확인합니다. 검사는 루프백 주소에서 실행되며 곡 데이터는 테스트 서버 메모리에만 저장합니다. 검사가 끝나면 Ctrl+C로 종료합니다.

```bash
node webApp/src/webMain/tests/pdf-browser-check.cjs
```

이 도구는 실제 PDF/JavaScript 브리지와 임시 HTTP API를 검사합니다. Compose 화면, Spring HTTP 처리와 PostgreSQL 저장은 별도 통합 검증 대상입니다.

선형 보간 로직의 수학적 무결성을 검증하기 위해 공통 단위 테스트를 구동합니다.
```bash
# WasmJs 타겟 테스트 실행
./gradlew :shared:wasmJsTest
```

빌드 없이 PDF·YouTube 브리지 회귀 검사를 실행할 수 있습니다. Node 기본 테스트 모듈만 사용하며, VM 모듈 옵션은 테스트에서 CDN 모듈을 가짜 PDF 엔진으로 대체하기 위한 설정입니다.

```bash
node --experimental-vm-modules --test webApp/src/webMain/tests/practice-media.test.cjs webApp/src/webMain/tests/practice-api.test.cjs
```

백엔드의 `YoutubeSearchServiceTest`에는 키 누락, 헤더 전송, 검색어 인코딩, 외부 오류 정보 차단 검사가 포함됩니다. Gradle 테스트는 컴파일과 웹 패키징을 수반하므로 별도로 실행해야 합니다.

`ApiContractTest`는 MockMvc로 특수문자 왕복, 입력 오류 400, 없는 곡의 조회·수정·삭제 404, 검색 장애 502 및 키 누락 503의 응답 계약을 검사합니다. 2026-09-13 작업에서는 Node 검사 17개와 사용자 PDF 9개·38페이지의 데스크톱/모바일 브리지 검사를 통과했습니다. 백엔드 테스트, webpack 개발 프록시 실제 기동과 전체 Compose 실행은 빌드 제한에 따라 미실행입니다.

PDF.js는 `practice-media.js`에서 **6.3.289**로 고정해 첫 PDF를 열 때 ES 모듈로 불러옵니다. 본체·worker·CMap·ICC 색상 프로파일·기본 글꼴·Wasm 디코더는 같은 버전의 jsDelivr 배포본을 사용합니다. 버전을 바꿀 때는 실제 PDF 렌더링과 브리지 검사를 함께 확인하세요. `isEvalSupported: false` 설정도 유지하지만, v6에서는 해당 eval 경로 자체가 제거되어 보안 조치의 근거는 패치된 엔진입니다. [Mozilla 보안 권고](https://github.com/mozilla/pdf.js/security/advisories/GHSA-wgrm-67xf-hhpq), [적용 릴리스](https://github.com/mozilla/pdf.js/releases/tag/v6.3.289)

## API 입력과 오류 계약

곡 저장/수정 시 제목은 공백이 아닌 255자 이하, 가수명은 선택값으로 255자 이하, 영상 ID는 영문·숫자·`_`·`-`로 이루어진 11자리입니다. 제목·가수명의 NUL 문자는 차단합니다. 앵커는 최대 10,000개이며 같은 시각은 중복할 수 없습니다. 시각·픽셀·선택적 페이지 좌표는 0 이상이고 Kotlin `Float` 범위에 들어오는 유한한 숫자여야 합니다. 기존 앵커의 `pagePosition: null`은 허용하지만, 필수 숫자의 `null`과 리스트 내부 `null`은 거부합니다. 검색어는 공백이 아닌 500자 이하입니다.

브라우저는 `JSON.stringify`로 문자열을 직렬화하므로 따옴표·역슬래시·줄바꿈·탭·한글을 수작업으로 이스케이프하지 않습니다. 서버의 오류 응답은 `application/problem+json`의 `status`, `detail`, `code`를 사용하며 입력 원문·내부 예외를 포함하지 않습니다. 브라우저도 프록시 HTML이나 오류 본문을 그대로 표시하지 않습니다. [JSON 직렬화](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/stringify), [Spring 검증](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 잘못된 JSON, 필수값·형식·범위 오류 |
| 404 | `NOT_FOUND` | 없는 곡 또는 경로 |
| 502 | `YOUTUBE_SEARCH_FAILED` | 외부 자동 검색 실패 |
| 503 | `YOUTUBE_SEARCH_DISABLED` | API 키 미설정으로 자동 검색 비활성화 |
| 500 | `REQUEST_FAILED` | 내부 처리 실패 |

---

## 📌 핵심 구현 특징 (Key Implementations)

1.  **선형 보간(Linear Interpolation) 연산**:
    영상 시각에 따라 두 앵커 사이의 페이지 위치를 보간합니다. 새 앵커는 `pagePosition`(0부터 시작하는 페이지 번호 + 페이지 내부 비율)을 저장합니다. 예를 들어 `1.25`는 두 번째 페이지의 위에서 25%인 지점입니다. 기준선은 뷰어 위쪽 여백 아래이며, 기록과 재생에 같은 기준을 사용합니다.
2.  **화면 크기에 맞춘 위치 복원**:
    현재 페이지 크기로 재생 픽셀을 계산하며, 창 크기 변경과 패널 복귀에도 문서 위치를 유지합니다. 로딩 중 받은 재생 목표는 해당 페이지가 준비되면 적용합니다. 마지막 페이지도 기준선까지 스크롤할 수 있도록 하단 여백을 둡니다. 페이지 사이 빈 여백은 다음 페이지 시작 위치로 맞춥니다.
3.  **스페이스바 싱크 수집 및 단축키 차단**:
    싱크 편집 중 악보 영역·Compose 연습 영역에 포커스가 있으면 Space로 현재 시각과 악보 위치를 기록합니다. PDF 영역에서는 기본 스크롤을 차단하고 키를 길게 누를 때 중복 기록을 막습니다. YouTube iframe에 포커스가 있으면 YouTube 자체 단축키를 사용합니다.

기존 데이터의 `scrollPixel`도 유지합니다. `pagePosition`이 없는 앵커가 하나라도 포함된 곡은 전체를 기존 픽셀 방식으로 재생합니다. 원래 화면 크기를 알 수 없어 자동 변환하지 않으며, 화면 크기 변경에 대응하려면 해당 싱크를 다시 기록해야 합니다. 새 필드를 보존하려면 웹과 백엔드 변경을 함께 적용해야 합니다. 기존 JSONB 기록은 선택 필드의 기본값 `null`로 읽도록 했으며, 실제 DB 왕복과 Kotlin/Compose 전체 실행은 별도 빌드 검증이 필요합니다.
