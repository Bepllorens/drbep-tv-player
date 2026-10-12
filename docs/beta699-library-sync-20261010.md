# Beta 699: progreso de listas ausentes

Reproducido en tablet 2410CRP4CG con release 698: tocar Películas cierra el proceso con SQLiteConstraintException, FOREIGN KEY constraint failed. La inicialización sincronizaba posiciones m3u guardadas para fuentes inexistentes; INSERT OR IGNORE no suprime las violaciones de clave foránea.

La sincronización ahora inserta preferencias únicamente para entradas de películas o series de una generación local activa o en previsualización. El catálogo vacío se crea normalmente. Las tareas de inicialización, metadatos y episodios gestionan errores y respetan la cancelación de coroutines. Se ofrece reintento y orientación para importar cuando la biblioteca está vacía.

Validación: build debug, release universal, lint/R8, firma oficial; pruebas unitarias de navegación/numeración y prueba Android validProgressSurvivesMissingAndDeletedSources en Fire .190: conserva progreso de película y episodio, ignora fuente ausente y acepta progreso de una fuente eliminada. Una ejecución del test completo de pantallas vacías falló porque ActivityScenario perdió la Activity; no se considera evidencia de validación visual de beta699.

Publicada únicamente en beta, versión 2.0.699-beta-library-sync. APK universal SHA-256 f4f95bd02b6039756744f1426986ebe749ac4a01878d37fefa40f2a74b1b7ef9. Firma SHA-256 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b. Descarga pública y metadatos verificados. Stable 582 y rescue 543 intactos. Respaldo de publicación: /opt/drbep-releases/beta699-publication-20261010.

La tablet permanece en 698 por petición del usuario, para probar el actualizador. Pendiente confirmar actualización y apertura funcional de Películas/Series en la tablet. No se ha desinstalado ni borrado su base de datos. Artefactos locales en artifacts/beta699 del backend, fuera de Git.
