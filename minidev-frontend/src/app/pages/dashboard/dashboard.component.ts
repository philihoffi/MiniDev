import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { PipelineProgressService } from '../../core/services/pipeline-progress.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  readonly pipelineProgress = inject(PipelineProgressService);
}
