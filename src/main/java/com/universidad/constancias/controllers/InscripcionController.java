package com.universidad.constancias.controllers;

import com.universidad.constancias.models.Inscripcion;
import com.universidad.constancias.services.InscripcionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inscripciones")
public class InscripcionController {
    private final InscripcionService servicio;

    public InscripcionController(InscripcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Inscripcion> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Inscripcion consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Inscripcion guardar(@RequestBody Inscripcion inscripcion) {
        return servicio.guardar(inscripcion);
    }
}
