# PostgreSQL 마이그레이션

2026-09-15: Flyway 설정과 V2를 추가했다. DBHub는 `SOURCE_UNREACHABLE`, Docker는 엔진 미실행 상태여서 **실제 DB 조회·기준선 등록·마이그레이션은 수행하지 않았다.** 아래 절차는 DB가 준비된 뒤 적용한다.

## 스키마 관리

Spring Boot 3.3.4 BOM이 관리하는 Flyway 10.10.0의 `flyway-core`와 `flyway-database-postgresql`을 사용한다. Flyway가 변경 이력을 적용한 후 Hibernate의 `ddl-auto: validate`가 매핑을 검사한다. 기존 DB를 임의로 승인하지 않도록 `baseline-on-migrate: false`, 삭제 방지를 위해 `clean-disabled: true`를 명시했다. [Spring 초기화 문서](https://github.com/spring-projects/spring-boot/blob/v3.3.4/spring-boot-project/spring-boot-docs/src/docs/antora/modules/how-to/pages/data-initialization.adoc)

| 상태 | 실행 방식 |
|---|---|
| 빈 스키마 | 서버 시작 시 V1 → V2 적용 후 Hibernate 검증 |
| Flyway 이력이 있는 DB | 적용된 파일의 체크섬 검증 후 미적용 버전 실행 |
| 테이블은 있지만 Flyway 이력이 없는 DB | 시작 실패. 아래 대조·백업·기준선 등록을 먼저 수행 |

`V1__init_schema.sql`은 수정하지 않는다. V2는 영상 ID 저장 공간을 50자에서 엔티티의 기존 기본 길이인 255자로 확장하고, 기존 Hibernate 생성 테이블에 없을 수 있는 JSON·생성/수정 시각 기본값을 맞춘다. 기존 행·ID·JSON 좌표·삭제 시각은 변경하지 않는다. 신규 API 입력의 영상 ID 11자리 제한은 그대로다.

V2는 PostgreSQL 트랜잭션 안에서 실행하며 잠금 대기는 5초, 문장 실행은 30초로 제한한다. `ALTER TABLE`의 잠금이 필요하므로 트래픽이 적은 시간에 적용한다. 시간 초과나 실패 시 원인을 확인한 뒤 재시도하며, 이미 성공한 SQL을 편집하거나 이력의 체크섬을 무조건 복구하지 않는다. [PostgreSQL ALTER TABLE](https://www.postgresql.org/docs/current/sql-altertable.html)

## 기존 DB를 처음 연결할 때

1. 서버 쓰기를 중지하고 **접속 대상·스키마를 확인**한다. DB 비밀번호는 기존 `SPRING_DATASOURCE_PASSWORD` 또는 비밀 저장소에서 주입한다. 저장소 파일에 복사하지 않는다.
2. `pg_dump`의 custom 형식으로 DB 전체를 백업하고 별도 DB에 복원 가능한지 확인한다. 덤프는 저장소 밖에 보관한다.
3. 다음 읽기 전용 SQL과 V1·V2를 대조한다. 예시는 `public` 스키마 기준이다. 실제 스키마가 다르면 자동 기준선을 켜지 말고 매핑부터 맞춘다.

```sql
BEGIN READ ONLY;
SELECT current_database(), current_user, current_schema(), version();
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' ORDER BY table_name;
SELECT column_name, data_type, character_maximum_length, is_nullable,
       column_default, is_identity, identity_generation
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'songs'
ORDER BY ordinal_position;
SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'songs';
SELECT count(*) AS rows, max(id) AS max_id,
       max(length(youtube_video_id)) AS longest_video_id,
       count(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted_rows,
       count(*) FILTER (WHERE jsonb_typeof(anchor_points) <> 'array') AS non_array_anchors
FROM public.songs;
COMMIT;
```

4. 아래 8개 컬럼, NOT NULL 및 ID 기본키/자동 증가가 맞아야 한다. 영상 ID 길이 50 또는 255, SQL 기본값의 부재는 V2가 처리한다. 그 밖의 테이블·컬럼·제약 차이, 자동 증가 값과 최대 ID의 불일치가 있으면 먼저 원인을 확인하고 별도 변경으로 해결한다. Hibernate 검증만으로 길이·기본값·모든 제약의 동등성을 보장할 수는 없다.
5. Flyway 이력이 없고 위 조건이 맞는 **기존 DB에만 버전 1 기준선**을 등록한다. 기준선은 V1을 실행하지 않고 이미 적용된 것으로 기록한다. 빈 DB에는 기준선을 등록하지 않는다. [Flyway baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline), [자동 기준선의 주의점](https://documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html)

Flyway CLI 10.10.0이 설치된 환경에서 저장소 루트를 기준으로 실행한다. 이 프로젝트는 CLI 자체를 설치하지 않는다. `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`는 확인한 접속 정보로 해당 셸에만 설정한다. 비밀번호를 명령 인자로 넘기지 않는다.

```powershell
# 백업·스키마 대조를 마친 기존 DB에서만 한 번 실행한다.
flyway -defaultSchema=public -schemas=public -baselineVersion=1 baseline
if ($LASTEXITCODE -ne 0) { throw 'Flyway baseline failed' }
flyway -defaultSchema=public -schemas=public -locations=filesystem:backend/src/main/resources/db/migration migrate
if ($LASTEXITCODE -ne 0) { throw 'Flyway migration failed' }
flyway -defaultSchema=public -schemas=public -locations=filesystem:backend/src/main/resources/db/migration validate
if ($LASTEXITCODE -ne 0) { throw 'Flyway validation failed' }
flyway -defaultSchema=public -schemas=public info
```

6. 이력의 V2 성공, 행 수·ID·앵커·삭제 시각 보존을 확인하고 서버를 시작한다. 기존 곡 조회·수정·소프트 삭제와 새 곡 저장을 확인한다. 이후 변경은 V3부터 새 파일로 추가한다.

## 데이터 기준

외부에서는 곡 목록·단건 조회와 싱크 저장 API가 `SongResponse`를 사용한다. 개념적으로 곡 하나가 영상 ID와 순서 있는 앵커 배열을 소유하며, 물리적으로 `songs` 한 행과 JSONB에 저장된다. PDF 원본·사용자 소유권은 아직 이 스키마에 없다.

| 컬럼 / 용어 | 타입·의미·제약 |
|---|---|
| `id` / 곡 ID | bigint 기본키, sequence 또는 identity 자동 증가 |
| `title` / 제목 | varchar(255), NOT NULL; API에서 공백·NUL 거부 |
| `artist` / 가수 | nullable varchar(255) |
| `youtube_video_id` / 영상 ID | V2부터 varchar(255), NOT NULL; 신규 API 입력은 11자리 |
| `anchor_points` / 싱크 앵커 | JSONB NOT NULL, 기본 `[]`; `timeSec`, `scrollPixel`, 선택적 `pagePosition` |
| `created_at` / 생성 시각 | timestamp NOT NULL, 기본 NOW() |
| `updated_at` / 수정 시각 | timestamp NOT NULL, 기본 NOW(); 엔티티 수정 시 갱신 |
| `deleted_at` / 삭제 시각 | nullable timestamp; 값이 있으면 일반 조회에서 제외 |

앵커는 곡과 함께 읽고 저장하는 기존 집계 구조를 유지한다. CRUD 쓰기는 기존 서비스의 트랜잭션 단위로 원자적으로 처리한다. PostgreSQL 기본 `READ COMMITTED`를 가정하며 낙관적 잠금은 아직 없어 동시 수정은 마지막 저장이 반영될 수 있다. 이번 단계에서 인덱스·잠금 전략을 추가하지 않는다.

운영 용량은 연결 후 `pg_total_relation_size('public.songs')`와 실제 곡·앵커 수로 산정한다. 현재 사용량은 미확인이다. 대략적인 데이터 예산은 곡 수 × (평균 메타데이터 + 평균 앵커 수 × 평균 앵커 JSON 크기)에 인덱스·여유 공간·백업을 더한다. 앵커 최대치는 API 기준 10,000개다. 서버 배포 시 RPO/RTO와 보존 기간을 정하고, 초기에는 변경 전 전체 백업과 일별 백업을 기본으로 삼는다. 하루 미만 복구 지점이 필요하면 WAL 보관/PITR를 추가한다.

V2는 데이터를 지우지 않는 전진 마이그레이션이다. 실패한 트랜잭션은 롤백된다. 성공 후 되돌림은 수정한 새 마이그레이션 또는 검증된 백업 복원으로 처리한다. 255자 컬럼을 50자로 무조건 축소하는 down SQL은 제공하지 않는다.

## 검증

폐기 가능한 PostgreSQL DB의 접속 정보를 `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`와 안전한 비밀번호 주입 방식으로 지정한 뒤 실행한다.

```powershell
psql -X -v ON_ERROR_STOP=1 -f backend/src/test/sql/migration-check.sql
```

테스트는 별도 스키마에서 V1 → V2, 기존 255자·identity·기본값 없는 형태, JSON·소프트 삭제·긴 기존 영상 ID 보존 및 SQL 기본값을 검사한다. 모든 변경은 마지막에 롤백하며 오류 시 연결 종료로 롤백된다. **SQL 검사이므로 Flyway 이력 처리나 Spring/Hibernate 통합 검사를 대신하지 않는다.** 이력 없는 기존 DB의 시작 거부, 명시적 baseline 이후 V2 실행, 재시작 시 중복 실행 방지는 실제 서버 통합 검사에서 별도로 확인해야 한다.
