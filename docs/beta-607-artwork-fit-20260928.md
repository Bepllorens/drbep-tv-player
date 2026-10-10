# Beta 607 · Encuadre de imágenes

Corrección solicitada tras probar la portada 606 en el Fire Stick .190.

- Las imágenes de tarjetas usan fitCenter con el tamaño de destino: se conserva
  el contenido y la proporción, sin ampliar para recortar los bordes.
- Los carteles verticales dejan espacio neutro lateral dentro de la tarjeta 16:9.
- El destacado utiliza FIT_END para mostrar la imagen completa alineada a la
  derecha, manteniendo la zona de texto a la izquierda.
- Los logos continúan ajustados proporcionalmente; Disney y Apple comparten
  ahora el ajuste sin recorte de las imágenes de portada.

Cinco comprobaciones existentes de foco, imágenes y carga local pasan.
Sin cambios de reproducción, sesiones o backend. Destino: beta local en .190.

Instalada en .190 mediante actualización -r, versionCode 607 confirmado.
Inicio muestra Unabomber y la imagen horizontal 1280×720 carga correctamente.
Sin errores fatales en el proceso comprobado. Verificación de estructura por
accesibilidad; valoración visual final pendiente del usuario en la televisión.
SHA-256 release: 88a9251316583f0500e70bc699ab4f7ea344bbfa2612239427f2b13d0d73520c
SHA-256 APK .190: a187c624c054ca5e37cb6c6feaebd6ebbe9e538fb70c8541456029cb5f4181db
Firma y payload comprobados. Se conserva APK 606 para rollback.
