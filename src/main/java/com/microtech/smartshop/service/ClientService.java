package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import java.util.List;

public interface ClientService {
    ClientResponseDTO createClient(ClientRequestDTO dto);
    List<ClientResponseDTO> getAllClients();
    ClientResponseDTO getClientById(String id);
    ClientResponseDTO getClientByUserId(String userId);
}