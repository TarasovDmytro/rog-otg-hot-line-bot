package ua.tarasov.hotline.models;

public enum StateOfRequest {
    SET_DEPARTMENT,
    WAIT_LOCATION,
    SET_LOCATION,
    SET_ROLES,
    WAIT_PHONE,
    SET_PHONE,
    WAIT_ADDRESS,
    SET_ADDRESS,
    WAIT_TEXT,
    SET_TEXT,
    CREATE_REQUEST,
    REQUEST_CREATED,
    SUPER_ADMIN_MANAGEMENT
}
