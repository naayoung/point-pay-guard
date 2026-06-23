import { FormEvent, useState } from "react";
import { api } from "../api/client";
import { PageCard } from "../components/PageCard";
import { ResultBox } from "../components/ResultBox";
import type { OrderResponse } from "../types/api";

type OrderCreateProps = {
  onCreated: (order: OrderResponse) => void;
  onFailure: (scope: string, error: unknown) => void;
};

export function OrderCreate({ onCreated, onFailure }: OrderCreateProps) {
  const [userId, setUserId] = useState("1");
  const [amount, setAmount] = useState("10000");
  const [result, setResult] = useState<OrderResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const order = await api.createOrder({
        userId: Number(userId),
        amount: Number(amount)
      });
      setResult(order);
      onCreated(order);
    } catch (requestError) {
      setError(requestError);
      onFailure("주문 생성", requestError);
    } finally {
      setLoading(false);
    }
  }

  return (
    <PageCard
      id="order-create"
      title="주문 생성"
      description="데모 사용자는 기본 userId 1로 생성됩니다."
    >
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>사용자 ID</span>
          <input
            min="1"
            required
            type="number"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
          />
        </label>
        <label>
          <span>주문 금액</span>
          <input
            min="1"
            required
            type="number"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
        </label>
        <button className="button button--primary" disabled={loading} type="submit">
          주문 생성
        </button>
      </form>
      <ResultBox loading={loading} result={result} error={error} />
    </PageCard>
  );
}
