import type { OrderStatus, PaymentStatus } from "../types/api";

type StatusBadgeProps = {
  status?: PaymentStatus | OrderStatus | string | null;
};

const statusClassMap: Record<string, string> = {
  APPROVED: "badge--approved",
  PAID: "badge--approved",
  CANCELED: "badge--canceled",
  FAILED: "badge--failed",
  PAYMENT_FAILED: "badge--failed",
  SETTLED: "badge--settled",
  APPROVING: "badge--working",
  PAYING: "badge--working",
  CANCELING: "badge--warning",
  READY: "badge--muted",
  CREATED: "badge--muted"
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const value = status ?? "NULL";
  const className = statusClassMap[value] ?? "badge--muted";

  return <span className={`status-badge ${className}`}>{value}</span>;
}
