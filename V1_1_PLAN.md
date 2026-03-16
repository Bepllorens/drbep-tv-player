# DRBEP TV Player v1.1 Plan

## Objetivo

Convertir la base `1.0` en una version mas mantenible y segura sin perder velocidad de iteracion en Fire Stick / Android TV.

La prioridad de `v1.1` no es anadir muchas funciones nuevas. La prioridad es estabilizar la app, reducir deuda tecnica en reproduccion/red/UI y dejar preparada una base razonable para `v1.2`.

## Estado de partida

- Version base etiquetada: `1.0`
- Build validado con `./gradlew assembleDebug`
- `versionName "1.0"` ya definido en `app/build.gradle`
- La mayor parte de la logica esta concentrada en `app/src/main/java/com/drbep/tvplayer/MainActivity.java`

## Objetivos de v1.1

1. Separar responsabilidades criticas de `MainActivity`.
2. Endurecer la configuracion de red y release.
3. Mejorar la robustez del playback y de las llamadas API.
4. Mover textos y constantes UI a recursos reutilizables.
5. Dejar una base preparada para evolucionar favoritos, EPG, grabaciones y recordatorios.

## No objetivos de v1.1

- Reescritura completa a Kotlin.
- Migracion completa a Compose.
- Redisenar por completo la experiencia visual.
- Soporte offline real.
- Sincronizacion cloud de favoritos o recordatorios.

## Backlog priorizado

### P0. Refactor minimo estructural

Impacto: Muy alto

Esfuerzo: L

Riesgo: Medio

Trabajo:

- Extraer un `PlayerController` para encapsular `ExoPlayer`, preparacion de `MediaItem`, DRM, fallback y estados de reproduccion.
- Extraer un `CatalogRepository` para carga de catalogo y fallback de `/api/channels`.
- Extraer un `EpgRepository` para `now`, guia por canal y resolucion de programas.
- Extraer un `RecordingsRepository` para listado y reproduccion de grabaciones.
- Extraer un `ReminderStore` para persistencia de recordatorios en `SharedPreferences`.
- Mantener `MainActivity` centrada en ciclo de vida, foco y coordinacion de UI.

Criterio de salida:

- `MainActivity.java` deja de ser el punto unico de red, playback y persistencia.
- Las rutas de reproduccion y catalogo quedan cubiertas por clases dedicadas.
- El build sigue funcionando sin regresiones visibles de zapping, overlay y favoritos.

### P0. Configuracion segura de build y red

Impacto: Muy alto

Esfuerzo: M

Riesgo: Bajo

Trabajo:

- Separar `debug` y `release` en `app/build.gradle`.
- Permitir `cleartext` solo en `debug` o en dominios/IPs explicitamente autorizados.
- Eliminar valores de red demasiado acoplados al entorno local como defaults de release.
- Definir propiedades de build para servidor, modo Fire Stick y politicas de seguridad por entorno.
- Preparar firma de release y activar `minifyEnabled` con reglas revisadas.

Criterio de salida:

- La app `release` no depende de una IP LAN hardcodeada.
- `network_security_config` deja de abrir trafico claro de forma global para produccion.
- Existe camino claro de build `debug` y `release`.

### P1. Robustez de red y errores

Impacto: Alto

Esfuerzo: M

Riesgo: Bajo

Trabajo:

- Centralizar llamadas HTTP en una capa comun.
- Normalizar timeouts, parseo JSON y gestion de errores.
- Introducir modelos de respuesta simples para catalogo, stream info, EPG y grabaciones.
- Mejorar mensajes de error visibles para el usuario y logs para diagnostico.
- Evitar duplicacion de logica de conexion y parseo.

Criterio de salida:

- Las llamadas API comparten una implementacion comun.
- Los errores de red y parseo no quedan mezclados con logica de UI.
- El soporte de logs para incidencias reales mejora.

### P1. Externalizacion de strings y constantes UI

Impacto: Medio

Esfuerzo: S

Riesgo: Bajo

Trabajo:

