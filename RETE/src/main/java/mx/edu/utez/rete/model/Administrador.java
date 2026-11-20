package mx.edu.utez.rete.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "administradores")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombreCompleto;

    @Column(unique = true)
    private String correo;

    private String password;
    private String rol; // esto solo acepta ADMIN
}