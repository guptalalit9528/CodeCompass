import React, { useState, useRef, useEffect } from 'react';
import {
  Share2,
  Sun,
  Moon,
  MoreVertical,
  Pin,
  Clock,
  Scale,
  GitBranch,
  Plus,
  FolderInput,
  Trash2,
  Menu,
  Timer,
  X,
  ChevronRight,
  Settings,
  Sparkles
} from 'lucide-react';
import { TopHeaderProps, ViewModeTab } from './TopHeader.types';

/**
 * TopHeader: Clean, two-tier modular navigation header component with a centered
 * glassmorphic Quick Actions Modal Dialog popup.
 *
 * Tier 1 (Top): Primary Navigation, Truncated Title, Emerald Branch Badge, Share & Options Button.
 * Tier 2 (Bottom): Monospace Session Telemetry & View Mode Segmented Control.
 * Centered Modal: Modern glassmorphic dialog with backdrop blur, action cards, and keyboard/backdrop dismissal.
 */
export const TopHeader: React.FC<TopHeaderProps> = ({
  conversationTitle,
  projectName,
  branches = [{ id: 'main', name: 'main' }],
  selectedBranchId = 'main',
  onSelectBranch,
  onNewBranch,
  onShare,
  theme = 'dark',
  onToggleTheme,
  onToggleMobileMenu,
  telemetry = { latencyMs: 0, tokens: 0, cost: 0 },
  onOpenTelemetry,
  activeTab = null,
  onSelectTab,
  projects = [],
  selectedProjectId = null,
  onMoveToProject,
  onClearConversation,
  className = ''
}) => {
  const [isOptionsModalOpen, setIsOptionsModalOpen] = useState(false);
  const modalRef = useRef<HTMLDivElement>(null);

  // Close modal on Escape key press
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOptionsModalOpen(false);
      }
    };

    if (isOptionsModalOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOptionsModalOpen]);

  const handleTabClick = (tab: ViewModeTab) => {
    if (onSelectTab) {
      onSelectTab(activeTab === tab ? null : tab);
    }
  };

  const displayTokens = telemetry.totalTokens !== undefined ? telemetry.totalTokens : telemetry.tokens;
  const displayCost = telemetry.totalCost !== undefined ? telemetry.totalCost : (telemetry.cost || 0);
  const formattedCost = `$${Number(displayCost).toFixed(4)}`;

  return (
    <>
      <header
        className={`sticky top-0 z-20 flex flex-col border-b border-border/60 bg-background/95 backdrop-blur shrink-0 transition-colors ${className}`}
      >
        {/* ========================================================
            TOP ROW: Primary Navigation, Truncated Title & Core Actions
            ======================================================== */}
        <div className="flex items-center justify-between px-4 py-2 gap-3 min-w-0">
          {/* Left: Navigation Breadcrumb, Title & Branch Badge */}
          <div className="flex items-center gap-2.5 min-w-0 overflow-hidden">
            {/* Mobile hamburger menu toggle */}
            {onToggleMobileMenu && (
              <button
                type="button"
                onClick={onToggleMobileMenu}
                aria-label="Open mobile menu"
                className="md:hidden p-1.5 rounded-md hover:bg-muted text-muted-foreground hover:text-foreground transition-colors shrink-0"
              >
                <Menu className="w-4 h-4" />
              </button>
            )}

            {/* Project Breadcrumb & Conversation Title */}
            <div className="flex items-center gap-1.5 min-w-0 overflow-hidden">
              {projectName && (
                <span className="text-xs text-muted-foreground whitespace-nowrap shrink-0 hidden xs:inline">
                  Project / <strong className="font-medium text-foreground">{projectName}</strong> ·
                </span>
              )}
              <h1
                className="truncate max-w-[150px] xs:max-w-[200px] sm:max-w-md font-semibold text-sm text-foreground cursor-default min-w-0"
                title={conversationTitle}
              >
                {conversationTitle || 'New conversation'}
              </h1>
            </div>

            {/* Branch Badge Indicator */}
            {branches.length > 0 && (
              <div
                className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 inline-flex items-center gap-1 font-medium transition-colors hover:bg-emerald-500/15"
                title="Active Branch"
              >
                <GitBranch className="w-3 h-3 shrink-0" />
                {branches.length === 1 ? (
                  <span className="font-mono text-[11px] font-semibold">{branches[0].name}</span>
                ) : (
                  <select
                    value={selectedBranchId}
                    onChange={(e) => onSelectBranch?.(e.target.value)}
                    className="bg-transparent border-0 text-emerald-600 dark:text-emerald-400 font-mono text-[11px] font-semibold focus:outline-none cursor-pointer py-0 pl-0 pr-1"
                    aria-label="Select active conversation branch"
                  >
                    {branches.map((b) => (
                      <option key={b.id} value={b.id} className="bg-popover text-popover-foreground">
                        {b.name}
                      </option>
                    ))}
                  </select>
                )}
                {onNewBranch && (
                  <button
                    type="button"
                    onClick={onNewBranch}
                    title="Create new branch from here"
                    className="hover:scale-110 active:scale-95 text-emerald-600 dark:text-emerald-400 font-bold px-0.5 transition-transform"
                  >
                    <Plus className="w-3 h-3" />
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Right: Core Actions (Share, Theme Toggle, Modal Trigger Button) */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            {/* Subtle Secondary Share Action */}
            <button
              type="button"
              onClick={onShare}
              className="bg-muted hover:bg-muted/80 text-foreground border border-border/50 text-xs font-medium px-2.5 sm:px-3 py-1 rounded-md transition-all shadow-sm flex items-center gap-1.5 active:scale-[0.98]"
              title="Share conversation with collaborators"
            >
              <Share2 className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Share</span>
            </button>

            {/* Ghost Theme Toggle */}
            <button
              type="button"
              onClick={onToggleTheme}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
              className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted/80 transition-colors"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>

            {/* Options & Quick Actions Modal Button */}
            <button
              type="button"
              onClick={() => setIsOptionsModalOpen(true)}
              aria-haspopup="dialog"
              aria-expanded={isOptionsModalOpen}
              aria-label="Options & Quick Actions"
              title="Options & Quick Actions"
              className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted/80 transition-colors"
            >
              <MoreVertical className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* ========================================================
            BOTTOM ROW: Sub-header Toolbar (Session Metrics & Segmented Views)
            ======================================================== */}
        <div className="flex items-center justify-between px-3 sm:px-4 py-1.5 bg-muted/20 text-xs text-muted-foreground border-t border-border/30 min-h-[30px] gap-2 overflow-x-auto">
          {/* Left Side: Session Telemetry Metrics */}
          <button
            type="button"
            onClick={onOpenTelemetry}
            title="View AI telemetry & token metrics"
            className="font-mono text-[11px] opacity-80 hover:opacity-100 hover:text-foreground transition-opacity flex items-center gap-1.5 cursor-pointer bg-transparent border-0 p-0 shrink-0"
          >
            <Timer className="w-3 h-3 text-emerald-500" />
            <span>
              <span className="hidden sm:inline">{telemetry.latencyMs} ms · </span>
              <span className="font-medium text-foreground">
                {displayTokens >= 1000 ? `${(displayTokens / 1000).toFixed(1)}k` : displayTokens.toLocaleString()} tok
              </span>
              <span className="hidden sm:inline"> · {formattedCost}</span>
            </span>
          </button>

          {/* Right Side: View Mode Segmented Control */}
          <div
            role="group"
            aria-label="Workspace views"
            className="flex items-center bg-muted/60 p-0.5 rounded-lg border border-border/40 gap-0.5 shrink-0"
          >
            {/* Context Segment */}
            <button
              type="button"
              onClick={() => handleTabClick('context')}
              aria-pressed={activeTab === 'context'}
              className={`flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded transition-all ${
                activeTab === 'context'
                  ? 'bg-background text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Pin className="w-3 h-3" />
              <span className="hidden xs:inline">Context</span>
            </button>

            {/* Timeline Segment */}
            <button
              type="button"
              onClick={() => handleTabClick('timeline')}
              aria-pressed={activeTab === 'timeline'}
              className={`flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded transition-all ${
                activeTab === 'timeline'
                  ? 'bg-background text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Clock className="w-3 h-3" />
              <span className="hidden xs:inline">Timeline</span>
            </button>

            {/* Compare Segment */}
            <button
              type="button"
              onClick={() => handleTabClick('compare')}
              aria-pressed={activeTab === 'compare'}
              className={`flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded transition-all ${
                activeTab === 'compare'
                  ? 'bg-background text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Scale className="w-3 h-3" />
              <span className="hidden xs:inline">Compare</span>
            </button>
          </div>
        </div>
      </header>

      {/* ========================================================
          CENTERED GLASSMORPHIC MODAL DIALOG POPUP
          ======================================================== */}
      {isOptionsModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-headline"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 transition-opacity animate-in fade-in duration-150"
          onClick={(e) => {
            // Backdrop click closes modal
            if (e.target === e.currentTarget) {
              setIsOptionsModalOpen(false);
            }
          }}
        >
          {/* Centered Modal Card Container */}
          <div
            ref={modalRef}
            className="w-full max-w-md bg-background/95 border border-border/80 rounded-2xl shadow-2xl overflow-hidden backdrop-blur-md animate-in fade-in zoom-in-95 duration-150 flex flex-col"
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between p-5 border-b border-border/50">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-primary/10 text-primary grid place-items-center">
                  <Settings className="w-4 h-4" />
                </div>
                <div>
                  <h2 id="modal-headline" className="text-base font-semibold text-foreground">
                    Options & Quick Actions
                  </h2>
                  <p className="text-xs text-muted-foreground">Manage workspace, forks, and history</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsOptionsModalOpen(false)}
                aria-label="Close dialog"
                className="p-1.5 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Body Content (Action Cards List) */}
            <div className="p-5 space-y-3">
              {/* 1. Move to Project Card */}
              <div className="flex items-center justify-between p-3 rounded-xl border border-border/40 hover:border-primary/40 hover:bg-muted/40 transition-all">
                <div className="flex items-center gap-3 min-w-0 flex-1">
                  <div className="w-9 h-9 rounded-lg bg-blue-500/10 text-blue-500 flex items-center justify-center shrink-0">
                    <FolderInput className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-foreground">Move to Project</div>
                    <div className="text-xs text-muted-foreground truncate">
                      Organize into a dedicated workspace
                    </div>
                  </div>
                </div>
                <div className="shrink-0 ml-3">
                  <select
                    value={selectedProjectId || ''}
                    onChange={(e) => {
                      onMoveToProject?.(e.target.value || null);
                      setIsOptionsModalOpen(false);
                    }}
                    className="text-xs rounded-lg border border-input bg-background/90 px-2.5 py-1.5 text-foreground focus:outline-none focus:ring-1 focus:ring-primary cursor-pointer max-w-[130px] truncate"
                  >
                    <option value="">None (General)</option>
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* 2. Share & Collaborate Card */}
              <button
                type="button"
                onClick={() => {
                  setIsOptionsModalOpen(false);
                  onShare?.();
                }}
                className="w-full text-left flex items-center justify-between p-3 rounded-xl border border-border/40 hover:border-primary/40 hover:bg-muted/40 transition-all cursor-pointer group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
                    <Share2 className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-foreground group-hover:text-primary transition-colors">
                      Share & Collaborate
                    </div>
                    <div className="text-xs text-muted-foreground truncate">
                      Invite team members or copy snapshot link
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-muted-foreground group-hover:text-primary group-hover:translate-x-0.5 transition-all shrink-0" />
              </button>

              {/* 3. Fork Solution Branch Card */}
              <button
                type="button"
                onClick={() => {
                  setIsOptionsModalOpen(false);
                  onNewBranch?.();
                }}
                className="w-full text-left flex items-center justify-between p-3 rounded-xl border border-border/40 hover:border-primary/40 hover:bg-muted/40 transition-all cursor-pointer group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
                    <GitBranch className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-foreground group-hover:text-primary transition-colors">
                      Fork Solution Branch
                    </div>
                    <div className="text-xs text-muted-foreground truncate">
                      Explore alternative fixes in an isolated branch
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-muted-foreground group-hover:text-primary group-hover:translate-x-0.5 transition-all shrink-0" />
              </button>

              {/* 4. Clear Messages Destructive Card */}
              <button
                type="button"
                onClick={() => {
                  setIsOptionsModalOpen(false);
                  onClearConversation?.();
                }}
                className="w-full text-left flex items-center justify-between p-3 rounded-xl border border-border/40 hover:border-red-500/30 hover:bg-red-500/10 transition-all cursor-pointer group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-red-500/10 text-red-500 flex items-center justify-center shrink-0">
                    <Trash2 className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-red-500">Clear Conversation</div>
                    <div className="text-xs text-muted-foreground truncate">
                      Permanently remove all messages in this thread
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-red-400 group-hover:translate-x-0.5 transition-transform shrink-0" />
              </button>
            </div>

            {/* Modal Footer */}
            <div className="flex justify-end px-5 py-3.5 bg-muted/20 border-t border-border/40">
              <button
                type="button"
                onClick={() => setIsOptionsModalOpen(false)}
                className="px-4 py-2 text-xs font-medium bg-muted hover:bg-muted/80 text-foreground rounded-lg transition-colors cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export const ResponsiveHeader = TopHeader;

export default TopHeader;
