package com.microtech.smartshop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartshopOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Serveur de développement local");

        Info info = new Info()
                .title("SmartShop API - MicroTech Maroc")
                .version("1.0.0")
                .description("API REST pour la gestion commerciale de MicroTech Maroc. " +
                        "Cette API permet de gérer les clients, produits, commandes et paiements.\n\n" +
                        "**Authentification:** L'API utilise des sessions HTTP. " +
                        "Connectez-vous d'abord via `/api/auth/login` pour accéder aux endpoints protégés.\n\n" +
                        "**Comptes de test:**\n" +
                        "- Admin: `username: admin`, `password: admin123`\n" +
                        "- Client: `username: techsolutions`, `password: client123`");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

