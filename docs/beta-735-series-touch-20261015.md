# Beta 735 — series, gestos y mejoras acumuladas

Versión: `2.0.735-beta-series-touch`, ARM32. Publicación en beta; stable y rescue permanecen intactos.

## Cambios desde la beta 728

- Catálogo catchup SQLite indexado, páginas de 60 programas y consulta independiente de la tira del reproductor. Refresco selectivo de tarjetas y programación.
- Series importadas: restauración de temporada, página y episodio al volver desde la ficha, y navegación entre temporadas y episodios.
- Gestos de reproducción: avance/retroceso en VOD y U7D, doble toque lateral y cancelación de gestos con varios dedos. La navegación del mando mantiene su recorrido.
- Optimización de navegación de TV y miniatura, logotipos integrados y presentación de metadatos de programación.

## Validación

- 681 pruebas unitarias sin fallos.
- Fire .190: pruebas instrumentadas `SeriesFocusReturnDeviceTest#episodesRestoreAfterReturningToDetails` y `PlaybackTouchSafetyDeviceTest` correctas.
- La primera ejecución del test de series necesitó habilitar la información de accesibilidad de ventanas; la repetición pasó.
- Tablet física no conectada durante las pruebas: falta validar la comodidad y respuesta real del doble toque y deslizamiento.
- Pruebas anteriores de beta 734: almacenamiento catchup, navegación entre días y páginas, y vuelta de ficha.

## Identidad de publicación

La APK del Fire lleva firma de pruebas. Se firmó su contenido con el certificado oficial sin recompilar, verificando igualdad de todas las entradas ZIP fuera de META-INF.

- SHA-256 APK Fire: `5ca3a1d396f2825ed1c68151133ff566eed37632daeda4dd0746e383040031c3`
- SHA-256 APK oficial: `d196935891480afa89cc4febfc9cd95d6882ca1034fa7ff44d781d9ca3a2e132`
- Certificado oficial: `0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b`
- Descarga: https://fire.tvbep.com/api/offline/app/apk?channel=beta
- Respaldo de publicación: `/opt/drbep-releases/beta735-publication-20261015` en el servidor.
- Artefactos locales: `/Users/bep/Documents/drbep/artifacts/series-touch-735`.

No se despliega backend con esta publicación. La 734 queda disponible como retorno en el Fire; el respaldo del canal conserva la beta oficial 728.
