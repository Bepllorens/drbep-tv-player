# Beta 609 · Fondo de portada y carrusel

Petición: sustituir el fondo negro por la imagen del destacado en casi toda la
pantalla y rotar entre títulos.

- Imagen panorámica como capa a pantalla completa, detrás de toda la portada.
- Degradados translúcidos en azul pizarra para conservar la legibilidad. Se elimina
  el rectángulo negro del destacado; las tarjetas mantienen su proporción propia.
- Rotación cada 10 segundos después de cargar una imagen; selección de los
  candidatos existentes en la sección y el filtro. Imagen, título y acción se
  actualizan conjuntamente al terminar la carga.
- Pausa al enfocar los controles del destacado, al bajar por el catálogo o mediante
  Pausar fondo. El componente se libera al cerrar la portada.
- No se fuerza un cartel vertical como fondo cuando no existe un panorama válido.
- Demo web actualizada en el mismo sitio privado con rotación y pausa, sin datos
  privados ni reproducción. Respeta reducción de movimiento y visibilidad de pestaña.

Cinco comprobaciones existentes de foco/arte/carga local pasan. La demo se revisó
visualmente y se comprobó el cambio de título/fondo y su pausa.
Demo: https://drbep-portada-demo.bepllorens50167.chatgpt.site
Commit de la demo: c94a5f83d366dfd19a69f88edaea39609c738ad5
Despliegue Sites confirmado como succeeded.

Instalada mediante -r en .190, versionCode 609 confirmado, sin borrar datos.
Compilación y firma verificadas; cinco pruebas estáticas pasan. Se retiraron
únicamente clases duplicadas generadas por el entorno antes de la compilación final.
SHA release: 2c6616aa147844bef7df361bf7ef004a9481fd115a0e55a910ad8ba7298f6058
SHA APK .190: ee8d4df9869a26c53334901d31305436627819739534258aefdc4c94be85d1cd
APK compatible con payload idéntico salvo META-INF. Beta 608 conservada.

Verificación visual real: captura de Inicio con Unabomber ocupando toda la
pantalla; captura posterior con Mayday, título y progreso correspondientes.
Archivos: /tmp/drbep-fire190-609-inicio.png y
/tmp/drbep-fire190-609-rotacion.png. Sin AndroidRuntime fatal en la consulta del
proceso actual. No se ha publicado el canal beta general ni se ha hecho push Android.
