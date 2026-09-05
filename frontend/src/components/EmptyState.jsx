export default function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="empty-state">
      {Icon && (
        <span className="empty-icon">
          <Icon size={26} strokeWidth={2} />
        </span>
      )}
      <h3>{title}</h3>
      <p>{text}</p>
      {action}
    </div>
  );
}