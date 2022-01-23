package ua.tarasov.hotline.models.models;

import lombok.Getter;

@Getter
public enum Department {
    JEU_DOKUCHAEVSKE("КП ЖЕУ ДОКУЧАЄВСЬКЕ"),
    KU_CNSP("КУ ЦНСП"),
    UNIVERSAL_SERVICE("ТОВ УНІВЕРСАЛ-СЕРВІС"),
    CHILDREN_ACTION_SERVICE("СЛУЖБА У СПРАВАХ ДІТЕЙ"),
    ROHAN_COUNCIL("РОГАНСЬКА РАДА"),
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
