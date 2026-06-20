import { Component, Input } from '@angular/core';

/**
 * Loading spinner component with configurable size and text.
 */
@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `
    <div class="flex items-center justify-center" [class]="containerClass">
      <svg
        class="animate-spin text-brand-600"
        [class]="sizeClass"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      @if (text) {
        <span class="ml-3 text-sm text-surface-500 dark:text-surface-400 font-medium">{{ text }}</span>
      }
    </div>
  `
})
export class LoadingSpinnerComponent {
  @Input() public size: 'sm' | 'md' | 'lg' = 'md';
  @Input() public text = '';

  /**
   * Gets the CSS size class based on the size input.
   * @returns {string} The Tailwind CSS size class.
   */
  public get sizeClass(): string {
    switch (this.size) {
      case 'sm': return 'w-4 h-4';
      case 'lg': return 'w-8 h-8';
      default: return 'w-6 h-6';
    }
  }

  /**
   * Gets the container CSS class based on the size input.
   * @returns {string} The Tailwind CSS container class.
   */
  public get containerClass(): string {
    return this.size === 'lg' ? 'py-12' : '';
  }
}
