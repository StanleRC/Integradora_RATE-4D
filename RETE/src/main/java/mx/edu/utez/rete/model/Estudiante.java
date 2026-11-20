package mx.edu.utez.rete.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estudiantes")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstudiante;

    private String nombre;

    @Column(unique = true)
    private String matricula;

    @Column(unique = true)
    private String correo;

    private String gradoGrupo;
    private String password;
    private String rol; // Este campo es para el valor de ESTUDIANTE
}