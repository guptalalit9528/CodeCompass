import React, { useEffect, useRef } from 'react';
import { AlertTriangle, X, Loader2 } from 'lucide-react';
import { DeleteProjectModalProps } from './DeleteProjectModal.types';

/**
 * DeleteProjectModal: Centered, high-emphasis confirmation Modal Dialog for project deletion.
 *
 * Designed with modern glassmorphism, explicit warning callout, and protection against accidental deletion.
 */
export const DeleteProjectModal: React.FC<DeleteProjectModalProps> = ({
  isOpen,
  projectId,
  projectName,
  isDeleting = false,
  onClose,
  onConfirm
}) => {
  const cardRef = useRef<HTMLDivElement>(null);

  // Close modal on Escape key press (only if not currently deleting)
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isDeleting) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, isDeleting, onClose]);

  if (!isOpen || !projectId) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-project-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 transition-opacity animate-in fade-in duration-150"
      onClick={(e) => {
        // Backdrop overlay click closes dialog without deleting
        if (e.target === e.currentTarget && !isDeleting) {
          onClose();
        }
      }}
    >
      {/* Centered Modal Card Container */}
      <div
        ref={cardRef}
        className="w-full max-w-md bg-background/95 border border-red-500/20 dark:border-red-500/30 rounded-2xl shadow-2xl overflow-hidden backdrop-blur-md animate-in fade-in zoom-in-95 duration-150 flex flex-col"
      >
        {/* Modal Header & Icon Badge */}
        <div className="flex items-start justify-between p-6 pb-4 border-b border-border/40 gap-4">
          <div className="flex items-center gap-3.5">
            {/* Soft Red Warning Badge */}
            <div className="p-3 rounded-xl bg-red-500/10 text-red-500 w-fit shrink-0 border border-red-500/20">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h2 id="delete-project-title" className="text-lg font-semibold text-foreground">
                Delete Project?
              </h2>
              <p className="text-xs text-muted-foreground">Permanent action requiring confirmation</p>
            </div>
          </div>
          {/* Top-Right Ghost Close Button */}
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            aria-label="Close dialog"
            className="p-1.5 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-50"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body Content */}
        <div className="p-6 space-y-4">
          <p className="text-sm text-muted-foreground leading-relaxed">
            Are you sure you want to delete{' '}
            <span className="font-semibold text-foreground">"{projectName}"</span>? This action cannot be undone
            and will permanently remove all associated chats, branches, and session data.
          </p>

          {/* Visual Accent: Subtle light-red warning callout box */}
          <div className="bg-red-500/5 border border-red-500/15 rounded-xl p-3 text-xs text-red-600 dark:text-red-400 flex items-start gap-2.5">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-red-500" />
            <div className="leading-snug">
              <strong>Warning:</strong> Any conversations, branches, and pinned context associated with this
              project will be deleted from your workspace.
            </div>
          </div>
        </div>

        {/* Action Footer Buttons */}
        <div className="flex items-center justify-end gap-2.5 px-6 py-4 bg-muted/20 border-t border-border/40">
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="px-4 py-2 text-xs font-medium bg-muted hover:bg-muted/80 rounded-lg text-foreground transition-colors disabled:opacity-50 cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => onConfirm(projectId)}
            disabled={isDeleting}
            className="px-4 py-2 text-xs font-semibold bg-red-600 hover:bg-red-700 text-white rounded-lg shadow-sm transition-colors flex items-center gap-2 disabled:opacity-60 cursor-pointer"
          >
            {isDeleting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>{isDeleting ? 'Deleting...' : 'Delete Project'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeleteProjectModal;
