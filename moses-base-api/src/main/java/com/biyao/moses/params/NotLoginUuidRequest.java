package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
public class NotLoginUuidRequest {
    @NotBlank
    private String uuid;
    @NotBlank
    private String siteId;
}
