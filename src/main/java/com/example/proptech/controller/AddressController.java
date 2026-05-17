package com.example.proptech.controller;

import com.example.proptech.repository.AddressRepository;
import com.example.proptech.service.AddressService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;
  //  private final AddressRepository addressRepository;

    public String getAddress(String dong) {
        return addressService.findDisplayName(dong);
    }

//    public String accessByRepository(){
//        return addressRepository.findByDong("hello");
//    }
}
