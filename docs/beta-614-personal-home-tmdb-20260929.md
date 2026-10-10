# Beta 614 — portada personal, orden de canales y TMDB

Instalada en 192.168.93.190:5555, com.drbep.tvplayer.offline, versionName 2.0.614-beta-home-tmdb.

- Canales de En directo: mantener OK inicia mover; flechas cambian posición en la cuadrícula (izquierda/derecha una posición, arriba/abajo una fila), OK guarda, Atrás cancela. Se ordena dentro de cada categoría; búsqueda activa no permite iniciar movimiento.
- Orden de canales y plataformas guardado por servidor/usuario y sincronizado con preferencias compartidas. Pendientes locales se reenvían tras recuperar conexión. Las listas se construyen después de filtrar derechos.
- Continuar viendo: todas las tarjetas 16:9. Panorámica TMDB cuando hay coincidencia inequívoca; imagen de catálogo como alternativa y composición de póster vertical sobre fondo desenfocado si hace falta.
- Películas/Series para descubrir: tamaño vertical uniforme.
- Destacado rotativo con títulos disponibles del catálogo autorizado, repartidos entre proveedores. TMDB enriquece imágenes, no concede derechos ni aporta un catálogo de reproducción independiente. El tono de fondo se deriva de la imagen.

Verificación real: La 1 UHD movida delante de La 1 en TDT; cancelar otro movimiento conserva el orden; force-stop/reinicio conserva La 1 UHD primero. Confirmado en DB servidor usuario 3, channels:group:tdt, 44 IDs. Se deja esa posición de prueba.
Capturas: /tmp/drbep-614-home.png, /tmp/drbep-614-movies.png, /tmp/drbep-614-home-tmdb-final.png.
9 tests Android de ordenación/permisos correctos. Compilación release y firma verificadas.
Release SHA-256: c5e0fd4e9a999ccff74fff4e2f537da1921c5e5394838bd005179091600b9d9e.
APK dispositivo SHA-256: a8ab1ffc61697dc2e9c587c48f19002a0e54ade9e2279515b3f204a54246d321.
No se ha publicado en el canal beta general ni se ha hecho commit/push.
