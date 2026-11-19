package mx.edu.utez.rete.controller;

import mx.edu.utez.rete.model.Taller;
import mx.edu.utez.rete.service.TallerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talleres")
public class TallerController {
    // Aqui se manejan las operaciones relacionadas con los talleres,
    // listar, inscribir, cancelar inscripciones y administrar talleres para los administradores

    private final TallerService tallerService;

    public TallerController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    @GetMapping
    public List<Taller> listar(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String orden) {
        return tallerService.obtenerCatalogo(q, orden);
    }

    @PostMapping("/inscribir")
    public Map<String, String> inscribir(@RequestParam int idEstudiante, @RequestParam int idTaller) {
        String resultado = tallerService.inscribirAlumno(idEstudiante, idTaller);
        return Map.of("mensaje", resultado);
    }

    @GetMapping("/mis-inscripciones")
    public List<Map<String, Object>> misInscripciones(@RequestParam int idEstudiante) {
        return tallerService.obtenerInscripcionesAlumno(idEstudiante);
    }

    @PostMapping("/cancelar")
    public Map<String, String> cancelar(@RequestParam int idInscripcion, @RequestParam int idEstudiante) {
        String resultado = tallerService.cancelarInscripcion(idInscripcion, idEstudiante);
        return Map.of("mensaje", resultado);
    }

    @GetMapping("/admin/todos")
    public List<Taller> listarTodosAdmin() {
        return tallerService.obtenerTodosParaAdmin();
    }

    @PostMapping("/admin/guardar")
    public Map<String, String> guardarTaller(@RequestBody Taller taller) {
        return Map.of("mensaje", tallerService.guardarTaller(taller));
    }

    @DeleteMapping("/admin/eliminar/{idTaller}")
    public Map<String, String> eliminarTaller(@PathVariable int idTaller) {
        return Map.of("mensaje", tallerService.eliminarTaller(idTaller));
    }

    @GetMapping("/admin/asistencia/{idTaller}")
    public List<Map<String, Object>> listaAsistencia(@PathVariable int idTaller) {
        return tallerService.obtenerListaAsistencia(idTaller);
    }
}