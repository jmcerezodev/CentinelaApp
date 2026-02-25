# 🛡️ Centinela - Sistema de Protección Legal

<p align="center">
<img src="[https://raw.githubusercontent.com/jmcerezodev/CentinelaApp/main/app/src/main/res/drawable/logo_centinela.png](https://www.google.com/search?q=https://raw.githubusercontent.com/jmcerezodev/CentinelaApp/main/app/src/main/res/drawable/logo_centinela.png)" width="160" alt="Centinela Logo">
</p>

<p align="center">
<img src="[https://img.shields.io/badge/Kotlin-2.0.21-purple?style=for-the-badge&logo=kotlin](https://www.google.com/search?q=https://img.shields.io/badge/Kotlin-2.0.21-purple%3Fstyle%3Dfor-the-badge%26logo%3Dkotlin)" alt="Kotlin Version">
<img src="[https://img.shields.io/badge/Jetpack_Compose-Latest-green?style=for-the-badge&logo=jetpackcompose](https://www.google.com/search?q=https://img.shields.io/badge/Jetpack_Compose-Latest-green%3Fstyle%3Dfor-the-badge%26logo%3Djetpackcompose)" alt="Compose">
<img src="[https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge](https://www.google.com/search?q=https://img.shields.io/badge/Architecture-MVVM-blue%3Fstyle%3Dfor-the-badge)" alt="MVVM">
<img src="[https://img.shields.io/badge/Android-14%2B-3DDC84?style=for-the-badge&logo=android](https://www.google.com/search?q=https://img.shields.io/badge/Android-14%252B-3DDC84%3Fstyle%3Dfor-the-badge%26logo%3Dandroid)" alt="Android Version">
</p>

**Centinela** es una plataforma avanzada de captura de evidencias diseñada para garantizar la seguridad personal y la integridad documental. A diferencia de las grabadoras convencionales, Centinela actúa como un ecosistema de audio forense que certifica la validez de las pruebas mediante criptografía, geolocalización y blindaje de procesos.

---

## ✨ Características Principales

### 🎙️ Captura de Audio Forense

* **Motor Optimizado**: Utiliza la fuente `VOICE_RECOGNITION` para priorizar frecuencias de voz humana con cancelación de ruido y eco activa.
* **Alta Fidelidad**: Formato MPEG-4 (AAC) a 44.1 kHz y 128 kbps para una claridad excepcional.
* **Gestión Inteligente**: Nomenclatura correlativa para evitar sobreescrituras y facilitar la organización cronológica.

### 🔐 Integridad y Certificación Legal

* **Firma Digital SHA-256**: Generación instantánea de un Hash SHA-256 único al finalizar la grabación, actuando como una "huella dactilar" digital que garantiza la inmutabilidad de la evidencia.
* **Geolocalización Vinculada**: Captura de coordenadas GPS y dirección física exacta (Reverse Geocoding) en el momento preciso de la grabación.
* **Informes Forenses (PDF)**: Exportación de documentos oficiales con metadatos técnicos, certificados de integridad y contexto geográfico listos para procesos judiciales.

### ⚡ Activación Discreta (Panic Button)

* **Detección por Hardware**: Inicio/Parada mediante 3 pulsaciones rápidas del botón Volumen Arriba (vía Accessibility Service).
* **Operación Invisible**: Totalmente funcional con la pantalla bloqueada. Confirmación de estados mediante patrones de vibración (feedback háptico) para uso discreto.

### 📱 Widget Premium (Jetpack Glance)

* Panel de control reactivo y apilable para el escritorio. Incluye botones de grabación directa, gestión de servicios y estado de vigilia sincronizado bidireccionalmente.

---

## 🏗️ Arquitectura y Seguridad

El proyecto está construido bajo una arquitectura modular robusta diseñada para la persistencia crítica de datos:

* **Patrones de Diseño**: MVVM con Clean Architecture y flujos reactivos (`StateFlow`).
* **Blindaje de Procesos**: Uso de *Foreground Services* y *WakeLocks* para evitar que el sistema suspenda el micrófono o cierre la app en grabaciones prolongadas.
* **Seguridad Biométrica**: Acceso protegido mediante **Biometric API** (huella/rostro), manteniendo el historial oculto hasta la identificación.
* **Persistencia**: [Room Database](https://developer.android.com/training/data-storage/room) para el almacenamiento seguro y cifrado del historial de evidencias.

---

## 🚀 Instalación y Uso

1. **Clonar el repositorio**:
```bash
git clone https://github.com/jmcerezodev/CentinelaApp.git

```


2. **Importar**: Abrir en **Android Studio Ladybug** (o superior).
3. **Configuración Crítica**:
    * Conceder permisos de Micrófono y Ubicación "Todo el tiempo".
    * Habilitar el **Servicio de Accesibilidad de Centinela** para permitir el control mediante botones físicos.



---

## ⚖️ Descargo de Responsabilidad

Esta aplicación es una herramienta tecnológica para la seguridad personal. El autor no se hace responsable del uso indebido de la misma. El usuario tiene la responsabilidad legal de cumplir con la normativa vigente en su jurisdicción sobre grabación de comunicaciones y protección de datos.

---

## 👨‍💻 Autor

**J.M. Cerezo** *Desarrollador de Software | Especialista en Soluciones Tecnológicas* Como desarrollador integral de software, mi enfoque se basa en la creación de soluciones informáticas personalizadas, seleccionando siempre la tecnología más eficiente y robusta para resolver los desafíos específicos de cada proyecto.

---

*Este proyecto forma parte de mi portafolio profesional, demostrando capacidad para integrar APIs de sistema complejas, seguridad de datos y patrones de diseño avanzados.*