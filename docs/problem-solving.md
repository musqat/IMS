# 문제 해결

운영하며 겪은 장애와 버그, 점검에서 드러난 미비다. 각 항목은 **문제 → 추적 → 원인 → 해결** 순이다.
`추적`은 원인을 어떻게 좁혔는지다. 증상만으로 갈리지 않던 것들이 여기 모인다.
원인이 남아 있는 항목은 `해결` 대신 `대응`으로 적는다.

로그와 응답은 전부 실제로 재현해 받은 값이다. 가정이 틀렸던 이야기는
[시행착오](trial-and-error.md) 에 따로 있다.

- [삭제하려는데 "이미 존재한다"고 나왔다](#삭제하려는데-이미-존재한다고-나왔다) — FK 위반 매핑
- [비밀번호를 1초에 12번 틀려도 통과했다](#비밀번호를-1초에-12번-틀려도-통과했다) — 로그인 시도 제한
- [기동이 not-null 위반으로 죽었다](#기동이-not-null-위반으로-죽었다) — 빌더 기본값
- [대시보드 KPI가 항상 0이었다](#대시보드-kpi가-항상-0이었다) — record 파생 메서드
- [409가 열 가지 원인을 가렸다](#409가-열-가지-원인을-가렸다) — 응답에 코드가 없었다
- [CI 첫 실행이 Permission denied](#ci-첫-실행이-permission-denied) — 실행 비트
- [배포가 14분 만에 타임아웃됐다](#배포가-14분-만에-타임아웃됐다) — 원인은 남아 있다

<br>

## 삭제하려는데 "이미 존재한다"고 나왔다

<details>
<summary><b>중복 키와 FK 위반이 같은 예외로 올라온다.</b> SQLState로 갈랐다</summary>

<br>

`문제`

재고가 있는 창고를 지우면 이 응답이 나왔다. 삭제 요청에 "이미 존재한다"는 답이라
사용자가 무엇을 해야 할지 알 수 없다.

```json
{"code":"DUPLICATE_RESOURCE","message":"이미 존재하는 리소스입니다.","data":null}
```

`추적`

응답만 보면 중복 등록 오류로 읽힌다. 서버 로그를 열어 보니 다른 예외였다.

```
SQL Error: 0, SQLState: 23503
ERROR: update or delete on table "warehouses" violates foreign key constraint
       "fkoipfe4s81wodvutx9i0rlmoyi" on table "inventories"
  Detail: Key (id)=(1) is still referenced from table "inventories".
```

SQLState `23503`은 외래 키 위반이다. 중복 키(`23505`)와 다른 코드인데 응답에서는 같아졌다.

`원인`

`WarehouseService.deleteWarehouse`가 참조를 확인하지 않고 삭제를 시도했다. DB가 FK 위반을
던지고, `GlobalExceptionHandler`가 이를 `DataIntegrityViolationException`으로 받아
`DUPLICATE_RESOURCE`로 매핑했다.

`DataIntegrityViolationException`은 중복 키와 FK 위반을 **둘 다** 포함한다.
Spring이 JDBC 예외를 이 하나로 묶기 때문에 매핑을 하나만 두면 원인이 뭉개진다.

`해결`

처음에는 창고 삭제에만 가드를 넣었다. **그게 부족했다.**

가드는 엔드포인트마다 붙는데 매핑은 전역이다. 창고를 고쳐도 같은 예외를 내는 다른 경로는
그대로다. 품목 삭제로 확인해 봤다.

```
POST /items                      201   (BOM 없음)
POST /warehouses/1/inventories   201   (재고만 등록)
DELETE /items/17                 409
{"code":"DUPLICATE_RESOURCE","message":"이미 존재하는 리소스입니다."}
```

같은 결함이 그대로 있었다. `ItemService.deleteItem`이 BOM 참조만 막고 재고·생산 기록은
안 막았다.

그래서 두 층으로 나눠 고쳤다.

**전역 매핑** — SQLState로 갈라 원인을 남긴다.

```java
ErrorCode errorCode = switch (sqlState(e)) {
    case "23503" -> ErrorCode.REFERENCED_RESOURCE;   // foreign_key_violation
    case "23505" -> ErrorCode.DUPLICATE_RESOURCE;    // unique_violation
    default -> ErrorCode.DUPLICATE_RESOURCE;
};
```

가드를 빠뜨린 경로가 생겨도 최소한 "참조 중"이라는 사실은 전달된다.

**엔드포인트 가드** — 삭제 전에 참조를 직접 확인하고 무엇이 막는지 알린다.

```java
if (inventoryRepository.existsByItemId(itemId)) {
    throw new ImsException(ErrorCode.ITEM_HAS_INVENTORY);
}
if (productionRepository.existsByItemId(itemId)) {
    throw new ImsException(ErrorCode.ITEM_HAS_PRODUCTION);
}
```

고친 뒤 같은 요청을 다시 쳤다.

```json
DELETE /items/17
{"code":"ITEM_HAS_INVENTORY","message":"재고가 남아 있어 품목을 삭제할 수 없습니다."}
```

이 결함은 테스트 주석에 이미 적혀 있었다.

```java
// 주의: FK 위반도 이 핸들러로 들어와 "이미 존재하는 리소스입니다"로 나간다.
// 참조 중인 창고/품목 삭제 시 사용자에게 부정확한 메시지가 전달된다.
```

적어두고 고치지 않았다. 지금은 SQLState 분기 테스트 세 개가 그 자리를 대신한다.

창고 쪽은 고치고 나니 기능이 닫혀 있다는 게 드러났다. 재고가 있는 창고는 사실상 영영 못
지운다. 재고와 생산 기록은 분석의 원본이라 함께 지울 수도 없다. `active` 플래그로
비활성화하는 경로를 따로 만들었다.

</details>


<br>

## 비밀번호를 1초에 12번 틀려도 통과했다

<details>
<summary><b>인증 실패가 상태를 남기지 않았다.</b> Redis 카운터로 계정별 잠금을 걸었다</summary>

<br>

`문제`

격리 스택에 로그인 요청을 반복해서 보냈다. 틀린 비밀번호가 아무 저항 없이 통과한다.

```
틀린 비밀번호 12회   [401 401 401 401 401 401 401 401 401 401 401 401]
소요                 1.0초
직후 정답 로그인      200
```

지연도 잠금도 CAPTCHA도 없다. 비밀번호 정책이 8자 + 영문·숫자라 사전 공격 대상이 넓다.

`추적`

로그인 경로에서 실패를 세는 곳을 찾았다. 없었다.

```java
if (!user.matchesPassword(request.password(), passwordEncoder)) {
    throw new ImsException(ErrorCode.LOGIN_FAILED);
}
```

Redis는 이미 붙어 있는데 리프레시 토큰 저장에만 쓰고 있었다.

`원인`

인증 실패가 401을 돌려줄 뿐 아무 상태도 남기지 않는다. 다음 요청은 이전 실패를 모른다.

`해결`

계정별 실패 횟수를 Redis에 센다. 5회를 넘기면 10분 잠근다.

```java
public void recordFailure(String email) {
    String key = KEY_PREFIX + email;
    redisTemplate.opsForValue().increment(key);
    redisTemplate.expire(key, LOCK_DURATION);
}
```

**잠금을 TTL로 푼다.** 해제 시각을 따로 저장하고 배치로 검사하는 대신 키가 스스로 사라지게 했다. 실패할 때마다 TTL을 다시 걸어서 잠긴 뒤에도 계속 두드리면 잠금이 연장된다.

**계정이 없어도 센다.** 존재하는 계정만 잠그면 잠기는지 여부가 곧 계정 존재 여부가 된다. 사용자 열거를 막으려고 응답 문구를 통일해 뒀는데 잠금이 그 구분을 되살린다.

```java
User user = userRepository.findByEmail(request.email()).orElse(null);
if (user == null || !user.matchesPassword(request.password(), passwordEncoder)) {
    loginAttemptService.recordFailure(request.email());
    throw new ImsException(ErrorCode.LOGIN_FAILED);
}
```

같은 요청을 다시 보냈다.

```
틀린 비밀번호 12회   [401 401 401 401 401 429 429 429 429 429 429 429]
직후 정답 로그인      429   {"code":"LOGIN_ATTEMPTS_EXCEEDED", ...}
다른 계정 정답       200
```

정답을 알아도 잠긴 동안은 못 들어간다. 다른 계정은 영향이 없다.

</details>

<br>

## 기동이 not-null 위반으로 죽었다

<details>
<summary><b><code>@Builder.Default</code>는 자바 빌더에서만 걸린다.</b> seed.sql은 그 경로를 안 탄다</summary>

<br>

`문제`

`Warehouse.active`를 추가한 뒤 애플리케이션이 시드 적재 단계에서 종료됐다.

```
[DataInitializer] Applying seed data...
Application run failed
ScriptStatementFailedException: Failed to execute SQL script statement #8
    of class path resource [seed.sql]
Caused by: PSQLException: ERROR: null value in column "active"
    of relation "warehouses" violates not-null constraint
  Detail: Failing row contains (1, ..., null, 서울특별시 금천구 디지털로, 서울 조립창고, 1).
```

`추적`

자바 코드로 창고를 만드는 경로는 정상이었다. 실패한 건 `seed.sql`의 INSERT 하나다.
두 경로의 차이를 보니 기본값이 걸리는 자리가 달랐다.

```
warehouseRepository.save(Warehouse.builder()...)   빌더를 탄다   → active = true
INSERT INTO warehouses (owner_id, name, ...)       빌더를 안 탄다 → active = null
```

`Detail` 줄의 `null`이 `active` 자리다. 컬럼 목록에 `active`가 없으니 DB 기본값을 찾는데
그것도 없었다.

`원인`

기본값을 `@Builder.Default`로만 걸었다. 이건 Lombok이 만드는 자바 빌더에서만 적용된다.
`seed.sql`은 JDBC로 직접 INSERT라 그 경로를 타지 않는다.

`해결`

DB 기본값을 함께 지정했다.

```java
@Builder.Default
@Column(nullable = false, columnDefinition = "boolean default true")
private boolean active = true;
```

`columnDefinition`을 떼고 빈 DB에 기동하면 위 예외가 그대로 재현된다.

같은 시드에서 한글이 깨진 적도 있다. `ScriptUtils.executeSqlScript`가 파일을 플랫폼 기본
문자셋으로 읽는데 한국어 윈도우는 MS949다.

```java
new EncodedResource(new ClassPathResource("seed.sql"), StandardCharsets.UTF_8)
```

리눅스는 기본이 UTF-8이라 배포 환경에서는 나타나지 않는다. 로컬에서만 나는 문제였고,
그래서 한 번은 콘솔 인코딩 탓으로 넘겼다가 다시 잡았다.

</details>

<br>

## 대시보드 KPI가 항상 0이었다

<details>
<summary><b>Jackson은 record의 컴포넌트만 직렬화한다.</b> 파생 메서드는 JSON에 안 실린다</summary>

<br>

`문제`

DB에 생산 기록이 75건인데 대시보드의 전체 건수가 0으로 나왔다. 같은 화면의
확인 필요(20)와 대기(5)는 정상이었다.

`추적`

API 응답을 그대로 받아 보니 필드 하나가 없었다.

```json
{"settled":45,"pending":5,"cancelled":20}
```

프론트가 `total`을 읽는데 응답에 그 키가 없다. `undefined`가 화면에서 0으로 렌더된다.

서버에서 `total`은 이렇게 만들고 있었다.

```java
record Counts(int settled, int pending, int cancelled) {
    public int total() { return settled + pending + cancelled; }
}
```

`원인`

Jackson은 record의 **컴포넌트**를 직렬화한다. 생성자 파라미터로 선언된 것만 필드가 된다.
파생 메서드는 아무리 public이어도 JSON에 실리지 않는다.

정상이던 `pending`·`anomaly`가 컴포넌트라 문제가 국소적으로 보였고, 그래서 늦게 찾았다.
프론트 타입 정의에는 "Jackson에 의해 직렬화됨"이라는 틀린 주석까지 달려 있었다.

`해결`

`total`을 컴포넌트로 옮겼다. 파생값을 응답에 실으려면 `@JsonProperty`를 붙이는 방법도
있는데, 계산을 서버에 두면 프론트가 합계를 다시 세지 않아도 된다.

</details>

<br>

## 409가 열 가지 원인을 가렸다

<details>
<summary><b>상태 코드로는 원인을 못 가른다.</b> 응답에 code를 넣었다</summary>

<br>

`문제`

프론트가 409를 받아도 무엇 때문인지 알 수 없었다. 응답에 HTTP 상태와 메시지만 있고
식별자가 없다. 409를 쓰는 `ErrorCode`가 열 개가 넘는다.

`추적`

같은 409를 내는 요청 셋을 나란히 쳐 봤다.

```
창고 삭제 (재고 잔존)     409   재고가 남아 있어 창고를 삭제할 수 없습니다.
품목 등록 (코드 중복)     409   이미 사용 중인 품목 코드입니다.
회원가입 (이메일 중복)    409   이미 사용 중인 이메일입니다.
```

메시지는 다른데 프론트가 문자열을 비교할 수는 없다. 문구가 바뀌면 분기가 깨진다.

`원인`

`ApiResponse.fail(ErrorCode)`가 `ErrorCode`에서 메시지만 꺼내고 이름을 버렸다.
서버는 원인을 알고 있는데 응답에 담지 않았다.

`해결`

응답에 `code`를 추가했다.

```java
public record ApiResponse<T>(String code, String message, T data)
```

같은 요청 셋의 응답이 이렇게 바뀐다.

```json
{"code":"WAREHOUSE_HAS_INVENTORY", ...}
{"code":"DUPLICATE_ITEM_CODE", ...}
{"code":"DUPLICATE_EMAIL", ...}
```

`SecurityConfig`의 필터 단계 401도 문자열 조립 대신 같은 직렬화를 타게 바꿨다.
소비처로 창고 삭제가 `WAREHOUSE_HAS_INVENTORY`면 비활성화를 제안하는 화면을 붙였다.

</details>

<br>

## CI 첫 실행이 Permission denied

<details>
<summary><b>Windows에는 실행 비트 개념이 없다.</b> 로컬 테스트로는 안 걸린다</summary>

<br>

`문제`

CI를 처음 붙였더니 백엔드 잡의 첫 스텝이 죽었다.

```
./gradlew: Permission denied
Error: Process completed with exit code 126
```

`추적`

러너에는 파일이 있는데 실행이 안 됐다. git이 저장한 모드를 봤다.

```
git ls-files -s backend/gradlew
100644 ...    ← 실행 비트 없음
```

`원인`

git은 파일 모드 중 실행 비트만 저장한다. Windows 파일 시스템에는 그 개념이 없어
저장소에 `100644`로 들어갔다. 리눅스 러너가 체크아웃하면 실행 권한이 없는 파일이 된다.

`해결`

```bash
git update-index --chmod=+x backend/gradlew
```

같은 실행에서 `npm audit`도 처음 돌려 봤다. 19건이 나왔고 메이저 업그레이드 없이 고쳐지는
12건을 적용해 7건이 남았다. 남은 것은 Next 14 → 16이 필요해 CI에는 넣지 않았다.

</details>

<br>

## 배포가 14분 만에 타임아웃됐다

<details>
<summary><b>멈춘 게 아니라 느린 것이었다.</b> 무엇이 늦췄는지는 못 밝혔다</summary>

<br>

`문제`

Render 배포가 실패해 **데모 링크가 통째로 닫혔다.** 프론트는 Vercel에 있어 화면은 뜨는데
모든 API가 실패한다.

애플리케이션 로그는 여기서 끊겼다.

```
Root WebApplicationContext: initialization completed
```

그 뒤로 아무것도 없고 Render가 이 줄을 남기고 끝냈다.

```
Port scan timeout reached, no open ports detected
```

`추적`

끊긴 자리만 보면 **기동이 멈춘 것**과 **아직 기동 중인 것**이 구분되지 않는다.
둘 다 마지막 줄이 같다.

애플리케이션 로그로는 못 가르니 DB 쪽을 봤다.

```
Render     11:50 UTC   포트 스캔 포기
Supabase   11:51 UTC   DDL 실행 중       ← 죽지 않았다
```

Render가 포기한 1분 뒤에도 애플리케이션이 Supabase에 DDL을 날리고 있었다.
멈춘 게 아니라 느린 것이었다.

`원인`

Render는 배포 후 일정 시간 안에 포트가 열리는지를 본다. 그 안에 리슨이 안 잡히면
배포를 실패로 처리한다.

Spring Boot는 `ApplicationContext` 초기화가 끝나야 톰캣이 포트를 연다.
**여기까지가 확인된 사실이고, 무엇이 초기화를 늦췄는지는 못 밝혔다.**

`ddl-auto`가 매 기동마다 스키마를 비교하는 것을 짐작했다. 로컬에서 세 모드를 재봤다.

```
update    11.1  12.4  11.7        평균 11.7초
none      12.1  11.2  20.8        평균 14.7초
validate  22.9  18.8  12.8        평균 18.2초
```

같은 설정 안의 편차가 설정 간 차이보다 크다. 지연 초기화도 같았다.
로컬은 CPU가 빠르고 DB가 같은 머신에 있어 **무료 티어와 Supabase 왕복이라는 조건 자체를
재현하지 못한다.** 배포 환경에서 재야 갈린다.

`대응`

같은 커밋을 재배포해서 넘어갔다.

```
1차   14분 경과, 포트 스캔 타임아웃
2차   157초에 기동 완료
```

코드도 설정도 바꾸지 않았다. 재시도로 통과한 것이지 원인은 그대로다.
기동 시간이 한도 근처라 배포마다 갈린다.

원인을 못 좁혔으니 고치는 방법도 정하지 못했다. `ddl-auto`를 걷어내는 안은 배포된 스키마에
맞는 baseline을 정확히 떠야 하고, 근거 없이 손대면 배포 위험만 는다.

</details>
