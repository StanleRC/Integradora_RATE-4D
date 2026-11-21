package mx.edu.utez.rete.controller;

import lombok.RequiredArgsConstructor;
import mx.edu.utez.rete.dto.InscripcionResponseDTO;
import mx.edu.utez.rete.dto.MensajeResponseDTO;
import mx.edu.utez.rete.service.InscripcionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/talleres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    @GetMapping("/mis-inscripciones")
    public ResponseEntity<List<InscripcionResponseDTO>> misInscripciones(
            @RequestParam Long idEstudiante) {
        return ResponseEntity.ok(inscripcionService.obtenerPorEstudiante(idEstudiante));
    }

    @PostMapping("/cancelar")
    public ResponseEntity<MensajeResponseDTO> cancelar(
            @RequestParam Long idInscripcion,
            @RequestParam Long idEstudiante) {
        try {
            inscripcionService.cancelarInscripcion(idInscripcion, idEstudiante);
            return ResponseEntity.ok(
                    new MensajeResponseDTO("Inscripción cancelada correctamente. Tu lugar ha sido liberado.")
            );
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(new MensajeResponseDTO(ex.getReason()));
        }
    }
}