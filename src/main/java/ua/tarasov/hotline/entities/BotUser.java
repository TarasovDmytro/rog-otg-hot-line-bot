package ua.tarasov.hotline.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@ToString
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
public class BotUser {
    @Id
    @Column(nullable = false)
    Long id;
    Integer warningCount;
    String fullName;
    String username;
    @ElementCollection(fetch = FetchType.EAGER)
    Set<Department> departments = new HashSet<>();
    @Enumerated(EnumType.STRING)
    Role role;

    String phone;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BotUser botUser = (BotUser) o;
        return id != null && Objects.equals(id, botUser.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
