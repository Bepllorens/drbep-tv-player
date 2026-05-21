# DRBEP TV Offline

Proyecto alternativo para instalar una app Fire Stick independiente en otra vivienda sin exponer el backend privado.

## Flujo previsto

1. El dashboard sigue siendo el origen maestro.
2. Se crea un usuario con permisos concretos para la vivienda/dispositivo.
3. Desde Gestion de usuarios y accesos se genera un token offline revocable.
4. La app `com.drbep.tvplayer.offline` descarga el snapshot desde Ajustes > Catalogo independiente.
5. En los siguientes arranques, la app usa el catalogo local hasta que caduque o se actualice.

## Generar token desde dashboard

En el backend, la rama `codex/offline-user-snapshots` anade:

- `GET /api/offline/snapshot`: genera el snapshot filtrado por permisos del usuario autenticado.
- `POST /api/users/{id}/offline-token`: crea un token Bearer para un usuario concreto.
- Boton `Token offline` dentro de Gestion de usuarios y accesos.

El token se muestra una sola vez. Para desactivarlo basta con revocar la sesion `DRBEP Offline` del usuario.

La app debe configurarse con:

- URL: `https://TU_DOMINIO/api/offline/snapshot?device_id=DEVICE_ID_DE_LA_APP`
- Token: el Bearer generado en el dashboard

Tambien se envia `X-DRBEP-Device-Id`, asi que el snapshot queda vinculado al dispositivo esperado.

## Exportar snapshot

El exportador manual sigue disponible para pruebas o snapshots publicados en un almacenamiento externo:

```bash
scripts/export_offline_catalog.py \
  --base-url http://192.168.93.223:8080 \
  --output catalog_snapshot.json \
  --subject "Casa playa" \
  --device-id "DEVICE_ID_DE_LA_APP" \
  --ttl-days 7 \
  --allow-platforms "Movistar,Tivify" \
  --no-include-adult
```

El formato actual incluye:

- `schema`: `drbep-offline-catalog-v2`
- `subject`: usuario/dispositivo autorizado
- `device_id`: dispositivo esperado
- `expires_at`: caducidad Unix en segundos
- `permissions`: resumen de permisos aplicados
- `catalog`: respuesta filtrada de `/api/channels/catalog?include_disabled=0`
- `vod`: respuesta de `/api/vod/tivify`
- `adult`: VOD adulto de Tivify
- `runtime_movies`: peliculas de Runtime

## Estado actual

- App con `applicationId` independiente: `com.drbep.tvplayer.offline`
- Nombre visible: `DRBEP TV Offline`
- Modo standalone activado por defecto
- Catalogo local persistente en almacenamiento interno
- Identidad local por dispositivo (`device_id`)
- Token por usuario configurable desde Ajustes
- Descarga de snapshot con `Authorization: Bearer ...` y `X-DRBEP-Device-Id`
- Caducidad obligatoria si el snapshot trae `expires_at`
- Rechaza snapshots cuyo `device_id` no coincide con el dispositivo local
- Ajustes para ver estado, cambiar URL, actualizar y borrar snapshot
- Si no hay snapshot local, intenta descargar desde `catalogSnapshotUrl`
- Backend con endpoint de snapshot filtrado por permisos de usuario
- Dashboard con generacion de tokens offline revocables
- Verificacion local del snapshot antes de aplicarlo
- Firma del snapshot en backend con validacion en la app

## Pendiente

- Publicacion automatica diaria del snapshot
- EPG offline opcional
- Tratamiento de canales que dependan de proxy vivo
- Flujo guiado para copiar URL/token al Fire Stick sin introducirlo a mano
