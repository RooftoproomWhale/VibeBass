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
./gradlew :webApp:wasmJsRun
```
*구동 후 `http://localhost:8080`에 접속하여 작동을 검증할 수 있습니다.*

### 2. Android 실행
```bash
./gradlew :androidApp:assembleDebug
```

---

## 🧪 테스트 가이드 (Running Tests)

선형 보간 로직의 수학적 무결성을 검증하기 위해 공통 단위 테스트를 구동합니다.
```bash
# WasmJs 타겟 테스트 실행
./gradlew :shared:wasmJsTest
```

---

## 📌 핵심 구현 특징 (Key Implementations)

1.  **선형 보간(Linear Interpolation) 연산**:
    유튜브 영상의 재생 시간(`currentTime`) 변화에 따라 이전 앵커와 다음 앵커 사이의 최적 스크롤 픽셀 값을 비례 연산하여 실시간 이동 위치를 0.1초 단위로 도출합니다.
2.  **물리 스무딩(Smoothing) 스크롤**:
    픽셀 연산에 따라 악보가 끊기듯 움직이는 것을 방지하기 위해 Compose의 `animateScrollToItem` 및 `AnimationSpec`을 결합한 감쇠 가속 스무딩 스크롤을 탑재했습니다.
3.  **스페이스바 싱크 수집 및 단축키 차단**:
    싱크 매니저 모드가 켜져 있을 때 악보 영역에서 `Spacebar`를 누르면, 유튜브의 디폴트 일시정지 동작을 브라우저 수준(`event.preventDefault()`)에서 차단하고 그 즉시 `currentTime`과 현재 `scroll_pixel`을 매핑해 앵커를 추가합니다.