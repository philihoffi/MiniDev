export type PipelineRunStatus = 'IDLE' | 'RUNNING' | 'SUCCESS' | 'FAILED';
export type PipelineNodeStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'WARNING';

export interface PipelineProgressEvent {
  runId: string;
  pipelineName: string;
  type:
    | 'RUN_STARTED'
    | 'RUN_FINISHED'
    | 'NODE_DISCOVERED'
    | 'NODE_STARTED'
    | 'NODE_FINISHED'
    | 'NODE_WARNING'
    | 'NODE_ERROR';
  nodeId: string | null;
  parentNodeId: string | null;
  nodeName: string | null;
  nodeType: string | null;
  status: string | null;
  message: string | null;
  timestamp: string;
}

export interface PipelineNodeState {
  id: string;
  parentNodeId: string | null;
  name: string;
  type: string;
  status: PipelineNodeStatus;
  message: string | null;
}

export interface PipelineRunViewState {
  runId: string | null;
  pipelineName: string | null;
  runStatus: PipelineRunStatus;
  lastUpdated: string | null;
  nodes: Record<string, PipelineNodeState>;
  nodeOrder: string[];
}

export interface PipelineNodeView extends PipelineNodeState {
  depth: number;
}

