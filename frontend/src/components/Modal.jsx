import { useEffect } from 'react';

export default function Modal({ open, onClose, title, subtitle, children, footer, narrow }) {
  useEffect(() => {
    if (!open) return undefined;
    const handleKey = (event) => {
      if (event.key === 'Escape') onClose?.();
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={`modal-panel ${narrow ? 'narrow' : ''}`} onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true">
        <div className="panel-header">
          <div>
            <h3 style={{ margin: 0 }}>{title}</h3>
            {subtitle && <p className="muted" style={{ margin: '4px 0 0' }}>{subtitle}</p>}
          </div>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            <span aria-hidden style={{ fontSize: 18, lineHeight: 1 }}>✕</span>
          </button>
        </div>
        {children}
        {footer && <div className="step-actions" style={{ justifyContent: 'flex-end' }}>{footer}</div>}
      </div>
    </div>
  );
}