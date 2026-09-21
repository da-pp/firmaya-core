/* ============================================================================
   FirmaYA - Catalogo inicial de tipos de evento de notificacion (CU-20).

   El catalogo definitivo de eventos configurables depende de PD-11
   (Eventos y preferencias de notificacion por defecto), que sigue [PENDIENTE]
   en la especificacion. Este catalogo es una propuesta minima necesaria para
   que EP-59..EP-63 (CU-20) sean funcionales: separa eventos operativos NO
   configurables (activacion, recuperacion de contrasena) de eventos de
   actividad configurables por el usuario.
   ============================================================================ */
SET NOCOUNT ON;

INSERT INTO firmaya.TipoEventoNotificacion (codigo, etiqueta, configurable, permite_correo, permite_plataforma, activo)
VALUES
    (N'ACTIVACION_CUENTA', N'Activacion de cuenta', 0, 1, 0, 1),
    (N'RECUPERACION_CONTRASENA', N'Recuperacion de contrasena', 0, 1, 0, 1),
    (N'CONTRATO_INVITACION_RECIBIDA', N'Invitacion a un contrato', 1, 1, 1, 1),
    (N'CONTRATO_COMENTARIO_NUEVO', N'Nuevo comentario en un contrato', 1, 1, 1, 1),
    (N'CONTRATO_SOLICITUD_FIRMA', N'Solicitud de firma pendiente', 1, 1, 1, 1),
    (N'CONTRATO_FIRMADO', N'Contrato firmado por todas las partes', 1, 1, 1, 1);
