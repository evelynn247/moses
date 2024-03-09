package com.biyao.moses.params;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Setter
@Getter
public class NotLoginUuidResponse  implements Serializable {
    private boolean isNew;
}
