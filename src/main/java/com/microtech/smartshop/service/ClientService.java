package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.request.ClientUpdateDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.dto.response.OrderHistoryDTO;

import java.util.List;

public interface ClientService {
    ClientResponseDTO createClient(ClientRequestDTO dto);
    List<ClientResponseDTO> getAllClients();
    ClientResponseDTO getClientById(String id);
    ClientResponseDTO getClientByUserId(String userId);
    ClientResponseDTO updateClient(String id, ClientUpdateDTO dto);
    void deleteClient(String id);
    List<OrderHistoryDTO> getClientOrderHistory(String clientId);
}