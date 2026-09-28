# Controles Netflix de la app offline

La beta 599 reproducía mediante una actividad Media3 separada, con controles
básicos, una sola variante de vídeo y una única pista de audio. Faltaban las
opciones del mando y el retorno explícito a televisión. Además, elegía el menor
bitrate entre las variantes con mayor resolución, por lo que un búfer 3840×2160
no demostraba que se estuviera usando la variante UHD de mayor calidad.

La beta 600 añadió inicialmente controles provisionales; el usuario pidió
usar la misma interfaz que el resto de VOD. La beta 601 reutiliza
TouchControlsComposeBinder, TouchControlsBarUiModel, TimeshiftBarUiModel y
TvOptionsPanelComposeBinder. Incluye las acciones habituales: pausa, desplazamiento, calidad,
audio, subtítulos WebVTT, retorno al catálogo y retorno a televisión. OK o Menú
abre los controles. Atrás los oculta; con los controles ocultos abre las opciones
de salida. Los botones multimedia funcionan también con los controles ocultos.

La selección inicial prioriza resolución y después el mayor bitrate del
manifiesto autorizado. El menú de calidad ofrece cada variante disponible y
muestra bitrate nominal del perfil, que no debe confundirse con velocidad de
red ni con una medición del bitrate instantáneo. La resolución decodificada se
muestra por separado. No se añade ni simula HDR, Dolby Vision o una pista que
el manifiesto no ofrezca.

Cambiar calidad, audio o subtítulos reconstruye las fuentes conservando posición
y estado de pausa; puede ocasionar una breve carga. Se reutiliza la sesión CDM
nativa. La salida libera el reproductor antes de retornar a MainActivity, que
restaura el catálogo o sintoniza TV según el resultado. La cuenta vinculada y su
almacenamiento no cambian.

Validación: compilación release, lintVital y 15 pruebas seleccionadas superadas;
comprobación real de la beta 603 en Fire Stick completada.


Los subtítulos completos en español se ordenan primero. Las pistas forzadas
usan su idioma real en vez de la etiqueta Desactivados que trae el servicio;
las pistas de accesibilidad de texto se identifican como SDH, no como
Audiodescripción. Hay una prueba de regresión para ambas etiquetas y el orden.

En el Fire Stick, antes del cambio final de presentación, se verificaron:
- Sesión conservada y reanudación del título sin nuevo código.
- Variantes 3840×2160 a 11,76 y 8,03 Mb/s; la selección inicial usa 11,76.
- Cambio real a 1920×1080 a 5,51 Mb/s manteniendo la posición y avanzando.
- Idiomas Español (España), Español e Inglés original, entre otros.
- Pistas completas y forzadas de subtítulos; selección de Español (España).
- Volver a TV retorna a MainActivity y sintoniza La1.

La beta 601 sustituye los botones y AlertDialog provisionales por el HUD y los
paneles Compose compartidos. NetflixPlaybackActivity usa ComponentActivity
para proporcionar los propietarios de ciclo de vida que requieren esos mismos
componentes. No se modifica el aspecto de los demás proveedores.


El usuario confirmó en el Fire Stick que el panel de la beta 601 coincide con
el de los otros VOD. Después solicitó carátula y FPS. La beta 602 reutiliza la
petición autenticada de CatalogRepository mediante PrivateVodArtwork, pasando
solo origen y consulta de carátula entre actividades. No transporta tokens por
Intent. El lector de FPS/Hz es VodDisplayRate.label, el mismo que utiliza el
reproductor común. La carátula se verificó cargada en el dispositivo; la pista
no informa Format.frameRate, por lo que la beta 603 añade una estimación basada
en la mediana de intervalos entre timestamps de presentación de fotogramas.
Se etiqueta con ≈ y no se confunde con los Hz de salida.


La beta 603 también conecta Ficha VOD a VodDetailPanelComposeBinder, usando
los metadatos del elemento seleccionado (con comprobación del ID), sinopsis,
fecha/año, duración y carátula autenticada. Antes abría solo opciones técnicas.
Verificación real en Fire Stick 192.168.93.190:
- Instalación conservando datos: versionCode 603, 2.0.603-beta-netflix-frame-rate.
- Unabomber: carátula cargada, 3840×2160, HEVC, perfil 11.76 Mb/s,
  ≈ 24,000 fps; salida 59,940 Hz.
- Ficha VOD: título, año 2026 y 101 min tal como los aporta el catálogo,
  sinopsis completa sobre Ted Kaczynski, carátula y posición 38:34 / 1:40:06.
- 15 pruebas sin fallos: NetflixTracks 5, NetflixMediaUrls 3,
  NetflixFrameRate 4, PrivateVodBrowser 3; lintVital y release correctos.
- APK distribución SHA-256: 38bc71147395dca88b1c08e7a5ee76f788d09591d26836f3c5be4ca9bfc0fc8d.
- APK compatible con la firma instalada: 85f14e7760e3387a6e8d5fe08799e7fd63eb1776eb779e3d1d14ee40bdddd94a.
  Payload idéntico salvo firma, transferencia verificada antes de instalar.
- Solo instalado en el Fire Stick de prueba; no publicado en el canal general.


## Beta 604: controles compactos y progreso en portada

El panel VOD compartido (moderno y clásico) agrupa los tres transportes en un
bloque, manteniendo tres acciones de mando independientes y sus índices.
Audio/subtítulos pasan a Idiomas; TV, Biblioteca, Ficha y Calidad siguen visibles.
En TV la fila cabe sin scroll horizontal; las pantallas táctiles estrechas
pueden distribuir los grupos en varias filas. La TV en directo conserva su fila.

NetflixContinueStore guarda solo metadatos de presentación y progreso local,
separados de la sesión de cuenta. Se actualiza cada 10 segundos y al liberar el
reproductor. La portada consulta estos registros con el permiso Netflix y permite
reanudar directamente; los títulos terminados dejan de aparecer como pendientes.
No se añade sincronización del progreso entre dispositivos. Las reproducciones
anteriores conservan su posición y obtienen la tarjeta al volver a reproducirlas.

Validación real en Fire Stick .190, versionCode 604 / 2.0.604-beta-vod-controls:
- Todos los controles visibles en la misma fila, x=232..1688 sobre 1920 px.
- Transportes agrupados x=888..1248, con tres zonas de 120 px.
- Avance 51:55 -> 52:25 y retroceso; Idiomas abre Audio/Subtítulos y OK del
  mando abre la lista de audio con Español (España) seleccionado.
- Tras salir a TV y reiniciar la app, portada: Unabomber, Visto hasta 51:55.
- La tarjeta reanuda y avanza a 52:10; 3840×2160, 11.76 Mb/s y ≈24 fps.
- Verificación mediante accesibilidad. La revisión automática bloqueó una
  captura de la portada por posible contenido privado; no se intentó eludirla.
- 36 pruebas seleccionadas sin fallos, lintVital y build release correctos.
- Se eliminaron únicamente clases generadas duplicadas para resolver el fallo R8.
- SHA-256 distribución: 5a2e82168da380d529ae0ad12e63248433a26773e520e019ccf4dd58301badfd.
- SHA-256 APK instalado: 95a6eea01dbf109cf4b2fa22b854924c691ec4c60b72c3d861fef681092b9075.
  Firma compatible con la instalación, payload idéntico salvo firma y transferencia
  verificada. Instalación con datos conservados, sin publicación general.
