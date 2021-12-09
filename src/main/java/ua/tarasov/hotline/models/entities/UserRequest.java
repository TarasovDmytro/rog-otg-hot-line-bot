package ua.tarasov.hotline.models.entities;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.model.Departments;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Getter
@Setter
@Entity
public class UserRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private Departments department;
    private LocalDateTime dateTime;
    private String bodyOfMessage;
    private Long chatId;
    private Integer messageId;
    private boolean state;
    private String location;

    public void setLocation(Location location) {
        this.location = new Gson().toJson(location);
    }

    public Location getLocation() {
        return new Gson().fromJson(location, Location.class);
    }

    public String getDateTime(){
        return this.dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss"));
    }
}
