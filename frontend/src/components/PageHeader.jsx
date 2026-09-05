export default function PageHeader({ eyebrow, title, intro, actions, children }) {
  return (
    <div className="page-shell">
      <div className="page-header-wrap">
        <div>
          {eyebrow && <p className="eyebrow">{eyebrow}</p>}
          <h2>{title}</h2>
        </div>
        {actions && <div className="topbar-actions">{actions}</div>}
      </div>
      {intro && <p className="page-intro">{intro}</p>}
      {children}
    </div>
  );
}