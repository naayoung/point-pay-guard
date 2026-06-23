import { FormEvent, useMemo, useState } from "react";
import { api } from "../api/client";
import { PageCard } from "../components/PageCard";
import { ResultBox } from "../components/ResultBox";
import type { PaymentResponse } from "../types/api";

type PaymentApproveProps = {
  onApproved: (payment: PaymentResponse) => void;
  onFailure: (scope: string, error: unknown) => void;
};

function newIdempotencyKey() {
  return `demo-${Date.now()}`;
}

export function PaymentApprove({ onApproved, onFailure }: PaymentApproveProps) {
  const [orderId, setOrderId] = useState("");
  const [userId, setUserId] = useState("1");
  const [amount, setAmount] = useState("10000");
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey());
  const [lastSubmitted, setLastSubmitted] = useState<{ orderId: string; key: string } | null>(null);
  const [result, setResult] = useState<PaymentResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  const responseChecks = useMemo(() => {
    if (!result) {
      return [];
    }

    const checks = [];
    if (userId && Number(userId) !== result.userId) {
      checks.push(`응답 userId(${result.userId})가 입력값(${userId})과 다릅니다.`);
    }
    if (amount && Number(amount) !== result.amount) {
      checks.push(`응답 amount(${result.amount})가 입력값(${amount})과 다릅니다.`);
    }
    return checks;
  }, [amount, result, userId]);

  async function submitApproval(
    event?: FormEvent<HTMLFormElement>,
    keyOverride = idempotencyKey,
    orderIdOverride = orderId
  ) {
    event?.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      // TODO: 백엔드 ApprovePaymentRequest는 orderId/idempotencyKey만 받는다.
      // userId/amount 입력은 포트폴리오 데모에서 응답 검증용으로만 사용한다.
      const payment = await api.approvePayment({
        orderId: Number(orderIdOverride),
        idempotencyKey: keyOverride
      });
      setLastSubmitted({ orderId: orderIdOverride, key: keyOverride });
      setResult(payment);
      onApproved(payment);
    } catch (requestError) {
      setError(requestError);
      onFailure("결제 승인", requestError);
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageCard
      id="payment-approve"
      title="결제 승인"
      description="같은 Idempotency Key 재요청 시 기존 결제 결과가 반환됩니다."
    >
      <form className="form-grid" onSubmit={submitApproval}>
        <label>
          <span>주문 ID</span>
          <input
            min="1"
            required
            type="number"
            value={orderId}
            onChange={(event) => setOrderId(event.target.value)}
          />
        </label>
        <label>
          <span>사용자 ID</span>
          <input
            min="1"
            type="number"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
          />
        </label>
        <label>
          <span>결제 금액</span>
          <input
            min="1"
            type="number"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
        </label>
        <label className="wide-field">
          <span>Idempotency Key</span>
          <div className="inline-control">
            <input
              required
              type="text"
              value={idempotencyKey}
              onChange={(event) => setIdempotencyKey(event.target.value)}
            />
            <button
              className="button button--secondary"
              type="button"
              onClick={() => setIdempotencyKey(newIdempotencyKey())}
            >
              새 키
            </button>
          </div>
        </label>
        <div className="button-row wide-field">
          <button className="button button--primary" disabled={loading} type="submit">
            결제 승인 요청
          </button>
          <button
            className="button button--secondary"
            disabled={loading || !lastSubmitted}
            type="button"
            onClick={() => {
              if (!lastSubmitted) {
                return;
              }
              setOrderId(lastSubmitted.orderId);
              setIdempotencyKey(lastSubmitted.key);
              void submitApproval(undefined, lastSubmitted.key, lastSubmitted.orderId);
            }}
          >
            같은 키로 재요청
          </button>
        </div>
      </form>

      {responseChecks.length ? (
        <div className="notice notice--warning">
          {responseChecks.map((check) => (
            <p key={check}>{check}</p>
          ))}
        </div>
      ) : null}

      <ResultBox loading={loading} result={result} error={error} />
    </PageCard>
  );
}
