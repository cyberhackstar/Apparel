export type NotificationType = 'ORDER_UPDATE' | 'PROMOTION' | 'GENERAL';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  link?: string;
  read: boolean;
  createdAt: string;
}
