VibeBass 프로젝트 재분석 및 개선 방향 — 2026-09-12
================================================

> 실행 기반 후속 변경 (2026-09-15): T05 코드를 반영했다. 웹 생성물을 `backend/build/generated-resources/web`로 옮겨 `bootJar`에만 연결하고, 서버 리소스 처리에서 오래된 static 경로를 제외했다. 커밋되어 있던 웹 생성물 9개를 제거했다. Spring Boot BOM의 Flyway core·PostgreSQL 모듈, `ddl-auto: validate`, 자동 baseline 차단을 설정하고, V1을 보존한 채 V2로 영상 ID 길이와 SQL 기본값을 맞췄다. 현재 선택된 Gradle 9.3.1의 공식 배포본 checksum을 복원하고 기존 Wrapper JAR의 공식 체크섬도 대조했다. OMA DB·Backend, ECC database-migrations, Serena·Context7·DBHub를 활용했다. 설정·체크섬·삭제 범위는 정적으로 확인했으나 DBHub 연결 실패와 Docker 엔진 미실행으로 **실제 DB 기준선 확인·마이그레이션은 남아 있다.** 빌드 제한으로 Gradle 태스크·JAR·Spring 통합 실행도 미검증이다. [DB 전환 절차와 SQL 검사](backend/DATABASE.md)를 추가했다. 다음 구현은 T06이며, DB 전환 검증은 T05의 미완료 검증 항목으로 유지한다. 아래 분석은 변경 전 기록이다.

> API 후속 변경 (2026-09-13): T04의 API 주소·JSON·입력/오류 처리를 구현했다. 브라우저 API를 같은 출처의 `/api`로 통일하고 개발 프록시를 추가했으며, 문자열 직렬화를 `JSON.stringify`로 교체했다. 서버에는 제목·영상 ID·중첩 앵커·중복 시각 검증, 필수 숫자 null 거부, 곡 없음 404 및 안전한 ProblemDetail 응답을 추가했다. OMA backend, ECC security-review, Serena·Context7 및 ECC Chrome DevTools를 적용했다. Node 검사 17개와 사용자 지정 PDF 9개·38페이지의 데스크톱/모바일 브리지 검증을 통과했다. 실제 파일에서 발견한 CMYK 프로파일 경고에 대해 동일 PDF.js 버전의 ICC 경로도 연결했다. 브라우저의 저장 왕복은 임시 HTTP API 검사이며 Spring·DB 통합 검증이 아니다. 백엔드 계약 테스트는 추가했으나 컴파일 제한으로 미실행이고, webpack 프록시 실제 기동·전체 Compose도 미검증이다. 테스트 PDF 목록·재실행 절차는 README에 기록했다. 다음 작업은 T05(생성물 경로·DB 마이그레이션·Wrapper 무결성)다. 아래 분석은 변경 전 기록이다.

> 싱크 후속 변경 (2026-09-13): T02의 좌표·재생 연결을 구현했다. 새 앵커는 페이지 내부 상대 위치를 함께 저장하고, 현재 화면 크기로 재생 위치를 계산한다. PDF 로딩 중 목표 위치 보관, 정지 중 앵커 변경 반영, 패널 복귀·반복 리사이즈 위치 유지, PDF 포커스에서 Space 기록을 연결했다. 기존 픽셀 앵커는 자동 변환하지 않고 기존 방식으로 재생한다. Node 회귀 검사 12개와 Chrome의 리사이즈·모바일 마지막 페이지·키보드 브리지 검증을 통과했다. 공통 Kotlin 보간 및 백엔드 JSON 호환 테스트는 추가했으나 빌드 제한으로 미실행이며, 실제 Compose 화면·DB 왕복도 미검증이다. T03의 페이지 순서·오래된 요청 격리는 앞선 개편에서 반영되어 해당 회귀 검사도 통과했다. 자동 검색은 키 미설정 상태를 유지하고 발급은 서버 배포 시 진행한다. 아래 분석은 변경 전 기록이다.

> 후속 변경 (2026-09-12): 이 분석을 작성한 뒤 웹 디자인을 전면 개편했다. 반응형 화면, PDF·YouTube 위치 연결, PDF 순서·오래된 요청 격리, 보관함 PDF 재연결 안내가 구현에 반영됐다. 아래 내용과 소스 행 번호는 개편 전 분석 시점의 기록이다. 현재 디자인 기준은 [DESIGN.md](DESIGN.md)을 참고한다. 브리지 회귀 테스트 5개와 320·375·768·1440px의 실제 PDF.js 미디어 검증은 통과했다. 전체 Kotlin/Compose 빌드와 화면 검증은 아직 수행하지 않았다.

> 보안 후속 변경 (2026-09-13): T01의 코드 조치를 반영했다. YouTube API 키 기본값을 제거하고 서버 환경변수만 사용하며, 키 누락 시 외부 호출 없이 503, 외부 검색 실패 시 상세 내용을 제거한 502를 반환하도록 수정했다. 키는 URL 대신 헤더로 전송한다. PDF.js는 6.3.289 ES 모듈로 교체하고 worker·CMap·글꼴·Wasm 버전을 통일했다. ECC security-review, Serena·Context7 MCP, ECC Chrome DevTools를 사용했다. Node 회귀 검사 8개와 실제 Chrome PDF 렌더링을 확인했다. 백엔드 회귀 테스트는 추가했으나 빌드·컴파일 제한에 따라 실행하지 않았다. **기존 키의 Google Cloud 교체·폐기 및 운영 제한 설정은 남아 있으며**, 현재 프로세스에 `YOUTUBE_API_KEY`가 없어 실제 인증 검색도 미검증이다. 설정·교체 절차는 [README.md](README.md)를 참고한다. 아래 분석은 변경 전 기록이다.

