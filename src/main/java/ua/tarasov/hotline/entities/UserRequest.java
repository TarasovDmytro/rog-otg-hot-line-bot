package ua.tarasov.hotline.entities;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;
import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.Department;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

//@ToString
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
public class UserRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    Long id;

    Department department;
    LocalDateTime dateTime;
    String bodyOfMessage;
    Long chatId;
    Integer messageId;
    boolean state;
    String address;
    String location;

    public Location getLocation() {
        return new Gson().fromJson(location, Location.class);
    }

    public void setLocation(Location location) {
        this.location = new Gson().toJson(location);
    }

    public String getDateTimeToString() {
        if (this.dateTime != null) {
            return this.dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss"));
        } else return "Не зареєстровано";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserRequest that = (UserRequest) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        String isLocation = getLocation() != null ? "Локація: +" : "Локація: --";
        return getDepartment().toString().substring("1. ".length()) + "\nID "
                + getMessageId() +
                "\n\n" + getBodyOfMessage() +
                "\n\nадреса: " + getAddress() + "\n" + isLocation + "\n\nЗареєстрована: " + getDateTimeToString();
    }
}
