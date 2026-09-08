# Rincón

Una app Android de productividad personal que se parece más a un escritorio
acogedor que a un panel de control. Notas de papel que se arrastran, tareas,
objetivos, hábitos, estudio, calendario y recordatorios — todo local, todo
offline.

> **Estado:** primera versión funcional (MVP pulido). Kotlin + Jetpack Compose,
> lista para generar APK.

---

## Cómo ejecutarla

### Opción A — Android Studio (recomendado)

1. Android Studio Ladybug (2024.2) o superior.
2. `File → Open` y elige la carpeta del proyecto.
3. Espera a que Gradle sincronice (descarga AGP 8.7.3, Kotlin 2.0.21 y Compose BOM 2024.11).
4. `Run ▶` sobre un emulador (API 26+) o un móvil con depuración USB activada.

### Opción B — línea de comandos

Necesitas JDK 17 y el Android SDK (`ANDROID_HOME` apuntando a él).

```bash
./gradlew assembleDebug          # APK de depuración
./gradlew installDebug           # instalar en el dispositivo conectado
./gradlew testDebugUnitTest      # tests unitarios
./gradlew assembleRelease        # APK optimizado (R8 + shrink)
```

APK generado en:

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Instalar a mano: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Opción C — descargar el APK de GitHub Actions

Cada push ejecuta el workflow *Android CI*, que compila, pasa los tests y sube
los APK como artefactos. Entra en la pestaña **Actions**, abre la última
ejecución y descarga `rincon-debug-apk` o `rincon-release-apk`.

> El `release` va firmado con la clave de depuración para que sea instalable sin
> configuración extra. Antes de publicar en Play hay que sustituirla por una
> keystore propia en `app/build.gradle.kts`.

---

## Requisitos

| | |
|---|---|
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 35 |
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.11.00) |
| JDK | 17 |

`minSdk 26` es una decisión consciente: permite usar `java.time` sin
*desugaring* y las APIs modernas de vibración, y cubre prácticamente todo el
parque de dispositivos en uso.

---

## Arquitectura

```
com.rincon.espacio
├── core/
│   ├── design/      Color, Type, Shape, Motion, Theme  ← el sistema visual
│   ├── feedback/    Hápticos + sonidos (capa única)
│   └── util/        Fechas en castellano
├── data/
│   ├── local/       Room: entidades, DAOs, base de datos
│   ├── prefs/       DataStore: ajustes
│   └── repo/        Repositorios + mappers entidad ↔ dominio
├── domain/
│   ├── model/       Modelos puros, sin Android
│   └── usecase/     ObserveDay: la única fuente de verdad de "el día"
├── notifications/   AlarmManager, canales, receptores, RepeatMath
├── di/              AppContainer (contenedor manual)
└── ui/
    ├── components/  NoteCanvas, PaperNote, TaskRow, GoalCard, HabitCard...
    ├── icons/       Familia de iconos propia (ImageVector, cero PNGs)
    ├── navigation/  Rutas + NavHost + barra inferior
    ├── screens/     Inicio, Escritorio, Hoy, Calendario, Yo, Objetivos...
    └── vm/          ViewModels + fábrica
```

Flujo de datos en una sola dirección: Room → Repositorio → `Flow` → ViewModel →
`StateFlow` → Compose. La UI nunca toca la base de datos ni el `AlarmManager`.

### Decisiones que conviene conocer

**La nota es el átomo.** Una nota puede quedarse como papel libre en el
escritorio o ganar fecha, hora, prioridad, subtareas, recordatorio y vínculo con
un objetivo o una asignatura. "Convertir en tarea" no crea otro objeto: enciende
un `boolean`. Por eso el escritorio y el sistema de productividad son la misma
cosa y no dos listas que se contradicen.

**Contenedor manual en lugar de Hilt.** El grafo es pequeño y estable; un
contenedor explícito (`di/AppContainer.kt`) se lee de un vistazo, evita una capa
de generación de código y acelera la compilación. Migrar a Hilt sería un cambio
localizado en ese archivo.

