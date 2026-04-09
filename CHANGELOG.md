## 1.4.12
- Fire Stick: OK sobre reproduccion normal ahora muestra la barra de timeshift en vez de ocultarla tras pausar, y BACK la cierra primero si estaba visible.

## 1.4.11
- Fire Stick: cuando la barra de timeshift esta visible, LEFT/RIGHT del mando hacen seek sobre esa barra en vez de abrir el overlay.

## 1.4.10
- Fire Stick: mantener pulsado FF en timeshift ahora detecta la repeticion del mando y salta directo a LIVE, en vez de avanzar tramo a tramo.

## 1.4.9
- Fire Stick: pulsacion larga en FF dentro de timeshift vuelve rapido a LIVE y mantiene la barra visible para confirmar el salto.

## 1.4.8
- Fire Stick: primera base de timeshift con mando. PLAY/PAUSE y REW/FF muestran la barra y permiten pausar o mover el directo cuando el canal soporta DVR.

## 1.4.7
- Fire Stick: corregido el salto de Siguiente en la timeline para que abra la franja correcta del siguiente programa del canal y no una fecha invalida.

## 1.4.6
- Fire Stick: anadido boton Siguiente en la timeline para saltar al siguiente programa del canal enfocado sin recorrer toda la fila.

## 1.4.5
- Fire Stick: los chips Ahora, +2h y Cerrar de la timeline ya son accesibles con mando y se pueden enfocar desde la primera fila con DPAD arriba.

## 1.4.4
- Fire Stick: el boton Ahora de la timeline salta de verdad a la franja actual en vez de reabrir la ventana anterior.
- Fire Stick: la timeline recuerda mejor el bloque enfocado y al reabrirse intenta caer en el programa en emision o en el ultimo punto usado.

## 1.4.3
- Fire Stick: el menu de acciones de un programa en timeline o EPG visual ahora ofrece cancelar la grabacion si ese evento ya estaba programado.
- Fire Stick: cancelar una grabacion programada desde timeline refresca la propia vista al momento.

## 1.4.2
- Fire Stick: anadidos presets 1/2/3 para multiview desde Herramientas.
- Fire Stick: puedes guardar el multiview actual o reabrir un preset guardado sin reconstruirlo a mano.

## 1.4.1
- Actualizado RecyclerView a 1.4.0 sobre la nueva base de build con API 36.

## 1.4.0
- minSdk sube a 23 para alinearse con las dependencias modernas de reproduccion.
- Actualizadas las dependencias de reproduccion a Media3 1.10.0.
- Leanback pasa a la release estable 1.2.0.

## 1.3.9
- Migrado el build a AGP 8.9.1 y Gradle 8.11.1 para soportar compileSdk 36.
- compileSdk sube a 36 sin cambiar todavia el targetSdk, preparando el proyecto para actualizar Media3 y otras dependencias.

## 1.3.8
- Fire Stick: al pulsar BACK durante una grabacion ahora pregunta si quieres salir de la grabacion y volver al canal anterior.
- Fire Stick: al confirmar la salida de una grabacion vuelve al canal lineal que estabas viendo antes en lugar de quedarse bloqueado.

## 1.3.7
- Fire Stick: al reabrir una grabacion pregunta si quieres continuar desde el punto guardado o empezar de nuevo.
- Fire Stick: pulsar BACK durante una grabacion vuelve al panel de grabaciones y guarda el punto de reproduccion.

## 1.3.6
- Fire Stick: timeline recuerda ventana y canal enfocado al reabrir.
- Fire Stick: EPG visual recuerda la tarjeta enfocada al volver a abrir.
- Fire Stick: grabaciones recuerda el modo y el item seleccionado.

## 1.3.5
- Fire Stick: zapping circular dentro del grupo visible.
- Fire Stick: recuperacion automatica de reproduccion en modo auto probando rutas alternativas antes de fallar.

# Changelog

## 1.3.4 - 2026-03-31

- Added Fire Stick visual EPG sections backed by the same EPG category endpoints used by the dashboard.
- Kept the original timeline guide stable while separating the new visual EPG experience.
- Improved Fire Stick visual EPG card sizing, poster centering, section ordering, and DPAD navigation.
- Filtered visual EPG content by the current platform context and excluded sports from the live-now rail.
- Refined Fire Stick visual EPG focus movement between rows and horizontal channel rails.

## 1.3.3 - 2026-03-26

- Added Fire Stick multiview 2x2 entry from Tools.
- Added remote navigation inside multiview: DPAD move, OK audio/fullscreen, MENU change channel, BACK close.
- Improved multiview stability by using TextureView for the four preview players.
- Completed multiview slot selection with global live channels when the visible filter has fewer than four.
- Fixed the multiview channel picker for Fire Stick focus and scrolling.
- Added clearer focused-row highlight in the multiview channel picker.
- Added logo prefetching to make multiview channel picker artwork appear faster.
- Refreshes the timeline immediately after scheduling so programmed events are marked in place.
- Hides touch-only quick access chips and favorite star in Fire Stick overlay.

