package mx.edu.utez.rete.repository;

import mx.edu.utez.rete.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {
}