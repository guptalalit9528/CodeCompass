export type ViewModeTab = 'context' | 'timeline' | 'compare' | null;

export interface ProjectOption {
  id: string;
  name: string;
}

export interface BranchOption {
  id: string;
  name: string;
}

export interface TelemetryMetrics {
  latencyMs: number;
  tokens: number;
  cost: number;
  /** Cumulative lifetime or workspace tokens accumulated across all sessions */
  totalTokens?: number;
  /** Cumulative lifetime or workspace estimated cost in USD */
  totalCost?: number;
  /** Cumulative total API requests */
  totalRequests?: number;
}

export interface TopHeaderProps {
  /** Active conversation title displayed in the header */
  conversationTitle: string;
  /** Optional project name for breadcrumb */
  projectName?: string | null;
  /** Available conversation branches */
  branches?: BranchOption[];
  /** Currently selected branch ID */
  selectedBranchId?: string;
  /** Callback when user changes branch */
  onSelectBranch?: (branchId: string) => void;
  /** Callback to fork or create a new branch */
  onNewBranch?: () => void;
  /** Callback when Share button is clicked */
  onShare?: () => void;
  /** Current color theme */
  theme?: 'dark' | 'light';
  /** Callback to toggle color theme */
  onToggleTheme?: () => void;
  /** Callback to toggle mobile sidebar */
  onToggleMobileMenu?: () => void;
  /** AI telemetry metrics (latency, tokens, estimated cost) */
  telemetry?: TelemetryMetrics;
  /** Callback when telemetry pill is clicked */
  onOpenTelemetry?: () => void;
  /** Currently active segmented view mode drawer/modal */
  activeTab?: ViewModeTab;
  /** Callback when a segmented view mode tab is selected */
  onSelectTab?: (tab: ViewModeTab) => void;
  /** Available projects for organizational assignment */
  projects?: ProjectOption[];
  /** Currently assigned project ID */
  selectedProjectId?: string | null;
  /** Callback to move conversation to another project */
  onMoveToProject?: (projectId: string | null) => void;
  /** Callback to clear conversation messages */
  onClearConversation?: () => void;
  /** Additional CSS class names */
  className?: string;
}
