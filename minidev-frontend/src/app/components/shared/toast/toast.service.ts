import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  dismissing: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private counter = 0;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: ToastType = 'info', durationMs = 4000) {
    const id = ++this.counter;
    const toast: Toast = { id, message, type, dismissing: false };
    this.toasts.update(t => [...t, toast]);

    setTimeout(() => this.dismiss(id), durationMs);
  }

  success(message: string) {
    this.show(message, 'success');
  }

  error(message: string) {
    this.show(message, 'error', 6000);
  }

  info(message: string) {
    this.show(message, 'info');
  }

  warning(message: string) {
    this.show(message, 'warning', 5000);
  }

  dismiss(id: number) {
    this.toasts.update(t => t.map(toast =>
      toast.id === id ? { ...toast, dismissing: true } : toast
    ));
    setTimeout(() => {
      this.toasts.update(t => t.filter(toast => toast.id !== id));
    }, 200);
  }
}
