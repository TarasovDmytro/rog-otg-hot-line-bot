package ua.tarasov.hotline.models;

public enum Department {
    JEU_DOKUCHAEVSKE("1. КП ЖЕУ ДОКУЧАЄВСЬКЕ"),
    KU_CNSP("2. КУ ЦНСП"),
    UNIVERSAL_SERVICE("3. ТОВ УНІВЕРСАЛ-СЕРВІС"),
    CHILDREN_ACTION_SERVICE("4. СЛУЖБА У СПРАВАХ ДІТЕЙ"),
    ROHAN_COUNCIL("5. РОГАНСЬКА РАДА"),
    HROMADSKA_VARTA("6. ГРОМАДСЬКА ВАРТА"),
    USER("Користувач");

    private final String departmentName;

    Department(String departmentName) {
        this.departmentName = departmentName;
    }

    @Override
    public String toString() {
        return departmentName;
    }
}
