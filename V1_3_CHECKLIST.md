# DRBEP TV Player v1.3 Checklist

## Epic 1. Guia timeline

- [ ] Crear estructura de datos para timeline visible por franjas
- [ ] Crear panel o pantalla de guia horizontal/vertical
- [ ] Implementar `jump to now`
- [ ] Mantener foco y scroll consistentes con mando
- [ ] Abrir acciones de programa desde timeline
- [ ] Validar rendimiento en Fire Stick

## Epic 2. Grabaciones programadas

- [ ] Consumir `GET /api/recordings/scheduled`
- [ ] Crear vista de programadas
- [ ] Mostrar estados de grabacion claramente
- [ ] Anadir refresco manual
- [ ] Permitir cancelar una grabacion programada si el endpoint es usable desde la app
- [ ] Mostrar conflictos o errores relevantes

## Epic 3. Listas personales

- [ ] Definir modelo de persistencia local para listas
- [ ] Crear store de listas personales
- [ ] Anadir accion de canal para meter/quitar de lista
- [ ] Integrar listas en el filtro del overlay
- [ ] Validar convivencia con favoritos

## Epic 4. Perfil por canal

- [ ] Extender preferencias por canal mas alla de `auto/direct/proxy`
- [ ] Crear dialogo o vista de perfil de canal
- [ ] Permitir ocultar canal o etiquetarlo
- [ ] Revisar compatibilidad con filtros y listas

## Epic 5. Diagnostico accionable

- [ ] Mostrar decision de playback de forma mas clara
- [ ] Incluir ultimo error por canal si aplica
- [ ] Anadir accion de reintento o cambio temporal de modo
- [ ] Validar que el diagnostico no rompa el flujo principal TV

## Epic 6. Validacion de cierre

- [ ] `assembleDebug` OK
- [ ] `assembleRelease` OK
- [ ] Timeline usable con mando
- [ ] Programadas visibles y gestionables
- [ ] Favoritos y listas personales OK
- [ ] Busqueda rapida OK
- [ ] Overlay principal OK
- [ ] Playback simple, proxy y DRM OK

## Corte minimo recomendable

- [ ] Timeline base
- [ ] Programadas base
- [ ] Validacion Fire Stick