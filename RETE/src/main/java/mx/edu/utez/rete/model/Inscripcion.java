package mx.edu.utez.rete.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idInscripcion;

    @Column(unique = true)
    private String folio;

    private Integer idEstudiante;
    private Integer idTaller;
    private LocalDateTime fechaInscripcion;
    private String estado;
}