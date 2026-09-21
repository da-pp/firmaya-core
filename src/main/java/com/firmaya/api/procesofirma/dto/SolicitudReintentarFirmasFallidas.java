package com.firmaya.api.procesofirma.dto;

import java.util.List;
import java.util.UUID;

public record SolicitudReintentarFirmasFallidas(List<UUID> idsSolicitudes) {
}
