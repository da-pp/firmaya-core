package com.firmaya.api.firma;

import com.firmaya.api.contratos.Contrato;
import com.firmaya.api.procesofirma.FirmanteProceso;
import com.firmaya.api.procesofirma.ProcesoFirma;
import com.firmaya.api.procesofirma.SolicitudFirma;

record ResolucionFirmante(Contrato contrato, ProcesoFirma proceso, FirmanteProceso firmante, SolicitudFirma solicitud) {
}
