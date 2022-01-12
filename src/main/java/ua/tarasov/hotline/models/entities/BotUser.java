package ua.tarasov.hotline.models.entities;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ua.tarasov.hotline.models.models.Department;
import ua.tarasov.hotline.models.models.Role;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
public class BotUser {
    @Id
    @Column(nullable = false)
    Long id;

    String fullName;
    String username;

    @ElementCollection(fetch = FetchType.EAGER)
    Set<Department> departments = new HashSet<>();

    @Enumerated(EnumType.STRING)
    Role role;

    String phone;
}
