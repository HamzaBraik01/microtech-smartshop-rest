package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.ProductRequestDTO;
import com.microtech.smartshop.dto.response.ProductResponseDTO;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gestion du catalogue de produits")
public class ProductController {

    private final ProductService productService;

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    @Operation(
            summary = "Lister tous les produits (avec pagination)",
            description = "Récupère la liste paginée des produits disponibles. Possibilité de recherche par nom. Les produits supprimés sont visibles uniquement pour les ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des produits récupérée")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @Parameter(description = "Numéro de page (commence à 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Terme de recherche (optionnel)") @RequestParam(required = false) String search,
            HttpServletRequest request) {

        User user = getCurrentUser(request);
        boolean isAdmin = (user != null && user.getRole() == UserRole.ADMIN);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponseDTO> products = productService.getAllProducts(search, pageable, isAdmin);

        return ResponseEntity.ok(products);
    }

    @Operation(
            summary = "Consulter un produit par ID",
            description = "Récupère les détails d'un produit spécifique"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit trouvé"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(
            summary = "Créer un nouveau produit",
            description = "Ajoute un nouveau produit au catalogue. Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produit créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @SecurityRequirement(name = "session")
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody @Valid ProductRequestDTO dto, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    @Operation(
            summary = "Modifier un produit",
            description = "Met à jour les informations d'un produit existant. Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @SecurityRequirement(name = "session")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody @Valid ProductRequestDTO dto, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @Operation(
            summary = "Supprimer un produit (soft delete)",
            description = "Marque un produit comme supprimé (soft delete). Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produit supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Produit non trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @SecurityRequirement(name = "session")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id, HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}