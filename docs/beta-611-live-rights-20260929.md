# Beta 611: asignaciones explicitas en En directo

Se corrige la excepcion admin de beta 610. Las categorias ahora requieren platform_ids/groups explicitos. En snapshots administrativos, Todos y las acciones de previsualizacion se restringen asimismo a plataformas o grupos asignados. Los snapshots no administrativos conservan la pertenencia filtrada por servidor para no perder concesiones individuales.

El servidor antiguo omite groups para administradores. La app resuelve exclusivamente los group_ids del snapshot contra /api/groups con su sesion existente en el host canonico HTTPS; no infiere permisos por nombres del catalogo. El mapa de nombres se guarda localmente y se intersecta siempre con los IDs del snapshot. Si no se resuelve, no se inventan concesiones. Cache serializada version 4, binaria 15 para regenerar la vista parseada.

Validacion: 16 tests de permisos/cache correctos, compilacion release correcta. Fire .190 instalado con versionCode 611, 2.0.611-beta-live-rights. La parrilla paso de 3055 a 2224 canales. Desaparecieron las categorias no asignadas Adultos, ADULTOS e Izzi Go; XxX, Musica, TDT y Deportes son grupos explicitamente asignados al usuario. Usuario admin sin concesiones individuales en servidor. No se han modificado sus permisos ni desplegado backend.

Release SHA256 a6a75e9814d0bb8017891f10385d82c36efd7458eb71f50c6b1a3e218e7f9d4e
APK dispositivo SHA256 d642b08e2c8f94b685e3556d8f9b973fd428ada925d323df9b91b72b93bb0282
Firma y payload entre variantes verificados. Instalacion conservando datos; sin publicacion general ni commit/push Android.
