/* eslint-disable react-refresh/only-export-components */
import { useEffect, useState } from 'react';

export function useCountUp(target) {
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (typeof target !== 'number' || Number.isNaN(target)) return undefined;
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      const id = requestAnimationFrame(() => setValue(target));
      return () => cancelAnimationFrame(id);
    }
    const duration = 900;
    const start = performance.now();
    let raf = 0;
    const step = (now) => {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(target * eased);
      if (progress < 1) raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [target]);

  return value;
}

export default function CountUp({ value, decimals = 0, className }) {
  const animated = useCountUp(Number(value) || 0);
  const formatted = animated.toLocaleString('en-IN', {
    maximumFractionDigits: decimals,
    minimumFractionDigits: decimals,
  });
  return <span className={className}>{formatted}</span>;
}