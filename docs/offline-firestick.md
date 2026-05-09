# DRBEP TV Offline

Proyecto alternativo para instalar una app Fire Stick independiente en otra vivienda sin exponer el backend privado.

## Flujo previsto

1. El dashboard sigue siendo el origen maestro.
2. Un exportador genera `catalog_snapshot.json` desde el backend.
3. Ese JSON se publica en una URL externa segura.
4. La app `com.drbep.tvplayer.offline` descarga el snapshot desde Ajustes > Catalogo independiente.
5. En los siguientes arranques, la app usa el catalogo local sin conectar con la red privada.

## Exportar snapshot

```bash
scripts/export_offline_catalog.py \
  --base-url http://192.168.93.223:8080 \
  --output catalog_snapshot.json
```

El formato actual incluye:

- `catalog`: respuesta de `/api/channels/catalog?include_disabled=0`
- `vod`: respuesta de `/api/vod/tivify`
- `adult`: VOD adulto de Tivify
- `runtime_movies`: peliculas de Runtime

## Estado actual

- App con `applicationId` independiente: `com.drbep.tvplayer.offline`
- Nombre visible: `DRBEP TV Offline`
- Modo standalone activado por defecto
- Catalogo local persistente en almacenamiento interno
- Ajustes para ver estado, cambiar URL, actualizar y borrar snapshot
- Si no hay snapshot local, intenta descargar desde `catalogSnapshotUrl`

## Pendiente

- Publicacion automatica diaria del snapshot
- EPG offline opcional
- Tratamiento de canales que dependan de proxy vivo
- Firma/verificacion del snapshot antes de aplicarlo
