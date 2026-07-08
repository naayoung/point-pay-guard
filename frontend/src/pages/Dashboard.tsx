import { useEffect, useState } from "react";
import { api, formatApiError } from "../api/client";
import { StatusBadge } from "../components/StatusBadge";
import type {
  DemoState,
  PaymentResponse,
  PaymentStatus,
  PaymentSummaryResponse
} from "../types/api";
import { formatCurrency, formatDateTime } from "../utils/format";

type DashboardProps = {
  state: DemoState;
  onReset: () => void;
};

function uniquePayments(payments: PaymentResponse[]) {
  return [...new Map(payments.map((payment) => [payment.paymentId, payment])).values()];
}

// 서버 집계에서 지정한 결제 상태의 건수를 찾고, 아직 조회 전이면 빈 값으로 표시한다.
function paymentCountByStatus(
  summary: PaymentSummaryResponse | null,
  status: PaymentStatus
) {
  return summary?.statusSummaries.find((item) => item.status === status)?.paymentCount ?? null;
}

export function Dashboard({ state, onReset }: DashboardProps) {
  // 결제·정산 지표는 브라우저 세션이 아닌 서버 전체 집계 응답을 기준으로 관리한다.
  const [paymentSummary, setPaymentSummary] = useState<PaymentSummaryResponse | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summaryError, setSummaryError] = useState<unknown>(null);
  const [summaryRefreshKey, setSummaryRefreshKey] = useState(0);

  // 최근 결제 목록은 전체 목록 API가 없으므로 현재 브라우저 세션에서 확인한 결제를 사용한다.
  const payments = uniquePayments(state.payments);

  useEffect(() => {
    // 의존 값이 바뀔 때 이전 요청 결과가 늦게 도착해 최신 집계를 덮지 않도록 활성 상태를 추적한다.
    let active = true;
    setSummaryLoading(true);
    setSummaryError(null);

    api
      .getPaymentSummary()
      .then((summary) => {
        if (active) {
          setPaymentSummary(summary);
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setSummaryError(error);
        }
      })
      .finally(() => {
        if (active) {
          setSummaryLoading(false);
        }
      });

    return () => {
      active = false;
    };
    // 결제 승인·취소 또는 정산 결과가 세션 상태에 반영되면 서버 집계를 자동으로 다시 조회한다.
  }, [state.payments, state.settlements, summaryRefreshKey]);

  // 주문 수만 세션 기준이며 나머지 결제·정산 지표는 서버 전체 기준이다.
  const metrics = [
    { label: "주문 수 (세션)", value: state.orders.length },
    { label: "전체 결제 수", value: paymentSummary?.totalPaymentCount ?? null },
    { label: "승인 완료 결제 수", value: paymentCountByStatus(paymentSummary, "APPROVED") },
    { label: "취소 결제 수", value: paymentCountByStatus(paymentSummary, "CANCELED") },
    { label: "정산 완료 수", value: paymentSummary?.settledPaymentCount ?? null },
    { label: "실패 건수", value: paymentCountByStatus(paymentSummary, "FAILED") }
  ];

  // 상세 목록과 실패 이력은 현재 세션에서 확인한 데이터 중 최신 항목만 노출한다.
  const recentPayments = [...payments].sort((a, b) => b.paymentId - a.paymentId).slice(0, 5);
  const recentFailures = state.failures.slice(0, 3);
  const summaryErrorMessage = summaryError ? formatApiError(summaryError).message : null;

  return (
    <section className="status-board" id="dashboard">
      <div className="status-board__heading">
        <div>
          <p className="eyebrow">Server aggregate</p>
          <h2>대시보드</h2>
          <p>결제·정산 지표는 서버 전체 기준이며, 주문과 최근 내역은 현재 세션 기준입니다.</p>
        </div>
        <div className="button-row">
          <button
            className="button button--secondary"
            disabled={summaryLoading}
            type="button"
            onClick={() => setSummaryRefreshKey((current) => current + 1)}
          >
            집계 새로고침
          </button>
          <button className="button button--ghost" type="button" onClick={onReset}>
            세션 초기화
          </button>
        </div>
      </div>

      <div aria-live="polite">
        {summaryLoading ? (
          <p className="dashboard-summary-state dashboard-summary-state--loading">
            서버의 전체 결제·정산 집계를 불러오는 중입니다.
          </p>
        ) : summaryErrorMessage ? (
          <p className="dashboard-summary-state dashboard-summary-state--error">
            집계를 불러오지 못했습니다. {summaryErrorMessage}
          </p>
        ) : paymentSummary ? (
          <p className="dashboard-summary-state dashboard-summary-state--success">
            전체 결제 요청 금액 {formatCurrency(paymentSummary.totalPaymentAmount)} · 누적 정산 금액{" "}
            {formatCurrency(paymentSummary.settledPaymentAmount)}
          </p>
        ) : null}
      </div>

      <div className="metrics-grid">
        {metrics.map((metric) => (
          <div className="metric" key={metric.label}>
            <span>{metric.label}</span>
            <strong>{metric.value === null ? "—" : metric.value.toLocaleString("ko-KR")}</strong>
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
