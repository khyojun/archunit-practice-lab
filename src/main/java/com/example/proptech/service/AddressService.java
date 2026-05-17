package com.example.proptech.service;

import com.example.proptech.domain.AddressMaster;
import com.example.proptech.repository.AddressRepository;

public class AddressService {
    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public String findDisplayName(String dong) {
        return addressRepository.findByDong(dong);
    }

    public String findAddressMaster(){
        AddressMaster addressMaster = new AddressMaster();

        //addressMaster.getLegacyFullAddress();

        return "hello";
    }
}