- Mover textos de estado, errores, menus y dialogos a `strings.xml`.
- Normalizar nombres de acciones y etiquetas de filtros.
- Mover constantes de timings y textos repetidos a una ubicacion mas clara.

Criterio de salida:

- `MainActivity` deja de contener literales de texto repartidos por toda la clase.
- La app queda lista para localizacion o ajuste de copy sin tocar logica.

### P1. Endurecimiento del flujo de playback

Impacto: Alto

Esfuerzo: M

Riesgo: Medio

Trabajo:

- Revisar la logica de eleccion entre URL principal, proxy y fallback.
- Unificar la deteccion de DASH/HLS/DRM.
- Mejorar comportamiento al fallar la carga de `stream info`.
- Evitar reintentos ambiguos cuando el estado del stream no esta resuelto.
- Anadir telemetria basica de reproduccion si el backend la puede consumir.

Criterio de salida:

- El flujo de reproduccion queda mas predecible.
- Los fallos entre proxy, manifest y fallback son mas faciles de entender y depurar.

### P2. Recordatorios mas fiables

Impacto: Medio

Esfuerzo: M

Riesgo: Medio

Trabajo:

- Evaluar migracion de los recordatorios a `WorkManager` o `AlarmManager`.
- Separar claramente recordatorios persistidos de notificaciones en pantalla.
- Definir si los recordatorios deben sobrevivir al cierre total de la app.

Criterio de salida:

- Hay una decision tecnica cerrada sobre el alcance real de recordatorios.
- Si se implementa en `v1.1`, deja de depender solo de un `Handler` activo dentro de la actividad.

### P2. Mejoras UX incrementales para TV

Impacto: Medio

Esfuerzo: M

Riesgo: Bajo

Trabajo:

- Mejorar foco visual de la lista y estados seleccionados.
- Dar mas contexto en overlay: canal, grupo, programa actual, estado DRM o modo compat.
- Revisar legibilidad de textos y densidad visual en TV a distancia.
- Preparar una mini guia mas clara para navegacion con mando.

Criterio de salida:

- La navegacion con mando se percibe mas clara sin rehacer toda la interfaz.

## Orden recomendado

### Fase 1

- Configuracion segura de build y red.
- Externalizacion de strings y constantes.

### Fase 2

- Refactor minimo estructural.
- Robustez de red y errores.

### Fase 3

- Endurecimiento del flujo de playback.
- Mejoras UX incrementales.

### Fase 4

- Recordatorios mas fiables.

## Estimacion resumida

- P0 refactor minimo estructural: 3 a 5 dias
- P0 configuracion segura build/red: 1 a 2 dias
- P1 robustez de red y errores: 1 a 2 dias
- P1 strings y constantes UI: 0.5 a 1 dia
- P1 playback hardening: 1 a 2 dias
- P2 recordatorios fiables: 1 a 2 dias
- P2 UX incremental: 1 a 2 dias

Estimacion total razonable para `v1.1`: 8 a 14 dias de trabajo enfocado.

## Riesgos principales

- Romper el zapping o la navegacion al mover logica fuera de `MainActivity`.
- Introducir regresiones en DRM/fallback al simplificar el flujo de reproduccion.
- Endurecer demasiado la red y bloquear entornos locales de pruebas si no se separa bien `debug` de `release`.

## Validacion minima para cerrar v1.1

- `assembleDebug` y `assembleRelease` completan correctamente.
- Zapping arriba/abajo sigue funcionando.
- Overlay, filtros y favoritos no regresionan.
- Catalogo carga tanto desde endpoint principal como desde fallback cuando proceda.
- Playback funciona para streams simples y para streams que requieran proxy/DRM.
- Grabaciones y mini guia siguen accesibles.
- La app `release` no usa configuracion de red insegura por defecto.

## Recomendacion practica

Si solo se hace una cosa en `v1.1`, que sea esta combinacion:

1. Separar `PlayerController` y `CatalogRepository`.
2. Crear `debug/release` limpios.
3. Mover strings a recursos.

Esa combinacion reduce deuda real sin convertir `v1.1` en una reescritura.