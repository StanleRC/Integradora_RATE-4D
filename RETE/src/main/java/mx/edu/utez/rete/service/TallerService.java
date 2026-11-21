package mx.edu.utez.rete.service;

import jakarta.annotation.PostConstruct;
import mx.edu.utez.rete.estructuradatos.ListaEnlazada;
import mx.edu.utez.rete.model.Estudiante;
import mx.edu.utez.rete.model.Inscripcion;
import mx.edu.utez.rete.model.Taller;
import mx.edu.utez.rete.repository.EstudianteRepository;
import mx.edu.utez.rete.repository.InscripcionRepository;
import mx.edu.utez.rete.repository.TallerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class TallerService {

    private final TallerRepository tallerRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;

    private final ListaEnlazada<Taller> talleres = new ListaEnlazada<>();
    private final ListaEnlazada<Inscripcion> inscripciones = new ListaEnlazada<>();

    public TallerService(TallerRepository tallerRepository,
                         InscripcionRepository inscripcionRepository,
                         EstudianteRepository estudianteRepository) {
        this.tallerRepository = tallerRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.estudianteRepository = estudianteRepository;
    }

    @PostConstruct
    public void inicializarDatos() {
        if (tallerRepository.count() == 0) {
            tallerRepository.save(new Taller(null, "Voleibol", "Entrenamiento y torneos", "Prof. Carlos", "Lunes", LocalTime.of(14, 0), LocalTime.of(16, 0), "Cancha 1", null, 20, 20, true));
            tallerRepository.save(new Taller(null, "Basquetbol", "Fundamentos y práctica", "Prof. Ana", "Lunes", LocalTime.of(15, 0), LocalTime.of(17, 0), "Cancha 2", null, 15, 8, true));
            tallerRepository.save(new Taller(null, "Pintura", "Técnicas en óleo", "Mtra. Elena", "Miércoles", LocalTime.of(14, 0), LocalTime.of(16, 0), "Aula Arte 2", null, 12, 5, true));
            tallerRepository.save(new Taller(null, "Canto", "Vocalización y ensamble", "Prof. Luis", "Viernes", LocalTime.of(13, 0), LocalTime.of(15, 0), "Auditorio", "/img/talleres/canto-1.png", 10, 10, true));
        }

        for (Taller t : tallerRepository.findAll()) {
            if ("Canto".equalsIgnoreCase(t.getNombreTaller()) &&
                    (t.getImagen() == null || t.getImagen().isBlank())) {
                t.setImagen("/img/talleres/canto-1.png");
                tallerRepository.save(t);
            }
            talleres.insertar(t);
        }
        for (Inscripcion i : inscripcionRepository.findAll()) {
            inscripciones.insertar(i);
        }
    }

    public List<Taller> obtenerCatalogo(String busqueda, String ordenarPor) {
        ListaEnlazada<Taller> filtrados = talleres.filtrar(t -> t.isActivo() &&
                (busqueda == null || busqueda.isBlank() ||
                        t.getNombreTaller().toLowerCase().contains(busqueda.toLowerCase())));

        if ("nombre".equalsIgnoreCase(ordenarPor)) {
            filtrados.ordenar(Comparator.comparing(Taller::getNombreTaller));
        } else if ("cupo".equalsIgnoreCase(ordenarPor)) {
            filtrados.ordenar(Comparator.comparingInt(t -> (t.getCupoMaximo() - t.getInscritosActuales())));
        }
        return filtrados.toList();
    }

    public String inscribirAlumno(int idEstudiante, int idTaller) {
        // 1. Validar que el alumno no tenga ya un taller activo (Regla: máximo 1 taller a la vez)
        Inscripcion inscripcionActiva = inscripciones.buscar(
                i -> i.getIdEstudiante() == idEstudiante && "Confirmada".equalsIgnoreCase(i.getEstado())
        );
        if (inscripcionActiva != null) {
            Taller tallerActual = talleres.buscar(t -> t.getIdTaller().equals(inscripcionActiva.getIdTaller()));
            String nombreActual = (tallerActual != null) ? tallerActual.getNombreTaller() : "otro taller";
            return "ERROR: Ya estás inscrito en " + nombreActual + ". Solo puedes cursar un taller a la vez; cancélalo primero si deseas cambiar.";
        }

        // 2. Validar existencia, estado y cupo del taller destino
        Taller tallerDestino = talleres.buscar(t -> t.getIdTaller() == idTaller);
        if (tallerDestino == null || !tallerDestino.isActivo()) {
            return "ERROR: El taller no existe o no está activo.";
        }
        if ("lleno".equals(tallerDestino.getEstadoCupo())) {
            return "ERROR: El taller ya alcanzó su cupo máximo.";
        }

        // 3. Actualizar cupo en ListaEnlazada y en MySQL
        tallerDestino.setInscritosActuales(tallerDestino.getInscritosActuales() + 1);
        tallerRepository.save(tallerDestino);

        // 4. Guardar inscripción con folio único
        String folio = "RETE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Inscripcion nueva = new Inscripcion(null, folio, idEstudiante, idTaller, LocalDateTime.now(), "Confirmada");
        Inscripcion guardada = inscripcionRepository.save(nueva);
        inscripciones.insertar(guardada);

        return "OK: Inscripción confirmada con folio " + folio;
    }

    public List<Map<String, Object>> obtenerInscripcionesAlumno(int idEstudiante) {
        ListaEnlazada<Inscripcion> delAlumno = inscripciones.filtrar(i -> i.getIdEstudiante() == idEstudiante);
        Estudiante alumno = estudianteRepository.findById(idEstudiante).orElse(null);
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Inscripcion insc : delAlumno.toList()) {
            Taller t = talleres.buscar(taller -> taller.getIdTaller().equals(insc.getIdTaller()));
            Map<String, Object> detalle = new HashMap<>();
            detalle.put("idInscripcion", insc.getIdInscripcion());
            detalle.put("folio", insc.getFolio());
            detalle.put("estado", insc.getEstado());
            detalle.put("fechaInscripcion", insc.getFechaInscripcion().toString());
            detalle.put("nombreAlumno", alumno != null ? alumno.getNombre() : "Estudiante");
            detalle.put("matricula", alumno != null ? alumno.getMatricula() : "");

            if (t != null) {
                detalle.put("nombreTaller", t.getNombreTaller());
                detalle.put("instructor", t.getInstructor());
                detalle.put("imagen", t.getImagen());
                detalle.put("horario", t.getDia() + " de " + t.getHoraInicio() + " a " + t.getHoraFin());
                detalle.put("aula", t.getAula());
            }
            resultado.add(detalle);
        }
        return resultado;
    }

    public String cancelarInscripcion(int idInscripcion, int idEstudiante) {
        Inscripcion insc = inscripciones.buscar(
                i -> i.getIdInscripcion() == idInscripcion &&
                        i.getIdEstudiante() == idEstudiante &&
                        "Confirmada".equalsIgnoreCase(i.getEstado())
        );

        if (insc == null) {
            return "ERROR: No se encontró una inscripción activa para cancelar.";
        }

        // Cambiar estado a Cancelada en memoria y MySQL
        insc.setEstado("Cancelada");
        inscripcionRepository.save(insc);

        // Liberar el cupo del taller inmediatamente
        Taller taller = talleres.buscar(t -> t.getIdTaller().equals(insc.getIdTaller()));
        if (taller != null && taller.getInscritosActuales() > 0) {
            taller.setInscritosActuales(taller.getInscritosActuales() - 1);
            tallerRepository.save(taller);
        }

        return "OK: Inscripción cancelada. El cupo ha sido liberado y ya puedes elegir otro taller.";
    }

    public List<Taller> obtenerTodosParaAdmin() {
        return talleres.toList();
    }

    public String guardarTaller(Taller datos) {
        datos.setImagen(normalizarImagen(datos.getImagen()));
        if (datos.getCupoMaximo() <= 0) {
            return "ERROR: El cupo máximo debe ser mayor a cero.";
        }
        if (datos.getHoraInicio() == null || datos.getHoraFin() == null ||
                !datos.getHoraInicio().isBefore(datos.getHoraFin())) {
            return "ERROR: La hora de inicio debe ser anterior a la hora de fin.";
        }

        // Regla 2.1 del DFR: evitar cruce de horarios para el mismo instructor usando ListaEnlazada
        Taller cruceInstructor = talleres.buscar(t ->
                (datos.getIdTaller() == null || !t.getIdTaller().equals(datos.getIdTaller())) &&
                        t.isActivo() &&
                        t.getInstructor().equalsIgnoreCase(datos.getInstructor().trim()) &&
                        t.getDia().equalsIgnoreCase(datos.getDia()) &&
                        t.getHoraInicio().isBefore(datos.getHoraFin()) &&
                        datos.getHoraInicio().isBefore(t.getHoraFin())
        );

        if (cruceInstructor != null) {
            return "ERROR: El instructor " + datos.getInstructor() + " ya imparte '" +
                    cruceInstructor.getNombreTaller() + "' en ese horario.";
        }

        if (datos.getIdTaller() == null) {
            // Creación de taller nuevo
            datos.setInscritosActuales(0);
            datos.setActivo(true);
            Taller guardado = tallerRepository.save(datos);
            talleres.insertar(guardado);
            return "OK: Taller creado correctamente.";
        } else {
            // Edición de taller existente
            Taller existente = talleres.buscar(t -> t.getIdTaller().equals(datos.getIdTaller()));
            if (existente == null) {
                return "ERROR: No se encontró el taller a editar.";
            }
            if (datos.getCupoMaximo() < existente.getInscritosActuales()) {
                return "ERROR: El nuevo cupo no puede ser menor a los alumnos ya inscritos (" +
                        existente.getInscritosActuales() + ").";
            }

            existente.setNombreTaller(datos.getNombreTaller());
            existente.setDescripcion(datos.getDescripcion());
            existente.setInstructor(datos.getInstructor());
            existente.setDia(datos.getDia());
            existente.setHoraInicio(datos.getHoraInicio());
            existente.setHoraFin(datos.getHoraFin());
            existente.setAula(datos.getAula());
            existente.setImagen(datos.getImagen());
            existente.setCupoMaximo(datos.getCupoMaximo());
            existente.setActivo(datos.isActivo());

            tallerRepository.save(existente);
            return "OK: Taller actualizado correctamente.";
        }
    }

    private String normalizarImagen(String imagen) {
        return imagen == null || imagen.isBlank() ? null : imagen.trim();
    }

    public String eliminarTaller(int idTaller) {
        Inscripcion tieneActivos = inscripciones.buscar(
                i -> i.getIdTaller() == idTaller && "Confirmada".equalsIgnoreCase(i.getEstado())
        );
        if (tieneActivos != null) {
            return "ERROR: No puedes eliminar un taller con alumnos inscritos activos. Edítalo y desactívalo en su lugar.";
        }

        boolean eliminado = talleres.eliminar(t -> t.getIdTaller() == idTaller);
        if (!eliminado) {
            return "ERROR: Taller no encontrado.";
        }
        tallerRepository.deleteById(idTaller);
        return "OK: Taller eliminado del sistema.";
    }

    public List<Map<String, Object>> obtenerListaAsistencia(int idTaller) {
        ListaEnlazada<Inscripcion> inscritosTaller = inscripciones.filtrar(
                i -> i.getIdTaller() == idTaller && "Confirmada".equalsIgnoreCase(i.getEstado())
        );
        List<Map<String, Object>> lista = new ArrayList<>();

        for (Inscripcion insc : inscritosTaller.toList()) {
            Estudiante est = estudianteRepository.findById(insc.getIdEstudiante()).orElse(null);
            Map<String, Object> fila = new HashMap<>();
            fila.put("folio", insc.getFolio());
            fila.put("matricula", est != null ? est.getMatricula() : "N/A");
            fila.put("nombre", est != null ? est.getNombre() : "Desconocido");
            fila.put("gradoGrupo", est != null ? est.getGradoGrupo() : "N/A");
            lista.add(fila);
        }
        return lista;
    }
}