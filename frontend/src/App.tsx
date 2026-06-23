import { useEffect, useState } from "react";
import { formatApiError } from "./api/client";
import { Dashboard } from "./pages/Dashboard";
import { OrderCreate } from "./pages/OrderCreate";
import { PaymentApprove } from "./pages/PaymentApprove";
import { PaymentCancel } from "./pages/PaymentCancel";
import { PaymentDetail } from "./pages/PaymentDetail";
import { Settlement } from "./pages/Settlement";
import type {
  ApiFailure,
  DemoState,
  OrderResponse,
  PaymentEventResponse,
  PaymentResponse,
  SettlementResponse
} from "./types/api";

const STORAGE_KEY = "pointpay-guard-demo-state";

const emptyState: DemoState = {
  orders: [],
  payments: [],
  eventsByPaymentId: {},
  settlements: [],
  failures: []
};

function loadState(): DemoState {
  const saved = window.localStorage.getItem(STORAGE_KEY);
  if (!saved) {
    return emptyState;
  }

  try {
    return { ...emptyState, ...JSON.parse(saved) };
  } catch {
    return emptyState;
  }
}

function upsertById<T>(items: T[], getId: (item: T) => number, next: T) {
  const withoutNext = items.filter((item) => getId(item) !== getId(next));
  return [next, ...withoutNext];
}

function App() {
  const [state, setState] = useState<DemoState>(loadState);

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, [state]);

  function handleOrderCreated(order: OrderResponse) {
    setState((current) => ({
      ...current,
      orders: upsertById(current.orders, (item) => item.orderId, order)
    }));
  }

  function handlePaymentChanged(payment: PaymentResponse) {
    setState((current) => ({
      ...current,
      payments: upsertById(current.payments, (item) => item.paymentId, payment)
    }));
  }

  function handleEventsLoaded(paymentId: number, events: PaymentEventResponse[]) {
    setState((current) => ({
      ...current,
      eventsByPaymentId: {
        ...current.eventsByPaymentId,
        [paymentId]: events
      }
    }));
  }

  function handleSettled(settlement: SettlementResponse, refreshedPayments: PaymentResponse[]) {
    setState((current) => {
      const nextPayments = refreshedPayments.reduce(
        (payments, payment) => upsertById(payments, (item) => item.paymentId, payment),
        current.payments
      );

      return {
        ...current,
        payments: nextPayments,
        settlements: [settlement, ...current.settlements]
      };
    });
  }

  function handleFailure(scope: string, error: unknown) {
    const formatted = formatApiError(error);
    const failure: ApiFailure = {
      scope,
      message: formatted.message,
      code: formatted.code,
      status: formatted.status,
      at: new Date().toISOString()
    };

    setState((current) => ({
      ...current,
      failures: [failure, ...current.failures].slice(0, 20)
    }));
  }

  function resetState() {
    setState(emptyState);
    window.localStorage.removeItem(STORAGE_KEY);
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Spring Boot API Demo</p>
          <h1>PointPay Guard</h1>
          <p>
            결제 상태 흐름, 멱등성 키 재요청, 취소 제한, 정산 처리와 이벤트 이력을 한 화면에서 확인합니다.
          </p>
        </div>
        <nav className="quick-nav" aria-label="demo sections">
          <a href="#dashboard">대시보드</a>
          <a href="#order-create">주문</a>
          <a href="#payment-approve">승인</a>
          <a href="#payment-cancel">취소</a>
          <a href="#settlement">정산</a>
          <a href="#payment-detail">조회</a>
        </nav>
      </header>

      <main>
        <Dashboard state={state} onReset={resetState} />

        <section className="workspace-grid" aria-label="payment workflow">
          <OrderCreate onCreated={handleOrderCreated} onFailure={handleFailure} />
          <PaymentApprove onApproved={handlePaymentChanged} onFailure={handleFailure} />
          <PaymentCancel onCanceled={handlePaymentChanged} onFailure={handleFailure} />
          <Settlement onSettled={handleSettled} onFailure={handleFailure} />
        </section>

        <PaymentDetail
          onPaymentLoaded={handlePaymentChanged}
          onEventsLoaded={handleEventsLoaded}
          onFailure={handleFailure}
        />
      </main>
    </div>
  );
}

export default App;
