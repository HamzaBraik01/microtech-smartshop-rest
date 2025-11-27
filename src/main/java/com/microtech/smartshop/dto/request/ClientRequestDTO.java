package com.microtech.smartshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientRequestDTO   {
    @NotBlank(message = "Le nom est requis")
    private String nom;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    private String telephone;

    @NotBlank(message = "Le username est requis")
    private String username;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;
}
