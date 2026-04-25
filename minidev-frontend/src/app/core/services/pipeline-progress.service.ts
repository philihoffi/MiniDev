import { Injectable, NgZone, OnDestroy, computed, inject, signal } from '@angular/core';
import {
  PipelineNodeState,
  PipelineNodeStatus,
  PipelineNodeView,
  PipelineProgressEvent,
  PipelineRunStatus,
  PipelineRunViewState
} from '../models/pipeline-progress.model';

const EMPTY_STATE: PipelineRunViewState = {
  runId: null,
  pipelineName: null,
  runStatus: 'IDLE',
  lastUpdated: null,
  nodes: {},
  nodeOrder: []
};

@Injectable({
  providedIn: 'root'
})
export class PipelineProgressService implements OnDestroy {
  private zone = inject(NgZone);
  private eventSource: EventSource | null = null;

  readonly connected = signal(false);
  readonly state = signal<PipelineRunViewState>(EMPTY_STATE);
  readonly nodeViews = computed<PipelineNodeView[]>(() => {
    const state = this.state();
    return state.nodeOrder
      .map(nodeId => state.nodes[nodeId])
      .filter((node): node is PipelineNodeState => !!node)
      .map(node => ({
        ...node,
        depth: this.calculateDepth(node, state.nodes)
      }));
  });

  constructor() {
    this.connect();
  }

  ngOnDestroy() {
    this.disconnect();
  }

  connect() {
    if (this.eventSource) {
      return;
    }

    this.eventSource = new EventSource('/api/events/PIPELINE');
    this.eventSource.addEventListener('ping', () => {
      this.zone.run(() => {
        this.connected.set(true);
      });
    });

    this.eventSource.addEventListener('event', event => {
      const data = this.parseMessageEvent(event as MessageEvent<string>);
      if (!data) {
        return;
      }
      this.zone.run(() => this.applyProgressEvent(data));
    });

    this.eventSource.onerror = () => {
      this.zone.run(() => {
        this.connected.set(false);
      });
    };
  }

  disconnect() {
    if (!this.eventSource) {
      return;
    }
    this.eventSource.close();
    this.eventSource = null;
    this.connected.set(false);
  }

  private parseMessageEvent(event: MessageEvent<string>): PipelineProgressEvent | null {
    try {
      return JSON.parse(event.data) as PipelineProgressEvent;
    } catch {
      return null;
    }
  }

  private applyProgressEvent(event: PipelineProgressEvent) {
    switch (event.type) {
      case 'RUN_STARTED':
        this.state.set({
          runId: event.runId,
          pipelineName: event.pipelineName,
          runStatus: 'RUNNING',
          lastUpdated: event.timestamp,
          nodes: {},
          nodeOrder: []
        });
        return;
      case 'RUN_FINISHED':
        this.state.update(state => ({
          ...state,
          runStatus: this.resolveRunStatus(event.status),
          lastUpdated: event.timestamp
        }));
        return;
      case 'NODE_DISCOVERED':
        this.upsertNode(event, 'PENDING');
        return;
      case 'NODE_STARTED':
        this.upsertNode(event, 'RUNNING');
        return;
      case 'NODE_FINISHED':
        this.upsertNode(event, event.status === 'SUCCESS' ? 'SUCCESS' : 'FAILED');
        return;
      case 'NODE_WARNING':
        this.upsertNode(event, 'WARNING');
        return;
      case 'NODE_ERROR':
        this.upsertNode(event, 'FAILED');
        return;
      default:
        return;
    }
  }

  private upsertNode(event: PipelineProgressEvent, defaultStatus: PipelineNodeStatus) {
    const nodeId = event.nodeId;
    if (!nodeId) {
      return;
    }

    this.state.update(state => {
      const existing = state.nodes[nodeId];
      const status = this.resolveNodeStatus(event.status, defaultStatus);
      const updatedNode: PipelineNodeState = {
        id: nodeId,
        parentNodeId: event.parentNodeId,
        name: event.nodeName ?? existing?.name ?? nodeId,
        type: event.nodeType ?? existing?.type ?? 'STEP',
        status,
        message: event.message
      };

      const nextNodes = {
        ...state.nodes,
        [nodeId]: updatedNode
      };
      const nextOrder = state.nodeOrder.includes(nodeId)
        ? state.nodeOrder
        : [...state.nodeOrder, nodeId];

      return {
        ...state,
        lastUpdated: event.timestamp,
        nodes: nextNodes,
        nodeOrder: nextOrder
      };
    });
  }

  private resolveRunStatus(status: string | null): PipelineRunStatus {
    if (status === 'SUCCESS') {
      return 'SUCCESS';
    }
    if (status === 'FAILED') {
      return 'FAILED';
    }
    return 'IDLE';
  }

  private resolveNodeStatus(status: string | null, fallback: PipelineNodeStatus): PipelineNodeStatus {
    switch (status) {
      case 'PENDING':
      case 'RUNNING':
      case 'SUCCESS':
      case 'FAILED':
      case 'WARNING':
        return status;
      default:
        return fallback;
    }
  }

  private calculateDepth(node: PipelineNodeState, allNodes: Record<string, PipelineNodeState>): number {
    let depth = 0;
    let cursor = node.parentNodeId ? allNodes[node.parentNodeId] : null;

    while (cursor) {
      depth++;
      cursor = cursor.parentNodeId ? allNodes[cursor.parentNodeId] : null;
    }

    return depth;
  }
}