현재 VibeBass는 **YouTube 영상과 PDF 악보를 연결하는 웹 중심의 초기 MVP**다. 제품의 핵심 흐름과 서버 저장 기반은 갖췄지만, 저장한 연습을 정확하게 다시 시작하는 경험과 배포 환경에서의 동작 보장이 아직 부족하다. 다음 개발 목표는 **“곡 하나를 등록하고, 싱크를 저장한 뒤, 앱을 다시 열어 같은 악보 위치에서 연습할 수 있다”**로 잡는 것이 적절하다.

분석 기준은 HEAD `6ce46f8`과 2026-09-12의 현재 작업 트리다. 주요 앱 모듈에는 이전 분석 이후 소스 변경이 없으며, OMA 설정과 개발 도구가 추가되어 있다. 기존 `.gitignore`, `AGENTS.md`, Gradle 버전 설정의 변경은 보존했다. 이번에는 설치된 스킬과 MCP를 사용해 기존 지적을 다시 확인하고, 실제 웹 함수의 비동기 동작을 격리 실행했다.

근거를 **코드 확인**, **격리 실행 재현**, **실환경 미검증**으로 구분한다. 격리 실행은 가짜 DOM·PDF 페이지·fetch 응답으로 기존 JavaScript 함수를 호출한 것이며, 실제 브라우저·PDF.js 엔진·Kotlin/Wasm 앱 전체의 실행을 의미하지 않는다. 빌드·컴파일·번들링·패키징 및 이를 수반하는 Gradle 테스트는 이번 요청에서 실행하지 않았다.

이번에 실제 적용한 도구와 검토 관점은 다음과 같다. 전체 설치 목록 대신 이 분석에 사용한 항목을 기록했다.

| 도구 / 스킬 | 실제 적용 | 결과와 한계 |
|---|---|---|
| OMA `plan` 워크플로 | 요구 범위, 우선순위·의존성·담당 역할·완료 기준 정리 | 중간 규모 분석. 제품 구현이나 신규 API 계약 확정은 범위 밖 |
| [oma-architecture](C:/Users/user/IdeaProjects/VibeBass/.codex/skills/oma-architecture/SKILL.md) | 현재 구조 유지와 재작성의 비용·위험 비교 | Recommendation 방식으로 점진 개선 권고 |
| [ECC Compose Multiplatform](C:/Users/user/.codex/plugins/cache/ecc/ecc/2.2.1/skills/compose-multiplatform-patterns/SKILL.md) | 상태 소유권, effect 재실행, expect/actual, 수명주기 검토 | 앵커 변경 시 effect 재실행 누락과 DOM/LazyListState 불일치 구체화 |
| [ECC security-review](C:/Users/user/.codex/plugins/cache/ecc/ecc/2.2.1/skills/security-review/SKILL.md) | 키·입력 검증·권한·외부 의존성·인증 백로그 검토 | 해당 항목만 적용. 다른 프레임워크 예제를 그대로 도입하지 않음 |
| Serena MCP | `initial_instructions`, `list_dir`, `find_file`, `search_for_pattern` | 파일·패턴 탐색 성공. `get_symbols_overview`는 Kotlin 언어 서버 초기화 실패. 심벌·참조·컴파일 진단 미완료 |
| Context7 MCP | Compose effect와 Spring Boot DB 초기화 문서 조회 | Spring 조회에 다른 버전 예제가 섞여 있어 3.3.4 공식 원문으로 재확인 |
| Web 도구·Node 기본 모듈 | 공식 문서 확인, 기존 HTML 함수의 응답 순서 제어 | 외부 서비스 호출 없이 비동기 문제 3건 재현. 신규 패키지 설치 없음 |
| Ponytail 지침 | 기존 함수·브라우저 기능 우선, 불필요한 추상화 보류 | 전면 재작성, 신규 DI/상태관리 프레임워크, 선제 캐시 계층을 권고하지 않음 |

이전 분석과 비교해 PDF 페이지 순서·문서 혼입을 격리 실행으로 재현했고, 이전 YouTube 검색 응답이 최신 영상 ID를 덮어쓰는 문제를 추가했다. 현재는 `.agents/`가 존재하므로 이전의 “규칙 파일 없음”을 현재 환경 문제로 보지 않는다.

현재 구현 범위는 다음과 같다.

| 영역 | 구현된 기반 | 현재 한계 |
|---|---|---|
| 공통 Kotlin/Compose | 메인 화면, 앵커 모델, 선형 보간 계산, 플랫폼 인터페이스 | 화면·세션 상태·API 호출·키보드·스크롤 처리가 한 화면에 집중 |
| Web/Wasm | 로컬 PDF 선택, PDF.js 렌더링, YouTube 시간 수신, 자동 검색, 싱크 저장·목록 조회 | PDF 복원·수동 보정 부족, 비동기 문서/영상 혼입, 좌표·레이아웃 검증 부족 |
| Backend | 곡 CRUD, PostgreSQL JSONB 앵커, 소프트 삭제, YouTube 검색 | 인증·소유권, 입력 검증, 오류 응답 계약, 실제 DB 통합 검증 부족 |
| Android/iOS | 프로젝트와 공통 화면 진입점 | 플레이어·PDF·파일 선택·저장/조회 actual이 빈 구현, 일부 공통 API actual 누락 |
| 빌드·품질 | Gradle Wrapper, 웹 포함 JAR 패키징 구성, 일부 단위 테스트 | 서버 테스트와 웹 배포 빌드 결합, CI 구성 미확인, Wrapper 배포본 checksum 설정 누락 |

