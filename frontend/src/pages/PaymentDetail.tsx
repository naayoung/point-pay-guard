import { FormEvent, useState } from "react";
import { api } from "../api/client";
import { PageCard } from "../components/PageCard";
import { ResultBox } from "../components/ResultBox";
import { StatusBadge } from "../components/StatusBadge";
import type { PaymentEventResponse, PaymentResponse } from "../types/api";
import { formatCurrency, formatDateTime } from "../utils/format";

type PaymentDetailProps = {
  onPaymentLoaded: (payment: PaymentResponse) => void;
  onEventsLoaded: (paymentId: number, events: PaymentEventResponse[]) => void;
  onFailure: (scope: string, error: unknown) => void;
};

export function PaymentDetail({
  onPaymentLoaded,
  onEventsLoaded,
  onFailure
}: PaymentDetailProps) {
  const [paymentId, setPaymentId] = useState("");
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [events, setEvents] = useState<PaymentEventResponse[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setPayment(null);
    setEvents([]);

    try {
      // TODO: 현재 백엔드는 paymentId 단건 조회만 제공한다.
      // 주문 ID/사용자 ID 기준 검색이 필요하면 별도 조회 API가 필요하다.
      const targetPaymentId = Number(paymentId);
      const [paymentResult, eventResult] = await Promise.all([
        api.getPayment(targetPaymentId),
        api.getPaymentEvents(targetPaymentId)
      ]);
      setPayment(paymentResult);
      setEvents(eventResult);
      onPaymentLoaded(paymentResult);
      onEventsLoaded(targetPaymentId, eventResult);
    } catch (requestError) {
      setError(requestError);
      onFailure("결제 조회", requestError);
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageCard
      id="payment-detail"
      title="결제 상태 조회 · 이벤트 이력"
      description="결제 ID로 현재 상태와 상태 전이 이벤트를 함께 확인합니다."
    >
      <form className="lookup-bar" onSubmit={handleSearch}>
        <label>
          <span>결제 ID 검색</span>
          <input
            min="1"
            required
            type="number"
            value={paymentId}
            onChange={(event) => setPaymentId(event.target.value)}
          />
        </label>
        <button className="button button--primary" disabled={loading} type="submit">
          조회
        </button>
      </form>

      {payment ? (
        <div className="detail-grid">
          <div>
            <span>결제 상태</span>
            <StatusBadge status={payment.status} />
          </div>
          <div>
            <span>주문 ID</span>
            <strong>{payment.orderId}</strong>
          </div>
          <div>
            <span>금액</span>
            <strong>{formatCurrency(payment.amount)}</strong>
          </div>
          <div>
            <span>상태값</span>
            <strong>{payment.status}</strong>
          </div>
          <div>
            <span>승인일시</span>
            <strong>{formatDateTime(payment.approvedAt)}</strong>
          </div>
          <div>
            <span>취소일시</span>
            <strong>{formatDateTime(payment.canceledAt)}</strong>
          </div>
          <div>
            <span>정산일시</span>
            <strong>{formatDateTime(payment.settledAt)}</strong>
          </div>
        </div>
      ) : null}

      {events.length ? (
        <div className="event-section">
          <h3>결제 이벤트 이력</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>eventType</th>
                  <th>beforeStatus</th>
                  <th>afterStatus</th>
                  <th>reason</th>
                  <th>createdAt</th>
                </tr>
              </thead>
              <tbody>
                {events.map((event) => (
                  <tr key={event.eventId}>
                    <td>{event.eventType}</td>
                    <td>
                      <StatusBadge status={event.beforeStatus} />
                    </td>
                    <td>
                      <StatusBadge status={event.afterStatus} />
                    </td>
                    <td>{event.reason}</td>
                    <td>{formatDateTime(event.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : payment ? (
        <p className="empty-text">표시할 이벤트가 없습니다.</p>
      ) : null}

      <ResultBox loading={loading} error={error} />
    </PageCard>
  );
}
