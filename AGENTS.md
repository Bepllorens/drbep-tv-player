# Instrucciones de desarrollo DRBEP

## Entorno obligatorio

El entorno de desarrollo de DRBEP es el servidor **192.168.93.185**, con usuario **drbep-dev**. Desarrollar, editar el codigo, compilar y ejecutar las pruebas alli. El Mac es un punto de acceso y puede usarse para ADB o comprobaciones visuales autorizadas; no volver a instalar en el Mac el entorno de compilacion ni usar sus copias antiguas como fuente canonica.

Si este repositorio se abre desde otro ordenador, entrar primero por SSH:

```bash
ssh drbep-dev-185
# Alternativa si ese alias no existe en el ordenador de acceso:
ssh drbep-dev@192.168.93.185
```

Verificar el servidor y el usuario antes de actuar. El alias `proxmox-drbep` entra como root y se reserva para administracion, no para el desarrollo habitual. Un worktree debe residir tambien en el .185 y conservar la base Git correcta.

Cargar las herramientas, tambien en shells SSH no interactivas:

```bash
source /srv/drbep-dev/env.sh
```

## Contexto que se debe leer

Antes de modificar codigo, leer completamente estos documentos del checkout del backend en el .185:

- `/srv/drbep-dev/repos/drbep/docs/CONTEXTO_DRBEP_PARA_OTRA_IA.md`
- `/srv/drbep-dev/repos/drbep/docs/INICIO_OTRA_IA.txt`

Comprobar el estado real y las instrucciones mas cercanas al archivo; los commits y versiones del documento son referencias fechadas, no una orden de volver a una version antigua. Seguir la peticion actual del usuario si cambia el alcance autorizado.

## Produccion y secretos

- Produccion esta en **192.168.93.223** y en **https://iptv.bepllorens.com**. No modificarla, reiniciarla, desplegar, publicar APK, cambiar canales beta/stable ni ejecutar sincronizadores reales salvo peticion expresa del usuario para esa operacion. Una tarea de desarrollo o un commit/push no lo autoriza.
- No usar bases de datos, configuraciones ni credenciales de produccion como fixtures. Preparar datos y configuracion de desarrollo independientes.
- No mostrar ni incorporar a Git contrasenas, claves privadas, tokens, sesiones, archivos de firma ni material de reproduccion. Cargar los secretos mediante los auxiliares existentes sin poner valores en argumentos ni activar trazas del shell.
- `/srv/drbep-dev/secrets/beta-publish.json` tiene una credencial administrativa completa; el campo beta no limita sus permisos. No usarla para comprobar una tarea que no requiere acceso a produccion.
- El .185 aloja otros servicios. No borrar contenedores ni reiniciar servicios ajenos para limpiar o compilar.

## Git y forma de trabajar

Comprobar `git status --short --branch`, el commit y las diferencias antes de editar. Conservar cambios ajenos. No usar reset destructivo, clean general, push forzado ni sincronizaciones con borrado para resolver diferencias.

Aplicar cambios acotados y validar el comportamiento relevante. Health, HTTP 200 y tests no sustituyen una prueba real de reproduccion. Para pruebas visuales en el Fire, verificar que la television este encendida en su entrada HDMI y que el mando este libre.

Responder en castellano. Informar que cambio, como se comprobo y que queda sin comprobar. No pedir otra vez una autorizacion ya concedida para la misma operacion; tampoco extenderla a otras operaciones.

## Repositorio Android

Checkout canonico: `/srv/drbep-dev/repos/drbep-tv-player`.
Rama de referencia: `qoder/disneyplus-vod-582`. Su nombre historico no limita el contenido a Disney. No sustituirla automaticamente por main.

El cliente es nativo, con Java, Kotlin, vistas tradicionales y Jetpack Compose; Media3/ExoPlayer es el reproductor principal. Leer el mapa de archivos del documento de contexto antes de reorganizar `MainActivity.java` o las superficies Compose.

Validacion habitual para el Fire ARM32:

```bash
cd /srv/drbep-dev/repos/drbep-tv-player
bash ./gradlew --no-daemon --max-workers=1 \
  :app:assembleDebug :app:testDebugUnitTest \
  -PupdateChannel=beta -PfireAbiOnly=true -PprofileBeta=true
```

