# Ladies Apparel — E-Commerce Backend

Production-grade Spring Boot monolith powering a D2C ladies-apparel e-commerce platform.

**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL · Spring Security (JWT) · Razorpay · Cloudinary · JavaMailSender

---

## Modules

| Package | Responsibility |
|---|---|
| `auth` | Registration, email OTP verification, login (JWT), forgot/reset password |
| `otp` | OTP generation, expiry, throttling (shared by auth + future flows) |
| `email` | Transactional email (OTP, order confirmation) |
| `category` | Category tree (self-referencing, for sub-categories) |
| `product` | Products, variants (size/color/SKU/stock), images, filtered search |
| `media` | Cloudinary upload/delete wrapper |
| `cart` | Per-user cart with live stock/price validation |
| `wishlist` | Saved products |
| `address` | Address book with auto default-address handling |
| `coupon` | Flat/percentage discount engine with per-user and global usage limits |
| `order` | Order placement, snapshot line items, cancellation, admin status pipeline |
| `payment` | Razorpay order creation, signature verification, webhook, refunds |
| `review` | Ratings & reviews with moderation queue and verified-purchase badge |
| `dashboard` | Admin summary, top products, low-stock alerts, sales report, CSV export |
| `banner` | Homepage banner/CMS management (admin-manageable marketing content) |
| `serviceability` | Pincode delivery-zone checks (COD eligibility, delivery estimate) |
| `security` / `config` / `common` | JWT filter chain, rate limiting, audit logging, exception handling, shared response wrappers |

### Hardening added on top of the core feature set
- **Refresh tokens** — short-lived access tokens + a rotating refresh token (`POST /api/auth/refresh-token`), so sessions don't force a re-login every 24h.
- **Account lockout** — 5 failed logins locks the account for 15 minutes; admins can also hard-block an account (`PATCH /api/admin/customers/{id}/block`).
- **Rate limiting** on login/register/OTP endpoints (in-memory sliding window; swap for Redis+Bucket4j if you scale past one instance).
- **Audit logging** — every mutating `/api/admin/**` request is recorded (who, what, when) — `GET /api/admin/audit-logs`.
- **Swagger/OpenAPI** — interactive docs at `/swagger-ui.html`, with a JWT bearer-auth button built in.
- **Docker** — `Dockerfile` + `docker-compose.yml` included (backend + Postgres).
- **Order-status emails** — customers are now emailed on Shipped / Out for Delivery / Delivered / Cancelled, not just at order placement.


---

## Setup

### 1. Prerequisites
- Java 21, Maven
- PostgreSQL running locally (or update `datasource.url`)
- A Razorpay account (test mode keys are fine to start)
- A Cloudinary account
- An email account with an app password (e.g. Gmail)

### 2. Database
```sql
CREATE DATABASE ladies_apparel_db;
```

### 3. Configure `src/main/resources/application.yml`
Fill in real values for:
- `spring.datasource.password`
- `spring.mail.username` / `spring.mail.password` (Gmail App Password, not your real password)
- `app.jwt.secret` — any long random string (256-bit+)
- `cloudinary.*`
- `razorpay.*` (key-id, key-secret, webhook-secret — webhook-secret must match what you set in Razorpay Dashboard → Webhooks)

### 4. Run

**Option A — Maven directly:**
```bash
mvn spring-boot:run
```

**Option B — Docker Compose (backend + Postgres in containers):**
```bash
docker compose up --build
```
(Fill in real secrets via environment variables or a `.env` file before doing this for anything beyond local testing — don't bake credentials into the image.)

The API starts on `http://localhost:8080`. Tables are auto-created via `hibernate.ddl-auto: update`. Interactive API docs: `http://localhost:8080/swagger-ui.html`.

---

## API surface (high level)

- `POST /api/auth/register`, `/verify-otp`, `/login`, `/refresh-token`, `/forgot-password`, `/reset-password`
- `GET /api/public/categories`, `/api/public/products`, `/api/public/products/{slug}`, `/api/public/banners`
- `GET /api/public/serviceability/{pincode}`
- `GET/POST/PUT/DELETE /api/cart`, `/api/wishlist`, `/api/addresses`
- `POST /api/orders`, `GET /api/orders`, `POST /api/orders/{orderNumber}/cancel`
- `POST /api/payments/razorpay/create-order/{orderNumber}`, `/verify`, `/webhook`
- `POST /api/reviews`, `GET /api/public/products/{productId}/reviews`
- `POST/PUT/DELETE /api/admin/products`, `/api/admin/categories`, `/api/admin/coupons`, `/api/admin/banners`, `/api/admin/serviceability`
- `GET /api/admin/customers`, `PATCH /api/admin/customers/{id}/block` / `/unblock`
- `GET /api/admin/dashboard/summary`, `/top-products`, `/low-stock`
- `GET /api/admin/reports/sales`, `/api/admin/reports/orders/export` (CSV)
- `GET /api/admin/audit-logs`

All `/api/admin/**` routes require an `ADMIN` or `SUPER_ADMIN` JWT role. Admin users must currently be promoted directly in the database (`UPDATE users SET role = 'ADMIN' WHERE email = '...'`) — a dedicated admin-promotion flow can be added later if needed.

---

## Design notes

- **Order line items are frozen snapshots** (name, size, price, image) at the moment of purchase — editing or deleting a product later never rewrites order history.
- **Pricing-sensitive values are always computed server-side** (cart subtotal, coupon discount, shipping) — the client never gets to supply a price.
- **Stock is deducted at order placement and restored on cancellation.**
- **COD orders auto-flip to `PAID`** when marked `DELIVERED`; Razorpay orders are confirmed via signature verification **and** a webhook fallback, so a closed browser tab can never leave an order stuck `PENDING` after money was actually captured.
- **Soft-delete throughout** (`active = false`) for products/categories/coupons — nothing that's ever been part of an order disappears from the database.
