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

