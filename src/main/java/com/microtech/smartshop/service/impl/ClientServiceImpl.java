package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.request.ClientUpdateDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.dto.response.OrderHistoryDTO;
import com.microtech.smartshop.entity.Client;
import com.microtech.smartshop.entity.Order;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.CustomerTier;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.exception.BusinessException;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.ClientMapper;
import com.microtech.smartshop.repository.ClientRepository;
import com.microtech.smartshop.repository.OrderRepository;
import com.microtech.smartshop.repository.UserRepository;
import com.microtech.smartshop.service.ClientService;
import com.microtech.smartshop.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ClientResponseDTO createClient(ClientRequestDTO dto) {
        if (clientRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Nom d'utilisateur déjà pris");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .motDePasse(PasswordUtil.hashPassword(dto.getPassword()))
                .role(UserRole.CLIENT)
                .build();
        user = userRepository.save(user);

        Client client = clientMapper.toEntity(dto);
        client.setUser(user);
        client.setFidelityLevel(CustomerTier.BASIC);
        client.setTotalOrders(0);
        client.setTotalSpent(BigDecimal.ZERO);

        client = clientRepository.save(client);

        return clientMapper.toDto(client);
    }

    @Override
    public List<ClientResponseDTO> getAllClients() {
        return clientRepository.findAll().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponseDTO getClientById(String id) {
        return clientRepository.findById(id)
                .map(clientMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    @Override
    public ClientResponseDTO getClientByUserId(String userId) {
        return clientRepository.findByUserId(userId)
                .map(clientMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Profil client introuvable"));
    }

    @Override
    @Transactional
    public ClientResponseDTO updateClient(String id, ClientUpdateDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        // Vérifier si l'email est déjà utilisé par un autre client
        if (!client.getEmail().equals(dto.getEmail())) {
            clientRepository.findByEmail(dto.getEmail()).ifPresent(existingClient -> {
                if (!existingClient.getId().equals(id)) {
                    throw new BusinessException("Email déjà utilisé par un autre client");
                }
            });
        }

        client.setNom(dto.getNom());
        client.setEmail(dto.getEmail());
        client.setTelephone(dto.getTelephone());

        client = clientRepository.save(client);
        return clientMapper.toDto(client);
    }

    @Override
    @Transactional
    public void deleteClient(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        List<Order> clientOrders = orderRepository.findByClientId(id);
        if (!clientOrders.isEmpty()) {
            throw new BusinessException("Impossible de supprimer un client ayant des commandes existantes. " +
                    "Le client a " + clientOrders.size() + " commande(s).");
        }

        User user = client.getUser();
        clientRepository.delete(client);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Override
    public List<OrderHistoryDTO> getClientOrderHistory(String clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client introuvable");
        }

        List<Order> orders = orderRepository.findByClientId(clientId);
        return orders.stream()
                .map(order -> OrderHistoryDTO.builder()
                        .id(order.getId())
                        .orderRef(order.getOrderRef())
                        .createdAt(order.getCreatedAt())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}