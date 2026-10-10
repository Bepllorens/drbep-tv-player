# Beta 612 — orden de plataformas y grupos en En directo

Pulsación larga de OK inicia el movimiento de una categoría. Arriba/abajo intercambian posiciones; OK guarda y Atrás cancela. Todos queda fijo. El orden se guarda localmente con ámbito de servidor y usuario del catálogo, sin modificar permisos ni el orden de canales.

LiveCategoryOrder aplica preferencias únicamente sobre categorías autorizadas actuales y añade al final nuevas categorías. Pruebas: tres casos de ordenación y seis del filtro de permisos, todos correctos.

Validado en Fire Stick 192.168.93.190:5555, paquete com.drbep.tvplayer.offline, versionCode 612:
- Pulsación larga de OK: aparece Moviendo y la ayuda de teclas.
- Movimiento hacia abajo y Atrás: restaura el orden y mantiene la portada abierta.
- Movistar desplazado hacia arriba, guardado con OK, app detenida y reabierta: permanece inmediatamente después de Todos.
- Se deja Movistar en esa posición de prueba; el resto conserva su orden relativo.

Build release SHA-256: ba63d06bdab16e3d8a7fb0575e9d2307e1b8af113bb4e410db92ab2e6d79566d.
APK instalado (firma compatible, payload verificado idéntico): 812ebfb3ef10461fbe2958761e728380f2f1e610f9e41ed4c4a5b1b9fa92501a.
No se ha publicado en el canal beta general ni se ha hecho commit/push.
