package mx.edu.utez.rete.service;

import jakarta.annotation.PostConstruct;
import mx.edu.utez.rete.estructuradatos.ListaEnlazada;
import mx.edu.utez.rete.model.Estudiante;
import mx.edu.utez.rete.repository.EstudianteRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    // Aqui se inyecta el repositorio de estudiantes para poder acceder a la base de datos

    private final EstudianteRepository estudianteRepository;
    private final ListaEnlazada<Estudiante> usuarios = new ListaEnlazada<>();

    public AuthService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    //Este metodo es solo de prueba para inicializar la base de datos con un alumno y un administrador
    // A quien lea esto no lo borre hasta que se termine de probar la aplicacion
    // Despues yo me encargo de modificarlo para que se pueda registrar un alumno y un administrador desde la aplicacion jeje
    @PostConstruct
    public void inicializarUsuarios() {
        // Si la tabla está vacia entonces creamos un alumno y un administrador de prueba
        if (estudianteRepository.count() == 0) {
            estudianteRepository.save(new Estudiante(
                    null, "Stanle Reyes Casimiro", "20253ds108",
                    "20253ds108@utez.edu.mx", "4D", "1234", "ESTUDIANTE"
            ));
            estudianteRepository.save(new Estudiante(
                    null, "Coordinador Talleres", "admin",
                    "admin@utez.edu.mx", "N/A", "admin123", "ADMIN"
            ));
        }

        List<Estudiante> enBD = estudianteRepository.findAll();
        for (Estudiante e : enBD) {
            usuarios.insertar(e);
        }
    }

    public Map<String, Object> login(String identificador, String password) {
        Map<String, Object> respuesta = new HashMap<>();

        if (identificador == null || identificador.isBlank() || password == null || password.isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Debes ingresar tu matrícula/correo y contraseña.");
            return respuesta;
        }

        Estudiante encontrado = usuarios.buscar(u ->
                (u.getMatricula().equalsIgnoreCase(identificador.trim()) ||
                        u.getCorreo().equalsIgnoreCase(identificador.trim())) &&
                        u.getPassword().equals(password)
        );

        if (encontrado == null) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Credenciales incorrectas.");
            return respuesta;
        }

        respuesta.put("exito", true);
        respuesta.put("mensaje", "Bienvenido, " + encontrado.getNombre());
        respuesta.put("idEstudiante", encontrado.getIdEstudiante());
        respuesta.put("nombre", encontrado.getNombre());
        respuesta.put("matricula", encontrado.getMatricula());
        respuesta.put("rol", encontrado.getRol());
        return respuesta;
    }

    public Map<String, Object> registrar(Estudiante nuevo) {
        Map<String, Object> respuesta = new HashMap<>();

        // Validacion de campos
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank() ||
                nuevo.getMatricula() == null || nuevo.getMatricula().isBlank() ||
                nuevo.getCorreo() == null || nuevo.getCorreo().isBlank() ||
                nuevo.getPassword() == null || nuevo.getPassword().isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Todos los campos obligatorios deben llenarse.");
            return respuesta;
        }

        // Aqui se valida el correo institucional
        // Si quieren permitir correos de otros dominios, pueden quitar esta validacion pero no lo recomiendo
        //Si lo van a hacer solo comenten esa parte de codigo
        if (!nuevo.getCorreo().toLowerCase().endsWith("@utez.edu.mx")) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Debes usar un correo institucional (@utez.edu.mx).");
            return respuesta;
        }

        // Verificar en la ListaEnlazada que no exista la matricula o el correo
        Estudiante duplicado = usuarios.buscar(estudiante ->
                estudiante.getMatricula().equalsIgnoreCase(nuevo.getMatricula().trim()) ||
                        estudiante.getCorreo().equalsIgnoreCase(nuevo.getCorreo().trim())
        );

        if (duplicado != null) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "La matricula o el correo ya estan registrados");
            return respuesta;
        }

        nuevo.setIdEstudiante(null);
        // Todo registro nuevo entra como estudiante por defecto
        //Aun me falta el admin :(
        nuevo.setRol("ESTUDIANTE");

        // Aqui se guarda en MySQL y se agrega a la ListaEnlazada
        Estudiante guardado = estudianteRepository.save(nuevo);
        usuarios.insertar(guardado);

        respuesta.put("exito", true);
        respuesta.put("mensaje", "Registro exitoso. Ya puedes iniciar sesión.");
        return respuesta;
    }
}