package dev.jmcerezo.centinela.ui.componentes

/**
 * Representa los diferentes tipos de permisos y consentimientos que la app requiere.
 */
sealed class PermisoConsentimiento(val titulo: String, val introduccion: String, val proposito: String) {
    object Accesibilidad : PermisoConsentimiento("Servicio de Accesibilidad", "Centinela requiere la API de Accesibilidad para funcionar en segundo plano.", "Detectar pulsaciones de los botones físicos de volumen para iniciar o detener grabaciones de emergencia sin necesidad de encender la pantalla.")
    object Microfono : PermisoConsentimiento("Permiso de Micrófono", "El acceso al micrófono es esencial para la funcionalidad principal de la app.", "Capturar y grabar audio de alta fidelidad para generar evidencias legales válidas.")
    object Ubicacion : PermisoConsentimiento("Permiso de Ubicación", "La ubicación añade una capa de validez legal a tus grabaciones.", "Certificar el lugar exacto (coordenadas GPS y dirección) donde se realizó la grabación de la evidencia.")
    object Notificaciones : PermisoConsentimiento("Permiso de Notificaciones", "Necesario para mantener el sistema de protección activo.", "Mostrar una notificación permanente que evita que el sistema Android cierre la aplicación y garantiza que la grabación no se interrumpa.")
    object Superposicion : PermisoConsentimiento("Aparecer encima", "Permite que el proceso de grabación tenga prioridad visual.", "Garantizar que la grabación no sea interrumpida por otras aplicaciones o por el bloqueo automático del sistema.")
    object Bateria : PermisoConsentimiento("Gestión de Batería", "Evita la suspensión automática por ahorro de energía.", "Asegurar que Centinela esté siempre listo para actuar, impidiendo que Android cierre el servicio de seguridad para ahorrar batería.")
    object Biometria : PermisoConsentimiento("Seguridad Biométrica", "Protege el acceso a tus grabaciones mediante la seguridad de tu dispositivo.", "Solicitar tu huella dactilar o reconocimiento facial cada vez que se abra la aplicación para garantizar que solo tú puedas ver las evidencias. Puedes cambiar esta opción en cualquier momento desde estos ajustes.")
}
