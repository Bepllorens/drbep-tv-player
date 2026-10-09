# Android beta 693: consolidación de las mejoras de TV y biblioteca

Se conserva el código compilado y probado en Fire TV hasta `2.0.693-beta-continuous-movies`, versionCode 693. Incluye navegación y estética renovadas, catchup/EPG y timeshift, listas M3U en SQLite, panel web del dispositivo, biblioteca TMDB y carruseles continuos.

La beta 693 elimina la realineación de cada tarjeta y añade carga por bloques de 24 a seis títulos del final. Las pruebas nativas recorrieron 53 títulos con el mando, comprobaron filtros y regreso desde la ficha. La prueba del repositorio recorrió las 748 películas en 32 bloques sin duplicados. Compilación del checkout consolidado correcta.

APK probado en el Fire (no se recompiló para publicarlo): SHA-256 `7973511114c65367d92ef1618efc1728c82060cf1cb1bfbf709d621ac38f9ccb`. El APK y las capturas permanecen como artefactos locales. Este commit no publica una versión estable ni instala un nuevo binario.

Código recuperado del directorio de trabajo de las betas. Los checkouts originales se conservan porque presentaron errores de lectura; esta consolidación se prepara sobre la rama remota existente.
