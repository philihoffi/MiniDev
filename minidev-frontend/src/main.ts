import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

function reportBootstrapError(error: unknown): void {
  console.error({
    level: 'error',
    message: 'frontend_bootstrap_failed',
    timestamp: new Date().toISOString(),
    context: error
  });
}

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => reportBootstrapError(err));
