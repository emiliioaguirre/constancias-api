package com.universidad.constancias.services;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.models.Solicitud;
import com.universidad.constancias.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.*;

@Service
public class SolicitudService {

    private final SolicitudRepository repositorio;
    private final EstudianteService estudiantes;
    private final InscripcionService inscripciones;

    private final AutenticacionService autenticacion;

    public SolicitudService(SolicitudRepository repositorio, EstudianteService estudiantes,
            InscripcionService inscripciones, AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
        this.repositorio = repositorio;
        this.estudiantes = estudiantes;
        this.inscripciones = inscripciones;
    }

    // consultar todas las solicitudes
    public List<Solicitud> listar() {
        if (autenticacion.esControlEscolar()) {
            return repositorio.findAll();
        }
        return repositorio.findByEstudianteMatricula(autenticacion.usuarioActual());
    }

    // buscar por id
    public Solicitud consultar(Long id) {
        Solicitud solicitud = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Solicitud no encontrada"));
        autenticacion.verificarPermisos(solicitud.getEstudiante().getMatricula());
        return solicitud;
    }

    // registrar la solicitud
    @Transactional
    public Solicitud guardar(Solicitud solicitud) {
        if (solicitud.getId() != null || solicitud.getEstudiante() == null
                || solicitud.getPeriodo() == null || solicitud.getPeriodo().isBlank()
                || solicitud.getImporte() == null || solicitud.getImporte().signum() <= 0
                || solicitud.getImporte().stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren estudiante, periodo e importe positivo, sin ID");
        }
        // buscar por matrícula
        String matricula = solicitud.getEstudiante().getMatricula();
        Estudiante estudiante = estudiantes.consultarEstudiante(matricula);
        solicitud.setEstudiante(estudiante);

        // revisar si estuvo activo en ese periodo
        String periodo = solicitud.getPeriodo();
        boolean inscrito = inscripciones.verificarEstadoPeriodo(matricula, periodo);

        if (!inscrito) {
            throw new ResponseStatusException(BAD_REQUEST, "No estás inscrito en el periodo solicitado");
        }

        // estado inicial de la solicitud
        solicitud.setPagada(false);
        solicitud.setGenerada(false);
        solicitud.setEnviada(false);
        solicitud.setDocumento(null);
        solicitud.setFecha(LocalDateTime.now());

        return repositorio.save(solicitud);
    }
}