Para compilar una beta firmada, usar `/srv/drbep-dev/build-signed-beta.py` despues de cargar el entorno. No publica ni incrementa automaticamente la version. El canal por defecto de Gradle es stable: ser explicito con beta. No generar otra firma ni desinstalar la app para resolver un conflicto.

Beta y stable usan el mismo identificador de aplicacion; no son instalaciones aisladas. Verificar version, firma, ABI y dispositivo antes de instalar. Para una futura publicacion autorizada, subir exactamente el binario probado, sin recompilarlo despues.

Preservar foco de mando, gestos tactiles, navegacion de series y retorno a la portada. Mantener listas de canales ligeras, controles discretos y degradados de la estetica actual. Al modificar video o superficies, probar zapeo, miniatura, vuelta desde pantalla completa y multipantalla; al modificar catchup, probar tambien acceso al timeshift y retorno al directo.

No reintroducir cargas masivas de catalogos al arrancar, parrillas completas en memoria ni temporizadores trabajando en pantallas ocultas. Conservar la base indexada, paginacion y refresco selectivo del catchup. No recordar canales o grupos de adultos como seleccion de arranque.

## Medidas de rendimiento en Fire

Las APK de depuracion sirven para diagnostico y pruebas funcionales, pero sus
costes de Compose, carga de clases y ejecucion no representan la APK optimizada
que se distribuye. Para comparar fluidez, usar una variante release perfilable:

```bash
bash ./gradlew --no-daemon --max-workers=1 :app:assembleRelease \
  -PupdateChannel=beta -PfireAbiOnly=true -PprofileBeta=true
```

Verificar el APK de esta compilacion mediante output-metadata.json, ABI, version,
firma y SHA256. No elegir un app-release.apk antiguo que siga en outputs. Una
compilacion privada puede firmarse con la identidad de pruebas ya instalada en
el Fire para preservar datos; esa firma no autoriza publicar el artefacto en beta.
No borrar datos ni desinstalar para cambiar de identidad. No publicar ni promover
una APK de pruebas sin autorizacion expresa y firma de distribucion correcta.

Comparar el mismo recorrido y grupo, separar primera entrada de recorridos
calientes, distinguir CPU de tiempo total y comprobar otras apps/carga del sistema.
No cerrar otras apps sin autorizacion. Si no hay mejora demostrable, retirar el
experimento. Conservar capturas, trazas y retorno a la miniatura como evidencias.
Detalle del diagnostico del 23 de octubre: docs/tv-row-layout-20261023.txt.

## ADB de tablet Android 11 o posterior

Para depuracion inalambrica TLS, el ADB Debian29 no es suficiente. Hay una copia
Linux del platform-tools37 oficial existente en `/srv/drbep-dev/tools/adb-modern`.
Usarlo con `-P 5038` en TODOS los comandos de tablet, para conservar el servidor
5037 que controla el Fire con `/usr/bin/adb`. No matar ni sustituir ese servidor.
Comprobar IP/puerto actuales; emparejar mediante el codigo que facilite el usuario,
sin guardar ese codigo en archivos, Git o informes. La Xiaomi puede mostrar un
aviso Instalar via USB con denegacion automatica a los10s: confirmar solo la APK
autorizada; no recordar decisiones generales ni desactivar verificaciones.

Instrumentacion de memoria/rendimiento en Fire con RAM limitada
- El parametro atrace -b es por CPU:32768 KiB en cuatro CPU puede reservar128MiB
  y provocar recuperacion de memoria, retrasos o cierres inducidos por la prueba.
  No interpretar esas muestras como comportamiento normal de la APK.
- Preferir dumpsys gfxinfo <paquete> reset/framestats para tiempos de dibujo;
  verificar portada estable antes y pantalla En directo despues. Un temporizador
  fijo de20s no garantiza que el arranque haya terminado ni que el tap abra TV.
- Si hace falta atrace, usar ventanas cortas y un presupuesto pequeno y explicito
  (ensayo actual2048KiB/CPU,8MiB total); no combinar sched y buffers grandes.
  Verificar que todos los CPU conservan el inicio real del evento antes de comparar.
- Al acabar comprobar tracing_on=0 y buffer_size_kb=1. Conservar muestras
  desfavorables y separar tiempo de hilo UI, espera previa y frame completo.
- Diagnostico y correccion del27/10: docs/tv-memory-20261027.txt.
