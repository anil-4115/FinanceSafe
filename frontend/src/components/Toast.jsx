/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useRef, useState } from 'react';
import { CheckCircle2, Info, XCircle } from 'lucide-react';

const ToastContext = createContext(() => {});

let counter = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
    const timer = timers.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timers.current.delete(id);
    }
  }, []);

  const show = useCallback((message, type = 'success', options = {}) => {
    const id = ++counter;
    setToasts((current) => [...current, { id, message, type }]);
    const timer = setTimeout(() => dismiss(id), options.duration ?? 4200);
    timers.current.set(id, timer);
    return id;
  }, [dismiss]);

  const toast = useCallback((message, options = {}) => show(message, options.type ?? 'success', options), [show]);

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-region" aria-live="polite">
        {toasts.map((toastItem) => {
          const Icon = toastItem.type === 'success' ? CheckCircle2 : toastItem.type === 'error' ? XCircle : Info;
          return (
            <div key={toastItem.id} className={`toast toast-${toastItem.type}`}>
              <span className="toast-icon"><Icon size={18} strokeWidth={2.4} /></span>
              <p>{toastItem.message}</p>
              <button type="button" className="toast-close" onClick={() => dismiss(toastItem.id)} aria-label="Dismiss">✕</button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}