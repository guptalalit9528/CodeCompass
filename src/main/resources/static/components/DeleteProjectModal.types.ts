export interface DeleteProjectModalProps {
  /** Whether the modal is currently open */
  isOpen: boolean;
  /** ID of the project to be deleted */
  projectId: string | null;
  /** Name of the project to be deleted */
  projectName: string;
  /** Whether the delete API request is currently in flight */
  isDeleting?: boolean;
  /** Callback when user cancels or closes the modal */
  onClose: () => void;
  /** Callback when user confirms deletion */
  onConfirm: (projectId: string) => Promise<void> | void;
}
