# Beta 615 — memoria de la portada

El Fire Stick .190 registró tres cierres `LOW_MEMORY` de la beta 614 el 29 de septiembre entre las 22:27 y las 22:35. No apareció una excepción Java correspondiente. El equipo dispone de aproximadamente 1,7 GB de RAM y estaba bajo presión de memoria, por lo que el registro no atribuye el cierre a una sola imagen.

La beta 615 reduce la memoria reservada por Glide en equipos de poca RAM, limita la imagen del destacado a 960 × 540, decodifica las imágenes de la portada en RGB_565, evita conservarlas en la caché de memoria de Glide y libera el destino y el drawable anteriores al sustituirlos. También corrige el tamaño solicitado en el enlace de la portada para no aplicar dos veces la conversión a píxeles.

Verificación: build release y pruebas `LiveCategoryOrderTest` y `LiveHomePolicyTest` correctos; `git diff --check` limpio. El APK firmado se instaló en el Fire Stick .190 sin borrar datos. La portada, Películas y En directo cargaron, y tres ciclos de navegación mantuvieron el mismo PID 21105. Tras la prueba, el último cierre registrado seguía siendo el de la beta 614. La memoria gráfica se estabilizó en torno a 61 MB tras navegar, con 175 MB PSS total en la última medición. Esto verifica la mitigación en una sesión corta, pero no descarta nuevos cierres si vuelve la presión global de memoria.