**Un único sistema de movimiento.** Todos los muelles viven en
`core/design/Motion.kt`. Ningún componente inventa sus propios números, así que
"reducir movimiento" funciona en toda la app a la vez y el carácter del
movimiento es coherente.

**Alarmas honestas.** Android restringe las alarmas exactas (permiso propio
desde Android 12, no autoconcedido desde Android 14). En vez de fingir que
siempre funcionan: si el sistema lo permite se usa `setExactAndAllowWhileIdle`;
si no, se degrada a `setWindow` con una ventana de 5 minutos. Las repeticiones
no usan `setRepeating` (inexacto): tras cada disparo se calcula y programa la
siguiente ocurrencia, y todo se reprograma al reiniciar, al cambiar la hora y al
actualizar la app.

**Coordenadas del escritorio.** `x` se guarda como fracción del ancho útil y `y`
en dp absolutos: al cambiar de móvil el escritorio se reparte proporcionalmente
en horizontal y conserva el orden vertical.

**Material 3 como base técnica, no como aspecto.** Se usa por el *ripple*, la
selección de texto y la accesibilidad; los colores, tipografías, formas y
controles (interruptor, casillas, selectores de fecha y hora, panel inferior)
son propios.

---

## La física de las notas

En `ui/components/NoteCanvas.kt`:

- la posición dibujada es un estado plano, escrito directamente desde el
  manejador del gesto, para que el papel **no vaya un fotograma por detrás del
  dedo**;
- al agarrar: escala +5,5 %, sombra más abierta y capa superior;
- al mover: inclinación proporcional a la dirección, con tope de 7°;
- al soltar: se predice el punto de reposo con la velocidad medida
  (`VelocityTracker`) y se llega a él con un muelle que hereda parte de esa
  velocidad — inercia y asentamiento en un solo movimiento;
- imán suave: se alinea con el canto de otra nota, se apila justo debajo o se
  ajusta a una rejilla ancha, siempre dentro de una tolerancia estrecha. Fuera
  de ella, la nota se queda exactamente donde la dejaste.

Con "Reducir movimiento" activo los muelles se vuelven críticos y la
inclinación baja al 25 %: la app sigue explicando lo que pasa, sin balanceo.

---

## Sonido y vibración

Los seis efectos (`res/raw/sfx_*.wav`) están **sintetizados**, no descargados:
tonos suaves con envolvente exponencial y ruido filtrado para el papel. Pesan
200 KB en total. Se pueden apagar del todo o bajar de intensidad desde *Yo →
Cómo se siente*.

---

## Tests

```bash
./gradlew testDebugUnitTest
```

Cubren la lógica que más duele si se rompe: el imán del escritorio, las rachas
de hábitos (incluidos los fines de semana), el cálculo de repeticiones de
recordatorios (fines de semana, meses de distinta longitud, puestas al día), las
utilidades de fecha y las reglas de progreso del dominio.

---

## Privacidad

Todo se guarda en el teléfono: Room para los datos y DataStore para los ajustes.
No hay cuentas, ni servidores, ni analítica, ni permisos de red. La app funciona
igual en modo avión.

Permisos que pide, y para qué:

| Permiso | Para qué |
|---|---|
| `POST_NOTIFICATIONS` | mostrar los recordatorios que tú creas |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | que lleguen a la hora, no minutos después |
| `VIBRATE` | retorno háptico breve |
| `RECEIVE_BOOT_COMPLETED` | reprogramar tus avisos tras reiniciar |

---

## Qué falta (a propósito)

La primera versión prefiere cinco cosas bien hechas a treinta a medias. Quedan
fuera, con la arquitectura ya preparada para ellas: sincronización en la nube,
widgets de pantalla de inicio, adjuntar imágenes a las notas, exportar/importar
y estadísticas de estudio a largo plazo.
