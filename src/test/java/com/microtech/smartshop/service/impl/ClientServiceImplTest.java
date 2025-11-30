package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.entity.Client;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.CustomerTier;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.ClientMapper;
import com.microtech.smartshop.repository.ClientRepository;
import com.microtech.smartshop.repository.UserRepository;
import com.microtech.smartshop.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client testClient;
    private User testUser;
    private ClientRequestDTO clientRequestDTO;
    private ClientResponseDTO clientResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .username("testclient")
                .motDePasse("hashedpassword")
                .role(UserRole.CLIENT)
                .build();

        testClient = Client.builder()
                .id("client-1")
                .nom("Test Client")
                .email("test@example.com")
                .telephone("0612345678")
                .fidelityLevel(CustomerTier.BASIC)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .user(testUser)
                .build();

        clientRequestDTO = new ClientRequestDTO();
        clientRequestDTO.setNom("Test Client");
        clientRequestDTO.setEmail("test@example.com");
        clientRequestDTO.setTelephone("0612345678");
        clientRequestDTO.setUsername("testclient");
        clientRequestDTO.setPassword("password123");

        clientResponseDTO = new ClientResponseDTO();
        clientResponseDTO.setId("client-1");
        clientResponseDTO.setNom("Test Client");
        clientResponseDTO.setEmail("test@example.com");
        clientResponseDTO.setFidelityLevel(CustomerTier.BASIC);
    }

    @Test
    void testCreateClient_Success() {
        // Given
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(any(ClientRequestDTO.class))).thenReturn(testClient);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("password123"))
                    .thenReturn("hashedpassword");

            // When
            ClientResponseDTO result = clientService.createClient(clientRequestDTO);

            // Then
            assertNotNull(result);
            assertEquals("Test Client", result.getNom());
            assertEquals(CustomerTier.BASIC, result.getFidelityLevel());
            verify(userRepository, times(1)).save(any(User.class));
            verify(clientRepository, times(1)).save(any(Client.class));
        }
    }

    @Test
    void testCreateClient_EmailAlreadyExists_ThrowsException() {
        // Given
        when(clientRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testClient));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            clientService.createClient(clientRequestDTO);
        });

        assertEquals("Email déjà utilisé", exception.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void testCreateClient_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testclient")).thenReturn(Optional.of(testUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            clientService.createClient(clientRequestDTO);
        });

        assertEquals("Nom d'utilisateur déjà pris", exception.getMessage());
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void testCreateClient_InitializesWithBasicTier() {
        // Given
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(any(ClientRequestDTO.class))).thenReturn(testClient);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(anyString()))
                    .thenReturn("hashedpassword");

            // When
            clientService.createClient(clientRequestDTO);

            // Then
            verify(clientRepository, times(1)).save(argThat(client ->
                    client.getFidelityLevel() == CustomerTier.BASIC &&
                            client.getTotalOrders() == 0 &&
                            client.getTotalSpent().compareTo(BigDecimal.ZERO) == 0
            ));
        }
    }

    @Test
    void testGetAllClients_Success() {
        // Given
        Client client2 = Client.builder()
                .id("client-2")
                .nom("Client 2")
                .email("client2@example.com")
                .fidelityLevel(CustomerTier.SILVER)
                .build();

        List<Client> clients = Arrays.asList(testClient, client2);
        when(clientRepository.findAll()).thenReturn(clients);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        // When
        List<ClientResponseDTO> result = clientService.getAllClients();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(clientRepository, times(1)).findAll();
    }

    @Test
    void testGetClientById_Success() {
        // Given
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(testClient));
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        // When
        ClientResponseDTO result = clientService.getClientById("client-1");

        // Then
        assertNotNull(result);
        assertEquals("client-1", result.getId());
        verify(clientRepository, times(1)).findById("client-1");
    }

    @Test
    void testGetClientById_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findById("client-1")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            clientService.getClientById("client-1");
        });

        assertEquals("Client introuvable", exception.getMessage());
    }

    @Test
    void testGetClientByUserId_Success() {
        // Given
        when(clientRepository.findByUserId("user-1")).thenReturn(Optional.of(testClient));
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        // When
        ClientResponseDTO result = clientService.getClientByUserId("user-1");

        // Then
        assertNotNull(result);
        verify(clientRepository, times(1)).findByUserId("user-1");
    }

    @Test
    void testGetClientByUserId_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            clientService.getClientByUserId("user-1");
        });

        assertEquals("Profil client introuvable", exception.getMessage());
    }

    @Test
    void testCreateClient_PasswordIsHashed() {
        // Given
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(any(ClientRequestDTO.class))).thenReturn(testClient);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("password123"))
                    .thenReturn("hashedpassword");

            // When
            clientService.createClient(clientRequestDTO);

            // Then
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getMotDePasse().equals("hashedpassword")
            ));
        }
    }

    @Test
    void testCreateClient_CreatesUserWithClientRole() {
        // Given
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(any(ClientRequestDTO.class))).thenReturn(testClient);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientResponseDTO);

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(anyString()))
                    .thenReturn("hashedpassword");

            // When
            clientService.createClient(clientRequestDTO);

            // Then
            verify(userRepository, times(1)).save(argThat(user ->
                    user.getRole() == UserRole.CLIENT &&
                            user.getUsername().equals("testclient")
            ));
        }
    }
}

