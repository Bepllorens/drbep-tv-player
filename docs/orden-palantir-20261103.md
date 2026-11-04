# Orden de listas y retorno de Palantir

Estado actualizado el 4 de noviembre de 2026. Sustituye el estado previo de desarrollo del 3/11.

La ordenación se desplegó inicialmente en r59 y se comprobó en Fire el 3/11. Un despliegue posterior de Tivify sustituyó ese backend por una compilación sin el comparador corregido.

El 4/11 se ha desplegado r60 sobre la fuente exacta del backend de Tivify, reproducida byte por byte antes de modificarla.
ZapiTV y Vodafone recuperan el orden de importación cuando no hay dial; diales explícitos y órdenes manuales siguen prevaleciendo.
El cambio queda limitado a esas dos plataformas. Se comprobaron 232 y 131 canales y el orden sin cambios del resto.
La fuente y la imagen persistente del despliegue también incluyen la corrección.

Palantir conserva Volver al vídeo en el código y en la compilación actual; pasan sus 13 pruebas de política, permisos, foco y reproducción.
La APK actual es la de Tivify posterior, no la candidata aislada inicial. No se ha reinstalado ni se ha modificado audio.
La validación física con el mando queda pendiente por indicación del usuario (TV no disponible).

Informe completo del backend:
docs/deploy-channel-order-r60-20261104.md
Ruta absoluta en .185:
/srv/drbep-dev/repos/drbep/docs/deploy-channel-order-r60-20261104.md

Tivify: su arreglo posterior se conserva sin modificar. Los diagnósticos del 3/11 son históricos y no describen por sí solos el estado actual.
Audio: el usuario confirmó que La bola negra 4K con 1fichier iba bien y pidió dejarlo sin cambios; se mantiene esa decisión.
No se publican APK ni se modifican los canales beta/stable/rescue.
