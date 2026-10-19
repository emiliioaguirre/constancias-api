package com.universidad.constancias.controllers;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.services.EstudianteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estudiantes")
public class EstudianteController {

    private final EstudianteService servicio;

    public EstudianteController(EstudianteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/autenticar")
    public String autenticarUsuario(org.springframework.security.core.Authentication usuario) {
        return usuario.getName();
    }

    @GetMapping
    public List<Estudiante> listar() {
        return servicio.listar();
    }

    @GetMapping("/{matricula}")
    public Estudiante consultarEstudiante(@PathVariable String matricula) {
        return servicio.consultarEstudiante(matricula);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Estudiante guardar(@RequestBody Estudiante estudiante) {
        return servicio.guardar(estudiante);
    }
}
