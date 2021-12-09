package ua.tarasov.hotline.models.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Enumerated;

@Getter
public enum Departments {
    ЖЕУ_ДОКУЧАЄВСЬКЕ,
    КУ_ЦНСП,
    ТОВ_УНІВЕРСАЛ_СЕРВІС,
    СЛУЖБА_У_СПРАВАХ_ДІТЕЙ,
    РАДА,
    USER
}
