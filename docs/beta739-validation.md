# Beta 739 — navegación VOD y arranque de miniatura

Trabajo en .185; sin cambios en backend, producción, DRM, fuentes ni canal stable. No publicada.
Incluye los cambios pendientes de beta 738 (diálogo continuar/reiniciar).

Cambios:
- El catálogo importado conserva su destino al abrir una película/episodio; el control vuelve al catálogo y contexto correspondiente.
- Etiquetas Volver a películas / Volver a series. Icono de catálogo diferenciado del icono de información.
- Política de calidad de miniatura activada antes de preparar el canal de arranque, evitando cambiarla después del primer fotograma al adjuntar la portada.

Pruebas Fire .190:
- Regreso desde Al filo del mañana al catálogo importado confirmado; conserva foco en esa película.
- Primer fotograma de miniatura en comparación inicial: 16.286 ms (738) frente a 8.940 ms (739); no es una media estadística.
- 739: primeros ~30 segundos sin fotogramas perdidos; después un pico de 13. No afirmar fluidez completa.
- Película a TV: 5.957 ms en referencia, 6.561 ms con limpieza explícita de Media3. Experimento descartado: se retiraron reset adicional y su test al no demostrar mejora. No afirmar que se haya resuelto el negro de transición.
- Presión de memoria y procesos ajenos observados; no se han detenido otras apps.
- Pendiente caracterizar la demora de preparación del directo y el pico residual de miniatura con medidas repetidas.

Validación final: 695 tests, cero fallos/errores; compilación universal correcta; instalación -r correcta en Fire .190 (739, firma de prueba compatible, datos conservados).
APK universal oficial SHA256: 6b124c58fa267613ead264122c2f557a386de536989cf067b0ed0565c8ac1231
APK Fire de prueba SHA256: d9049158f31eb1030da75b6a9303dcdd72f665626fba717e08dda7b34290db4f
Repetición de transición previa a instalación final: primer fotograma 5622 ms, READY 5752 ms, buffer inicial 1514 ms. La imagen llega al terminar la preparación: no se observó una demora adicional de superficie. Falta desglosar red/manifiesto/segmento/decodificador; no atribuir a una causa única sin esa medida.
