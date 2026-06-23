import { useState } from "react";
import { api } from "../api/client";
import { PageCard } from "../components/PageCard";
import { ResultBox } from "../components/ResultBox";
import type { PaymentResponse, SettlementResponse } from "../types/api";

type SettlementProps = {
  onSettled: (settlement: SettlementResponse, refreshedPayments: PaymentResponse[]) => void;
  onFailure: (scope: string, error: unknown) => void;
};

export function Settlement({ onSettled, onFailure }: SettlementProps) {
  const [result, setResult] = useState<SettlementResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleRun() {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const settlement = await api.runSettlement();
      const refreshedPayments = await Promise.all(
        settlement.settledPaymentIds.map((paymentId) => api.getPayment(paymentId))
      );
      setResult(settlement);
      onSettled(settlement, refreshedPayments);
    } catch (requestError) {
      setError(requestError);
      onFailure("정산 처리", requestError);
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageCard
      id="settlement"
      title="정산 처리"
      description="현재 APPROVED 상태인 결제를 SETTLED로 일괄 전이합니다."
    >
      <div className="action-row">
        <button className="button button--primary" disabled={loading} type="button" onClick={handleRun}>
          정산 실행
        </button>
      </div>
      <ResultBox loading={loading} result={result} error={error} />
    </PageCard>
  );
}
