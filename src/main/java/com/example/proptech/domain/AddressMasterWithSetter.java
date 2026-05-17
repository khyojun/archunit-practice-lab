package com.example.proptech.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter(AccessLevel.NONE)
public class AddressMasterWithSetter {
    private String gu;
    private String sido;
    private String sigungu;
    private String dong;

    @Deprecated
    @Getter(AccessLevel.NONE)
    public String legacyFullAddress;

}
