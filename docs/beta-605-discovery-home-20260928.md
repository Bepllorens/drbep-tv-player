# Beta 605 · Portada de descubrimiento

Primera implementación nativa de la propuesta de portada DRBEP para televisión.
Destino de prueba: Fire Stick 192.168.93.190. No se publica en el canal general.

- Navegación fija: Inicio, En directo, Películas, Series y Mi biblioteca.
- Cambiar de pestaña con el mando no inicia reproducción.
- Destacado con metadatos y carátula reales; sin carrusel automático.
- Filtros por plataforma VOD y por operador de televisión. Logotipos configurados,
  iconos oficiales para Netflix/Disney/Apple y nombre de respaldo.
- Continuar viendo conserva posiciones y separa películas y series.
- Catálogo local y primera página de metadatos privados (Netflix, HBO, Apple TV)
  cargada en segundo plano. Ver catálogo abre el navegador completo existente.
- Series privadas abren episodios directamente desde la portada.
- Mi biblioteca agrupa progreso, grabaciones, favoritos y accesos existentes.
- Se conserva la sección, el filtro y el desplazamiento vertical durante la sesión.

La actualización es in situ, sin desinstalar ni modificar las sesiones o el DRM.
El APK para este dispositivo usa su certificado existente; su contenido se compara
entrada por entrada con el APK release, excluyendo únicamente META-INF.

Validación: compilación release, firma y SHA-256; 16 pruebas unitarias seleccionadas
(portada, cierre de peticiones, navegador privado y presentación Netflix).
En dispositivo: las cinco secciones, filtro HBO, apertura de episodios de Colisión,
y vuelta conservando Series/HBO. La comprobación del dispositivo usa accesibilidad,
coordenadas de elementos y eventos del mando; queda pendiente la valoración visual
personal del usuario. No se revalida toda la reproducción de proveedores en esta beta.

Los catálogos privados muestran una selección inicial, no todo su catálogo en memoria.
Los errores de carga mantienen disponibles los accesos a los navegadores existentes.

Artefactos finales:
- Release universal SHA-256: `281eec2f5e3d7436059a1b07e02c704372d2601d546c13996e23d739433f5677`.
- APK compatible con .190 SHA-256: `0b6e8fd90bcd4240935366326ca6ab94b00b25e7f7f475bbb4ea9854a21f43d8`.
- Versión: `2.0.605-beta-discovery-home`, código 605.
- Cinco controles adicionales de foco/carga de carátulas/local-first correctos.
- Mando real: izquierda a Series, abajo y OK abren HBO Series desde el destacado.

Fuentes de iconos de respaldo: páginas públicas de Disney+ y tv.apple.com
(apple-touch-icon), y assets.nflxext.com (nficon2016.png). El resto reutiliza los
logotipos de plataformas del catálogo y las fuentes previamente configuradas.
