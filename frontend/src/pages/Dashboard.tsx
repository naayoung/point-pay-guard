import { StatusBadge } from "../components/StatusBadge";
import type { DemoState, PaymentResponse } from "../types/api";
import { formatCurrency, formatDateTime } from "../utils/format";

type DashboardProps = {
  state: DemoState;
  onReset: () => void;
};

function uniquePayments(payments: PaymentResponse[]) {
  return [...new Map(payments.map((payment) => [payment.paymentId, payment])).values()];
}

export function Dashboard({ state, onReset }: DashboardProps) {
  const payments = uniquePayments(state.payments);
  const settledIds = new Set(
    state.settlements.flatMap((settlement) => settlement.settledPaymentIds)
  );

  // TODO: /api/payments/summary를 화면에 연결해 결제·정산 지표를 서버 전체 집계로 교체한다.
  const metrics = [
    { label: "주문 수", value: state.orders.length },
    { label: "결제 수", value: payments.length },
    { label: "승인 완료 결제 수", value: payments.filter((p) => p.status === "APPROVED").length },
    { label: "취소 결제 수", value: payments.filter((p) => p.status === "CANCELED").length },
    { label: "정산 완료 수", value: settledIds.size },
    { label: "실패 건수", value: payments.filter((p) => p.status === "FAILED").length }
  ];

  const recentPayments = [...payments].sort((a, b) => b.paymentId - a.paymentId).slice(0, 5);
  const recentFailures = state.failures.slice(0, 3);

  return (
    <section className="status-board" id="dashboard">
      <div className="status-board__heading">
        <div>
          <p className="eyebrow">Demo session</p>
          <h2>대시보드</h2>
          <p>현재 화면에서 생성하거나 조회한 데이터 기준으로 집계합니다.</p>
        </div>
        <button className="button button--ghost" type="button" onClick={onReset}>
          세션 초기화
        </button>
      </div>

      <div className="metrics-grid">
        {metrics.map((metric) => (
          <div className="metric" key={metric.label}>
            <span>{metric.label}</span>
            <strong>{metric.value.toLocaleString("ko-KR")}</strong>
          </div>
        ))}
      </div>

      <div className="dashboard-split">
        <div className="compact-panel">
          <h3>최근 결제</h3>
          {recentPayments.length ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>결제 ID</th>
                    <th>주문 ID</th>
                    <th>금액</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {recentPayments.map((payment) => (
                    <tr key={payment.paymentId}>
                      <td>{payment.paymentId}</td>
                      <td>{payment.orderId}</td>
                      <td>{formatCurrency(payment.amount)}</td>
                      <td>
                        <StatusBadge status={payment.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="empty-text">아직 화면에서 확인한 결제가 없습니다.</p>
          )}
        </div>

        <div className="compact-panel">
          <h3>최근 실패</h3>
          {recentFailures.length ? (
            <ul className="failure-list">
              {recentFailures.map((failure) => (
                <li key={`${failure.scope}-${failure.at}-${failure.message}`}>
                  <strong>{failure.scope}</strong>
                  <span>{failure.message}</span>
                  <small>
                    {failure.status ? `HTTP ${failure.status} · ` : ""}
                    {failure.code ? `${failure.code} · ` : ""}
                    {formatDateTime(failure.at)}
                  </small>
                </li>
              ))}
            </ul>
          ) : (
            <p className="empty-text">아직 실패한 요청이 없습니다.</p>
          )}
        </div>
      </div>
    </section>
  );
}
