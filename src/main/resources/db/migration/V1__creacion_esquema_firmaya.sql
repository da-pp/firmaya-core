/* ============================================================================
   FirmaYA - Esquema inicial para Microsoft SQL Server
   Migracion Flyway V1, aplicada sobre la base de datos firmaya_db ya existente
   en el contenedor docker "firmaya-sqlserver" (ver compose.yaml).

   Contenido identico al script de referencia del proyecto:
   FirmaYA_Creacion_Base_Datos_SQLServer_v1.sql
   Alcance: 21 casos de uso / 69 endpoints de la especificacion en espanol v2.

   Distincion documental:
   - Reglas confirmadas: D01-D10, P01-P08, B01-B05.
   - Diseno tecnico propuesto: UUID/UNIQUEIDENTIFIER, esquema firmaya, tabla de
     parametros, estructuras de sesiones, tokens, eventos y notificaciones.
   - NO se fija ningun valor pendiente: se registra como NULL en ParametroSistema.
   - Los secretos de tokens se guardan como hash; el OTP como derivacion protegida
     con secreto del servidor (NO hash simple del numero de seis digitos).
   - NO hay DROP TABLE, datos de prueba, usuarios administradores ni contrasenas.
   ============================================================================ */
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() IN (N'master', N'model', N'msdb', N'tempdb')
    THROW 50001, 'Seleccione la base de datos de FirmaYA antes de ejecutar.', 1;

