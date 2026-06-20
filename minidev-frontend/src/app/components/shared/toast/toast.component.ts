import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

/**
 * Toast notification display component.
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-lg shadow-elevated border text-sm font-medium max-w-sm animate-slide-in-right"
          [class]="getClass(toast.type)"
          [class.animate-shake]="toast.dismissing"
          [class.opacity-0]="toast.dismissing"
          [class.transition-opacity]="toast.dismissing"
          (click)="toastService.dismiss(toast.id)"
        >
          <span class="shrink-0">
            @switch (toast.type) {
              @case ('success') {
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
                </svg>
              }
              @case ('error') {
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
                </svg>
              }
              @case ('warning') {
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/>
                </svg>
              }
              @default {
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z"/>
                </svg>
              }
            }
          </span>
          <span class="flex-1">{{ toast.message }}</span>
        </div>
      }
    </div>
  `
})
export class ToastComponent {
  public toastService = inject(ToastService);

  /**
   * Gets the CSS class for the given toast type.
   * @param {string} type - The toast type string.
   * @returns {string} The Tailwind CSS class string for the toast.
   */
  public getClass(type: string): string {
    switch (type) {
      case 'success':
        return 'bg-success-50 dark:bg-success-500/10 border-success-500/20 text-success-600 dark:text-success-500';
      case 'error':
        return 'bg-danger-50 dark:bg-danger-500/10 border-danger-500/20 text-danger-600 dark:text-danger-500';
      case 'warning':
        return 'bg-warning-50 dark:bg-warning-500/10 border-warning-500/20 text-warning-600 dark:text-warning-500';
      default:
        return 'bg-white dark:bg-surface-800 border-surface-200 dark:border-surface-600 text-surface-700 dark:text-surface-300';
    }
  }
}
