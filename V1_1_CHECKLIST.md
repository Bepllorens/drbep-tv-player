# DRBEP TV Player v1.1 Checklist

## Epic 1. Build y seguridad de red

- [x] Separar configuracion `debug` y `release` en `app/build.gradle`
- [x] Mover `network security config` a source sets por build type
- [x] Evitar `cleartext` global por defecto en `release`
- [x] Eliminar default de release basado en IP LAN fija
- [ ] Revisar firma de release
- [ ] Ajustar reglas de ProGuard tras validar reproduccion y Glide

## Epic 2. Strings y constantes UI

- [x] Mover strings estaticos de layouts a recursos
- [x] Mover mensajes base de estado y error a `strings.xml`
- [ ] Extraer menus de acciones de canal a arrays de recursos
- [ ] Extraer textos de favoritos, filtros y recordatorios restantes
- [ ] Revisar copy para distancia de visionado en TV

## Epic 3. Refactor minimo estructural

- [x] Crear `PlayerController`
- [x] Crear `CatalogRepository`
- [ ] Crear `EpgRepository`
- [ ] Crear `RecordingsRepository`
- [ ] Crear `ReminderStore`
- [ ] Reducir `MainActivity` por debajo de 1000 lineas

## Epic 4. Robustez playback y API

- [ ] Centralizar cliente HTTP comun
- [ ] Unificar parseo y manejo de errores JSON
- [ ] Revisar decision entre URL directa, proxy y fallback
- [ ] Mejorar logs de diagnostico de playback
- [ ] Validar DASH, HLS, Widevine y ClearKey con casos reales

## Epic 5. Validacion de release

- [x] `assembleDebug` OK
- [x] `assembleRelease` OK
- [ ] Zapping arriba/abajo OK
- [ ] Overlay y filtros OK
- [ ] Favoritos OK
- [ ] Mini guia OK
- [ ] Grabaciones OK
- [ ] Playback simple y DRM OK

## Siguiente corte recomendado

- Extraer `PlayerController`
- Extraer `CatalogRepository`
- Terminar de mover strings restantes