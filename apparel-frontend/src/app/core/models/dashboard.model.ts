export interface DashboardSummary {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  todayRevenue: number;
  todayOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
}

export interface TopProduct {
  productId: number;
  productName: string;
  totalSold: number;
}

export interface LowStockVariant {
  variantId: number;
  productName: string;
  size: string;
  color: string;
  sku: string;
  stockQuantity: number;
}

export interface DailySales {
  date: string;
  revenue: number;
  ordersCount: number;
}
