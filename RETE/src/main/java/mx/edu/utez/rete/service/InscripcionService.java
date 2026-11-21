package mx.edu.utez.rete.service;

import lombok.RequiredArgsConstructor;
import mx.edu.utez.rete.dto.InscripcionResponseDTO;
import mx.edu.utez.rete.model.Estudiante;
import mx.edu.utez.rete.model.Inscripcion;
import mx.edu.utez.rete.model.Taller;
import mx.edu.utez.rete.repository.EstudianteRepository;
import mx.edu.utez.rete.repository.InscripcionRepository;
import mx.edu.utez.rete.repository.TallerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final TallerRepository tallerRepository;

    @Transactional(readOnly = true)
    public List<InscripcionResponseDTO> obtenerPorEstudiante(Long idEstudiante) {
        int estudianteId = validarId(idEstudiante, "estudiante");
        return inscripcionRepository
                .findByIdEstudianteAndEstadoNotOrderByIdInscripcionDesc(estudianteId, "Cancelada")
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Transactional
    public void cancelarInscripcion(Long idInscripcion, Long idEstudiante) {
        int inscripcionId = validarId(idInscripcion, "inscripción");
        int estudianteId = validarId(idEstudiante, "estudiante");

        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La inscripción no existe."
                ));

        if (!Integer.valueOf(estudianteId).equals(inscripcion.getIdEstudiante())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para cancelar esta inscripción."
            );
        }

        if (!"Confirmada".equalsIgnoreCase(inscripcion.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Solo se pueden cancelar inscripciones confirmadas."
            );
        }

        inscripcion.setEstado("Cancelada");
        inscripcionRepository.save(inscripcion);

        if (inscripcion.getIdTaller() != null) {
            tallerRepository.findById(inscripcion.getIdTaller()).ifPresent(taller -> {
                if (taller.getInscritosActuales() > 0) {
                    taller.setInscritosActuales(taller.getInscritosActuales() - 1);
                    tallerRepository.save(taller);
                }
            });
        }
    }

    private InscripcionResponseDTO mapearADTO(Inscripcion inscripcion) {
        Taller taller = inscripcion.getIdTaller() == null
                ? null
                : tallerRepository.findById(inscripcion.getIdTaller()).orElse(null);
        Estudiante estudiante = inscripcion.getIdEstudiante() == null
                ? null
                : estudianteRepository.findById(inscripcion.getIdEstudiante()).orElse(null);

        return InscripcionResponseDTO.builder()
                .idInscripcion(inscripcion.getIdInscripcion() == null
                        ? null : inscripcion.getIdInscripcion().longValue())
                .idTaller(taller == null ? null : taller.getIdTaller().longValue())
                .nombreTaller(taller != null ? taller.getNombreTaller() : "Taller sin nombre")
                .imagen(taller != null ? taller.getImagen() : null)
                .instructor(taller != null ? taller.getInstructor() : "Por asignar")
                .horario(taller != null
                        ? taller.getDia() + " de " + taller.getHoraInicio() + " a " + taller.getHoraFin()
                        : "Por definir")
                .aula(taller != null ? taller.getAula() : "Por definir")
                .folio(inscripcion.getFolio())
                .estado(inscripcion.getEstado())
                .nombreAlumno(estudiante != null ? estudiante.getNombre() : "Estudiante")
                .matricula(estudiante != null ? estudiante.getMatricula() : "N/A")
                .build();
    }

    private int validarId(Long id, String tipo) {
        if (id == null || id < 1 || id > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de " + tipo + " no es válido.");
        }
        return id.intValue();
    }
}