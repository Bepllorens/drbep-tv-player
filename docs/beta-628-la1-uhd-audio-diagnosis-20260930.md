# La 1 UHD en Fire Stick .190 — diagnóstico de audio (30-09-2026)

La beta 628 queda instalada en el .190 con reproducción audiovisual normal y la corrección del contador de recuperaciones: las transiciones BUFFERING→READY de menos de 250 ms ya no se cuentan. La 1 UHD sigue sin reproducción estable y puede acabar en `StuckPlayerException` tras 10 s sin progreso. No se ha publicado la beta en el canal de actualizaciones.

Pruebas controladas en el mismo dispositivo:

- Beta 624: `playWhenReady=true`, `playing=true`, velocidad 1, supresión 0; tras el primer fotograma la posición queda en 0–4 s mientras el offset de directo crece.
- Beta 625, vídeo UHD sin audio (temporal): el decodificador avanzó a más de 2.500 fotogramas renderizados antes de una recuperación. Esta variante muda se retiró.
- Beta 626, audio/vídeo con `DefaultAudioSink` estándar (sin wrapper de diagnóstico): el bloqueo se reproduce. Se retiró el experimento.
- Beta 627, audio AAC sin vídeo (temporal): el reproductor también se atasca. La pista de AudioFlinger aparecía activa y con fotogramas listos pero sin consumo en la observación inicial. Esta variante sin vídeo se retiró.
- Beta 628: La 1 HD del mismo Fire Stick avanza visualmente y su pista PCM de 48 kHz sí acumula fotogramas consumidos en AudioFlinger. Reiniciar el Fire Stick no hizo aparecer un dispositivo HDMI distinto en `dumpsys audio`, pero la reproducción HD funciona; la indicación `type:0x400` no basta para atribuir el fallo a la TV.
- El MPD UHD servido localmente es XML válido, dinámico, con ventana de 10 s. Ofrece vídeo HEVC 1080p/2160p, audio AAC estéreo español e inglés a 128 kb/s y timelines alineados al inicio. Seleccionar manualmente la pista inglesa también acaba en bloqueo. Se restauró la selección automática.

Lo comprobado descarta el contador, la pausa de la app, el wrapper de diagnóstico, el perfil 4K como causa única y un segmento exclusivamente español. La causa aún no está demostrada: queda por investigar el reloj de audio y la sincronización DASH/Media3 de este origen, comparando los tiempos de muestras reales de ambas pistas con el MPD y con La 1 HD. No considerar La 1 UHD reparado por el hecho de que aparezca un fotograma tras una recuperación.
