package com.microtech.smartshop.entity;
import com.microtech.smartshop.enums.CustomerTier;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "clients")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "fidelity_level")
    private CustomerTier fidelityLevel;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_spent", precision = 19, scale = 2)
    private BigDecimal totalSpent;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
