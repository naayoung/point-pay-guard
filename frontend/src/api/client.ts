import type {
  ApprovePaymentRequest,
  CancelPaymentRequest,
  CreateOrderRequest,
  ErrorResponse,
  OrderResponse,
  PaymentEventResponse,
  PaymentResponse,
  PaymentSummaryResponse,
  SettlementResponse
} from "../types/api";

const API_BASE = "/api";

export class ApiClientError extends Error {
  status: number;
  payload?: ErrorResponse | unknown;

  constructor(status: number, message: string, payload?: ErrorResponse | unknown) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.payload = payload;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const hasBody = options.body !== undefined;
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(hasBody ? { "Content-Type": "application/json" } : {}),
      ...options.headers
    }
  });

  const text = await response.text();
  const data = text ? tryParseJson(text) : null;

  if (!response.ok) {
    const message =
      isErrorResponse(data) && data.message
        ? data.message
        : response.statusText || "API 요청에 실패했습니다.";
    throw new ApiClientError(response.status, message, data);
  }

  return data as T;
}

function tryParseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function isErrorResponse(value: unknown): value is ErrorResponse {
  return (
    typeof value === "object" &&
    value !== null &&
    "code" in value &&
    "message" in value
  );
}

export function formatApiError(error: unknown): {
  message: string;
  code?: string;
  status?: number;
  fieldErrors?: ErrorResponse["fieldErrors"];
} {
  if (error instanceof ApiClientError) {
    const payload = isErrorResponse(error.payload) ? error.payload : undefined;
    return {
      message: payload?.message ?? error.message,
      code: payload?.code,
      status: error.status,
      fieldErrors: payload?.fieldErrors
    };
  }

  if (error instanceof Error) {
    return { message: error.message };
  }

  return { message: "알 수 없는 오류가 발생했습니다." };
}

export const api = {
  createOrder(payload: CreateOrderRequest) {
    return request<OrderResponse>("/orders", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  approvePayment(payload: ApprovePaymentRequest) {
    return request<PaymentResponse>("/payments/approve", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  cancelPayment(paymentId: number, payload: CancelPaymentRequest) {
    return request<PaymentResponse>(`/payments/${paymentId}/cancel`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  getPayment(paymentId: number) {
    return request<PaymentResponse>(`/payments/${paymentId}`);
  },

  getPaymentEvents(paymentId: number) {
    return request<PaymentEventResponse[]>(`/payments/${paymentId}/events`);
  },

  // 서버에 저장된 전체 결제와 누적 정산 현황을 상태별로 조회한다.
  getPaymentSummary() {
    return request<PaymentSummaryResponse>("/payments/summary");
  },

  runSettlement() {
    return request<SettlementResponse>("/settlements/run", {
      method: "POST"
    });
  }
};
