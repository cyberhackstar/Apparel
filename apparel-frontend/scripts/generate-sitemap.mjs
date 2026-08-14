/**
 * Generates public/sitemap.xml dynamically from your live Spring Boot API.
 * Run this during deployment before `ng build`.
 *
 * Usage:
 *   API_URL=https://api.bhawesh.shop/api SITE_URL=https://apparel.bhawesh.shop node scripts/generate-sitemap.mjs
 */

const API_URL = process.env['API_URL'] || 'http://localhost:8080/api';
const SITE_URL = process.env['SITE_URL'] || 'https://apparel.bhawesh.shop';
const OUTPUT_PATH = new URL('../public/sitemap.xml', import.meta.url);

function escapeXml(unsafe) {
  return String(unsafe).replace(/[<>&'"]/g, (c) => {
    switch (c) {
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '&':
        return '&amp;';
      case "'":
        return '&apos;';
      case '"':
        return '&quot;';
    }
  });
}

function urlEntry(loc, changefreq, priority, lastmod = new Date().toISOString().split('T')[0]) {
  return `  <url>
    <loc>${escapeXml(loc)}</loc>
    <lastmod>${lastmod}</lastmod>
    <changefreq>${changefreq}</changefreq>
    <priority>${priority}</priority>
  </url>`;
}

async function fetchAllProducts() {
  const products = [];
  let page = 0;
  const size = 50;

  try {
    while (true) {
      const res = await fetch(`${API_URL}/public/products?page=${page}&pageSize=${size}`);
      if (!res.ok) throw new Error(`HTTP ${res.status} while fetching products at page ${page}`);

      const json = await res.json();
      const pagedData = json.data;
      if (!pagedData || !Array.isArray(pagedData.content)) break;

      products.push(...pagedData.content);
      if (pagedData.last || products.length >= pagedData.totalElements) break;
      page++;
    }
  } catch (err) {
    console.warn(
      `[Warning] Could not fetch products from API (${err.message}). Generating sitemap with static pages only.`,
    );
  }

  return products;
}

async function fetchAllCategories() {
  const flat = [];
  try {
    const res = await fetch(`${API_URL}/public/categories`);
    if (!res.ok) throw new Error(`HTTP ${res.status} while fetching categories`);

    const json = await res.json();
    const categories = json.data || [];

    for (const cat of categories) {
      flat.push(cat);
      if (cat.subCategories && Array.isArray(cat.subCategories)) {
        flat.push(...cat.subCategories);
      }
    }
  } catch (err) {
    console.warn(`[Warning] Could not fetch categories from API (${err.message}).`);
  }

  return flat;
}

async function main() {
  console.log(`[SEO] Generating sitemap using API: ${API_URL}`);

  const today = new Date().toISOString().split('T')[0];

  // Core Static & Trust Pages
  const staticUrls = [
    urlEntry(`${SITE_URL}/`, 'daily', '1.0', today),
    urlEntry(`${SITE_URL}/products`, 'daily', '0.9', today),
    urlEntry(`${SITE_URL}/about-us`, 'monthly', '0.6', today),
    urlEntry(`${SITE_URL}/contact-us`, 'monthly', '0.6', today),
    urlEntry(`${SITE_URL}/shipping-and-returns`, 'monthly', '0.5', today),
    urlEntry(`${SITE_URL}/privacy-policy`, 'monthly', '0.4', today),
    urlEntry(`${SITE_URL}/terms-and-conditions`, 'monthly', '0.4', today),
  ];

  const [products, categories] = await Promise.all([fetchAllProducts(), fetchAllCategories()]);

  // Dynamic Categories (Filter Links)
  const categoryUrls = categories.map((cat) =>
    urlEntry(`${SITE_URL}/products?categoryId=${cat.id}`, 'weekly', '0.8', today),
  );

  // Dynamic Product Pages
  const productUrls = products.map((prod) =>
    urlEntry(`${SITE_URL}/products/${prod.slug}`, 'weekly', '0.8', today),
  );

  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${[...staticUrls, ...categoryUrls, ...productUrls].join('\n')}
</urlset>
`;

  const fs = await import('node:fs/promises');
  await fs.writeFile(OUTPUT_PATH, xml, 'utf-8');

  console.log(`[SEO] Sitemap successfully created at public/sitemap.xml`);
  console.log(`- ${staticUrls.length} Static Pages`);
  console.log(`- ${categoryUrls.length} Category Filters`);
  console.log(`- ${productUrls.length} Products`);
}

main().catch((err) => {
  console.error('[SEO] Sitemap generation failed:', err);
  process.exit(1);
});
