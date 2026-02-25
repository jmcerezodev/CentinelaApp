# 🛡️ Centinela - Sistema de Protección Legal

<p align="center">
<img src="[https://raw.githubusercontent.com/tu-usuario/centinela/main/app/src/main/res/drawable/logo_centinela.png](https://www.google.com/search?q=https://raw.githubusercontent.com/tu-usuario/centinela/main/app/src/main/res/drawable/logo_centinela.png)" width="160" alt="Centinela Logo">
</p>

<p align="center">
<img src="[https://img.shields.io/badge/Kotlin-2.0.21-purple?style=for-the-badge&logo=kotlin](https://www.google.com/search?q=https://img.shields.io/badge/Kotlin-2.0.21-purple%3Fstyle%3Dfor-the-badge%26logo%3Dkotlin)" alt="Kotlin Version">
<img src="[https://img.shields.io/badge/Jetpack_Compose-Latest-green?style=for-the-badge&logo=jetpackcompose](https://www.google.com/search?q=https://img.shields.io/badge/Jetpack_Compose-Latest-green%3Fstyle%3Dfor-the-badge%26logo%3Djetpackcompose)" alt="Compose">
<img src="[https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge](https://www.google.com/search?q=https://img.shields.io/badge/Architecture-MVVM-blue%3Fstyle%3Dfor-the-badge)" alt="MVVM">
<img src="[https://img.shields.io/badge/Android-14%2B-3DDC84?style=for-the-badge&logo=android](https://www.google.com/search?q=https://img.shields.io/badge/Android-14%252B-3DDC84%3Fstyle%3Dfor-the-badge%26logo%3Dandroid)" alt="Android Version">
</p>

**Centinela** es una solución avanzada de seguridad para Android diseñada para la captura de evidencias de audio con integridad legal certificada. A diferencia de las grabadoras convencionales, está optimizada para situaciones críticas donde la rapidez, la discreción y la inmutabilidad de la prueba son fundamentales.

---

## ✨ Características Principales

* 🎙️ **Activación por Hardware**: Inicio y parada de grabaciones mediante triple pulsación del botón volumen arriba, operativo incluso con la pantalla bloqueada vía `AccessibilityService`.
* 🔐 **Criptografía Forense (SHA-256)**: Generación automática de un hash único al finalizar cada audio para garantizar que la evidencia no ha sido manipulada.
* 📍 **Geolocalización Forense**: Vinculación de cada archivo con coordenadas GPS precisas y dirección física mediante *Fused Location Provider*.
* ⚡ **Persistencia de Proceso**: Implementación de *Foreground Services* y *WakeLocks* de alta prioridad para evitar la suspensión por optimización de energía de Android.
* 📱 **UI Reactiva**: Interfaz fluida desarrollada íntegramente en **Jetpack Compose** que refleja cambios de la base de datos en tiempo real.
* 🔇 **Feedback Háptico**: Confirmación de estados del sistema mediante patrones de vibración para un uso 100% discreto sin contacto visual.

---

## 🏗️ Arquitectura y Stack Tecnológico

El proyecto se rige por los principios de **Clean Architecture** y el patrón **MVVM**:

* **Interfaz**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material Design 3.
* **Persistencia**: [Room Database](https://developer.android.com/training/data-storage/room) con manejo de flujos asíncronos (`Flow` / `StateFlow`).
* **Inyección de Dependencias**: Implementación nativa mediante **AndroidViewModelFactory**, gestionando el ciclo de vida y la instanciación de singletons de forma eficiente y ligera.
* **Hardware & APIs**:
* `MediaRecorder`: Configurado para voz humana (AAC/MPEG_4 a 44.1 kHz).
* `Fused Location Provider API`: Geoposicionamiento de alta precisión.
* `Accessibility API`: Intercepción de eventos de botones físicos a nivel de sistema.



---

## 🚀 Instalación y Requisitos

1. **Clonar el repositorio**:
```bash
git clone https://github.com/tu-usuario/centinela.git

```


2. **Importar**: Abrir en **Android Studio Ladybug** o superior.
3. **Despliegue**: Se recomienda el uso de un **dispositivo físico** para probar la interceptación de botones de hardware y los sensores GPS.

> [!IMPORTANT]
> Para habilitar la activación por botones físicos, es necesario activar manualmente el **Servicio de Accesibilidad de Centinela** en los Ajustes del sistema de su dispositivo Android.

---

## 🛠️ Detalles de Implementación Técnica

* **Seguridad de Datos**: El ViewModel expone la lista de grabaciones mediante un `StateFlow` optimizado con `SharingStarted.WhileSubscribed(5000)`, asegurando que la base de datos solo se consulte cuando la app está activa.
* **Calidad de Audio**: Configurado a 44.1 kHz con un bitrate de 128 kbps para garantizar claridad pericial sin exceder el almacenamiento del dispositivo.

---

## ⚖️ Descargo de Responsabilidad

Esta aplicación es una herramienta tecnológica para la seguridad personal. El autor no se hace responsable del uso indebido de la misma. El usuario es responsable de conocer y cumplir la legislación vigente en su jurisdicción sobre grabación de comunicaciones y protección de datos.

---

## 👨‍💻 Autor

**J.M. Cerezo** *Desarrollador de Aplicaciones Móviles* [](linkedin.com/in/jmcerezodev)

---

*Este proyecto forma parte de mi portafolio profesional centrado en desarrollo nativo Android con Kotlin y seguridad móvil.*