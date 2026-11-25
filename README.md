# Artefgo Metro – Launcher estilo Windows Phone para XTRONS UA95MTVL

Launcher para Android en tres columnas:

| Izquierda | Centro | Derecha |
|---|---|---|
| Mosaicos tipo Windows Phone con todas las apps (Radio, DSP, ZLink, Ajustes…). Mantén presionado un mosaico para anclarlo, hacerlo ancho o ver la info de la app. | Carro animado en carretera. El cielo sigue la hora real: amanecer, día, tarde más cálida y oscura, y noche con luna y estrellas desde las 18:00. Faros y postes se encienden de noche. Si llueve, la escena muestra lluvia. | Hora, fecha, clima, velocímetro, RPM, altitud, voltaje de batería y la canción que suena, con controles. |

### Temas y fondo
En configuración → **tema** hay **50 temas**:
- 20 oscuros clásicos de Windows Phone, con todos los mosaicos del mismo color.
- 10 claros con fondo blanco.
- 10 degradados: Atardecer, Océano, Aurora, Neón, Bosque, Lava, Medianoche, Uva, Menta y Carbón.
- 8 multicolor: Mosaico, Pastel, Neón, Tierra, Frío, Cálido, Grises y Arcoíris.
- 2 especiales: Fibra de carbono y Cristal.

En **fondo con imagen** puedes poner una foto tuya de fondo y ajustar cuánto se oscurece. El tema sigue dando el color de los mosaicos.

**Editar:** mantén presionado un mosaico o widget (o toca el lápiz) para moverlo, cambiarle tamaño o color, renombrarlo, ocultarlo, quitarlo o añadir widgets.

Toca el carro (o el engrane) para elegir **marca, tipo (sedán, hatchback, SUV, mini SUV) y color**. También hay una opción para previsualizar cualquier hora del día.

## Compilar el APK (sin instalar nada)
1. Sube esta carpeta a un repositorio de GitHub.
2. GitHub Actions compila solo (pestaña **Actions → Compilar APK**).
3. Descarga `ArtefgoMetro.apk` desde **Releases → latest** y cópialo a una USB.

También puedes abrir el proyecto en Android Studio y darle *Run*.

## Instalar en la radio
1. Instala el APK desde la USB con el administrador de archivos de la radio.
2. Presiona *Inicio* y elige **Artefgo Metro** → *Siempre*. Si no pregunta, en configuración → sistema → **Launcher predeterminado**.
3. Acepta los permisos de ubicación y Bluetooth.
4. En la tarjeta de música, toca **Permitir acceso** y activa *Artefgo Metro* en el acceso a notificaciones.

## De dónde sale cada dato
- **Velocidad y altitud:** GPS de la radio. Si hay OBD2 conectado, la velocidad se toma del carro.
- **Clima:** Open-Meteo (gratis, sin clave) con la ubicación del GPS. Necesita internet (4G o Wi-Fi).
- **RPM y voltaje de batería:** Android no da estos datos por sí solo. Necesitas un adaptador **OBD2 ELM327 Bluetooth** (unos $150–300 MXN). Emparéjalo con la radio y elígelo en configuración → OBD2. También muestra la temperatura del motor.
- **Canción:** se lee de cualquier app con controles multimedia: Spotify, YouTube Music, Poweramp, Tidal, Deezer, Apple Music, la música Bluetooth de la radio, etc.

## Limitaciones conocidas
- **Android Auto / CarPlay por ZLink:** la canción del teléfono solo aparece si ZLink publica sus controles multimedia en Android. Muchas versiones sí lo hacen, pero no todas.
- Las marcas aparecen por nombre. Los logotipos oficiales no se dibujan porque son marcas registradas.
