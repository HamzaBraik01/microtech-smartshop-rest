package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.entity.Client;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.CustomerTier;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.exception.BusinessException;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.ClientMapper;
import com.microtech.smartshop.repository.ClientRepository;
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
}