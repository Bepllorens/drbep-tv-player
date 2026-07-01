# Orange TV Platform Integration Plan

Fecha: 2026-06-30

## Objetivo

Integrar Orange TV en DRBEP manteniendo la prioridad de trafico directo: el servidor debe resolver autenticacion, tokens, manifests y claves, pero los segmentos de video deben ir directos desde el dispositivo a la CDN de Orange siempre que sea posible.

## Estado del script existente

Fuente actual: `/opt/drbep-data/scripts/orange`

- `client.py` autentica contra Orange TV, obtiene canales suscritos, resuelve `playingUrl`, `casToken`, KID/key y genera listas.
- `server.py` expone endpoints como `/stream/{channel_id}`, `/playlist` y `/license`.
- Para DASH, el servidor descarga el MPD real, lo parchea con `BaseURL` hacia la CDN final y anade ClearKey.
- Este modelo es adecuado para DRBEP porque el proxy solo sirve el manifest ligero; el trafico pesado de segmentos puede seguir siendo directo.

## Arquitectura recomendada

### 1. Servicio auxiliar `orange-proxy`

Crear un servicio Docker separado, similar a `runtime-proxy` o `smart-proxy`.

- Montar `/opt/drbep-data/scripts/orange`.
- Ejecutar `server.py` en un puerto interno, por ejemplo `8795`.
- Protegerlo con `ORANGE_PROXY_TOKEN`.
- No exponerlo publicamente sin pasar por DRBEP.
- Mantener credenciales, token y puerto en variables de entorno.

### 2. Backend DRBEP

DRBEP debe ser el orquestador de plataforma.

- Registrar plataforma `Orange TV`.
- Importar canales desde Orange mediante JSON, preferentemente un endpoint nuevo del proxy como `/channels`.
- Evitar depender del M3U como fuente principal salvo como fallback.
- Exponer un endpoint DRBEP estable, por ejemplo `/api/orange/stream/{id}` o `/orange/manifest/{id}`.
- El endpoint DRBEP debe devolver el MPD parcheado o redirigir/controlar la llamada al proxy interno.

### 3. Datos por canal

Cada canal importado debe conservar estos campos:

- `id`: ID externo de Orange.
- `name`: nombre visible del canal.
- `dial`: numero de canal si Orange lo proporciona.
- `platform_name`: `Orange TV`.
- `group`: bouquet/grupo de Orange o `OrangeTV`.
- `logo`: URL del logo del canal.
- `tvg_id`: identificador estable para EPG.
- `play_url`: endpoint DRBEP/proxy que devuelve el manifest fresco.
- `source_url`: URL final/origen si se conoce y es segura de publicar.
- `stream_type`: `dash` o `hls`.
- `drm_scheme`: normalmente `clearkey` para DASH.
- `clearkey`: KID/key por canal cuando este disponible.
- `playback_profile`: `orange_direct_dash` o equivalente.
- `direct_playback`: `true` cuando el manifest apunta a CDN externa y los segmentos no pasan por DRBEP.

El campo `logo` es obligatorio para los canales Orange importados si Orange lo proporciona. Debe viajar intacto por:

- importador Orange,
- catalogo DRBEP,
- dashboard,
- snapshot offline,
- app offline.

Si Orange no devuelve logo para un canal, el importador debe dejar `logo` vacio pero registrar el caso para diagnostico.

### 4. App offline

La app offline debe recibir Orange como DASH directo con ClearKey cuando sea posible.

- El snapshot debe incluir `logo`, `drm_scheme`, `clearkey`, `playback_profile` y `direct_playback`.
- El player debe tratar Orange como `Directo DASH` si los segmentos salen a CDN externa.
- Solo usar proxy pesado como fallback para canales que no reproduzcan directo.

### 5. Dashboard y actividad

El dashboard debe mostrar Orange como plataforma normal.

- En catalogo/listas: mostrar logos importados.
- En actividad: clasificar como trafico directo si el MPD parcheado usa `BaseURL` de CDN Orange.
- Si cae a `/proxy_media` o equivalente, marcarlo como trafico servidor/fallback.

## Fases de implementacion

1. Convertir el script Orange en servicio Docker controlado.
2. Anadir endpoint JSON `/channels` al proxy con todos los campos, incluido `logo`.
3. Crear importador/refresh Orange en DRBEP.
4. Publicar canales Orange en `/api/channels/catalog` y snapshots offline.
5. Ajustar resolver/diagnostico para `orange_direct_dash`.
6. Probar canales TDT, premium y 4K/HEVC si existen.
7. Anadir fallback controlado para casos donde el directo no funcione.

## Criterio de exito

- Los canales Orange aparecen en dashboard y app offline con nombre, dial y logo.
- Al reproducir, el dashboard etiqueta la mayoria como trafico directo.
- El servidor solo sirve manifest/licencia/token salvo fallback explicito.
- La app offline puede zapear Orange sin depender de `127.0.0.1` ni de procesos manuales.
