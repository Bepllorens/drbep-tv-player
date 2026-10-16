# Validacion beta 746: caja negra (2026-10-16)

## Resultado
Compilada en .185 e instalada con -r en Fire .190, AFTKRT ARM32. Registro normal activado al finalizar.
No se publica ni se hace commit/push; produccion y stable sin cambios.

## Pruebas automatizadas
707 tests, sin fallos, errores ni omitidos. Incluyen cinco tests nuevos:
persistencia entre procesos, rotacion y exportacion acotada con archivos ajenos excluidos,
pausa/detalle/cola limitada, tolerancia a cola de archivo corrupta y autenticacion/Origin del panel.
JavaScript del panel comprobado con node --check. git diff --check correcto.

## Dispositivo y panel
- La 745 inicial mostro video en 6890 ms desde prepare y eventos reales legibles por API.
- Sesion por PIN, consulta de eventos, activar detalle, marcar incidencia, exportar ZIP y pausar probados en Fire.
- Incidencia exportada incluye contexto posterior hasta 26893 ms despues de la marca (instantanea realizada a los 30 s).
- Interfaz revisada en navegador a ancho estrecho: botones visibles y eventos en area de desplazamiento.
- Se corrigio antes de finalizar una metrica engañosa: los frames repetidos no deben contar como primera imagen desde prepare.
- 746 usa SURFACE_FRAME desde el cambio real de superficie, separado de PLAY_FRAME.
- Duraciones de superficie observadas: 1334, 1826 y 211 ms. Imagen visible tras volver a Inicio.
- La pausa sobrevive a reinstalacion y reinicio; despues se reactivo el registro normal.
- Tras otro reinicio: tres sesiones en el historial, enabled=true, detailed=false, lost=0, writeErrors=0.
- Tamano observado final 39134 bytes. ZIP valido 5087 bytes, 252 filas incluyendo instantanea; 77 eventos de la version 746.
- ZIP solo contiene eventos JSONL y README. Verificado esquema de valores numericos y nombres fijos, sin contenido/URLs/credenciales.

## Coste: evidencia y limites
- 746 con registro pausado: primera miniatura 8329 ms desde prepare, 0 descartados hasta rendered715.
- 746 con registro normal: primera miniatura 8110 ms desde prepare. 10 descartados acumulados hasta rendered720, maximo consecutivo3.
- Son dos arranques con variabilidad de red/carga/JIT, no una prueba controlada. No concluir aceleracion ni ausencia de tirones, ni atribuir los descartes a la caja negra sin mas evidencia.
- top -H observo drbep-health con 0.01 s CPU acumulada y drbep-blackbox con 0.06 s, ambos 0.0% en esa muestra, despues de varios minutos.
- La cola/rotacion y el muestreo acotados reducen riesgo de impacto. No se han provocado crash ni ANR reales del Fire; su captura solo se ha revisado en codigo. Se comprobaron motivos de salida ofrecidos por Android y persistencia tras force-stop.
- No se repiten pruebas completas de catchup/timeshift/multiview; no se modificaron sus rutas.

## Artefactos
/srv/drbep-dev/artifacts/beta746-blackbox
- release.apk SHA256 34fbe2ae82cf5ed7935b04a509de085e828675e0571354ec44a9dd382d3b4167
- fire.apk SHA256 0a68063f50fd77de396e5ad0d65868a47840aa328821301f42b08f4a3bc370ea
- build.log, tests.log, fire-blackbox-validation.zip.
El Fire conserva su firma compatible de pruebas, distinta de la firma oficial de release.apk. No publicar fire.apk.
