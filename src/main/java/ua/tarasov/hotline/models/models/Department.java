package ua.tarasov.hotline.models.models;

import lombok.Getter;

@Getter
public enum Department {
    ЖЕУ_ДОКУЧАЄВСЬКЕ("КП ЖЕУ ДОКУЧАЄВСЬКЕ"),
    КУ_ЦНСП("КУ ЦНСП"),
    ТОВ_УНІВЕРСАЛ_СЕРВІС("ТОВ УНІВЕРСАЛ-СЕРВІС"),
    СЛУЖБА_У_СПРАВАХ_ДІТЕЙ("СЛУЖБА У СПРАВАХ ДІТЕЙ"),
    РАДА("РАДА"),
    USER("Користувач");

    private final String departmentName;

    Department(String departmentName){
        this.departmentName = departmentName;
    }

    @Override
    public String toString() {
        return departmentName;
    }
}
