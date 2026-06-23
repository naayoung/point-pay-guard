import { FormEvent, useState } from "react";
import { api } from "../api/client";
import { PageCard } from "../components/PageCard";
import { ResultBox } from "../components/ResultBox";
import type { PaymentResponse } from "../types/api";

type PaymentCancelProps = {
  onCanceled: (payment: PaymentResponse) => void;
  onFailure: (scope: string, error: unknown) => void;
};

export function PaymentCancel({ onCanceled, onFailure }: PaymentCancelProps) {
  const [paymentId, setPaymentId] = useState("");
  const [reason, setReason] = useState("사용자 요청");
  const [result, setResult] = useState<PaymentResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const payment = await api.cancelPayment(Number(paymentId), {
        reason: reason.trim() || null
      });
      setResult(payment);
      onCanceled(payment);
    } catch (requestError) {
      setError(requestError);
      onFailure("결제 취소", requestError);
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageCard
      id="payment-cancel"
      title="결제 취소"
      description="APPROVED 상태 결제만 CANCELED로 전이할 수 있습니다."
    >
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>결제 ID</span>
          <input
            min="1"
            required
            type="number"
            value={paymentId}
            onChange={(event) => setPaymentId(event.target.value)}
          />
        </label>
        <label className="wide-field">
          <span>취소 사유</span>
          <input
            type="text"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        </label>
        <button className="button button--danger" disabled={loading} type="submit">
          결제 취소 요청
        </button>
      </form>
      <ResultBox loading={loading} result={result} error={error} />
    </PageCard>
  );
}
