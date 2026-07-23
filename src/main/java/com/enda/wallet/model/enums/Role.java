package com.enda.wallet.model.enums;

import lombok.Getter;
import lombok.Setter;



public enum Role {
    INITIATEUR,
    VALIDATEUR,
    FINANCE,
    ADMIN;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isValidator() {
        return this == VALIDATEUR || this == ADMIN;
    }

    public boolean isInitiator() {
        return this == INITIATEUR || this == ADMIN;
    }

    public boolean isFinance() {
        return this == FINANCE || this == ADMIN;
    }
}