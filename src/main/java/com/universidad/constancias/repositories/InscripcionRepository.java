package com.universidad.constancias.repositories;

import com.universidad.constancias.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByEstudianteMatriculaAndPeriodoAndActivoTrue(
            String matricula, String periodo);
    List<Inscripcion> findByEstudianteMatricula(String matricula);
}
