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
    // Aqui se inyectan los repositorios de Taller, Inscripcion y Estudiante para poder acceder a la base de datos

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
        //Esto es solo para inicializar la base de datos con algunos talleres de prueba
        //Aun tengo que ver que hacer cuando borremos estos talleres de prueba, si los borramos de la base de datos o los dejamos ahi
        if (tallerRepository.count() == 0) {
            tallerRepository.save(new Taller(null, "Voleibol", "Entrenamiento y torneos", "Prof. Carlos", "Lunes", LocalTime.of(14, 0), LocalTime.of(16, 0), "Cancha 1", 20, 20, true));
            tallerRepository.save(new Taller(null, "Basquetbol", "Fundamentos y práctica", "Prof. Ana", "Lunes", LocalTime.of(15, 0), LocalTime.of(17, 0), "Cancha 2", 15, 8, true));
            tallerRepository.save(new Taller(null, "Pintura", "Técnicas en óleo", "Mtra. Elena", "Miércoles", LocalTime.of(14, 0), LocalTime.of(16, 0), "Aula Arte 2", 12, 5, true));
            tallerRepository.save(new Taller(null, "Canto", "Vocalización y ensamble", "Prof. Luis", "Viernes", LocalTime.of(13, 0), LocalTime.of(15, 0), "Auditorio", 10, 10, true));
        }

        //Este metodo es para inicializar las listas enlazadas con los datos de la base de datos
        for (Taller t : tallerRepository.findAll()) {
            talleres.insertar(t);
        }
        for (Inscripcion i : inscripcionRepository.findAll()) {
            inscripciones.insertar(i);
        }
    }

    //Metodo para obtener el catalogo de talleres, filtrando por nombre y ordenando por nombre o cupo disponible
    // Por las pruebas que hice funciona bien, pero si quieren pueden hacer pruebas y ver si hay algun error
    public List<Taller> obtenerCatalogo(String busqueda, String ordenarPor) {
        ListaEnlazada<Taller> filtrados = talleres.filtrar(taller -> taller.isActivo() &&
                (busqueda == null || busqueda.isBlank() ||
                        taller.getNombreTaller().toLowerCase().contains(busqueda.toLowerCase())));

        if ("nombre".equalsIgnoreCase(ordenarPor)) {
            filtrados.ordenar(Comparator.comparing(Taller::getNombreTaller));
        } else if ("cupo".equalsIgnoreCase(ordenarPor)) {
            filtrados.ordenar(Comparator.comparingInt(taller -> (taller.getCupoMaximo() - taller.getInscritosActuales())));
        }
        return filtrados.toList();
    }

    //Metodo para inscribir a un alumno a un taller, validando que no tenga otro taller activo y que el taller tenga cupo disponible
    public String inscribirAlumno(int idEstudiante, int idTaller) {
        // Esto valida que el alumno no tenga ya un taller activo
        Inscripcion inscripcionActiva = inscripciones.buscar(
                i -> i.getIdEstudiante() == idEstudiante && "Confirmada".equalsIgnoreCase(i.getEstado())
        );
        if (inscripcionActiva != null) {
            Taller tallerActual = talleres.buscar(t -> t.getIdTaller().equals(inscripcionActiva.getIdTaller()));
            String nombreActual = (tallerActual != null) ? tallerActual.getNombreTaller() : "otro taller";
            return "Ya estás inscrito en " + nombreActual + ". Solo puedes cursar un taller a la vez; cancélalo primero si deseas cambiar.";
        }

        // Esto valida la existencia, estado y cupo del taller al que se quiere inscribir el alumno
        Taller tallerDestino = talleres.buscar(t -> t.getIdTaller() == idTaller);
        if (tallerDestino == null || !tallerDestino.isActivo()) {
            return "El taller no existe o no está activo.";
        }
        if ("lleno".equals(tallerDestino.getEstadoCupo())) {
            return "El taller ya alcanzó su cupo máximo.";
        }

        // Aqui se actualiza el cupo en ListaEnlazada y en MySQL
        tallerDestino.setInscritosActuales(tallerDestino.getInscritosActuales() + 1);
        tallerRepository.save(tallerDestino);

        // Guardamos la inscripcion con un folio unico
        //Aun o estoy seguro de esto, pero creo que es mejor generar un folio unico para cada inscripcion, asi podemos identificarla facilmente
        String folio = "RETE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Inscripcion nueva = new Inscripcion(null, folio, idEstudiante, idTaller, LocalDateTime.now(), "Confirmada");
        Inscripcion guardada = inscripcionRepository.save(nueva);
        inscripciones.insertar(guardada);

        return "Inscripción confirmada con folio " + folio;
    }

    //Metodo para obtener las inscripciones de un alumno, incluyendo los detalles del taller y del alumno
    //Me dio muchos problemas esto y necesita pruebas, pero creo que funciona bien... espero
    public List<Map<String, Object>> obtenerInscripcionesAlumno(int idEstudiante) {
        ListaEnlazada<Inscripcion> delAlumno = inscripciones.filtrar(i -> i.getIdEstudiante() == idEstudiante);
        Estudiante alumno = estudianteRepository.findById(idEstudiante).orElse(null);
        List<Map<String, Object>> resultado = new ArrayList<>();

        // Aqui se recorre la lista de inscripciones del alumno y se agregan los detalles del taller y del alumno a un mapa
        for (Inscripcion inscripcion : delAlumno.toList()) {
            Taller buscarTaller = talleres.buscar(taller -> taller.getIdTaller().equals(inscripcion.getIdTaller()));
            Map<String, Object> detalle = new HashMap<>();
            detalle.put("idInscripcion", inscripcion.getIdInscripcion());
            detalle.put("folio", inscripcion.getFolio());
            detalle.put("estado", inscripcion.getEstado());
            detalle.put("fechaInscripcion", inscripcion.getFechaInscripcion().toString());
            detalle.put("nombreAlumno", alumno != null ? alumno.getNombre() : "Estudiante");
            detalle.put("matricula", alumno != null ? alumno.getMatricula() : "");

            // Aqui se agregan los detalles del taller si existe
            if (buscarTaller != null) {
                detalle.put("nombreTaller", buscarTaller.getNombreTaller());
                detalle.put("instructor", buscarTaller.getInstructor());
                detalle.put("horario", buscarTaller.getDia() + " de " + buscarTaller.getHoraInicio() + " a " + buscarTaller.getHoraFin());
                detalle.put("aula", buscarTaller.getAula());
            }
            resultado.add(detalle);
        }
        return resultado;
    }

    // Metodo para cancelar la inscripcion de un alumno a un taller, validando que exista y que este confirmada
    // Tambien libera el cupo del taller inmediatamente
    public String cancelarInscripcion(int idInscripcion, int idEstudiante) {
        Inscripcion inscripcion = inscripciones.buscar(
                i -> i.getIdInscripcion() == idInscripcion &&
                        i.getIdEstudiante() == idEstudiante &&
                        "Confirmada".equalsIgnoreCase(i.getEstado())
        );

        if (inscripcion == null) {
            return "No se encontró una inscripción activa para cancelar.";
        }

        // Cambiar estado a Cancelada en memoria y MySQL
        inscripcion.setEstado("Cancelada");
        inscripcionRepository.save(inscripcion);

        // Liberar el cupo del taller inmediatamente
        Taller taller = talleres.buscar(t -> t.getIdTaller().equals(inscripcion.getIdTaller()));
        if (taller != null && taller.getInscritosActuales() > 0) {
            taller.setInscritosActuales(taller.getInscritosActuales() - 1);
            tallerRepository.save(taller);
        }

        return "Inscripción cancelada correctamente. El cupo ha sido liberado y ya puedes elegir otro taller.";
    }

    public List<Taller> obtenerTodosParaAdmin() {
        return talleres.toList();
    }

    public String guardarTaller(Taller datos) {
        if (datos.getCupoMaximo() <= 0) {
            return "El cupo máximo debe ser mayor a cero.";
        }
        if (datos.getHoraInicio() == null || datos.getHoraFin() == null ||
                !datos.getHoraInicio().isBefore(datos.getHoraFin())) {
            return "La hora de inicio debe ser anterior a la hora de fin.";
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
            // Aqui se crea el taller nuevo
            datos.setInscritosActuales(0);
            datos.setActivo(true);
            Taller guardado = tallerRepository.save(datos);
            talleres.insertar(guardado);
            return "Taller creado correctamente.";
        } else {
            // Aqui se edita un taller existente, validando que el nuevo cupo no sea menor a los alumnos ya inscritos
            Taller existente = talleres.buscar(t -> t.getIdTaller().equals(datos.getIdTaller()));
            if (existente == null) {
                return "No se encontró el taller a editar.";
            }
            if (datos.getCupoMaximo() < existente.getInscritosActuales()) {
                return "El nuevo cupo no puede ser menor a los alumnos ya inscritos (" +
                        existente.getInscritosActuales() + ").";
            }

            existente.setNombreTaller(datos.getNombreTaller());
            existente.setDescripcion(datos.getDescripcion());
            existente.setInstructor(datos.getInstructor());
            existente.setDia(datos.getDia());
            existente.setHoraInicio(datos.getHoraInicio());
            existente.setHoraFin(datos.getHoraFin());
            existente.setAula(datos.getAula());
            existente.setCupoMaximo(datos.getCupoMaximo());
            existente.setActivo(datos.isActivo());

            tallerRepository.save(existente);
            return "Taller actualizado correctamente.";
        }
    }

    // Metodo para eliminar un taller, validando que no tenga alumnos inscritos activos
    public String eliminarTaller(int idTaller) {
        Inscripcion tieneActivos = inscripciones.buscar(
                i -> i.getIdTaller() == idTaller && "Confirmada".equalsIgnoreCase(i.getEstado())
        );
        if (tieneActivos != null) {
            return "No puedes eliminar un taller con alumnos inscritos activos. Edítalo y desactívalo en su lugar.";
            //Lo de desactivar fue idea mia jeje porque pero creo que es mejor que no se pueda eliminar un taller con alumnos inscritos activos, asi evitamos problemas
        }

        boolean eliminado = talleres.eliminar(t -> t.getIdTaller() == idTaller);
        if (!eliminado) {
            return "Taller no encontrado.";
        }
        tallerRepository.deleteById(idTaller);
        return "Taller eliminado del sistema.";
    }

    // Metodo para obtener la lista de asistencia de un taller, incluyendo folio, matricula, nombre y grado/grupo del alumno
    public List<Map<String, Object>> obtenerListaAsistencia(int idTaller) {
        ListaEnlazada<Inscripcion> inscritosTaller = inscripciones.filtrar(
                inscripcion -> inscripcion.getIdTaller() == idTaller && "Confirmada".equalsIgnoreCase(inscripcion.getEstado())
        );
        // Aqui se crea una lista de mapas para devolver los datos de los alumnos inscritos al taller
        List<Map<String, Object>> lista = new ArrayList<>();

        // Aqui se recorre la lista de inscripciones y se obtiene el estudiante correspondiente de la base de datos para agregar sus datos al mapa
        for (Inscripcion inscripcion : inscritosTaller.toList()) {
            Estudiante estudiante = estudianteRepository.findById(inscripcion.getIdEstudiante()).orElse(null);
            Map<String, Object> fila = new HashMap<>();
            fila.put("folio", inscripcion.getFolio());
            fila.put("matricula", estudiante != null ? estudiante.getMatricula() : "N/A");
            fila.put("nombre", estudiante != null ? estudiante.getNombre() : "Desconocido");
            fila.put("gradoGrupo", estudiante != null ? estudiante.getGradoGrupo() : "N/A");
            lista.add(fila);
        }
        return lista;
    }
}