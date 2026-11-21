package mx.edu.utez.rete.repository;

import mx.edu.utez.rete.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    List<Inscripcion> findByIdEstudianteAndEstadoNotOrderByIdInscripcionDesc(
            Integer idEstudiante,
            String estadoExcluido
    );
}