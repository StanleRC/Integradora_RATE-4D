package mx.edu.utez.rete.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;


@Entity
@Table(name = "talleres")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Taller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTaller;
    private String nombreTaller;
    private String descripcion;
    private String instructor;
    private String dia; //Lunes, Martes etc
    private LocalTime horaInicio; // 14:00
    private LocalTime horaFin;    // 16:00
    private String aula;
    private int cupoMaximo;
    private int inscritosActuales;
    private boolean activo;

    public String getEstadoCupo() {
        return (inscritosActuales >= cupoMaximo) ? "lleno" : "libre";
    }
}