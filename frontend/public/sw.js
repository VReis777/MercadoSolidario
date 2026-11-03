// Service worker mínimo — instalação PWA + cache leve dos arquivos do app.
const CACHE = 'ms-app-v1';
const APP_SHELL = ['/', '/manifest.webmanifest', '/logo-mercado-solidario.png'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
  );
  self.clients.claim();
});

// Dados/API sempre da rede (app depende de dados frescos);
// arquivos estáticos usam cache quando a rede falha.
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.pathname.startsWith('/api/')) return;
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request).then((cached) => {
      // Rotas SPA: volta para o index em modo offline.
      if (event.request.mode === 'navigate') return caches.match('/');
      return cached;
    }))
  );
});
