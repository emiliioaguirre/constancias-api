package com.universidad.constancias.services;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.models.Inscripcion;
import com.universidad.constancias.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InscripcionService {

    private final InscripcionRepository repositorio;
    private final EstudianteService estudiantes;

    private final AutenticacionService autenticacion;

    public InscripcionService(InscripcionRepository repositorio, EstudianteService estudiantes,
            AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
        this.repositorio = repositorio;
        this.estudiantes = estudiantes;
    }

    // consultar las inscripciones registradas
    public List<Inscripcion> listar() {
        if (autenticacion.esControlEscolar()) {
            return repositorio.findAll();
        }
        return repositorio.findByEstudianteMatricula(autenticacion.usuarioActual());
    }

    public Inscripcion consultar(Long id) {
        Inscripcion inscripcion = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inscripción no encontrada"));
        autenticacion.verificarPermisos(inscripcion.getEstudiante().getMatricula());
        return inscripcion;
    }

    public Inscripcion guardar(Inscripcion inscripcion) {
        // validar estudiante y periodo
        if (inscripcion.getId() != null || inscripcion.getEstudiante() == null
                || inscripcion.getPeriodo() == null || inscripcion.getPeriodo().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requiere el estudiante y periodo");
        }
        // relacionar la inscripción con un estudiante registrado
        String matricula = inscripcion.getEstudiante().getMatricula();
        Estudiante estudiante = estudiantes.consultarEstudiante(matricula);
        inscripcion.setEstudiante(estudiante);
        return repositorio.save(inscripcion);
    }

    // buscar una inscripción activa de esa matrícula y periodo
    public boolean verificarEstadoPeriodo(String matricula, String periodo) {
        return repositorio.existsByEstudianteMatriculaAndPeriodoAndActivoTrue(matricula, periodo);
    }
}