현재 선언된 버전은 Kotlin 2.4.0, Compose Multiplatform 1.11.1, Spring Boot 3.3.4, AGP 9.1.1, Gradle 9.3.1이다. 조합의 빌드 호환성은 이번에 검증하지 않았다. Gradle daemon JVM 21, backend toolchain 17, Android JVM target 11은 서로 다른 역할이므로 숫자가 다르다는 이유만으로 충돌로 판단하지 않는다. Spring MVC에서 WebClient를 사용하는 현재 구성도 그 자체를 결함으로 분류하지 않는다.

컨트롤러·서비스·저장소 분리, 순수 함수로 분리한 보간 계산, 공통/플랫폼 코드의 경계는 유지할 만한 기반이다. 현재 규모에서는 이 구조를 활용해 핵심 흐름을 완성하고, 변경하는 부분부터 책임을 나누는 접근이 적절하다.

개선은 다음 순서로 진행하는 것을 권한다. 외부 공개 여부에 따라 인증의 착수 시점은 달라지지만, 공개 서비스의 완료 조건에서는 빠질 수 없다.

| 우선순위 | 개선 과제 | 완료 기준 |
|---|---|---|
| P0 · 즉시 | API 키 기본값 제거와 PDF.js 취약점 조치 | 저장소 설정에 실제 키 기본값이 없고, 취약점 조치 및 PDF 렌더 회귀 확인 완료 |
| P1 · 핵심 기능 | 앵커 수집·재생 좌표 통일 | 스크롤 후 기록한 위치로 재생 시 돌아가며, 화면 폭 변경에도 같은 악보 지점을 가리킴 |
| P1 · 핵심 기능 | PDF/검색 비동기 작업의 현재 세션 확인 | 이전 문서의 페이지·메타데이터·검색 결과가 현재 문서와 영상을 바꾸지 않음 |
| P1 · 핵심 기능 | PDF와 곡·영상·싱크의 저장/복원 완성 | 새로고침 후 곡을 선택하면 정확한 악보와 싱크를 복원하고, 다른 곡과 섞이지 않음 |
| P1 · 실행 기반 | API 주소·빌드 구성·DB 초기화 정리 | 서버 주소·DB 변경 절차·생성물 경로가 명확함. 빌드 실행 검증은 명시적인 요청 후 수행 |
| P1 · 외부 공개 전 | 인증·소유권·검색 호출 제한 | 사용자 A가 사용자 B의 곡/PDF를 읽거나 변경할 수 없고, 검색 호출량 제한이 적용됨 |
| P1 · 사용성 | 제목/가수/영상 수동 보정, 저장 상태와 실패 복구 | 자동 추출·검색이 실패해도 직접 등록 가능하고, 저장 중복 클릭과 실패를 구분함 |
| P2 · 유지보수 | 화면 상태·저장소·PDF/플레이어 브리지 분리 | 핵심 세션 동작을 UI 없이 검증하고, 플랫폼 자원 생성/해제 경계가 명확함 |
| P2 · 제품 확장 | 반복 연습·속도 조절·모바일 구현·마디 강조 | 실제 연습 피드백을 바탕으로 순서를 정하고, 플랫폼마다 동일한 핵심 시나리오 통과 |

각 과제의 근거와 구현 방향은 아래와 같다.

