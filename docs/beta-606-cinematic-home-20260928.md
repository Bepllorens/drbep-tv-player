# Beta 606 · Revisión visual de la portada

Revisión de la beta 605 a partir de la valoración del usuario en el Fire Stick.

- Fondos negro/carbón y tipografía blanca; foco blanco y selección neutra.
- Cabecera en una fila para dar más espacio al contenido.
- Tarjetas 16:9 con imágenes a sangre, títulos superpuestos sobre degradado y
  progreso sobre la imagen. El escalado conserva la proporción (centerCrop).
- Servicios con logotipos grandes, fondo de marca y sin el rótulo duplicado debajo.
  Netflix utiliza su marca completa; Disney y Apple usan sus imágenes horizontales
  públicas de presentación, inspeccionadas antes de integrarlas.
- Destacado a todo el ancho. El cargador comprueba las proporciones reales entre
  hasta ocho candidatos y elige una imagen horizontal. Mantiene asociados imagen,
  título y acción, y fija la elección hasta cambiar de sección/filtro.
- Si ningún candidato tiene imagen horizontal válida, usa recorte proporcional
  como respaldo. No estira la imagen ni sustituye el arte por otra película.

Se mantiene el alcance local de prueba en .190 y las sesiones existentes.
No se cambia el backend ni se consulta autenticación o reproducción de proveedores
para cargar imágenes. El destacado resuelve imágenes promocionales públicas de Netflix y Apple TV
mediante sus páginas oficiales, con un cliente sin cookies ni credenciales. Se
limita la lectura HTML a 2 MB conservando los metadatos presentes en ese tramo;
se validan el identificador, el dominio de la página y el dominio de la imagen.
Si no hay arte horizontal, se conserva el respaldo del catálogo. No se aumenta
artificialmente la resolución de los originales.

APK release universal: `edcd990ccad5fc8fe07c6869a49b9f1f0b966b56357b2ef830cd07a8a5dc65b6`.
APK compatible .190: `e26a15961b86cc3f4e96981bcb9d3c1a4ae2fa943825c54d086538dd92bcc797`.
Firma release verificada; contenido del APK para .190 idéntico salvo firma META-INF.
Rollback local conservado: beta 605 firmada para el mismo dispositivo.

Cinco comprobaciones de foco, carga de imágenes y renderizado local correctas.

Verificación final en .190: Inicio muestra Unabomber y el cargador confirma
un fondo horizontal real de 1280×720 (candidato 0). Título, acción y progreso
56:37 corresponden a la misma película. Sin errores fatales en el proceso actual.
La navegación Inicio/Directo/Películas mediante mando se comprobó en esta revisión.
Las 16 pruebas unitarias seleccionadas de navegación/presentación pasaron antes
de añadir el resolvedor público; las cinco comprobaciones estáticas también pasan
con el código final. Compilación release y firma verificadas; instalación -r
correcta, sin borrar datos. Inicio queda abierto. No se publica el canal general.

Imagen pública de referencia: https://www.netflix.com/es/title/82010386
Otros catálogos pueden seguir limitados a miniaturas de 480 px o carteles verticales.
