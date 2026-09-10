# Seguimiento de HBO — beta 550

Implementado en el Fire Stick 192.168.93.164 y servidor drbep-backend:hbo-progress-20260910.

- Historial por usuario, con posición y duración guardadas cada 15 segundos.
- Una tarjeta por serie en Continuar viendo, con temporada, episodio y progreso; también películas pendientes.
- Al alcanzar el 98% se marca visto y se busca el siguiente episodio, incluso entre temporadas. Si falla la consulta se reintenta al abrir la portada. El último episodio terminado se retira de Continuar viendo.
- Sincronización mediante las preferencias existentes del servidor. Los metadatos HBO están limitados y no contienen enlaces de reproducción ni credenciales.
- Portadas autenticadas solo contra los orígenes de confianza.

Validación: 11 pruebas Android pasan; pruebas específicas del servidor pasan; compilación release firmada correcta. En el Fire Stick El drama apareció en 06:34, se reanudó y el servidor registró posición 438.781 s y duración 6326 s. Los cambios de temporada y recuperación entre dispositivos se probaron automáticamente, sin reproducir un episodio completo real.

APK SHA256: 1a1332b9f7bf2fb6a8eff980cfd5c03bb8c4b5a3b3ad123545bb289f99b1f420.
No se ha publicado OTA global: beta instalada en el Fire Stick de prueba.

Fuentes remotas: /opt/drbep-tv-player-hbo-progress y /opt/drbep-hbo-progress-server. Ramas codex/hbo-watch-progress y codex/hbo-watch-progress-server.

El servidor incluye la copia exacta de reproducción HBO de /opt/drbep-backups/builds/private-vod-20260909, que no estaba íntegramente versionada. La primera prueba detectó la ruta ausente; se restauró la imagen anterior y se corrigió la base antes del despliegue definitivo. Salud 200 y ruta HBO autenticada verificadas.

Rollback del servidor: /opt/drbep/docker-compose.override.yml.before-hbo-progress-20260910. La imagen anterior permanece disponible.
