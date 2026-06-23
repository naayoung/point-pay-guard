import type { ReactNode } from "react";

type PageCardProps = {
  title: string;
  description?: string;
  children: ReactNode;
  id?: string;
};

export function PageCard({ title, description, children, id }: PageCardProps) {
  return (
    <section className="page-card" id={id}>
      <div className="page-card__header">
        <div>
          <h2>{title}</h2>
          {description ? <p>{description}</p> : null}
        </div>
      </div>
      {children}
    </section>
  );
}
