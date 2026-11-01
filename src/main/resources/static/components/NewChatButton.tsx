import React from 'react';
import { Plus } from 'lucide-react';
import { NewChatButtonProps } from './NewChatButton.types';

/**
 * NewChatButton: Prominent, full-width secondary action button for starting a fresh chat session.
 *
 * Implements subtle/secondary styling (`bg-muted hover:bg-muted/80 text-foreground border border-border/50`)
 * to maintain balanced visual hierarchy alongside the primary execution element (Send button).
 */
export const NewChatButton: React.FC<NewChatButtonProps> = ({
  onClick,
  shortcut = '⌘N',
  title = 'New chat (⌘N or Ctrl+Shift+O)',
  disabled = false,
  className = ''
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={`bg-muted hover:bg-muted/80 text-foreground border border-border/50 rounded-xl px-3 py-2.5 shadow-sm font-medium text-sm flex items-center justify-between w-full transition-all active:scale-[0.99] disabled:opacity-50 cursor-pointer ${className}`}
    >
      <div className="flex items-center gap-2">
        <Plus className="w-4 h-4 shrink-0 text-muted-foreground" strokeWidth={2.2} />
        <span>New Chat</span>
      </div>
      <kbd className="bg-background text-muted-foreground border border-border/60 text-[10px] px-1.5 py-0.5 rounded font-mono font-medium tracking-wide">
        {shortcut}
      </kbd>
    </button>
  );
};

export default NewChatButton;
