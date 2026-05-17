package com.example.proptech.domain;

import lombok.Getter;

@Getter
public class AddressMasterWithUpdateFieldMethod {
    private String gu;
    private String sido;
    private String sigungu;
    private String dong;

    @Deprecated
    //@Getter(AccessLevel.NONE)
    public String legacyFullAddress;


    public void updateLegacyFullAddress(String legacyFullAddress) {
        this.legacyFullAddress = legacyFullAddress;
    }
}
