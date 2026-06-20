import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  dismissing: boolean;
}

/**
 * Service to manage toast notifications.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private counter = 0;
  public readonly toasts = signal<Toast[]>([]);

  /**
   * Shows a toast notification.
   * @param {string} message - The message to display.
   * @param {ToastType} type - The toast type.
   * @param {number} durationMs - Duration in milliseconds before auto-dismiss.
   */
  public show(message: string, type: ToastType = 'info', durationMs = 4000): void {
    const id = ++this.counter;
    const toast: Toast = { id, message, type, dismissing: false };
    this.toasts.update(t => [...t, toast]);

    setTimeout(() => this.dismiss(id), durationMs);
  }

  /**
   * Shows a success toast.
   * @param {string} message - The message to display.
   */
  public success(message: string): void {
    this.show(message, 'success');
  }

  /**
   * Shows an error toast.
   * @param {string} message - The message to display.
   */
  public error(message: string): void {
    this.show(message, 'error', 6000);
  }

  /**
   * Shows an info toast.
   * @param {string} message - The message to display.
   */
  public info(message: string): void {
    this.show(message, 'info');
  }

  /**
   * Shows a warning toast.
   * @param {string} message - The message to display.
   */
  public warning(message: string): void {
    this.show(message, 'warning', 5000);
  }

  /**
   * Dismisses a toast by id.
   * @param {number} id - The toast id to dismiss.
   */
  public dismiss(id: number): void {
    this.toasts.update(t => t.map(toast =>
      toast.id === id ? { ...toast, dismissing: true } : toast
    ));
    setTimeout(() => {
      this.toasts.update(t => t.filter(toast => toast.id !== id));
    }, 200);
  }
}
