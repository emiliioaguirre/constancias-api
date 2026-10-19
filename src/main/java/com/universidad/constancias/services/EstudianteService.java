package com.universidad.constancias.services;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.repositories.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityExistsException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

import java.util.List;

@Service
public class EstudianteService {

    private final EstudianteRepository repositorio;

    private final PasswordEncoder encoder;
    private final EntityManager entidades;
    private final AutenticacionService autenticacion;

    public EstudianteService(EstudianteRepository repositorio, PasswordEncoder encoder,
            AutenticacionService autenticacion, EntityManager entidades) {
        this.entidades = entidades;
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.autenticacion = autenticacion;
    }

    // consultar todos los estudiantes
    public List<Estudiante> listar() {
        if (autenticacion.esControlEscolar()) {
            return repositorio.findAll();
        }
        return List.of(consultarEstudiante(autenticacion.usuarioActual()));
    }

    // guardar los datos en la base
    @Transactional
    public Estudiante guardar(Estudiante estudiante) {
        if (estudiante.getMatricula() == null || estudiante.getMatricula().length() != 9
                || estudiante.getNombre() == null || estudiante.getNombre().isBlank()
                || estudiante.getCorreo() == null || !estudiante.getCorreo().contains("@")) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren matrícula de 9 caracteres, nombre y correo");
        }
        if (estudiante.getContrasena() == null || estudiante.getContrasena().length() < 8
                || estudiante.getContrasena().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requiere una contraseña de 8 a 72 bytes");
        }
        if (repositorio.existsById(estudiante.getMatricula())) {
            throw new ResponseStatusException(CONFLICT, "La matrícula ya está registrada");
        }
        estudiante.setContrasena(encoder.encode(estudiante.getContrasena()));
        try {
            entidades.persist(estudiante);
            entidades.flush();
            return estudiante;
        } catch (EntityExistsException | ConstraintViolationException error) {
            throw new ResponseStatusException(CONFLICT, "La matrícula ya está registrada");
        }
    }

    public Estudiante consultarEstudiante(String matricula) {
        // la matrícula no puede estar vacía
        if (matricula == null || matricula.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "La matrícula es obligatoria");
        }
        autenticacion.verificarPermisos(matricula);
        // buscar por matrícula
        return repositorio.findById(matricula)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Estudiante no encontrado"));
    }

}
