package ua.tarasov.hotline.models.entities;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.model.Role;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Getter
@Setter
@Entity
public class BotUser {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    private String fullName;
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Departments> departments = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Role role;

    private String phone;
}