IF SCHEMA_ID(N'firmaya') IS NOT NULL
   AND EXISTS (SELECT 1 FROM sys.tables WHERE schema_id = SCHEMA_ID(N'firmaya'))
    THROW 50002, 'El esquema firmaya ya tiene tablas. Use migraciones; no reejecute la instalacion inicial.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF SCHEMA_ID(N'firmaya') IS NULL
        EXEC(N'CREATE SCHEMA firmaya AUTHORIZATION dbo');

    /* 01. Identidad interna. Administracion y activacion son independientes. */
    CREATE TABLE firmaya.Usuario (
        id_usuario UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Usuario_Id DEFAULT NEWSEQUENTIALID(),
        nombre NVARCHAR(100) NOT NULL,
        apellido NVARCHAR(100) NOT NULL,
        correo_electronico NVARCHAR(254) NOT NULL,
        correo_normalizado AS LOWER(LTRIM(RTRIM(correo_electronico))) PERSISTED,
        hash_contrasena NVARCHAR(255) NULL,
        rol_global VARCHAR(24) NOT NULL,
        estado_administrativo VARCHAR(8) NOT NULL CONSTRAINT DF_Usuario_EstadoAdm DEFAULT ('ACTIVO'),
        estado_activacion VARCHAR(12) NOT NULL CONSTRAINT DF_Usuario_Activacion DEFAULT ('PENDIENTE'),
        intentos_inicio_fallidos INT NOT NULL CONSTRAINT DF_Usuario_Intentos DEFAULT (0),
        bloqueo_hasta DATETIMEOFFSET(3) NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Usuario_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_activacion DATETIMEOFFSET(3) NULL,
        fecha_actualizacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Usuario_Actualizacion DEFAULT SYSDATETIMEOFFSET(),
        marca_concurrencia ROWVERSION,
        CONSTRAINT PK_Usuario PRIMARY KEY (id_usuario),
        CONSTRAINT UQ_Usuario_Correo UNIQUE (correo_normalizado),
        CONSTRAINT CK_Usuario_Rol CHECK (rol_global IN ('ADMINISTRADOR','ABOGADO','AGENTE_INMOBILIARIO')),
        CONSTRAINT CK_Usuario_EstadoAdm CHECK (estado_administrativo IN ('ACTIVO','INACTIVO')),
        CONSTRAINT CK_Usuario_Activacion CHECK (estado_activacion IN ('PENDIENTE','COMPLETADA')),
        CONSTRAINT CK_Usuario_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) BETWEEN 1 AND 100),
        CONSTRAINT CK_Usuario_Apellido CHECK (LEN(LTRIM(RTRIM(apellido))) BETWEEN 1 AND 100),
        CONSTRAINT CK_Usuario_Correo CHECK (LEN(LTRIM(RTRIM(correo_electronico))) BETWEEN 3 AND 254),
        CONSTRAINT CK_Usuario_Intentos CHECK (intentos_inicio_fallidos >= 0),
        CONSTRAINT CK_Usuario_Activado CHECK (
            (estado_activacion = 'PENDIENTE' AND fecha_activacion IS NULL)
            OR (estado_activacion = 'COMPLETADA' AND fecha_activacion IS NOT NULL AND hash_contrasena IS NOT NULL)
        )
    );

    /* 02. Las sesiones internas son revocables por usuario y por sesion. */
    CREATE TABLE firmaya.SesionInterna (
        id_sesion_interna UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_SesionInterna_Id DEFAULT NEWSEQUENTIALID(),
        id_usuario UNIQUEIDENTIFIER NOT NULL,
        hash_credencial VARBINARY(32) NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_SesionInterna_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultima_actividad DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_SesionInterna_Actividad DEFAULT SYSDATETIMEOFFSET(),
        fecha_expiracion DATETIMEOFFSET(3) NOT NULL,
        fecha_revocacion DATETIMEOFFSET(3) NULL,
        direccion_ip VARCHAR(45) NULL,
        agente_usuario NVARCHAR(512) NULL,
        CONSTRAINT PK_SesionInterna PRIMARY KEY (id_sesion_interna),
        CONSTRAINT FK_SesionInterna_Usuario FOREIGN KEY (id_usuario) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_SesionInterna_Credencial UNIQUE (hash_credencial),
        CONSTRAINT CK_SesionInterna_Fechas CHECK (fecha_expiracion > fecha_creacion AND fecha_ultima_actividad >= fecha_creacion)
    );

    /* 03-05. Plantillas versionadas y su formulario dinamico. */
    CREATE TABLE firmaya.Plantilla (
        id_plantilla UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Plantilla_Id DEFAULT NEWSEQUENTIALID(),
        nombre NVARCHAR(200) NOT NULL,
        tipo_contrato VARCHAR(15) NOT NULL,
        descripcion NVARCHAR(500) NULL,
        estado VARCHAR(8) NOT NULL CONSTRAINT DF_Plantilla_Estado DEFAULT ('ACTIVA'),
        id_version_actual UNIQUEIDENTIFIER NULL,
        id_usuario_creador UNIQUEIDENTIFIER NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Plantilla_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_actualizacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Plantilla_Actualizacion DEFAULT SYSDATETIMEOFFSET(),
        marca_concurrencia ROWVERSION,
        CONSTRAINT PK_Plantilla PRIMARY KEY (id_plantilla),
        CONSTRAINT FK_Plantilla_Creador FOREIGN KEY (id_usuario_creador) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT CK_Plantilla_Tipo CHECK (tipo_contrato IN ('ARRENDAMIENTO','VENTA','MANDATO','OTRO')),
        CONSTRAINT CK_Plantilla_Estado CHECK (estado IN ('ACTIVA','INACTIVA')),
        CONSTRAINT CK_Plantilla_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) BETWEEN 1 AND 200)
    );

    CREATE TABLE firmaya.VersionPlantilla (
        id_version_plantilla UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_VersionPlantilla_Id DEFAULT NEWSEQUENTIALID(),
        id_plantilla UNIQUEIDENTIFIER NOT NULL,
        numero_version INT NOT NULL,
        contenido NVARCHAR(MAX) NOT NULL,
        id_usuario_autor UNIQUEIDENTIFIER NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_VersionPlantilla_Creacion DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_VersionPlantilla PRIMARY KEY (id_version_plantilla),
        CONSTRAINT FK_VersionPlantilla_Plantilla FOREIGN KEY (id_plantilla) REFERENCES firmaya.Plantilla(id_plantilla),
        CONSTRAINT FK_VersionPlantilla_Autor FOREIGN KEY (id_usuario_autor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_VersionPlantilla_Numero UNIQUE (id_plantilla, numero_version),
        CONSTRAINT UQ_VersionPlantilla_Propiedad UNIQUE (id_plantilla, id_version_plantilla),
        CONSTRAINT CK_VersionPlantilla_Numero CHECK (numero_version >= 1),
        CONSTRAINT CK_VersionPlantilla_Contenido CHECK (LEN(LTRIM(RTRIM(contenido))) > 0)
    );

    ALTER TABLE firmaya.Plantilla ADD CONSTRAINT FK_Plantilla_VersionActual
        FOREIGN KEY (id_plantilla, id_version_actual)
        REFERENCES firmaya.VersionPlantilla(id_plantilla, id_version_plantilla);

    CREATE TABLE firmaya.CampoPlantilla (
        id_campo_plantilla UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_CampoPlantilla_Id DEFAULT NEWSEQUENTIALID(),
        id_version_plantilla UNIQUEIDENTIFIER NOT NULL,
        nombre_marcador NVARCHAR(100) NOT NULL,
        etiqueta NVARCHAR(200) NOT NULL,
        tipo_dato VARCHAR(8) NOT NULL,
        obligatorio BIT NOT NULL CONSTRAINT DF_CampoPlantilla_Obligatorio DEFAULT (0),
        orden_visual INT NOT NULL,
        valor_predeterminado NVARCHAR(MAX) NULL,
        restricciones_json NVARCHAR(MAX) NULL,
        CONSTRAINT PK_CampoPlantilla PRIMARY KEY (id_campo_plantilla),
        CONSTRAINT FK_CampoPlantilla_Version FOREIGN KEY (id_version_plantilla) REFERENCES firmaya.VersionPlantilla(id_version_plantilla),
        CONSTRAINT UQ_CampoPlantilla_Marcador UNIQUE (id_version_plantilla, nombre_marcador),
        CONSTRAINT UQ_CampoPlantilla_Orden UNIQUE (id_version_plantilla, orden_visual),
        CONSTRAINT CK_CampoPlantilla_Tipo CHECK (tipo_dato IN ('TEXTO','NUMERO','FECHA','BOOLEANO')),
        CONSTRAINT CK_CampoPlantilla_Orden CHECK (orden_visual >= 0),
        CONSTRAINT CK_CampoPlantilla_Nombre CHECK (LEN(LTRIM(RTRIM(nombre_marcador))) > 0),
        CONSTRAINT CK_CampoPlantilla_RestriccionesJson CHECK (restricciones_json IS NULL OR ISJSON(restricciones_json) = 1)
    );

    /* 06-07. Contrato y contenido historico. Puntero actual = ciclo FK controlado. */
    CREATE TABLE firmaya.Contrato (
        id_contrato UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Contrato_Id DEFAULT NEWSEQUENTIALID(),
        nombre NVARCHAR(200) NOT NULL,
        tipo_contrato VARCHAR(15) NOT NULL,
        partes_involucradas NVARCHAR(1000) NOT NULL,
        fecha_inicio DATE NOT NULL,
        fecha_expiracion DATE NULL,
        descripcion_propiedad NVARCHAR(2000) NULL,
        id_responsable UNIQUEIDENTIFIER NOT NULL,
        id_plantilla UNIQUEIDENTIFIER NOT NULL,
        id_version_plantilla UNIQUEIDENTIFIER NOT NULL,
        id_version_actual UNIQUEIDENTIFIER NULL,
        estado VARCHAR(19) NOT NULL CONSTRAINT DF_Contrato_Estado DEFAULT ('BORRADOR'),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Contrato_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultima_actividad DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Contrato_Actividad DEFAULT SYSDATETIMEOFFSET(),
        fecha_firma DATETIMEOFFSET(3) NULL,
        fecha_archivado DATETIMEOFFSET(3) NULL,
        marca_concurrencia ROWVERSION,
        CONSTRAINT PK_Contrato PRIMARY KEY (id_contrato),
        CONSTRAINT FK_Contrato_Responsable FOREIGN KEY (id_responsable) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_Contrato_VersionPlantilla FOREIGN KEY (id_plantilla, id_version_plantilla)
            REFERENCES firmaya.VersionPlantilla(id_plantilla, id_version_plantilla),
        CONSTRAINT CK_Contrato_Estado CHECK (estado IN ('BORRADOR','EN_REVISION','LISTO_PARA_FIRMAR','FIRMADO','ARCHIVADO')),
        CONSTRAINT CK_Contrato_Tipo CHECK (tipo_contrato IN ('ARRENDAMIENTO','VENTA','MANDATO','OTRO')),
        CONSTRAINT CK_Contrato_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) BETWEEN 1 AND 200),
        CONSTRAINT CK_Contrato_Partes CHECK (LEN(LTRIM(RTRIM(partes_involucradas))) BETWEEN 1 AND 1000),
        CONSTRAINT CK_Contrato_Firma CHECK (
            (estado IN ('FIRMADO','ARCHIVADO') AND fecha_firma IS NOT NULL)
            OR (estado NOT IN ('FIRMADO','ARCHIVADO') AND fecha_firma IS NULL)
        ),
        CONSTRAINT CK_Contrato_Archivo CHECK (
            (estado = 'ARCHIVADO' AND fecha_archivado IS NOT NULL)
            OR (estado <> 'ARCHIVADO' AND fecha_archivado IS NULL)
        )
        /* Regla fecha_expiracion >= fecha_inicio: PENDIENTE, no se impone. */
    );

    CREATE TABLE firmaya.VersionContrato (
        id_version_contrato UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_VersionContrato_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        numero_version INT NOT NULL,
        contenido NVARCHAR(MAX) NOT NULL,
        hash_sha256 VARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        id_usuario_autor UNIQUEIDENTIFIER NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_VersionContrato_Creacion DEFAULT SYSDATETIMEOFFSET(),
        comentario_version NVARCHAR(500) NULL,
        id_version_restaurada UNIQUEIDENTIFIER NULL,
        CONSTRAINT PK_VersionContrato PRIMARY KEY (id_version_contrato),
        CONSTRAINT FK_VersionContrato_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_VersionContrato_Autor FOREIGN KEY (id_usuario_autor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_VersionContrato_Numero UNIQUE (id_contrato, numero_version),
        CONSTRAINT UQ_VersionContrato_Propiedad UNIQUE (id_contrato, id_version_contrato),
        CONSTRAINT UQ_VersionContrato_HashVersion UNIQUE (id_version_contrato, hash_sha256),
        CONSTRAINT CK_VersionContrato_Numero CHECK (numero_version >= 1),
        CONSTRAINT CK_VersionContrato_Contenido CHECK (LEN(LTRIM(RTRIM(contenido))) >= 1),
        CONSTRAINT CK_VersionContrato_Hash CHECK (
            LEN(hash_sha256) = 64 AND hash_sha256 NOT LIKE '%[^0-9a-f]%'
        )
    );

    ALTER TABLE firmaya.VersionContrato ADD CONSTRAINT FK_VersionContrato_Restaurada
        FOREIGN KEY (id_contrato, id_version_restaurada)
        REFERENCES firmaya.VersionContrato(id_contrato, id_version_contrato);

    ALTER TABLE firmaya.Contrato ADD CONSTRAINT FK_Contrato_VersionActual
        FOREIGN KEY (id_contrato, id_version_actual)
        REFERENCES firmaya.VersionContrato(id_contrato, id_version_contrato);

    /* 08-09. Participantes e invitaciones. Responsable se representa en Contrato. */
    CREATE TABLE firmaya.Participante (
        id_participante UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Participante_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_usuario_interno UNIQUEIDENTIFIER NULL,
        nombre NVARCHAR(150) NOT NULL,
        correo_electronico NVARCHAR(254) NOT NULL,
        correo_normalizado AS LOWER(LTRIM(RTRIM(correo_electronico))) PERSISTED,
        rol_participacion VARCHAR(12) NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Participante_Creacion DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_Participante PRIMARY KEY (id_participante),
        CONSTRAINT FK_Participante_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_Participante_Usuario FOREIGN KEY (id_usuario_interno) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_Participante_Correo UNIQUE (id_contrato, correo_normalizado),
        CONSTRAINT UQ_Participante_Propiedad UNIQUE (id_contrato, id_participante),
        CONSTRAINT CK_Participante_Rol CHECK (rol_participacion IN ('FIRMANTE','REVISOR','SOLO_LECTURA')),
        CONSTRAINT CK_Participante_Nombre CHECK (LEN(LTRIM(RTRIM(nombre))) BETWEEN 1 AND 150),
        CONSTRAINT CK_Participante_Correo CHECK (LEN(LTRIM(RTRIM(correo_electronico))) BETWEEN 3 AND 254)
    );

    CREATE TABLE firmaya.Invitacion (
        id_invitacion UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Invitacion_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_participante UNIQUEIDENTIFIER NOT NULL,
        id_usuario_emisor UNIQUEIDENTIFIER NOT NULL,
        mensaje_personalizado NVARCHAR(500) NULL,
        estado VARCHAR(16) NOT NULL CONSTRAINT DF_Invitacion_Estado DEFAULT ('PENDIENTE'),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Invitacion_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultimo_envio DATETIMEOFFSET(3) NULL,
        fecha_aceptacion DATETIMEOFFSET(3) NULL,
        fecha_expiracion DATETIMEOFFSET(3) NULL,
        CONSTRAINT PK_Invitacion PRIMARY KEY (id_invitacion),
        CONSTRAINT FK_Invitacion_Participante FOREIGN KEY (id_contrato, id_participante)
            REFERENCES firmaya.Participante(id_contrato, id_participante),
        CONSTRAINT FK_Invitacion_Emisor FOREIGN KEY (id_usuario_emisor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_Invitacion_Participante UNIQUE (id_participante),
        CONSTRAINT CK_Invitacion_Estado CHECK (estado IN ('PENDIENTE','ENVIADA','ERROR_ENVIO','ACEPTADA','VENCIDA','REVOCADA')),
        CONSTRAINT CK_Invitacion_Fechas CHECK (fecha_expiracion IS NULL OR fecha_expiracion > fecha_creacion)
        /* Catalogo de estados de invitacion = propuesta tecnica PD-04. */
    );

    /* 10-12. Un proceso congela version/hash y su lista de firmantes. */
    CREATE TABLE firmaya.ProcesoFirma (
        id_proceso_firma UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_ProcesoFirma_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_version_objetivo UNIQUEIDENTIFIER NOT NULL,
        hash_objetivo VARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        estado VARCHAR(10) NOT NULL CONSTRAINT DF_ProcesoFirma_Estado DEFAULT ('PREPARADO'),
        finalizado BIT NOT NULL CONSTRAINT DF_ProcesoFirma_Finalizado DEFAULT (0),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_ProcesoFirma_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_cancelacion DATETIMEOFFSET(3) NULL,
        fecha_finalizacion DATETIMEOFFSET(3) NULL,
        motivo_cancelacion NVARCHAR(500) NULL,
        id_usuario_cancelador UNIQUEIDENTIFIER NULL,
        CONSTRAINT PK_ProcesoFirma PRIMARY KEY (id_proceso_firma),
        CONSTRAINT FK_ProcesoFirma_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_ProcesoFirma_Version FOREIGN KEY (id_contrato, id_version_objetivo)
            REFERENCES firmaya.VersionContrato(id_contrato, id_version_contrato),
        CONSTRAINT FK_ProcesoFirma_Hash FOREIGN KEY (id_version_objetivo, hash_objetivo)
            REFERENCES firmaya.VersionContrato(id_version_contrato, hash_sha256),
        CONSTRAINT FK_ProcesoFirma_Cancelador FOREIGN KEY (id_usuario_cancelador) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT UQ_ProcesoFirma_Propiedad UNIQUE (id_contrato, id_proceso_firma),
        CONSTRAINT UQ_ProcesoFirma_Version UNIQUE (id_proceso_firma, id_version_objetivo),
        CONSTRAINT CK_ProcesoFirma_Estado CHECK (estado IN ('PREPARADO','ACTIVO','CANCELADO','COMPLETADO')),
        CONSTRAINT CK_ProcesoFirma_Cierre CHECK (
            (estado IN ('PREPARADO','ACTIVO') AND finalizado = 0 AND fecha_cancelacion IS NULL AND fecha_finalizacion IS NULL)
            OR (estado = 'CANCELADO' AND finalizado = 1 AND fecha_cancelacion IS NOT NULL AND fecha_finalizacion IS NULL)
            OR (estado = 'COMPLETADO' AND finalizado = 1 AND fecha_finalizacion IS NOT NULL AND fecha_cancelacion IS NULL)
        )
    );

    CREATE UNIQUE INDEX UX_ProcesoFirma_UnicoVigente
        ON firmaya.ProcesoFirma(id_contrato) WHERE finalizado = 0;

    CREATE TABLE firmaya.FirmanteProceso (
        id_firmante_proceso UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_FirmanteProceso_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_proceso_firma UNIQUEIDENTIFIER NOT NULL,
        id_participante UNIQUEIDENTIFIER NOT NULL,
        nombre_congelado NVARCHAR(150) NOT NULL,
        correo_congelado NVARCHAR(254) NOT NULL,
        fecha_congelacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_FirmanteProceso_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_FirmanteProceso PRIMARY KEY (id_firmante_proceso),
        CONSTRAINT FK_FirmanteProceso_Proceso FOREIGN KEY (id_contrato, id_proceso_firma)
            REFERENCES firmaya.ProcesoFirma(id_contrato, id_proceso_firma),
        CONSTRAINT FK_FirmanteProceso_Participante FOREIGN KEY (id_contrato, id_participante)
            REFERENCES firmaya.Participante(id_contrato, id_participante),
        CONSTRAINT UQ_FirmanteProceso_Unico UNIQUE (id_proceso_firma, id_participante),
        CONSTRAINT UQ_FirmanteProceso_Propiedad UNIQUE (id_proceso_firma, id_firmante_proceso)
    );

    CREATE TABLE firmaya.SolicitudFirma (
        id_solicitud_firma UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_SolicitudFirma_Id DEFAULT NEWSEQUENTIALID(),
        id_proceso_firma UNIQUEIDENTIFIER NOT NULL,
        id_firmante_proceso UNIQUEIDENTIFIER NOT NULL,
        estado VARCHAR(15) NOT NULL CONSTRAINT DF_SolicitudFirma_Estado DEFAULT ('PENDIENTE'),
        estado_entrega VARCHAR(10) NOT NULL CONSTRAINT DF_SolicitudFirma_Entrega DEFAULT ('PENDIENTE'),
        canal VARCHAR(18) NOT NULL CONSTRAINT DF_SolicitudFirma_Canal DEFAULT ('CORREO_ELECTRONICO'),
        mensaje_personalizado NVARCHAR(500) NULL,
        fecha_limite DATE NULL,
        fecha_expiracion_enlace DATETIMEOFFSET(3) NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_SolicitudFirma_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultimo_envio DATETIMEOFFSET(3) NULL,
        fecha_firma DATETIMEOFFSET(3) NULL,
        numero_renovaciones INT NOT NULL CONSTRAINT DF_SolicitudFirma_Renovaciones DEFAULT (0),
        CONSTRAINT PK_SolicitudFirma PRIMARY KEY (id_solicitud_firma),
        CONSTRAINT FK_SolicitudFirma_Firmante FOREIGN KEY (id_proceso_firma, id_firmante_proceso)
            REFERENCES firmaya.FirmanteProceso(id_proceso_firma, id_firmante_proceso),
        CONSTRAINT UQ_SolicitudFirma_Firmante UNIQUE (id_firmante_proceso),
        CONSTRAINT UQ_SolicitudFirma_Propiedad UNIQUE (id_solicitud_firma, id_proceso_firma, id_firmante_proceso),
        CONSTRAINT CK_SolicitudFirma_Estado CHECK (estado IN ('PENDIENTE','NOTIFICADA','VENCIDA','FIRMADA','REVOCADA','BLOQUEADA')),
        CONSTRAINT CK_SolicitudFirma_Entrega CHECK (estado_entrega IN ('PENDIENTE','ENVIADA','ERROR')),
        CONSTRAINT CK_SolicitudFirma_Canal CHECK (canal = 'CORREO_ELECTRONICO'),
        CONSTRAINT CK_SolicitudFirma_Renovaciones CHECK (numero_renovaciones >= 0),
        CONSTRAINT CK_SolicitudFirma_Fechas CHECK (
            (estado = 'FIRMADA' AND fecha_firma IS NOT NULL)
            OR (estado <> 'FIRMADA' AND fecha_firma IS NULL)
        )
        /* Estado/entrega separados; catalogo exacto PD-04 por confirmar. */
    );

    /* 13-15. Aceptacion, OTP y firma: cada evidencia referencia una solicitud. */
    CREATE TABLE firmaya.AceptacionFirma (
        id_aceptacion UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_AceptacionFirma_Id DEFAULT NEWSEQUENTIALID(),
        id_solicitud_firma UNIQUEIDENTIFIER NOT NULL,
        id_version_contrato UNIQUEIDENTIFIER NOT NULL,
        hash_version VARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        texto_aceptacion NVARCHAR(1000) NOT NULL,
        fecha_aceptacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_AceptacionFirma_Fecha DEFAULT SYSDATETIMEOFFSET(),
        direccion_ip VARCHAR(45) NULL,
        CONSTRAINT PK_AceptacionFirma PRIMARY KEY (id_aceptacion),
        CONSTRAINT FK_AceptacionFirma_Solicitud FOREIGN KEY (id_solicitud_firma)
            REFERENCES firmaya.SolicitudFirma(id_solicitud_firma),
        CONSTRAINT FK_AceptacionFirma_VersionHash FOREIGN KEY (id_version_contrato, hash_version)
            REFERENCES firmaya.VersionContrato(id_version_contrato, hash_sha256),
        CONSTRAINT UQ_AceptacionFirma_Solicitud UNIQUE (id_solicitud_firma),
        CONSTRAINT UQ_AceptacionFirma_Propiedad UNIQUE (id_aceptacion, id_solicitud_firma),
        CONSTRAINT CK_AceptacionFirma_Texto CHECK (LEN(LTRIM(RTRIM(texto_aceptacion))) > 0)
    );

    CREATE TABLE firmaya.DesafioOtp (
        id_desafio_otp UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_DesafioOtp_Id DEFAULT NEWSEQUENTIALID(),
        id_solicitud_firma UNIQUEIDENTIFIER NOT NULL,
        id_aceptacion UNIQUEIDENTIFIER NOT NULL,
        verificador_otp VARBINARY(32) NOT NULL,
        intentos_fallidos TINYINT NOT NULL CONSTRAINT DF_DesafioOtp_Intentos DEFAULT (0),
        activo BIT NOT NULL CONSTRAINT DF_DesafioOtp_Activo DEFAULT (1),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_DesafioOtp_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_expiracion DATETIMEOFFSET(3) NOT NULL,
        fecha_consumo DATETIMEOFFSET(3) NULL,
        fecha_invalidacion DATETIMEOFFSET(3) NULL,
        fecha_bloqueo DATETIMEOFFSET(3) NULL,
        CONSTRAINT PK_DesafioOtp PRIMARY KEY (id_desafio_otp),
        CONSTRAINT FK_DesafioOtp_Aceptacion FOREIGN KEY (id_aceptacion, id_solicitud_firma)
            REFERENCES firmaya.AceptacionFirma(id_aceptacion, id_solicitud_firma),
        CONSTRAINT UQ_DesafioOtp_Propiedad UNIQUE (id_desafio_otp, id_solicitud_firma),
        CONSTRAINT CK_DesafioOtp_Intentos CHECK (intentos_fallidos BETWEEN 0 AND 3),
        CONSTRAINT CK_DesafioOtp_Fechas CHECK (fecha_expiracion > fecha_creacion),
        CONSTRAINT CK_DesafioOtp_Consumo CHECK (fecha_consumo IS NULL OR activo = 0),
        CONSTRAINT CK_DesafioOtp_Invalidacion CHECK (fecha_invalidacion IS NULL OR activo = 0),
        CONSTRAINT CK_DesafioOtp_Bloqueo CHECK (fecha_bloqueo IS NULL OR intentos_fallidos = 3)
    );
    CREATE UNIQUE INDEX UX_DesafioOtp_UnoActivo
        ON firmaya.DesafioOtp(id_solicitud_firma) WHERE activo = 1;

    CREATE TABLE firmaya.Firma (
        id_firma UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Firma_Id DEFAULT NEWSEQUENTIALID(),
        id_solicitud_firma UNIQUEIDENTIFIER NOT NULL,
        id_proceso_firma UNIQUEIDENTIFIER NOT NULL,
        id_firmante_proceso UNIQUEIDENTIFIER NOT NULL,
        id_aceptacion UNIQUEIDENTIFIER NOT NULL,
        id_desafio_otp UNIQUEIDENTIFIER NOT NULL,
        id_version_contrato UNIQUEIDENTIFIER NOT NULL,
        hash_version VARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        fecha_firma DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Firma_Fecha DEFAULT SYSDATETIMEOFFSET(),
        direccion_ip VARCHAR(45) NOT NULL,
        verificacion_otp_exitosa BIT NOT NULL,
        CONSTRAINT PK_Firma PRIMARY KEY (id_firma),
        CONSTRAINT FK_Firma_Solicitud FOREIGN KEY (id_solicitud_firma, id_proceso_firma, id_firmante_proceso)
            REFERENCES firmaya.SolicitudFirma(id_solicitud_firma, id_proceso_firma, id_firmante_proceso),
        CONSTRAINT FK_Firma_Aceptacion FOREIGN KEY (id_aceptacion, id_solicitud_firma)
            REFERENCES firmaya.AceptacionFirma(id_aceptacion, id_solicitud_firma),
        CONSTRAINT FK_Firma_Desafio FOREIGN KEY (id_desafio_otp, id_solicitud_firma)
            REFERENCES firmaya.DesafioOtp(id_desafio_otp, id_solicitud_firma),
        CONSTRAINT FK_Firma_VersionProceso FOREIGN KEY (id_proceso_firma, id_version_contrato)
            REFERENCES firmaya.ProcesoFirma(id_proceso_firma, id_version_objetivo),
        CONSTRAINT FK_Firma_VersionHash FOREIGN KEY (id_version_contrato, hash_version)
            REFERENCES firmaya.VersionContrato(id_version_contrato, hash_sha256),
        CONSTRAINT UQ_Firma_Solicitud UNIQUE (id_solicitud_firma),
        CONSTRAINT UQ_Firma_FirmanteProceso UNIQUE (id_firmante_proceso),
        CONSTRAINT CK_Firma_Verificacion CHECK (verificacion_otp_exitosa = 1),
        CONSTRAINT CK_Firma_Ip CHECK (LEN(LTRIM(RTRIM(direccion_ip))) BETWEEN 3 AND 45)
    );

    /* 16. PDF conservado. HASH PDF se registra DESPUES de generar todos sus bytes. */
    CREATE TABLE firmaya.DocumentoFinal (
        id_documento_final UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_DocumentoFinal_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_proceso_firma UNIQUEIDENTIFIER NOT NULL,
        id_version_contrato UNIQUEIDENTIFIER NOT NULL,
        hash_contenido VARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        hash_pdf VARCHAR(64) COLLATE Latin1_General_100_BIN2 NULL,
        nombre_archivo NVARCHAR(255) NULL,
        ubicacion_privada NVARCHAR(1024) NULL,
        tamano_bytes BIGINT NULL,
        estado VARCHAR(12) NOT NULL CONSTRAINT DF_DocumentoFinal_Estado DEFAULT ('GENERANDO'),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_DocumentoFinal_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_generacion DATETIMEOFFSET(3) NULL,
        detalle_error NVARCHAR(1000) NULL,
        CONSTRAINT PK_DocumentoFinal PRIMARY KEY (id_documento_final),
        CONSTRAINT FK_DocumentoFinal_Proceso FOREIGN KEY (id_contrato, id_proceso_firma)
            REFERENCES firmaya.ProcesoFirma(id_contrato, id_proceso_firma),
        CONSTRAINT FK_DocumentoFinal_VersionProceso FOREIGN KEY (id_proceso_firma, id_version_contrato)
            REFERENCES firmaya.ProcesoFirma(id_proceso_firma, id_version_objetivo),
        CONSTRAINT FK_DocumentoFinal_VersionHash FOREIGN KEY (id_version_contrato, hash_contenido)
            REFERENCES firmaya.VersionContrato(id_version_contrato, hash_sha256),
        CONSTRAINT UQ_DocumentoFinal_Contrato UNIQUE (id_contrato),
        CONSTRAINT CK_DocumentoFinal_Estado CHECK (estado IN ('GENERANDO','DISPONIBLE','ERROR')),
        CONSTRAINT CK_DocumentoFinal_Tamano CHECK (tamano_bytes IS NULL OR tamano_bytes > 0),
        CONSTRAINT CK_DocumentoFinal_Hash CHECK (
            hash_pdf IS NULL OR (LEN(hash_pdf) = 64 AND hash_pdf NOT LIKE '%[^0-9a-f]%')
        ),
        CONSTRAINT CK_DocumentoFinal_Disponible CHECK (
            estado <> 'DISPONIBLE'
            OR (hash_pdf IS NOT NULL AND nombre_archivo IS NOT NULL AND ubicacion_privada IS NOT NULL
                AND tamano_bytes IS NOT NULL AND fecha_generacion IS NOT NULL)
        )
    );

    /* 17. Comentarios asociados para siempre a la version especifica. */
    CREATE TABLE firmaya.Comentario (
        id_comentario UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Comentario_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        id_version_contrato UNIQUEIDENTIFIER NOT NULL,
        id_usuario_autor UNIQUEIDENTIFIER NULL,
        id_participante_autor UNIQUEIDENTIFIER NULL,
        texto NVARCHAR(1000) NOT NULL,
        texto_seleccionado NVARCHAR(MAX) NULL,
        referencia_fragmento_json NVARCHAR(MAX) NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Comentario_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_Comentario PRIMARY KEY (id_comentario),
        CONSTRAINT FK_Comentario_Version FOREIGN KEY (id_contrato, id_version_contrato)
            REFERENCES firmaya.VersionContrato(id_contrato, id_version_contrato),
        CONSTRAINT FK_Comentario_Usuario FOREIGN KEY (id_usuario_autor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_Comentario_Participante FOREIGN KEY (id_contrato, id_participante_autor)
            REFERENCES firmaya.Participante(id_contrato, id_participante),
        CONSTRAINT CK_Comentario_Autor CHECK (
            (id_usuario_autor IS NOT NULL AND id_participante_autor IS NULL)
            OR (id_usuario_autor IS NULL AND id_participante_autor IS NOT NULL)
        ),
        CONSTRAINT CK_Comentario_Texto CHECK (LEN(LTRIM(RTRIM(texto))) BETWEEN 1 AND 1000),
        CONSTRAINT CK_Comentario_Referencia CHECK (referencia_fragmento_json IS NULL OR ISJSON(referencia_fragmento_json) = 1)
    );

    /* 18-22. Notificaciones: catalogo configurable, canales globales, eventos y envios. */
    CREATE TABLE firmaya.TipoEventoNotificacion (
        id_tipo_evento UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_TipoEvento_Id DEFAULT NEWSEQUENTIALID(),
        codigo VARCHAR(80) NOT NULL,
        etiqueta NVARCHAR(150) NOT NULL,
        configurable BIT NOT NULL CONSTRAINT DF_TipoEvento_Configurable DEFAULT (1),
        permite_correo BIT NOT NULL CONSTRAINT DF_TipoEvento_Correo DEFAULT (1),
        permite_plataforma BIT NOT NULL CONSTRAINT DF_TipoEvento_Plataforma DEFAULT (1),
        activo BIT NOT NULL CONSTRAINT DF_TipoEvento_Activo DEFAULT (1),
        CONSTRAINT PK_TipoEventoNotificacion PRIMARY KEY (id_tipo_evento),
        CONSTRAINT UQ_TipoEvento_Codigo UNIQUE (codigo),
        CONSTRAINT CK_TipoEvento_Codigo CHECK (LEN(LTRIM(RTRIM(codigo))) > 0),
        CONSTRAINT CK_TipoEvento_Canales CHECK (permite_correo = 1 OR permite_plataforma = 1)
    );

    CREATE TABLE firmaya.CanalPreferidoUsuario (
        id_usuario UNIQUEIDENTIFIER NOT NULL,
        canal VARCHAR(18) NOT NULL,
        fecha_actualizacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_CanalPreferido_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_CanalPreferidoUsuario PRIMARY KEY (id_usuario, canal),
        CONSTRAINT FK_CanalPreferido_Usuario FOREIGN KEY (id_usuario) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT CK_CanalPreferido_Canal CHECK (canal IN ('CORREO_ELECTRONICO','PLATAFORMA'))
        /* Al menos un canal por usuario se valida en la transaccion del servicio. */
    );

    CREATE TABLE firmaya.PreferenciaNotificacion (
        id_usuario UNIQUEIDENTIFIER NOT NULL,
        id_tipo_evento UNIQUEIDENTIFIER NOT NULL,
        habilitada BIT NOT NULL CONSTRAINT DF_PreferenciaNotificacion_Habilitada DEFAULT (1),
        fecha_actualizacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_PreferenciaNotificacion_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_PreferenciaNotificacion PRIMARY KEY (id_usuario, id_tipo_evento),
        CONSTRAINT FK_PreferenciaNotificacion_Usuario FOREIGN KEY (id_usuario) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_PreferenciaNotificacion_Tipo FOREIGN KEY (id_tipo_evento) REFERENCES firmaya.TipoEventoNotificacion(id_tipo_evento)
        /* No permitir desactivar eventos operativos: validacion en ServicioPreferenciasNotificacion. */
    );

    CREATE TABLE firmaya.Notificacion (
        id_notificacion UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_Notificacion_Id DEFAULT NEWSEQUENTIALID(),
        id_tipo_evento UNIQUEIDENTIFIER NOT NULL,
        id_contrato UNIQUEIDENTIFIER NULL,
        id_usuario_destinatario UNIQUEIDENTIFIER NULL,
        id_participante_destinatario UNIQUEIDENTIFIER NULL,
        canal VARCHAR(18) NOT NULL,
        correo_destino NVARCHAR(254) NULL,
        titulo NVARCHAR(200) NOT NULL,
        mensaje NVARCHAR(2000) NOT NULL,
        estado_entrega VARCHAR(10) NOT NULL CONSTRAINT DF_Notificacion_Entrega DEFAULT ('PENDIENTE'),
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_Notificacion_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultimo_envio DATETIMEOFFSET(3) NULL,
        fecha_lectura DATETIMEOFFSET(3) NULL,
        referencia_idempotencia VARCHAR(100) NULL,
        CONSTRAINT PK_Notificacion PRIMARY KEY (id_notificacion),
        CONSTRAINT FK_Notificacion_Tipo FOREIGN KEY (id_tipo_evento) REFERENCES firmaya.TipoEventoNotificacion(id_tipo_evento),
        CONSTRAINT FK_Notificacion_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_Notificacion_Usuario FOREIGN KEY (id_usuario_destinatario) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_Notificacion_Participante FOREIGN KEY (id_contrato, id_participante_destinatario)
            REFERENCES firmaya.Participante(id_contrato, id_participante),
        CONSTRAINT CK_Notificacion_ParticipanteContrato CHECK (
            id_participante_destinatario IS NULL OR id_contrato IS NOT NULL
        ),
        CONSTRAINT CK_Notificacion_Destinatario CHECK (
            (id_usuario_destinatario IS NOT NULL AND id_participante_destinatario IS NULL)
            OR (id_usuario_destinatario IS NULL AND id_participante_destinatario IS NOT NULL)
        ),
        CONSTRAINT CK_Notificacion_Canal CHECK (canal IN ('CORREO_ELECTRONICO','PLATAFORMA')),
        CONSTRAINT CK_Notificacion_Entrega CHECK (estado_entrega IN ('PENDIENTE','ENVIADA','ERROR')),
        CONSTRAINT CK_Notificacion_Lectura CHECK (fecha_lectura IS NULL OR canal = 'PLATAFORMA'),
        CONSTRAINT CK_Notificacion_Correo CHECK (canal <> 'CORREO_ELECTRONICO' OR correo_destino IS NOT NULL)
        /* NO guardar URL tokenizada o OTP en titulo/mensaje ni en metadatos. */
    );

    CREATE TABLE firmaya.IntentoNotificacion (
        id_intento UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_IntentoNotificacion_Id DEFAULT NEWSEQUENTIALID(),
        id_notificacion UNIQUEIDENTIFIER NOT NULL,
        numero_intento INT NOT NULL,
        resultado VARCHAR(8) NOT NULL,
        fecha_intento DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_IntentoNotificacion_Fecha DEFAULT SYSDATETIMEOFFSET(),
        identificador_proveedor NVARCHAR(200) NULL,
        detalle_error NVARCHAR(1000) NULL,
        CONSTRAINT PK_IntentoNotificacion PRIMARY KEY (id_intento),
        CONSTRAINT FK_IntentoNotificacion_Notificacion FOREIGN KEY (id_notificacion) REFERENCES firmaya.Notificacion(id_notificacion),
        CONSTRAINT UQ_IntentoNotificacion_Numero UNIQUE (id_notificacion, numero_intento),
        CONSTRAINT CK_IntentoNotificacion_Numero CHECK (numero_intento >= 1),
        CONSTRAINT CK_IntentoNotificacion_Resultado CHECK (resultado IN ('ENVIADO','ERROR'))
    );

    /* 23. Tokens por proposito, solo hash criptografico; nunca URL original. */
    CREATE TABLE firmaya.TokenAcceso (
        id_token UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_TokenAcceso_Id DEFAULT NEWSEQUENTIALID(),
        hash_token VARBINARY(32) NOT NULL,
        proposito VARCHAR(28) NOT NULL,
        id_usuario UNIQUEIDENTIFIER NULL,
        id_invitacion UNIQUEIDENTIFIER NULL,
        id_solicitud_firma UNIQUEIDENTIFIER NULL,
        id_participante UNIQUEIDENTIFIER NULL,
        id_usuario_emisor UNIQUEIDENTIFIER NULL,
        fecha_emision DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_TokenAcceso_Emision DEFAULT SYSDATETIMEOFFSET(),
        fecha_expiracion DATETIMEOFFSET(3) NOT NULL,
        fecha_consumo DATETIMEOFFSET(3) NULL,
        fecha_revocacion DATETIMEOFFSET(3) NULL,
        motivo_revocacion NVARCHAR(300) NULL,
        CONSTRAINT PK_TokenAcceso PRIMARY KEY (id_token),
        CONSTRAINT UQ_TokenAcceso_Hash UNIQUE (hash_token),
        CONSTRAINT FK_TokenAcceso_Usuario FOREIGN KEY (id_usuario) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_TokenAcceso_Invitacion FOREIGN KEY (id_invitacion) REFERENCES firmaya.Invitacion(id_invitacion),
        CONSTRAINT FK_TokenAcceso_Solicitud FOREIGN KEY (id_solicitud_firma) REFERENCES firmaya.SolicitudFirma(id_solicitud_firma),
        CONSTRAINT FK_TokenAcceso_Participante FOREIGN KEY (id_participante) REFERENCES firmaya.Participante(id_participante),
        CONSTRAINT FK_TokenAcceso_Emisor FOREIGN KEY (id_usuario_emisor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT CK_TokenAcceso_Proposito CHECK (proposito IN ('ACTIVACION','RECUPERACION_CONTRASENA','INVITACION','FIRMA','CONSULTA')),
        CONSTRAINT CK_TokenAcceso_Destino CHECK (
            (proposito IN ('ACTIVACION','RECUPERACION_CONTRASENA') AND id_usuario IS NOT NULL AND id_invitacion IS NULL AND id_solicitud_firma IS NULL AND id_participante IS NULL)
            OR (proposito = 'INVITACION' AND id_usuario IS NULL AND id_invitacion IS NOT NULL AND id_solicitud_firma IS NULL AND id_participante IS NULL)
            OR (proposito = 'FIRMA' AND id_usuario IS NULL AND id_invitacion IS NULL AND id_solicitud_firma IS NOT NULL AND id_participante IS NULL)
            OR (proposito = 'CONSULTA' AND id_usuario IS NULL AND id_invitacion IS NULL AND id_solicitud_firma IS NULL AND id_participante IS NOT NULL)
        ),
        CONSTRAINT CK_TokenAcceso_Fechas CHECK (fecha_expiracion > fecha_emision),
        CONSTRAINT CK_TokenAcceso_Uso CHECK (fecha_consumo IS NULL OR fecha_consumo >= fecha_emision),
        CONSTRAINT CK_TokenAcceso_Revocacion CHECK (fecha_revocacion IS NULL OR fecha_revocacion >= fecha_emision)
    );

    /* 24. Sesion externa derivada del token y ligada a un participante. */
    CREATE TABLE firmaya.SesionExterna (
        id_sesion_externa UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_SesionExterna_Id DEFAULT NEWSEQUENTIALID(),
        id_token_origen UNIQUEIDENTIFIER NOT NULL,
        id_participante UNIQUEIDENTIFIER NOT NULL,
        hash_credencial VARBINARY(32) NOT NULL,
        proposito VARCHAR(10) NOT NULL,
        fecha_creacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_SesionExterna_Creacion DEFAULT SYSDATETIMEOFFSET(),
        fecha_ultima_actividad DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_SesionExterna_Actividad DEFAULT SYSDATETIMEOFFSET(),
        fecha_expiracion DATETIMEOFFSET(3) NOT NULL,
        fecha_revocacion DATETIMEOFFSET(3) NULL,
        direccion_ip VARCHAR(45) NULL,
        CONSTRAINT PK_SesionExterna PRIMARY KEY (id_sesion_externa),
        CONSTRAINT FK_SesionExterna_Token FOREIGN KEY (id_token_origen) REFERENCES firmaya.TokenAcceso(id_token),
        CONSTRAINT FK_SesionExterna_Participante FOREIGN KEY (id_participante) REFERENCES firmaya.Participante(id_participante),
        CONSTRAINT UQ_SesionExterna_Hash UNIQUE (hash_credencial),
        CONSTRAINT CK_SesionExterna_Proposito CHECK (proposito IN ('INVITACION','CONSULTA','FIRMA')),
        CONSTRAINT CK_SesionExterna_Fechas CHECK (fecha_expiracion > fecha_creacion AND fecha_ultima_actividad >= fecha_creacion)
        /* Servicio valida token->participante->proceso y estado en cada peticion. */
    );

    /* 25. Auditoria de transiciones; datos anteriores y posteriores en columnas. */
    CREATE TABLE firmaya.HistorialEstadoContrato (
        id_historial_estado UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_HistorialEstado_Id DEFAULT NEWSEQUENTIALID(),
        id_contrato UNIQUEIDENTIFIER NOT NULL,
        estado_anterior VARCHAR(19) NOT NULL,
        estado_nuevo VARCHAR(19) NOT NULL,
        id_usuario_actor UNIQUEIDENTIFIER NULL,
        id_proceso_firma UNIQUEIDENTIFIER NULL,
        motivo NVARCHAR(500) NULL,
        fecha_cambio DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_HistorialEstado_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_HistorialEstadoContrato PRIMARY KEY (id_historial_estado),
        CONSTRAINT FK_HistorialEstado_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_HistorialEstado_Usuario FOREIGN KEY (id_usuario_actor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_HistorialEstado_Proceso FOREIGN KEY (id_contrato, id_proceso_firma)
            REFERENCES firmaya.ProcesoFirma(id_contrato, id_proceso_firma),
        CONSTRAINT CK_HistorialEstado_Transicion CHECK (
            (estado_anterior = 'BORRADOR' AND estado_nuevo = 'EN_REVISION')
            OR (estado_anterior = 'EN_REVISION' AND estado_nuevo = 'LISTO_PARA_FIRMAR')
            OR (estado_anterior = 'LISTO_PARA_FIRMAR' AND estado_nuevo = 'EN_REVISION')
            OR (estado_anterior = 'LISTO_PARA_FIRMAR' AND estado_nuevo = 'FIRMADO')
            OR (estado_anterior = 'FIRMADO' AND estado_nuevo = 'ARCHIVADO')
        )
    );

    /* 26. Evento puntual de reenvio/renovacion; no introducir estado 'RENOTIFICADO'. */
    CREATE TABLE firmaya.HistorialSolicitudFirma (
        id_historial_solicitud UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_HistorialSolicitud_Id DEFAULT NEWSEQUENTIALID(),
        id_solicitud_firma UNIQUEIDENTIFIER NOT NULL,
        tipo_evento VARCHAR(21) NOT NULL,
        estado_anterior VARCHAR(15) NULL,
        estado_nuevo VARCHAR(15) NULL,
        id_usuario_actor UNIQUEIDENTIFIER NULL,
        fecha_evento DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_HistorialSolicitud_Fecha DEFAULT SYSDATETIMEOFFSET(),
        detalle NVARCHAR(1000) NULL,
        CONSTRAINT PK_HistorialSolicitudFirma PRIMARY KEY (id_historial_solicitud),
        CONSTRAINT FK_HistorialSolicitud_Solicitud FOREIGN KEY (id_solicitud_firma) REFERENCES firmaya.SolicitudFirma(id_solicitud_firma),
        CONSTRAINT FK_HistorialSolicitud_Usuario FOREIGN KEY (id_usuario_actor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT CK_HistorialSolicitud_Tipo CHECK (
            tipo_evento IN ('CREACION','ENVIO','REENVIO','RENOVACION','ERROR_ENTREGA','VENCIMIENTO','REVOCACION','FIRMA')
        )
    );

    /* 27. Auditoria transversal: actor usuario, parte externa o sistema. */
    CREATE TABLE firmaya.EventoAuditoria (
        id_evento_auditoria UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_EventoAuditoria_Id DEFAULT NEWSEQUENTIALID(),
        fecha_evento DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_EventoAuditoria_Fecha DEFAULT SYSDATETIMEOFFSET(),
        tipo_actor VARCHAR(12) NOT NULL,
        id_usuario_actor UNIQUEIDENTIFIER NULL,
        id_participante_actor UNIQUEIDENTIFIER NULL,
        tipo_accion VARCHAR(80) NOT NULL,
        tipo_entidad VARCHAR(60) NOT NULL,
        id_entidad UNIQUEIDENTIFIER NULL,
        id_contrato UNIQUEIDENTIFIER NULL,
        id_version_contrato UNIQUEIDENTIFIER NULL,
        descripcion NVARCHAR(2000) NOT NULL,
        datos_anteriores_json NVARCHAR(MAX) NULL,
        datos_posteriores_json NVARCHAR(MAX) NULL,
        hash_sha256 VARCHAR(64) COLLATE Latin1_General_100_BIN2 NULL,
        direccion_ip VARCHAR(45) NULL,
        identificador_traza VARCHAR(100) NULL,
        CONSTRAINT PK_EventoAuditoria PRIMARY KEY (id_evento_auditoria),
        CONSTRAINT FK_EventoAuditoria_Usuario FOREIGN KEY (id_usuario_actor) REFERENCES firmaya.Usuario(id_usuario),
        CONSTRAINT FK_EventoAuditoria_Participante FOREIGN KEY (id_participante_actor) REFERENCES firmaya.Participante(id_participante),
        CONSTRAINT FK_EventoAuditoria_Contrato FOREIGN KEY (id_contrato) REFERENCES firmaya.Contrato(id_contrato),
        CONSTRAINT FK_EventoAuditoria_Version FOREIGN KEY (id_contrato, id_version_contrato)
            REFERENCES firmaya.VersionContrato(id_contrato, id_version_contrato),
        CONSTRAINT CK_EventoAuditoria_VersionContrato CHECK (
            id_version_contrato IS NULL OR id_contrato IS NOT NULL
        ),
        CONSTRAINT CK_EventoAuditoria_TipoActor CHECK (
            (tipo_actor = 'SISTEMA' AND id_usuario_actor IS NULL AND id_participante_actor IS NULL)
            OR (tipo_actor = 'USUARIO' AND id_usuario_actor IS NOT NULL AND id_participante_actor IS NULL)
            OR (tipo_actor = 'PARTICIPANTE' AND id_usuario_actor IS NULL AND id_participante_actor IS NOT NULL)
        ),
        CONSTRAINT CK_EventoAuditoria_Antes CHECK (datos_anteriores_json IS NULL OR ISJSON(datos_anteriores_json) = 1),
        CONSTRAINT CK_EventoAuditoria_Despues CHECK (datos_posteriores_json IS NULL OR ISJSON(datos_posteriores_json) = 1),
        CONSTRAINT CK_EventoAuditoria_Hash CHECK (
            hash_sha256 IS NULL OR (LEN(hash_sha256) = 64 AND hash_sha256 NOT LIKE '%[^0-9a-f]%')
        )
        /* NUNCA guardar contrasenas, hashes de contrasena, tokens, OTP ni URL secretas. */
    );

    /* 28. Parametros pendientes: NULL significa NO APROBADO / NO CONFIGURADO. */
    CREATE TABLE firmaya.ParametroSistema (
        clave VARCHAR(100) NOT NULL,
        valor NVARCHAR(1000) NULL,
        tipo_valor VARCHAR(10) NOT NULL,
        descripcion NVARCHAR(500) NOT NULL,
        pendiente_definicion BIT NOT NULL CONSTRAINT DF_ParametroSistema_Pendiente DEFAULT (1),
        fecha_actualizacion DATETIMEOFFSET(3) NOT NULL CONSTRAINT DF_ParametroSistema_Fecha DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT PK_ParametroSistema PRIMARY KEY (clave),
        CONSTRAINT CK_ParametroSistema_Tipo CHECK (tipo_valor IN ('ENTERO','TEXTO','BOOLEANO','JSON')),
        CONSTRAINT CK_ParametroSistema_Pendiente CHECK (
            (pendiente_definicion = 1 AND valor IS NULL)
            OR (pendiente_definicion = 0 AND valor IS NOT NULL AND LEN(LTRIM(RTRIM(valor))) > 0)
        )
    );

    /* Indices para listados/consultas de los 69 endpoints. */
    CREATE INDEX IX_Usuario_RolEstado ON firmaya.Usuario(rol_global, estado_administrativo);
    CREATE INDEX IX_SesionInterna_Usuario ON firmaya.SesionInterna(id_usuario, fecha_expiracion);
    CREATE INDEX IX_Plantilla_TipoEstado ON firmaya.Plantilla(tipo_contrato, estado);
    CREATE INDEX IX_CampoPlantilla_Version ON firmaya.CampoPlantilla(id_version_plantilla, orden_visual);
    CREATE INDEX IX_Contrato_ResponsableEstado ON firmaya.Contrato(id_responsable, estado, fecha_ultima_actividad DESC);
    CREATE INDEX IX_Contrato_EstadoExpiracion ON firmaya.Contrato(estado, fecha_expiracion) INCLUDE (fecha_firma, fecha_ultima_actividad);
    CREATE INDEX IX_Contrato_FechaFirma ON firmaya.Contrato(fecha_firma) WHERE fecha_firma IS NOT NULL;
    CREATE INDEX IX_VersionContrato_Historial ON firmaya.VersionContrato(id_contrato, numero_version DESC);
    CREATE INDEX IX_Participante_Usuario ON firmaya.Participante(id_usuario_interno) WHERE id_usuario_interno IS NOT NULL;
    CREATE INDEX IX_Invitacion_Estado ON firmaya.Invitacion(id_contrato, estado);
    CREATE INDEX IX_FirmanteProceso_Participante ON firmaya.FirmanteProceso(id_participante);
    CREATE INDEX IX_SolicitudFirma_ProcesoEstado ON firmaya.SolicitudFirma(id_proceso_firma, estado, fecha_expiracion_enlace);
    CREATE INDEX IX_DesafioOtp_Solicitud ON firmaya.DesafioOtp(id_solicitud_firma, fecha_creacion DESC);
    CREATE INDEX IX_Firma_Proceso ON firmaya.Firma(id_proceso_firma, fecha_firma);
    CREATE INDEX IX_Comentario_VersionFecha ON firmaya.Comentario(id_version_contrato, fecha_creacion);
    CREATE INDEX IX_Notificacion_UsuarioBandeja ON firmaya.Notificacion(id_usuario_destinatario, canal, fecha_creacion DESC)
        INCLUDE (fecha_lectura, estado_entrega);
    CREATE UNIQUE INDEX UX_Notificacion_Idempotencia ON firmaya.Notificacion(referencia_idempotencia)
        WHERE referencia_idempotencia IS NOT NULL;
    CREATE INDEX IX_Notificacion_Entrega ON firmaya.Notificacion(estado_entrega, fecha_creacion);
    CREATE INDEX IX_TokenAcceso_Usuario ON firmaya.TokenAcceso(id_usuario, proposito, fecha_expiracion);
    CREATE INDEX IX_TokenAcceso_Invitacion ON firmaya.TokenAcceso(id_invitacion, fecha_expiracion) WHERE id_invitacion IS NOT NULL;
    CREATE INDEX IX_TokenAcceso_Solicitud ON firmaya.TokenAcceso(id_solicitud_firma, fecha_expiracion) WHERE id_solicitud_firma IS NOT NULL;
    CREATE INDEX IX_TokenAcceso_Participante ON firmaya.TokenAcceso(id_participante, fecha_expiracion) WHERE id_participante IS NOT NULL;
    CREATE INDEX IX_SesionExterna_Participante ON firmaya.SesionExterna(id_participante, fecha_expiracion);
    CREATE INDEX IX_HistorialEstado_ContratoFecha ON firmaya.HistorialEstadoContrato(id_contrato, fecha_cambio DESC);
    CREATE INDEX IX_HistorialSolicitud_SolicitudFecha ON firmaya.HistorialSolicitudFirma(id_solicitud_firma, fecha_evento DESC);
    CREATE INDEX IX_EventoAuditoria_Fecha ON firmaya.EventoAuditoria(fecha_evento DESC);
    CREATE INDEX IX_EventoAuditoria_UsuarioFecha ON firmaya.EventoAuditoria(id_usuario_actor, fecha_evento DESC);
    CREATE INDEX IX_EventoAuditoria_ContratoFecha ON firmaya.EventoAuditoria(id_contrato, fecha_evento DESC);
    CREATE INDEX IX_EventoAuditoria_AccionFecha ON firmaya.EventoAuditoria(tipo_accion, fecha_evento DESC);

    /* Parametros APROBADOS, registrados como configuracion; no reemplazan pruebas. */
    INSERT INTO firmaya.ParametroSistema (clave, valor, tipo_valor, descripcion, pendiente_definicion) VALUES
       ('ACTIVACION_HORAS', N'24', 'ENTERO', N'B01: vigencia del enlace de activacion en horas.', 0),
       ('INVITACION_DIAS', N'7', 'ENTERO', N'B03: vigencia del enlace de invitacion en dias.', 0),
       ('RECUPERACION_MINUTOS', N'30', 'ENTERO', N'CU-21: vigencia del enlace de recuperacion.', 0),
       ('OTP_MINUTOS', N'10', 'ENTERO', N'CU-08: vigencia del codigo OTP.', 0),
       ('OTP_INTENTOS_MAXIMOS', N'3', 'ENTERO', N'CU-08: bloqueo del enlace tras tres intentos fallidos.', 0),
       ('INACTIVIDAD_EXTERNA_MINUTOS', N'60', 'ENTERO', N'CU-04: inactividad de la sesion externa.', 0),
       ('AVISO_EXPIRACION_EXTERNA_MINUTOS', N'5', 'ENTERO', N'CU-04: aviso previo al fin de la sesion.', 0),
       ('LOGIN_INTENTOS_MAXIMOS', N'5', 'ENTERO', N'CU-19: intentos fallidos de inicio de sesion.', 0),
       ('LOGIN_BLOQUEO_MINUTOS', N'15', 'ENTERO', N'CU-19: bloqueo temporal por intentos fallidos.', 0);

    /* Requisitos SIN DEFINIR: intencionalmente NULL. No inferir valores. */
    INSERT INTO firmaya.ParametroSistema (clave, valor, tipo_valor, descripcion, pendiente_definicion) VALUES
       ('FIRMA_ENLACE_MAXIMO_MINUTOS', NULL, 'ENTERO', N'PD-01: duracion maxima de enlaces de firma (B03).', 1),
       ('CONSULTA_ENLACE_MINUTOS', NULL, 'ENTERO', N'PD-02: vigencia de enlace renovado de consulta.', 1),
       ('SESION_INTERNA_MINUTOS', NULL, 'ENTERO', N'PD-03: vigencia de sesiones internas.', 1),
       ('RECUPERACION_USUARIO_INACTIVO', NULL, 'TEXTO', N'PD-03: politica para usuarios deshabilitados.', 1),
       ('OTP_REENVIOS_MAXIMOS', NULL, 'ENTERO', N'PD-05: cantidad maxima de reenvios de OTP.', 1),
       ('OTP_INTERVALO_REENVIO_SEGUNDOS', NULL, 'ENTERO', N'PD-05: intervalo minimo entre reenvios de OTP.', 1),
       ('OTP_POLITICA_DESBLOQUEO', NULL, 'TEXTO', N'PD-05: politica de desbloqueo tras agotar intentos.', 1),
       ('CANONIZACION_CONTRATO', NULL, 'TEXTO', N'PD-07: formato de bytes definido para SHA-256.', 1),
       ('RETENCION_AUDITORIA_DIAS', NULL, 'ENTERO', N'PD-10: politica de conservacion de auditoria.', 1),
       ('RETENCION_DOCUMENTOS_DIAS', NULL, 'ENTERO', N'PD-08: politica de conservacion de documentos.', 1);

    /* Triggers de proteccion: las filas historicas no pueden alterarse ni borrarse
       mediante DML ordinario. CREATE TRIGGER requiere lote propio; EXEC lo aísla.
       Aplicar permisos DB para evitar que la aplicacion pueda ALTER/DISABLE TRIGGER. */
    EXEC(N'CREATE TRIGGER firmaya.TR_VersionPlantilla_Inmutable
          ON firmaya.VersionPlantilla AFTER UPDATE, DELETE AS
          BEGIN SET NOCOUNT ON;
              THROW 50011, ''Las versiones de plantilla son inmutables.'', 1;
          END');

    EXEC(N'CREATE TRIGGER firmaya.TR_VersionContrato_Inmutable
          ON firmaya.VersionContrato AFTER UPDATE, DELETE AS
          BEGIN SET NOCOUNT ON;
              THROW 50012, ''Las versiones de contrato son inmutables.'', 1;
          END');

    EXEC(N'CREATE TRIGGER firmaya.TR_Firma_Inmutable
          ON firmaya.Firma AFTER UPDATE, DELETE AS
          BEGIN SET NOCOUNT ON;
              THROW 50013, ''Las firmas registradas son inmutables.'', 1;
          END');

    EXEC(N'CREATE TRIGGER firmaya.TR_EventoAuditoria_Inmutable
          ON firmaya.EventoAuditoria AFTER UPDATE, DELETE AS
          BEGIN SET NOCOUNT ON;
              THROW 50014, ''Los eventos de auditoria no se modifican ni eliminan.'', 1;
          END');

    EXEC(N'CREATE TRIGGER firmaya.TR_DocumentoFinal_Protegido
          ON firmaya.DocumentoFinal AFTER UPDATE, DELETE AS
          BEGIN SET NOCOUNT ON;
              IF EXISTS (SELECT 1 FROM deleted WHERE estado = ''DISPONIBLE'')
                  THROW 50015, ''El documento final disponible no puede modificarse ni borrarse.'', 1;
              IF EXISTS (SELECT 1 FROM deleted) AND NOT EXISTS (SELECT 1 FROM inserted)
                  THROW 50016, ''No se permite eliminar documentos finales.'', 1;
          END');

    COMMIT TRANSACTION;
    PRINT N'FirmaYA: instalacion inicial finalizada correctamente.';
    PRINT N'Objetos instalados: 28 tablas; consultar sys.tables para verificar.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
