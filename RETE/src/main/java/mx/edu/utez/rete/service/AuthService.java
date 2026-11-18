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

    private final EstudianteRepository estudianteRepository;
    private final ListaEnlazada<Estudiante> usuarios = new ListaEnlazada<>();

    public AuthService(EstudianteRepository estudianteRepository) {
        this.estudianteRepository = estudianteRepository;
    }

    @PostConstruct
    public void inicializarUsuarios() {
        // Si la tabla está vacía, creamos un alumno y un administrador de prueba
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

        // Búsqueda usando el método buscar() de tu ListaEnlazada
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

        // Validación de campos obligatorios (DFR)
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank() ||
                nuevo.getMatricula() == null || nuevo.getMatricula().isBlank() ||
                nuevo.getCorreo() == null || nuevo.getCorreo().isBlank() ||
                nuevo.getPassword() == null || nuevo.getPassword().isBlank()) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Todos los campos obligatorios deben llenarse.");
            return respuesta;
        }

        // Regla 1.1 del DFR: validar correo institucional
        if (!nuevo.getCorreo().toLowerCase().endsWith("@utez.edu.mx")) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Debes usar un correo institucional (@utez.edu.mx).");
            return respuesta;
        }

        // Verificar en la ListaEnlazada que no exista la matrícula o el correo
        Estudiante duplicado = usuarios.buscar(u ->
                u.getMatricula().equalsIgnoreCase(nuevo.getMatricula().trim()) ||
                        u.getCorreo().equalsIgnoreCase(nuevo.getCorreo().trim())
        );

        if (duplicado != null) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "La matrícula o el correo ya están registrados.");
            return respuesta;
        }

        nuevo.setIdEstudiante(null);
        nuevo.setRol("ESTUDIANTE"); // Todo registro nuevo entra como estudiante por defecto

        // Guardar en MySQL y agregar a la ListaEnlazada en memoria
        Estudiante guardado = estudianteRepository.save(nuevo);
        usuarios.insertar(guardado);

        respuesta.put("exito", true);
        respuesta.put("mensaje", "Registro exitoso. Ya puedes iniciar sesión.");
        return respuesta;
    }
}