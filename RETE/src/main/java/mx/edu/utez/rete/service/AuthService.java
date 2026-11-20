package mx.edu.utez.rete.service;

import jakarta.annotation.PostConstruct;
import mx.edu.utez.rete.estructuradatos.ListaEnlazada;
import mx.edu.utez.rete.model.Administrador;
import mx.edu.utez.rete.model.Estudiante;
import mx.edu.utez.rete.repository.AdministradorRepository;
import mx.edu.utez.rete.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final EstudianteRepository estudianteRepository;
    private final AdministradorRepository administradorRepository;

    private final ListaEnlazada<Estudiante> estudiantes = new ListaEnlazada<>();
    private final ListaEnlazada<Administrador> administradores = new ListaEnlazada<>();

    public AuthService(EstudianteRepository estudianteRepository,
                       AdministradorRepository administradorRepository) {
        this.estudianteRepository = estudianteRepository;
        this.administradorRepository = administradorRepository;
    }

    @PostConstruct
    public void inicializarUsuarios() {
        // Aqui se precargan usuarios de prueba en la base de datos
        if (estudianteRepository.count() == 0) {
            estudianteRepository.save(new Estudiante(
                    null, "Stanle Reyes Casimiro", "20253ds108",
                    "20253ds108@utez.edu.mx", "4D", "1234", "ESTUDIANTE"
            ));
        }

        // Aqui se precarga un administrador de prueba en la base de datos
        if (administradorRepository.count() == 0) {
            administradorRepository.save(new Administrador(
                    null, "Coordinador de Talleres", "admin@utez.edu.mx", "admin123", "ADMIN"
            ));
        }

        // Aqui se cargan los usuarios de la base de datos a las listas enlazadas
        for (Estudiante estudiante : estudianteRepository.findAll()) {
            estudiantes.insertar(estudiante);
        }
        for (Administrador administrador : administradorRepository.findAll()) {
            administradores.insertar(administrador);
        }
    }

    // Aqui se manejan las operaciones de autenticación y registro de usuarios, tanto estudiantes como administradores
    public Map<String, Object> login(String identificador, String password) {
        Map<String, Object> respuesta = new HashMap<>();

        if (identificador == null || identificador.isBlank() || password == null || password.isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Debes ingresar tu correo/matrícula y contraseña.");
            return respuesta;
        }

        String idLimpio = identificador.trim();

        // Buscar primero en la ListaEnlazada de Estudiantes
        Estudiante estEncontrado = estudiantes.buscar(u ->
                (u.getMatricula().equalsIgnoreCase(idLimpio) ||
                        u.getCorreo().equalsIgnoreCase(idLimpio)) &&
                        u.getPassword().equals(password)
        );

        if (estEncontrado != null) {
            respuesta.put("exito", true);
            respuesta.put("mensaje", "Bienvenido, " + estEncontrado.getNombre());
            respuesta.put("idEstudiante", estEncontrado.getIdEstudiante());
            respuesta.put("nombre", estEncontrado.getNombre());
            respuesta.put("matricula", estEncontrado.getMatricula());
            respuesta.put("rol", estEncontrado.getRol());
            return respuesta;
        }

        // Si no es estudiante buscar en la ListaEnlazada de Administradores
        Administrador adminEncontrado = administradores.buscar(admin ->
                admin.getCorreo().equalsIgnoreCase(idLimpio) &&
                        admin.getPassword().equals(password)
        );

        if (adminEncontrado != null) {
            respuesta.put("exito", true);
            respuesta.put("mensaje", "Bienvenido, " + adminEncontrado.getNombreCompleto());
            respuesta.put("id", adminEncontrado.getId());
            respuesta.put("nombre", adminEncontrado.getNombreCompleto());
            respuesta.put("correo", adminEncontrado.getCorreo());
            respuesta.put("rol", adminEncontrado.getRol());
            return respuesta;
        }

        respuesta.put("exito", false);
        respuesta.put("mensaje", "Credenciales incorrectas.");
        return respuesta;
    }

    public Map<String, Object> registrar(Estudiante nuevo) {
        Map<String, Object> respuesta = new HashMap<>();

        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank() ||
                nuevo.getMatricula() == null || nuevo.getMatricula().isBlank() ||
                nuevo.getCorreo() == null || nuevo.getCorreo().isBlank() ||
                nuevo.getPassword() == null || nuevo.getPassword().isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Todos los campos obligatorios deben llenarse.");
            return respuesta;
        }

        if (!nuevo.getCorreo().toLowerCase().endsWith("@utez.edu.mx")) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Debes usar un correo institucional (@utez.edu.mx).");
            return respuesta;
        }

        Estudiante duplicado = estudiantes.buscar(estudiante ->
                estudiante.getMatricula().equalsIgnoreCase(nuevo.getMatricula().trim()) ||
                        estudiante.getCorreo().equalsIgnoreCase(nuevo.getCorreo().trim())
        );

        if (duplicado != null) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "La matrícula o el correo ya están registrados.");
            return respuesta;
        }

        nuevo.setIdEstudiante(null);
        nuevo.setRol("ESTUDIANTE");

        Estudiante guardado = estudianteRepository.save(nuevo);
        estudiantes.insertar(guardado);

        respuesta.put("exito", true);
        respuesta.put("mensaje", "Registro exitoso. Ya puedes iniciar sesión.");
        return respuesta;
    }

    public Map<String, Object> registrarAdministrador(Administrador nuevo) {
        Map<String, Object> respuesta = new HashMap<>();

        if (nuevo.getNombreCompleto() == null || nuevo.getNombreCompleto().isBlank() ||
                nuevo.getCorreo() == null || nuevo.getCorreo().isBlank() ||
                nuevo.getPassword() == null || nuevo.getPassword().isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Nombre completo, correo y contraseña son obligatorios.");
            return respuesta;
        }

        Administrador duplicado = administradores.buscar(admin ->
                admin.getCorreo().equalsIgnoreCase(nuevo.getCorreo().trim())
        );

        if (duplicado != null) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Ya existe un administrador registrado con ese correo.");
            return respuesta;
        }

        nuevo.setId(null);
        if (nuevo.getRol() == null || nuevo.getRol().isBlank()) {
            nuevo.setRol("ADMIN");
        }

        Administrador guardado = administradorRepository.save(nuevo);
        administradores.insertar(guardado);

        respuesta.put("exito", true);
        respuesta.put("mensaje", "Administrador registrado correctamente.");
        return respuesta;
    }

    public List<Administrador> listarAdministradores() {
        return administradores.toList();
    }
}