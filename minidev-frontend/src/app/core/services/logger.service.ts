import { Injectable } from '@angular/core';

/**
 * Application logging service.
 */
@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  /**
   * Logs an info-level message.
   * @param {string} message - The log message.
   * @param {unknown} context - Optional context data.
   */
  public info(message: string, context?: unknown): void {
    this.write('info', message, context);
  }

  /**
   * Logs a warning-level message.
   * @param {string} message - The log message.
   * @param {unknown} context - Optional context data.
   */
  public warn(message: string, context?: unknown): void {
    this.write('warn', message, context);
  }

  /**
   * Logs an error-level message.
   * @param {string} message - The log message.
   * @param {unknown} context - Optional context data.
   */
  public error(message: string, context?: unknown): void {
    this.write('error', message, context);
  }

  private write(level: 'info' | 'warn' | 'error', message: string, context?: unknown): void {
    const payload = {
      level,
      message,
      timestamp: new Date().toISOString(),
      context
    };

    if (level === 'info') {
      console.info(payload);
      return;
    }

    if (level === 'warn') {
      console.warn(payload);
      return;
    }

    console.error(payload);
  }
}

