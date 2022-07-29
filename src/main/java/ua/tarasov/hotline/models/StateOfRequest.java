package ua.tarasov.hotline.models;

public enum StateOfRequest {
    SET_DEPARTMENT,
    SET_LOCATION,
    SET_ROLES,
    WAIT_ADDRESS,
    SET_ADDRESS,
    WAIT_TEXT,
    SET_TEXT,
    CREATE_REQUEST,
    REQUEST_CREATED,
    ROLES_CREATED
}
