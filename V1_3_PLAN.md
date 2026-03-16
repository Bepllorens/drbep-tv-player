# DRBEP TV Player v1.3 Plan

## Objetivo

Convertir la base funcional de `v1.2` en una experiencia TV mas completa y operable, con tres saltos claros de producto:

1. guia de TV real con timeline,
2. control operativo de grabaciones programadas,
3. personalizacion mas util del catalogo para uso diario.

La prioridad de `v1.3` no es seguir anadiendo overlays aislados. La prioridad es pasar de una app que ya reproduce bien a una app que tambien organiza, programa y navega contenido como producto TV.

## Estado de partida

- `v1.2` ya cubre overlay enriquecido, favoritos ordenables, mini guia, grabaciones, busqueda rapida, playback diagnostics y preferencia por canal `auto/direct/proxy`.
- Build `debug` y `release` validado.
- Fire Stick ya tiene una navegacion bastante madura con mando.
- `MainActivity` sigue siendo el principal coordinador de UI, aunque la app ya cuenta con varios repositorios y coordinadores extraidos.

## Objetivos de v1.3

1. Crear una guia tipo timeline util de verdad para navegacion por franja horaria.
2. Exponer desde la app el control de grabaciones programadas y sus estados.
3. Introducir listas personales o colecciones ademas de favoritos.
4. Dar mas control por canal y mas herramientas de diagnostico accionable.
5. Mantener consistencia de navegacion TV sin degradar zapping ni tiempos de respuesta.

## No objetivos de v1.3

- Reescritura a Kotlin o Compose.
- Sincronizacion cloud de perfiles, favoritos o listas.
- Edicion administrativa completa del backend desde la TV.
- Soporte offline.
- Motor de recomendaciones complejo.

## Backlog priorizado

### P0. Guia timeline real

Impacto: Muy alto

Esfuerzo: L

Riesgo: Medio

Trabajo:

- Crear pantalla o panel de guia con horas en horizontal y canales en vertical.
- Anadir salto rapido a `ahora`.
- Permitir navegar entre programas con el mando sin perder contexto.
- Abrir acciones de programa desde la propia timeline.
- Mostrar estado de programa actual y siguiente de forma consistente con la mini guia actual.

Criterio de salida:

- Existe una guia timeline claramente superior a la mini guia en lista.
- La navegacion con mando es usable sin depender de touch ni scroll erratico.
- Se puede grabar o crear recordatorio desde un programa seleccionado.

### P0. Gestion de grabaciones programadas

Impacto: Muy alto

Esfuerzo: M

Riesgo: Medio

Trabajo:

- Consumir `GET /api/recordings/scheduled`.
- Crear vista de grabaciones programadas con estado `scheduled`, `recording`, `completed`, `failed`, `stopped` si aplica.
- Permitir cancelar programadas si el backend ya lo soporta.
- Exponer conflictos o fallos visibles para el usuario.
- Conectar la programacion actual desde EPG con esta vista operativa.

Criterio de salida:

- La app no solo programa, tambien deja ver y gestionar lo programado.
- El usuario puede entender que se va a grabar y que ha fallado.

### P1. Listas personales y catalogo propio

Impacto: Alto

Esfuerzo: M

Riesgo: Medio

Trabajo:

- Mantener favoritos como lista rapida simple.
- Introducir listas personales locales, por ejemplo `Deportes`, `Noticias`, `Kids`, `Mis canales`.
- Permitir anadir o quitar canal a una lista desde el menu de acciones.
- Anadir filtro por lista personalizada dentro del overlay.
- Persistir listas localmente con una estructura clara y migrable.

Criterio de salida:

- El usuario puede organizar el catalogo mas alla de favoritos.
- El overlay soporta listas personales sin romper filtros existentes.

### P1. Perfil enriquecido por canal

Impacto: Alto

Esfuerzo: M

Riesgo: Bajo

Trabajo:

- Extender el store actual por canal para guardar mas preferencias.
- Evaluar campos como oculto, etiqueta personalizada, lista por defecto, modo playback, preferencia de arranque.
- Anadir una pequena pantalla o dialogo de perfil de canal.
- Reusar datos existentes de diagnostico para hacer visibles decisiones de playback.

Criterio de salida:

- El canal deja de ser solo un item reproducible y pasa a tener un perfil configurable.

### P1. Diagnostico mas accionable

Impacto: Medio

Esfuerzo: S-M

Riesgo: Bajo

Trabajo:

- Enriquecer el diagnostico con ultimos errores de canal, ruta aplicada y preferencia configurada.
- Permitir reintento controlado desde diagnostico o cambio rapido temporal de modo.
- Mostrar si una decision de playback viene de `auto`, `proxy`, `direct` o fallback por error.

Criterio de salida:

- El diagnostico deja de ser solo informativo y ayuda a resolver incidencias reales.

### P2. Home o hub de arranque

Impacto: Medio

Esfuerzo: M-L

Riesgo: Medio

Trabajo:

- Evaluar pantalla inicial tipo hub con recientes, favoritos, grabaciones y programas destacados.
- Mantener acceso inmediato al ultimo canal o modo live.
- No introducir latencia molesta al arranque.

Criterio de salida:

- Solo entrar si mejora claramente la experiencia sin penalizar el acceso rapido al directo.

## Slices recomendados

### v1.3.1

- Base de guia timeline.
- Modelo visual minimo.
- Navegacion horizontal/vertical y `jump to now`.

### v1.3.2

- Vista de grabaciones programadas.
- Estados y refresco.
- Cancelacion de programadas si backend/permiso lo permiten.

### v1.3.3

- Listas personales locales.
- Filtro por listas.
- Acciones de anadir/quitar canal a lista.

### v1.3.4

- Perfil enriquecido por canal.
- Diagnostico accionable.
- Ajustes finos de playback por canal.

## Orden recomendado

### Fase 1

- Guia timeline real.
- Reutilizacion de acciones de programa ya existentes.

### Fase 2

- Grabaciones programadas y estados.
- Cancelacion o control operativo.

### Fase 3

- Listas personales.
- Perfil enriquecido por canal.

### Fase 4

- Diagnostico accionable.
- Evaluacion de home de arranque.

## Riesgos principales

- Hacer una timeline compleja que se sienta lenta en Fire Stick.
- Sobrecargar la UI con demasiadas superficies nuevas sin una jerarquia clara.
- Cruzar demasiada logica de listas, favoritos y filtros y romper el overlay actual.
- Exponer acciones de grabaciones programadas sin revisar bien permisos o contrato backend.

## Validacion minima para cerrar v1.3

- `assembleDebug` y `assembleRelease` completan correctamente.
- Guía timeline usable con mando.
- Programacion y listado de grabaciones programadas operativos.
- Favoritos y listas personales conviven sin regresiones.
- Playback simple, proxy y DRM siguen funcionando.
- Busqueda rapida y overlay actual no regresionan.
- Fire Stick mantiene navegacion fluida sin bloqueos perceptibles.

## Recomendacion practica

Si solo se hace una cosa fuerte en `v1.3`, que sea esta combinacion:

1. Guia timeline real.
2. Vista operativa de grabaciones programadas.

Eso ya justifica por si solo un salto de version visible para el usuario.