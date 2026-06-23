import { formatApiError } from "../api/client";

type ResultBoxProps = {
  title?: string;
  loading?: boolean;
  result?: unknown;
  error?: unknown;
};

export function ResultBox({ title = "요청 결과", loading, result, error }: ResultBoxProps) {
  if (loading) {
    return (
      <div className="result-box result-box--loading">
        <strong>{title}</strong>
        <span>요청 처리 중...</span>
      </div>
    );
  }

  if (error) {
    const formatted = formatApiError(error);

    return (
      <div className="result-box result-box--error">
        <strong>{title}</strong>
        <p>{formatted.message}</p>
        <div className="result-meta">
          {formatted.status ? <span>HTTP {formatted.status}</span> : null}
          {formatted.code ? <span>{formatted.code}</span> : null}
        </div>
        {formatted.fieldErrors?.length ? (
          <ul className="field-errors">
            {formatted.fieldErrors.map((fieldError) => (
              <li key={`${fieldError.field}-${fieldError.message}`}>
                {fieldError.field}: {fieldError.message}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    );
  }

  if (result) {
    return (
      <div className="result-box result-box--success">
        <strong>{title}</strong>
        <pre>{JSON.stringify(result, null, 2)}</pre>
      </div>
    );
  }

  return null;
}
