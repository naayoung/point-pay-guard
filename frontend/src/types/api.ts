export type PaymentStatus =
  | "READY"
  | "APPROVING"
  | "APPROVED"
  | "FAILED"
  | "CANCELING"
  | "CANCELED"
  | "SETTLED";

export type OrderStatus =
  | "CREATED"
  | "PAYING"
  | "PAID"
  | "PAYMENT_FAILED"
  | "CANCELED"
  | "SETTLED";

export type PaymentEventType =
  | "PAYMENT_CREATED"
  | "PAYMENT_APPROVING"
  | "PAYMENT_APPROVED"
  | "PAYMENT_FAILED"
  | "PAYMENT_CANCELING"
  | "PAYMENT_CANCELED"
  | "PAYMENT_SETTLED";

export interface CreateOrderRequest {
  userId: number;
  amount: number;
}

export interface ApprovePaymentRequest {
  orderId: number;
  idempotencyKey: string;
}

export interface CancelPaymentRequest {
  reason?: string | null;
}

export interface OrderResponse {
  orderId: number;
  userId: number;
  amount: number;
  status: OrderStatus;
  createdAt: string;
}

export interface PaymentResponse {
  paymentId: number;
  orderId: number;
  userId: number;
  amount: number;
  status: PaymentStatus;
  idempotencyKey: string;
  approvedAt: string | null;
  canceledAt: string | null;
  settledAt: string | null;
}

/** 결제 상태 하나에 해당하는 전체 건수와 금액 합계다. */
export interface PaymentStatusSummaryResponse {
  status: PaymentStatus;
  paymentCount: number;
  totalAmount: number;
}

/** 전체 결제 현황과 SETTLED 상태 기준 누적 정산 현황이다. */
export interface PaymentSummaryResponse {
  totalPaymentCount: number;
  totalPaymentAmount: number;
  settledPaymentCount: number;
  settledPaymentAmount: number;
  statusSummaries: PaymentStatusSummaryResponse[];
}

export interface PaymentEventResponse {
  eventId: number;
  paymentId: number;
  eventType: PaymentEventType;
  beforeStatus: PaymentStatus | null;
  afterStatus: PaymentStatus;
  reason: string;
  createdAt: string;
}

export interface SettlementResponse {
  settledCount: number;
  totalSettledAmount: number;
  settledPaymentIds: number[];
}

export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors: FieldErrorDetail[];
}

export interface ApiFailure {
  scope: string;
  message: string;
  code?: string;
  status?: number;
  at: string;
}

export interface DemoState {
  orders: OrderResponse[];
  payments: PaymentResponse[];
  eventsByPaymentId: Record<string, PaymentEventResponse[]>;
  settlements: SettlementResponse[];
  failures: ApiFailure[];
}
