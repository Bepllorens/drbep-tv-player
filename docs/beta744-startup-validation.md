# Beta 744: refrescos de Inicio (2026-10-16)

Desarrollo y compilacion en .185; pruebas ADB en Fire .190. Sin publicacion, commit/push ni cambios en produccion. Conservados los cambios pendientes anteriores.

## Cambios
- Favoritos calculados una vez por construccion del modelo de Inicio, en vez de cuatro. Sin cache persistente: cada modelo vuelve a comprobar el estado.
- Buscar primero el ID solicitado en findAllowedHomeLiveChannel y despues comprobar visibilidad y proteccion del candidato. No se elimina ninguna comprobacion de acceso.
- Los avisos de resumen guardan el estado mas reciente y se agrupan por fotograma. El refresco se publica despues del callback de animacion y reutiliza la espera de navegacion de 450 ms y las guardas de generacion/cierre.
- Sin modificaciones del reproductor, rutas, asociaciones EPG, DRM ni superficies en esta iteracion.

## Medidas en el Fire
Arranques de proceso conservando catalogo; no instalaciones limpias ni ensayo controlado de red.
- Referencia 742 durante esta tarea: catalogo 717 ms; modelos iniciales 609/551/174 ms; miniatura 6314 ms desde prepare, 0 descartados en muestra inicial.
- Iteracion 743: miniatura 7797/6744 ms. No demuestra aceleracion. Primeros modelos 869/950/434 y 557/482/301 ms.
- Final 744 prueba 1: catalogo 613 ms; modelos 739/352/121 ms; principal 5234 ms y miniatura 6904 ms desde prepare. Activity a primera miniatura 8509 ms. 2 frames descartados al principio; contador estable hasta rendered1110.
- Final 744 prueba 2: catalogo 689 ms; modelos 576/868/305 ms; primera miniatura 7396 ms desde prepare (8970 ms desde Activity). 0 descartados hasta rendered718.
- Refrescos posteriores 744 observados 84-145 ms, con picos iniciales aun presentes.
Estas medidas no permiten afirmar que 744 arranque mas rapido que 742. Los cambios reducen trabajo redundante y agrupan avisos, pero falta aislar el coste inicial restante.

## Validacion
- 702 pruebas, 0 fallos, errores u omitidos.
- Build release universal firmado correcto; APK Fire conserva firma compatible previamente instalada. install -r correcto.
- Inicio con La 1, pantalla completa por mando y retorno a Inicio con video visible.
- EPG de La 2 actualizada al programa de las 11:10 al volver.
- Retorno: modelo acaba 11:17:10.794; frame miniatura 11:17:12.784. No medirlo como tiempo total de la pulsacion.
- No se han repetido pruebas completas de catchup, timeshift o multiview, que no se modificaron en esta tarea.
- Capturas Mac: /private/tmp/drbep744-full.png y /private/tmp/drbep744-return.png.

## Artefactos
/srv/drbep-dev/artifacts/beta744-startup
- release.apk SHA256 da95d758c5e3333a32d372843e514358e603fc95e9ab212d5c78dec8e2e925cf
- fire.apk SHA256 0c6b6de4e6ee6ae8e381ffa88457cdafa356da7b9ff78f307d12c343f699ef39
- build.log y tests.log
No publicar fire.apk: mantiene la firma de prueba del dispositivo, distinta de la oficial.
