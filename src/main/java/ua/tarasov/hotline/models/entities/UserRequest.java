package ua.tarasov.hotline.models.entities;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.model.Department;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
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
    String location;

    public void setLocation(Location location) {
        this.location = new Gson().toJson(location);
    }

    public Location getLocation() {
        return new Gson().fromJson(location, Location.class);
    }

    public String getDateTime() {
        return this.dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss"));
    }
}
