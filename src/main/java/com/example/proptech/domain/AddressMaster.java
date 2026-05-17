package com.example.proptech.domain;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class AddressMaster {
    private String gu;
    private String sido;
    private String sigungu;
    private String dong;

    @Deprecated
    @Getter(AccessLevel.NONE)
    public String legacyFullAddress;

}
