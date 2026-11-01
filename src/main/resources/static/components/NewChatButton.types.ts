export interface NewChatButtonProps {
  /** Callback triggered when user clicks "+ New Chat" */
  onClick: () => void;
  /** Display label for keyboard shortcut (default: '⌘N' or 'Ctrl+Shift+O') */
  shortcut?: string;
  /** Button title tooltip text */
  title?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Optional custom CSS classes */
  className?: string;
}
