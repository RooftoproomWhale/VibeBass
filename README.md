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
*구동 후 `http://localhost:8080`에 접속하여 작동을 검증할 수 있습니다.*

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

선형 보간 로직의 수학적 무결성을 검증하기 위해 공통 단위 테스트를 구동합니다.
```bash
# WasmJs 타겟 테스트 실행
./gradlew :shared:wasmJsTest
```

빌드 없이 PDF·YouTube 브리지 회귀 검사를 실행할 수 있습니다. Node 기본 테스트 모듈만 사용하며, VM 모듈 옵션은 테스트에서 CDN 모듈을 가짜 PDF 엔진으로 대체하기 위한 설정입니다.

```bash
node --experimental-vm-modules --test webApp/src/webMain/tests/practice-media.test.cjs
```

백엔드의 `YoutubeSearchServiceTest`에는 키 누락, 헤더 전송, 검색어 인코딩, 외부 오류 정보 차단 검사가 포함됩니다. Gradle 테스트는 컴파일과 웹 패키징을 수반하므로 별도로 실행해야 합니다.

PDF.js는 `practice-media.js`에서 **6.3.289**로 고정해 첫 PDF를 열 때 ES 모듈로 불러옵니다. 본체·worker·CMap·기본 글꼴·Wasm 디코더는 같은 버전의 jsDelivr 배포본을 사용합니다. 버전을 바꿀 때는 실제 PDF 렌더링과 브리지 검사를 함께 확인하세요. `isEvalSupported: false` 설정도 유지하지만, v6에서는 해당 eval 경로 자체가 제거되어 보안 조치의 근거는 패치된 엔진입니다. [Mozilla 보안 권고](https://github.com/mozilla/pdf.js/security/advisories/GHSA-wgrm-67xf-hhpq), [적용 릴리스](https://github.com/mozilla/pdf.js/releases/tag/v6.3.289)

---

## 📌 핵심 구현 특징 (Key Implementations)

1.  **선형 보간(Linear Interpolation) 연산**:
    유튜브 영상의 재생 시간(`currentTime`) 변화에 따라 이전 앵커와 다음 앵커 사이의 최적 스크롤 픽셀 값을 비례 연산하여 실시간 이동 위치를 0.1초 단위로 도출합니다.
2.  **물리 스무딩(Smoothing) 스크롤**:
    픽셀 연산에 따라 악보가 끊기듯 움직이는 것을 방지하기 위해 Compose의 `animateScrollToItem` 및 `AnimationSpec`을 결합한 감쇠 가속 스무딩 스크롤을 탑재했습니다.
3.  **스페이스바 싱크 수집 및 단축키 차단**:
    싱크 매니저 모드가 켜져 있을 때 악보 영역에서 `Spacebar`를 누르면, 유튜브의 디폴트 일시정지 동작을 브라우저 수준(`event.preventDefault()`)에서 차단하고 그 즉시 `currentTime`과 현재 `scroll_pixel`을 매핑해 앵커를 추가합니다.