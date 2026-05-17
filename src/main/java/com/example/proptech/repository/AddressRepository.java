package com.example.proptech.repository;

import com.example.proptech.domain.AddressMaster;

public class AddressRepository {
    public String findByDong(String dong) {
        return "경기도 화성시 동탄구 " + dong;
    }

    public void callByGetMethod(){
        AddressMaster addressMaster = new AddressMaster();
        //addressMaster.getLegacyFullAddress();
    }
}
