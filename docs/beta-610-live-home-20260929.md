# Beta 610 — En directo

## Comportamiento

La pestaña En directo usa una lista lateral de plataformas y grupos, una cuadrícula de canales con logos y EPG y un panel de previsualización 16:9. Al entrar se conserva el canal sintonizado, si pertenece al catálogo autorizado. Si no hay un canal seleccionado en la categoría, se espera al foco del usuario sin arrancar el primero del catálogo. Al mover el foco se selecciona el canal; tras 450 ms se usa el reproductor existente. OK abre pantalla completa. Atrás cierra primero los controles que estén abiertos y después regresa a En directo conservando el canal. La EPG visible se actualiza cada 15 segundos. La búsqueda se limita a los canales de la categoría visible.

Los accesos virtuales (`drbep-action:`, como el menú de competiciones DAZN) no son emisiones y quedan fuera de esta parrilla.

## Permisos

El catálogo suministrado por el servidor sigue siendo la fuente de canales autorizados (permisos por plataforma, grupo o canal individual; denegaciones aplicadas en servidor). El cliente no deduce el acceso a una plataforma de la presencia de un único canal. La barra lateral usa `permissions.admin`, `platform_ids` y `groups`, y oculta las categorías sin canales visibles. Los canales con acceso individual permanecen en Todos. Se mantiene el filtro parental y se vuelve a comprobar la pertenencia al catálogo vigente al previsualizar o abrir un canal.

Se persisten los metadatos de acceso en el formato binario 14; la caché serializada pasa a versión 3. Las cachés incompatibles se regeneran desde el snapshot, sin borrar cuentas ni datos del dispositivo.

## Validación

- Pruebas unitarias de permisos: grupo sin plataforma, plataforma sin grupo, concesión individual, directo deshabilitado, metadatos ausentes, administrador y accesos virtuales.
- Roundtrip de permisos en caché junto a las pruebas de formatos anteriores.
- Cinco comprobaciones existentes de foco, imágenes y arranque local.
- Demo privada con datos ficticios: navegación, categorías, búsqueda y acción de pantalla completa comprobadas en navegador local.
- Publicación demo: `acce89e5db42506a6a7303d8e749d0d2847307cf`.
- Demo: https://drbep-portada-demo.bepllorens50167.chatgpt.site

## Binarios

- Release: `3e57afafe26dfb378c84f703a29f7a90d26bbf4e69a189e7680a3f7d2c58ccd4`.
- APK firmado para el dispositivo: `72fc701ee46bdbe764bc2a63b17b111fd67980e2232b5ada2f9b642a0521cb61`.
- Firma esperada y payload idéntico entre variantes comprobados.
- Pruebas: 10 de CatalogSnapshotStore y 6 de LiveHomePolicy, sin errores; 5 comprobaciones estáticas existentes correctas.
- Compilación release final correcta (1m08s). Se retiraron 469 archivos `.class` duplicados generados bajo `app/build/intermediates`; uno bloqueaba la lectura de Gradle. No se modificaron fuentes para resolver esa incidencia.

Comprobaciones reales en Fire Stick .190: LA 1 visible en previsualización y en pantalla completa; Atrás devuelve a En directo con LA 1 seleccionado. El filtro Movistar reduce 3.055 canales a 156. DPAD derecha cambia la selección a LA 2. La EPG de LA 1 cambió de programa durante la prueba, actualizando la ficha. En el binario final, al reiniciar y entrar en En directo se conservó LA 2. Se repitió Movistar → LA 1 → pantalla completa → Atrás: volvió a la misma categoría (156 canales) y a LA 1. Captura real final: `/tmp/drbep-fire190-610-final.png`. No se ha publicado al canal general ni hecho commit/push del repositorio Android.
