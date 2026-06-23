# PointPay Guard Backend

Redis 기반 멱등성 키와 락을 사용해 포인트 결제 승인, 취소, 정산 과정의 중복 요청과 상태 전이 문제를 다루는 Spring Boot 백엔드입니다.

## 핵심 구현

- 결제 승인 트랜잭션: 결제 상태 변경, 포인트 잔액 차감, 주문 상태 변경, 이벤트 이력 저장
- 상태 전이 제한: `READY -> APPROVING -> APPROVED/FAILED`, `APPROVED -> CANCELING/SETTLED`
- Redis 멱등성 키: 같은 `idempotencyKey`로 들어온 결제 승인 중복 요청 방지
- Redis 락: 동일 주문 승인, 동일 결제 취소, 정산 배치 동시 실행 방지
- 결제 이벤트 이력: 상태 변경 전후 상태와 사유 저장
- 전역 예외 처리: 도메인 예외와 검증 오류를 일관된 JSON 응답으로 변환

## 구현 의도

이 프로젝트는 단순 CRUD보다 결제 도메인에서 자주 발생하는 실무 문제를 작게 재현하는 데 초점을 둡니다.

핵심 목표는 다음과 같습니다.

- 같은 결제 버튼을 여러 번 눌러도 중복 결제가 발생하지 않게 한다.
- 승인, 취소, 정산이 동시에 실행되어도 결제 상태와 포인트 잔액이 꼬이지 않게 한다.
- 결제 상태는 정해진 흐름으로만 이동하게 한다.
- 실패한 결제도 상태와 이벤트 이력을 남겨 원인을 추적할 수 있게 한다.
- Redis는 빠른 중복 요청 차단과 락에 사용하고, 최종 정합성은 DB 트랜잭션과 제약 조건으로 보장한다.

## 처리 흐름

### 결제 승인

```text
1. Redis idempotency key 선점
2. 동일 orderId 기준 Redis lock 획득
3. DB transaction 시작
4. 주문 조회 및 주문 상태 검증
5. 동일 주문의 활성 결제 존재 여부 확인
6. 지갑 조회 및 포인트 잔액 확인
7. 결제 생성: READY
8. 결제 승인 처리 시작: APPROVING
9. 포인트 차감
10. 결제 승인 완료: APPROVED
11. 주문 상태 변경: PAID
12. 결제 이벤트 이력 저장
13. DB transaction commit
14. Redis idempotency key 완료 처리
15. Redis lock 해제
```

잔액이 부족하면 결제를 `FAILED` 상태로 변경하고, 주문은 `PAYMENT_FAILED` 상태로 남깁니다. 실패도 이벤트 이력에 저장되기 때문에 왜 실패했는지 조회할 수 있습니다.

### 결제 취소

```text
1. 동일 paymentId 기준 Redis lock 획득
2. DB transaction 시작
3. 결제와 지갑을 쓰기 락으로 조회
4. APPROVED -> CANCELING 상태 전이
5. 포인트 환불
6. 주문 상태 변경: CANCELED
7. CANCELING -> CANCELED 상태 전이
8. 결제 이벤트 이력 저장
9. DB transaction commit
10. Redis lock 해제
```

이미 정산된 결제는 `SETTLED -> CANCELED` 전이가 허용되지 않으므로 취소할 수 없습니다.

### 정산 처리

```text
1. 정산 배치 Redis lock 획득
2. APPROVED 상태 결제 조회
3. APPROVED -> SETTLED 상태 전이
4. 주문 상태 변경: SETTLED
5. 결제 이벤트 이력 저장
```

## 상태 전이

결제 상태는 아래 흐름만 허용합니다.

```text
READY -> APPROVING -> APPROVED -> SETTLED
READY -> APPROVING -> FAILED
APPROVED -> CANCELING -> CANCELED
```

허용하지 않는 예시는 다음과 같습니다.

```text
FAILED -> APPROVED
CANCELED -> APPROVED
SETTLED -> CANCELED
```

상태 전이는 `Payment.transitionTo()`와 `PaymentStatus.canTransitionTo()`에서 검증합니다.

## Redis와 DB 역할

Redis는 두 가지 문제를 해결합니다.

- 멱등성 키: 동일한 `idempotencyKey`를 가진 결제 승인 요청을 중복 처리하지 않음
- 락: 동일 주문 승인, 동일 결제 취소, 정산 배치가 동시에 실행되지 않도록 제어

다만 Redis만으로 결제 정합성을 보장하지 않습니다. Redis TTL 만료, 네트워크 실패, 재시도 상황을 고려해 DB에서도 한 번 더 방어합니다.

- `payments.idempotency_key` unique constraint로 중복 결제 생성 방지
- `@Transactional`로 결제 상태, 주문 상태, 지갑 잔액, 이벤트 이력 변경을 하나의 작업으로 처리
- JPA pessimistic lock으로 같은 주문, 결제, 지갑에 대한 동시 변경 방지

## 예외 처리

도메인 예외는 `BusinessException` 하위 타입으로 관리하고, `GlobalExceptionHandler`에서 일관된 JSON 응답으로 변환합니다.

주요 예외 상황은 다음과 같습니다.

- 존재하지 않는 주문, 결제, 사용자, 지갑
- 잔액 부족
- 이미 처리 중이거나 완료된 주문 결제
- 허용되지 않는 결제 상태 전이
- 동일 멱등성 키 요청이 아직 처리 중인 경우
- Redis lock 획득 실패

응답 예시는 다음과 같습니다.

```json
{
  "code": "INVALID_PAYMENT_STATE",
  "message": "결제 상태를 SETTLED에서 CANCELING(으)로 변경할 수 없습니다.",
  "path": "/api/payments/1/cancel",
  "timestamp": "2026-06-17T13:00:00Z",
  "fieldErrors": []
}
```

## 실행

```bash
cd backend
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

기본 프로필은 H2를 사용하고, `local` 프로필은 `docker-compose.yml`의 PostgreSQL과 Redis를 사용합니다.

앱 시작 시 데모 사용자와 지갑이 자동 생성됩니다.

```text
userId: 1
initial balance: 100000
```

## 주요 API

### 주문 생성

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"amount":10000}'
```

### 결제 승인

```bash
curl -X POST http://localhost:8080/api/payments/approve \
  -H 'Content-Type: application/json' \
  -d '{"orderId":1,"idempotencyKey":"order-1-first-try"}'
```

### 결제 취소

```bash
curl -X POST http://localhost:8080/api/payments/1/cancel \
  -H 'Content-Type: application/json' \
  -d '{"reason":"사용자 요청"}'
```

### 정산 처리

```bash
curl -X POST http://localhost:8080/api/settlements/run
```

### 결제 상태와 이벤트 조회

```bash
curl http://localhost:8080/api/payments/1
curl http://localhost:8080/api/payments/1/events
```

## 테스트

```bash
cd backend
./gradlew test
```

테스트는 H2 기반으로 결제 승인, 잔액 부족 실패, 취소 환불, 정산 후 취소 불가, 상태 전이 규칙을 검증합니다.
