package mx.edu.utez.rete.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionResponseDTO {
    private Long idInscripcion;
    private Long idTaller;
    private String nombreTaller;
    private String imagen;
    private String instructor;
    private String horario;
    private String aula;
    private String folio;
    private String estado;
    private String nombreAlumno;
    private String matricula;
}