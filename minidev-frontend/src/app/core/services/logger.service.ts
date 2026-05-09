import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  info(message: string, context?: unknown): void {
    this.write('info', message, context);
  }

  warn(message: string, context?: unknown): void {
    this.write('warn', message, context);
  }

  error(message: string, context?: unknown): void {
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