1. **키와 PDF 처리 의존성을 우선 정리해야 한다.**

   [application.yml:25](C:/Users/user/IdeaProjects/VibeBass/backend/src/main/resources/application.yml:25)에 YouTube API 키 형태의 문자열이 기본값으로 들어 있다. 키 유효성이나 외부 노출 범위는 확인하지 않았다. 환경변수 기본값에서 제거하고, 실제 사용 키라면 교체 및 사용 제한을 적용해야 한다. 키 값은 이 문서에 기록하지 않았다.

   [index.html:9](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:9)은 PDF.js 3.11.174를 사용한다. 이 버전은 CVE-2024-4367의 공식 영향 범위에 포함되며, 현재 getDocument 설정에 isEvalSupported 비활성화도 없다. 악성 PDF를 열 때 호스팅 도메인에서 JavaScript가 실행될 수 있다는 공식 권고가 있다. 해당 취약점에 대한 임시 완화는 isEvalSupported=false이며, 패치가 반영된 버전으로 교체하고 본체·worker·cMap 버전을 함께 맞춰야 한다. 실제 악성 PDF 실행은 수행하지 않았다. [Mozilla 공식 보안 권고](https://github.com/mozilla/pdf.js/security/advisories/GHSA-wgrm-67xf-hhpq)

   업그레이드 목표 버전은 최신 공식 권고와 현재 브리지 호환성을 함께 확인해 고정해야 한다. 이 판정은 현재 사용 중인 3.11.174와 해당 공식 권고를 대조한 결과이며, 전체 의존성 취약점 스캔을 완료했다는 의미는 아니다.

2. **핵심 제품 가치인 싱크 정확도를 먼저 검증하고 좌표를 통일해야 한다.**

   [App.kt:138](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:138)은 LazyListState의 index/offset으로 기록할 픽셀을 계산한다. [PdfSheetViewer.wasmJs.kt:28](C:/Users/user/IdeaProjects/VibeBass/shared/src/wasmJsMain/kotlin/com/woong/vibebass/components/PdfSheetViewer.wasmJs.kt:28)은 실제 DOM scrollTop을 그 상태에 전달하지만, 화면에는 이 상태를 사용하는 LazyColumn/LazyRow가 없다. AndroidX upstream 소스의 scroll은 첫 레이아웃을 기다리는 경로를 포함하므로, 연결되지 않은 상태를 좌표 저장소로 사용하는 것은 신뢰하기 어렵다. 프로젝트에 해결된 정확한 Compose 바이너리와의 일치 및 실제 증상은 미검증이다. [AndroidX LazyListState 소스](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyListState.kt)

   최소 수정은 DOM이 알려준 실제 문서 위치를 직접 상태로 보관하고 가상 300px 항목 환산을 제거하는 것이다. 현재 두 경로에서 호출하는 PDF 스크롤 명령도 한 경로로 합친다. 새 스크롤 엔진을 먼저 만들 필요는 없다.

   기록 경로는 scrollTop을 사용하고, [재생 경로:294](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:294)는 받은 픽셀에서 고정값 250을 뺀다. 뷰어의 스크롤 위치와 고정 플레이헤드가 가리키는 문서 위치가 혼용되어 있다. 예를 들어 scrollTop 500을 그대로 기록하면 재생 함수는 250으로 이동을 요청한다. 저장 좌표의 의미를 먼저 정의하고 수집·재생의 변환을 대칭으로 맞춰야 한다.

   격리 실행에서도 입력 500에 대한 scrollTo 요청값 250을 확인했다. 이 관찰 자체가 보정식의 오류를 뜻하지는 않는다. 기록값이 scrollTop인지, 플레이헤드가 가리키는 문서 Y인지에 따라 맞는 식이 달라진다. 플레이헤드 기준 위치를 저장한다면 기록 때 해당 오프셋을 더하고 재생 때 같은 기준으로 빼야 한다.

   추가로 [App.kt:105](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:105)의 LaunchedEffect는 currentTime과 isSyncMode만 키로 사용한다. 내부에서 사용하는 anchorPoints가 바뀌어도 두 키가 그대로면 다시 실행되지 않는다. 일시정지 상태에서 앵커를 바꾸거나 같은 시각의 다른 싱크를 로드하는 상황을 포함해 의존성을 맞춰야 한다. [Compose effect 공식 문서](https://developer.android.com/develop/ui/compose/side-effects)

   PDF canvas의 표시 폭은 90%인데 앵커는 절대 픽셀만 저장한다. 화면 폭에 따라 페이지 높이가 달라지므로 같은 픽셀이 같은 마디를 가리킨다고 보장할 수 없다. 파일 식별자와 좌표 버전을 두고, 페이지 번호와 페이지 내부 정규화 Y좌표 등 문서 기준 위치를 저장하는 방향이 적절하다. 기존 pixel 데이터에는 원래 렌더 크기 정보가 없어 자동 변환이 가능한지 먼저 확인해야 한다.

   우선 검증할 시나리오는 수동 스크롤 후 Space 기록, 기록한 시각으로 탐색, 일시정지 후 곡 변경, 창 크기 변경이다. 보간 함수 최적화는 그 이후다. 현재 [SyncCalculator:16](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/sync/SyncManager.kt:16)은 호출마다 정렬하지만, 현재 데이터 규모에서 성능 병목인지 측정한 결과는 없다.

3. **저장된 곡을 다시 여는 흐름을 하나의 연습 세션으로 완성해야 한다.**

   [SongData:3](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/sync/SongData.kt:3)와 서버 Song에 PDF 식별자/위치가 없다. [보관함 선택:374](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:374)은 영상·앵커·표시 파일명만 바꾸고 pdfPath는 그대로 둔다. 따라서 새로고침 후 PDF를 복원할 수 없고, 이미 다른 PDF가 열려 있으면 새 곡의 싱크가 기존 악보에 적용될 수 있다.

   [새 PDF 선택:537](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:537)에서도 이전 앵커를 초기화하지 않는다. 곡 ID, PDF 식별자, 영상 ID, 메타데이터, 앵커, 수정 여부를 한 세션으로 관리하고 곡 전환 시 함께 교체해야 한다.

   [검색 완료 콜백:248](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:248)은 요청이 현재 문서의 것인지 확인하지 않는다. 이전 문서 A, 새 문서 B 순으로 검색하고 B 응답을 먼저 완료한 격리 실행에서 영상 ID가 B → A로 전달되었다. 상태 초기화에 더해 비동기 작업이 시작된 세션 식별자를 응답 시 확인해야 한다. AbortController와 PDF 렌더 취소를 사용하되 이미 완료 대기 중인 콜백에도 현재 세션 확인을 적용한다.

   우선 사용 범위를 기준으로 저장 방식을 정하면 된다. 개인 로컬 MVP라면 파일 재선택과 동일 파일 확인으로 시작할 수 있다. 같은 브라우저에서 파일까지 자동 복원해야 하면 IndexedDB를 검토한다. 여러 장치에서 복원하는 서비스라면 서버/객체 스토리지에 PDF를 저장하고 곡과 연결해야 한다. 서버 업로드에는 파일 형식·크기 검증, 소유권에 따른 읽기 권한, 업로드 실패 및 남은 파일 정리도 포함해야 한다. 공개 URL을 Song에 넣는 것만으로 개인 악보 접근 제어가 완성되지는 않는다.

4. **메타데이터 보정과 실제 수정 저장이 필요하다.**

   PDF에서 추출한 제목/가수는 [index.html:192](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:192)의 검색어 생성에 쓰인다. 실제 저장은 [App.kt:492](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:492)에서 파일명을 제목으로 사용하고, 특정 제목에만 고정 가수를 적용한다. 추출값이 곡 메타데이터로 저장되는 구조가 아니다.

   제목·가수 수정 필드와 YouTube URL/영상 ID 직접 입력을 제공해야 한다. 스캔 PDF, 틀린 자동 검색 결과, 검색 할당량 소진 상황에서도 등록을 끝낼 수 있어야 한다. 자동 검색 실패는 현재 console에만 기록된다.

   [웹 저장 함수:91](C:/Users/user/IdeaProjects/VibeBass/shared/src/wasmJsMain/kotlin/com/woong/vibebass/sync/SyncDataManager.wasmJs.kt:91)는 항상 POST를 사용한다. 기존 곡을 수정한 뒤 저장해도 새 레코드를 만드는 흐름이며, 백엔드의 PUT/DELETE를 사용하는 UI는 없다. 선택된 곡 ID를 유지해 신규 생성과 수정 저장을 구분하고, 저장 중 비활성화·성공/실패 상태·삭제 및 되돌리기를 연결해야 한다.

5. **로컬 실행과 실제 배포의 차이를 없애야 한다.**

   [곡 저장/조회 브리지:91](C:/Users/user/IdeaProjects/VibeBass/shared/src/wasmJsMain/kotlin/com/woong/vibebass/sync/SyncDataManager.wasmJs.kt:91)와 [YouTube 검색:241](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:241)이 http://localhost:8082로 고정되어 있다. 원격 서버에서 페이지를 열어도 요청은 사용자의 컴퓨터를 향한다. 단일 JAR로 제공하는 웹은 같은 출처의 /api 경로를 쓰고, 개발 서버 및 모바일에는 명시적인 API 주소 설정을 제공하는 것이 자연스럽다.

   [backend 빌드:49](C:/Users/user/IdeaProjects/VibeBass/backend/build.gradle.kts:49)의 copyWebDist는 웹 배포 빌드를 실행하고 결과를 src/main/resources/static에 복사한다. processResources가 이 작업에 의존하므로 서버 테스트도 웹 도구 체인에 묶인다. 생성물을 build 아래 리소스 디렉터리에 두고 서버 검증과 웹 포함 배포 태스크를 분리하면 실행 비용과 잔존 산출물 문제를 줄일 수 있다.

   현재 [Wrapper 설정](C:/Users/user/IdeaProjects/VibeBass/gradle/wrapper/gradle-wrapper.properties:1)은 배포본 버전 변경과 함께 distributionSha256Sum이 제거된 상태다. 선택한 정확한 배포본의 공식 checksum으로 복구하는 작업을 권한다. 기존 버전의 checksum을 재사용하면 안 된다. 다운로드나 Wrapper 실행은 이번에 수행하지 않았다. [Gradle 배포본 검증 문서](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification)

   V1 SQL은 있지만 Flyway/Liquibase 의존성이 없고 [application.yml:13](C:/Users/user/IdeaProjects/VibeBass/backend/src/main/resources/application.yml:13)은 ddl-auto:update다. 현재 구성만으로 db/migration의 SQL이 자동 실행되지는 않는다. 기존 DB 상태를 확인한 후 마이그레이션 도구와 기준선을 도입하고, 운영 스키마는 마이그레이션 및 검증으로 관리해야 한다.

   Spring Boot 3.3.4 공식 문서는 Flyway 자동 실행에 해당 모듈이 필요하고 PostgreSQL에는 DB 전용 모듈이 필요하다고 설명한다. 기존 V1을 재사용하되 실제 운영 스키마와 먼저 대조해야 한다. 예를 들어 SQL의 영상 ID 길이는 50인데 엔티티에는 길이가 지정되어 있지 않다. 현재 DB 구조는 접속해서 확인하지 않았다. [프로젝트 버전의 DB 초기화 문서](https://github.com/spring-projects/spring-boot/blob/v3.3.4/spring-boot-project/spring-boot-docs/src/docs/antora/modules/how-to/pages/data-initialization.adoc)

6. **외부 공개 전에는 인증과 소유권 검증을 완료해야 한다.**

   [SongController:12](C:/Users/user/IdeaProjects/VibeBass/backend/src/main/kotlin/com/woong/vibebass/controller/SongController.kt:12)는 인증 없이 곡 CRUD를 노출하고, 저장 모델에는 소유자 필드가 없다. 서버를 공개하면 접근 가능한 호출자가 다른 곡을 조회·변경·삭제할 수 있는 구조다. YouTube 검색 API에도 호출 제한이 없다. 실제 서버 공개 여부는 확인하지 않았다.

   인증 수단을 정한 다음 모든 곡/PDF 작업에 서버 측 소유권 검사를 적용해야 한다. 로그인 화면 추가나 CORS 제한만으로는 이 조건을 충족하지 않는다. 서로 다른 사용자로 조회·수정·삭제·PDF 접근이 차단되는지 검증하고 검색 호출 제한도 추가해야 한다.

   [기존 인증 백로그](C:/Users/user/IdeaProjects/VibeBass/PROJECT_BACKLOG.md:87)의 JWT localStorage 보관 제안은 재검토 대상이다. 현재의 동일 출처 웹/JAR 구성을 유지한다면 서버 세션과 HttpOnly·Secure 쿠키를 우선 비교하고, 상태 변경 요청의 CSRF 방어를 함께 설계하는 편이 단순하다. JavaScript가 읽을 수 있는 저장소에 인증 정보를 두는 것은 PDF 처리의 XSS 위험과도 연결된다. 모바일 토큰 전략은 모바일 인증을 구현할 때 결정한다. [OWASP 저장소 지침](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#local-storage), [Spring Security CSRF 지침](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

   API 계약도 보강해야 한다. [SongRequest:6](C:/Users/user/IdeaProjects/VibeBass/backend/src/main/kotlin/com/woong/vibebass/dto/SongRequest.kt:6)은 기본 공백/null 검증 위주이며 앵커 시간·좌표·개수·중복 정책이 없다. 없는 곡은 일반 예외를 던지고 명시적인 404 매핑이 없다. 입력 오류, 없는 곡, 검색 결과 없음, 외부 서비스 장애를 클라이언트가 구분할 수 있는 오류 코드로 제공해야 한다.

   기존 5초 연결/응답 타임아웃과 일부 5xx 재시도는 유지할 만한 기반이다. 재시도 필터는 IllegalStateException만 대상으로 하므로 모든 네트워크 장애를 재시도한다고 해석하면 안 된다. 결과 없음·할당량/키 오류·타임아웃을 구분하고 전체 요청 시간 상한을 정한다. 목록은 모든 곡의 앵커까지 한 번에 반환하므로 데이터가 커질 때 요약 목록과 상세 조회를 분리하는 것이 다음 개선이며, 현재 성능 장애로 판정하지 않는다.

7. **DOM과 Compose의 화면·상태 경계를 단순화해야 한다.**

   [Compose 레이아웃:200](C:/Users/user/IdeaProjects/VibeBass/shared/src/commonMain/kotlin/com/woong/vibebass/App.kt:200)은 좌측 38%, 플레이어 높이 200dp를 사용한다. [DOM 플레이어:44](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:44)는 폭 calc(40% - 32px), 높이 240px, 고정 top 64px다. 위치 갱신 함수는 비어 있다. 서로 다른 기준과 매우 높은 z-index 때문에 화면 크기에 따라 버튼/텍스트를 가릴 위험이 있다. 실제 겹침 범위는 화면 검증이 필요하다.

   실제 컴포넌트 영역으로 DOM 위치를 맞추거나, 웹에서 레이아웃을 책임지는 계층을 한 곳으로 정해야 한다. 화면 폭에 따라 세로 배치도 제공하고, Space 입력이 PDF 영역·Compose 영역·YouTube iframe 포커스에서 어떻게 동작하는지 확인해야 한다. 문서에 적힌 브라우저 preventDefault 처리는 현재 HTML에서 확인되지 않았다.

   App에서 곡 전환과 저장 상태를 한곳에서 처리하고 PDF 스크롤은 뷰어 쪽으로 옮기는 정도부터 시작하면 된다. 이미 도입된 Compose 상태와 lifecycle 도구를 재사용한다. HTTP 저장과 PDF 스크롤을 같은 SyncDataManager에서 처리하는 책임은 나누되 실제 재사용 요구가 생길 때까지 추가 모듈·DI 프레임워크·단일 구현용 인터페이스를 만들지 않는다. 수작업 JSON 생성은 따옴표만 이스케이프하므로, 웹에서는 기본 JSON.stringify를 활용해 역슬래시·제어 문자까지 처리하는 방안을 우선 검토한다.

   [PDF 렌더링:261](C:/Users/user/IdeaProjects/VibeBass/webApp/src/webMain/resources/index.html:261)은 전 페이지 비동기 요청 완료 순서대로 canvas를 붙인다. 2페이지 응답을 먼저 완료시키면 DOM 삽입 순서가 [2, 1]이 되었고, 새 문서 렌더 후 이전 문서 응답을 완료시키면 [새 문서, 이전 문서]가 되었다. 두 현상은 기존 함수를 사용한 격리 실행으로 재현했다. 페이지별 DOM 위치를 먼저 고정하고 모든 완료 콜백에서 현재 문서 여부를 확인해야 한다.

   문서 전환 시 작업 취소·Object URL 해제와 컴포넌트 종료 시 타이머/콜백 정리도 필요하다. 긴 문서의 메모리·프레임 성능은 측정하지 않았으므로 가시 영역 렌더링 같은 최적화는 실제 사용 문서에서 문제가 확인될 때 추가한다.

8. **모바일 확장은 웹의 핵심 계약이 안정된 뒤 진행하는 편이 효율적이다.**

   [Android 저장소:3](C:/Users/user/IdeaProjects/VibeBass/shared/src/androidMain/kotlin/com/woong/vibebass/sync/SyncDataManager.android.kt:3)와 [iOS 저장소:3](C:/Users/user/IdeaProjects/VibeBass/shared/src/iosMain/kotlin/com/woong/vibebass/sync/SyncDataManager.ios.kt:3)의 저장/조회는 빈 본문이고 공통 선언의 scrollToPdfPixel actual도 빠져 있다. 플레이어와 PDF actual도 뼈대 수준이다. 현재 상태를 모바일 기능 지원 완료로 표현해서는 안 된다.

   먼저 플랫폼별 컴파일 정합성을 확인하고 지원 범위를 문서화해야 한다. 이후 실제 플레이어·파일 선택·PDF 렌더링·공통 저장소를 연결한다. iOS 빌드와 기기 검증은 macOS 환경에서 별도로 수행해야 한다.

현재 구조를 유지할지의 선택은 다음처럼 비교했다. 제품 요구와 팀 규모는 기존 웹 우선 로드맵을 기준으로 했으며, 실제 사용자 조사나 트래픽 측정은 하지 않았다.

| 대안 | 얻는 것 | 비용·한계 | 판단 |
|---|---|---|---|
| 현재 KMP/Compose + Spring 구조에서 흐름별 수정 | 기존 UI·보간·CRUD·테스트 재사용, 작은 단위로 검증 가능 | DOM 브리지의 상태·배치 책임을 명확히 해야 함 | 현재 권고 |
| 웹 UI를 다른 프레임워크로 전면 재작성 | DOM 중심 구현을 통합하기 쉬움 | UI·키보드·저장·동기화를 다시 구현/검증, 기존 모바일 공유 코드 재사용 감소 | 웹 제품 방향이 확정되고 브리지 비용이 반복 측정될 때 재검토 |
| Android/iOS 기능을 먼저 완성 | 네이티브 연습 경험 검증 가능 | 두 플랫폼의 PDF/영상/파일 처리가 추가되고 기존 세션 결함도 함께 해결해야 함 | 웹 저장·싱크 흐름 안정 후 진행 |

실행 단위로는 아래 세 단계가 적절하다. 일정은 작업자 수와 목표 사용 범위가 정해지지 않아 날짜 대신 완료 조건으로 제시한다. 아래는 개선 제안이며, 이번 분석 요청으로 구현까지 승인된 것은 아니다.

| 단계 | 작업 묶음 | 다음 단계로 넘어갈 기준 |
|---|---|---|
| 1. 신뢰할 수 있는 웹 연습 | 키/PDF.js 조치, 싱크 좌표 통일, 비동기 세션 확인, 제목·영상 수동 선택 | 임의 PDF를 골라 영상을 지정하고 앵커를 기록한 뒤 같은 지점으로 재생 가능 |
| 2. 저장하고 다시 쓰는 서비스 | PDF 보존 방식 구현, 수정/삭제, API 주소, DB 마이그레이션, 오류 복구, 공개 시 인증·소유권 | 새로고침·다른 장치에서 곡/악보/싱크 복원, 사용자 간 접근 차단, 실패 원인 표시 |
| 3. 반복 연습과 플랫폼 확장 | 구간 반복·속도 조절·앵커 미세 조정/되돌리기, 화면 대응, 모바일, 마디 하이라이트 | 실제 연습 피드백과 플랫폼별 동일 시나리오로 우선순위·완료 여부 판단 |

다음 작업은 아래 단위로 나누는 것이 좋다. 담당은 향후 실행 시 사용할 역할이며 이번 분석은 plan 워크플로에 따라 인라인으로 수행했다. 실행 순위가 같은 작업은 의존성이 없으면 독립 진행할 수 있다. P0/P1은 위 표의 중요도이고, 아래 순위는 의존성에 따른 순서다.

| ID | 작업 | 담당 역할 | 실행 순위 | 의존성 | 완료 기준 |
|---|---|---|---|---|---|
| T01 | 키 기본값·PDF.js 조치 | backend + frontend | 1 | 없음 | 비밀값 기본값 제거, 해당 취약점 완화/패치, 정상 악보 렌더 확인 |
| T02 | 싱크 좌표·effect 의존성 정리 | frontend | 1 | 없음 | 수동 스크롤 위치 기록, 정지 중 앵커 변경 반영, 같은 문서 지점 재생 |
| T03 | PDF/검색 세션 확인·작업 정리 | frontend | 1 | 없음 | 응답 순서를 뒤집어도 페이지 정렬·현재 문서·현재 영상 유지 |
| T04 | API 주소·JSON·입력/오류 처리 | backend + frontend | 1 | 없음 | 서버 주소에서 API 동작, 특수문자 왕복, 잘못된 입력/없는 곡/검색 장애 구분 |
| T05 | 생성물 경로·DB 마이그레이션·Wrapper 무결성 | backend + db | 1 | 없음 | 기존 DB 기준선 및 변경 절차 확인, 소스 밖 생성물 경로, 해당 버전 checksum 설정 |
| T06 | 세션 전환·수동 보정·기존 곡 수정 저장 | frontend + backend | 2 | T02, T03, T04 | 곡과 악보가 섞이지 않음, 영상 수동 지정, PUT 수정으로 중복 생성 방지 |
| T07 | 인증·소유권·검색 호출 제한 | backend | 2 | T04, T05 | 다른 사용자 곡 조회/수정/삭제 차단, 세션 보호·CSRF·검색 제한 확인 |
| T08 | 선택한 범위의 PDF 영구 보관/복원 | frontend + backend + db | 3 | T05, T06; 공개 서비스는 T07 | 로컬 재선택 또는 파일 자동 복원 범위 명시, 선택한 범위에서 정확한 PDF 복원 |
| T09 | 핵심 흐름 통합 검증·문서 동기화 | qa + docs | 4 | T01~T08의 선택 범위 | 등록→기록→저장→재접속 시나리오와 실패 복구 검증, 구현 범위 문서 일치 |
| T10 | 모바일·연습 편의 확장 | mobile + frontend | 5 | T09 | 플랫폼별 동일 핵심 시나리오 확인 후 구간 반복·속도·하이라이트 확장 |

특히 T07은 선택적으로 늦춰도 되는 공개 기능이 아니다. 개인 로컬 개발에서는 공개하지 않은 상태로 진행할 수 있지만, 다중 사용자 공개 서비스는 T07 완료를 출시 조건으로 둔다. 실제 빌드·컴파일이 필요한 검증은 별도 명시 요청 이후 수행한다.

품질 검증은 테스트 숫자보다 아래 사용자 흐름을 기준으로 보강하는 것이 효과적이다.

| 검증 영역 | 먼저 추가할 검증 |
|---|---|
| 싱크 | DOM 스크롤→앵커 저장→같은 시각 재생, 문서/화면 크기 변경, 중복 시간 정책 |
| 저장/복원 | PDF·영상·앵커를 함께 저장하고 새 세션에서 복원, 수정 시 중복 생성 방지 |
| 서버/DB | 실제 PostgreSQL JSONB 왕복, 수정·소프트 삭제, 빈 DB 초기화 및 마이그레이션 |
| API/보안 | 잘못된 앵커 400, 없는 곡 404, 다른 사용자 데이터 접근 차단 |
| 외부 서비스 | YouTube 결과 없음·할당량 오류·서버 오류·타임아웃 구분과 UI 복구 |
| 화면/자원 | 여러 폭에서 버튼 접근, 다중 페이지 순서, 연속 파일 교체, 로드 실패 종료 |

기존 테스트는 선언 기준 총 12개이며, 그중 3개는 1+2=3 예제다. 실제 기능을 다루는 것은 공통 보간 3개와 백엔드 서비스 6개다. 서버 테스트는 저장소/WebClient를 대체한 단위 테스트여서 HTTP 상태, 실제 DB JSONB·삭제 필터, 브라우저 흐름을 보장하지 않는다. 저장소에서 CI 워크플로를 찾지 못했으므로 서버 테스트·웹 빌드·공통 로직 검증을 자동 실행하는 구성을 추가해야 한다.

이번 검증에서 실제 확인한 결과는 다음과 같다.

| 검증 | 결과 | 증거의 범위 |
|---|---|---|
| 현재 소스·설정·테스트 탐색 | 완료 | Serena 파일/패턴 검색과 필요한 범위의 파일 읽기 |
| Kotlin 심벌·참조/LSP 진단 | 사용 불가 | Kotlin 언어 서버 초기화 실패. 해당 호출은 재시도하지 않음 |
| PDF 페이지 순서 | 격리 실행 재현 | 2페이지 먼저 완료 → 실제 삽입 [2, 1] |
| PDF 문서 전환 | 격리 실행 재현 | 새 문서 뒤 이전 응답 완료 → [새 문서, 이전 문서] 혼입 |
| YouTube 검색 순서 | 격리 실행 재현 | 최신 검색 먼저 완료 → 영상 ID [최신, 이전] 전달 |
| 플레이헤드 좌표 변환 | 격리 실행 관찰 | 입력 500 → scrollTop 250. 올바름은 저장 좌표 의미와 함께 판단 |
| Kotlin 단위 테스트·웹/모바일 빌드·DB 통합·브라우저 E2E | 미실행 | 현재 요청과 빌드 금지 지침에 따른 제한. 통과로 주장하지 않음 |

Node 검증은 기존 HTML의 함수를 읽어 실행하며 테스트용 fetch로 외부 호출을 대체했다. 실행 명령의 성공은 위 관찰을 재현했다는 뜻이며 제품이 정상이라는 뜻은 아니다. 정상 페이지 순서 [1, 2]와 이전 응답 무시를 기대하는 회귀 검증은 수정 작업 시 추가해야 한다. 2026-09-10의 Gradle 배포본 다운로드 PKIX 오류는 이전 관찰이며 이번에 재시도하지 않았으므로 현재 환경의 상태로 단정하지 않는다.

기존 문서도 구현과 함께 갱신해야 한다. PROJECT_STATUS의 프런트 CRUD는 실제로 저장/조회 중심이고 수정/삭제 UI가 없으며, 자동 추출 메타데이터는 저장값에 연결되지 않는다. 웹 산출물 복사 위치도 문서의 build/resources 설명과 실제 src/main/resources 설정이 다르다. PROJECT_BACKLOG의 고정 날짜 로드맵은 현재 완료 여부를 나타내지 못하므로 위 완료 기준 중심으로 재정리하는 것이 좋다.

현재 `.agents/`의 실행 정책·규칙과 로컬 설정은 읽을 수 있다. 로컬 설정의 한국어·auto 모델·Serena 선택을 확인했다. 기본 CUE/YAML과 로컬 설정에 서로 다른 provider/preset 값이 있어 개발 환경 설명에는 유효한 설정과 적용 순서를 명시하는 것이 좋다. 설정 파일 자체는 수정하지 않았다.

이번 결과물은 갱신된 분석 문서와 OMA의 로컬 계획·검증 기록이다. 애플리케이션 소스는 변경하지 않았다. 향후 구현 우선 목표는 T02·T03·T06을 통해 **한 곡의 악보·영상·싱크가 끝까지 같은 세션을 가리키게 만드는 것**이다.
