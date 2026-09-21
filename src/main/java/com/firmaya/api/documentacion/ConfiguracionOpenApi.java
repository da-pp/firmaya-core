package com.firmaya.api.documentacion;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone la documentacion interactiva (Swagger UI) de los 69 endpoints en
 * /swagger-ui.html, publica sin autenticacion (ver SeguridadConfig). La autenticacion real
 * de cada endpoint sigue siendo por cookie de sesion (EP-01): iniciar sesion desde el propio
 * Swagger UI ("Try it out" sobre POST /api/v1/autenticacion/iniciar-sesion) basta, porque el
 * navegador conserva la cookie `sesion_firmaya` y la reenvia automaticamente en las siguientes
 * llamadas del mismo origen.
 */
@Configuration
public class ConfiguracionOpenApi {

    @Bean
    public OpenAPI documentacionFirmaYa() {
        return new OpenAPI().info(new Info()
                .title("FirmaYA API")
                .version("v1")
                .description("Documentacion interactiva de los endpoints REST de FirmaYA. "
                        + "Para probar operaciones protegidas: ejecute primero "
                        + "POST /api/v1/autenticacion/iniciar-sesion con 'Try it out'; el navegador "
                        + "guarda la cookie de sesion y las siguientes llamadas quedan autenticadas."));
    }
}
