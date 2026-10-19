package com.universidad.constancias.controllers;

import com.universidad.constancias.models.Solicitud;
import com.universidad.constancias.services.SolicitudService;
import com.universidad.constancias.services.GenerarConstanciaService;
import com.universidad.constancias.services.EnvioCorreoService;
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
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {

    private final SolicitudService servicio;
    private final GenerarConstanciaService documentos;
    private final EnvioCorreoService correos;

    public SolicitudController(SolicitudService servicio, GenerarConstanciaService documentos,
            EnvioCorreoService correos) {
        this.servicio = servicio;
        this.documentos = documentos;
        this.correos = correos;
    }

    @GetMapping
    public List<Solicitud> listar() {
        return servicio.listar();
    }

    // el id llega en la url
    @GetMapping("/{id}")
    public Solicitud consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    // recibe el json y lo manda al servicio
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Solicitud guardar(@RequestBody Solicitud solicitud) {
        return servicio.guardar(solicitud);
    }

    @PostMapping("/{id}/generar")
    public Solicitud generar(@PathVariable Long id) {
        servicio.consultar(id);
        return documentos.generarConstancia(id);
    }

    @PostMapping("/{id}/enviar")
    public Solicitud enviar(@PathVariable Long id) {
        servicio.consultar(id);
        return correos.enviarConstancia(id);
    }
}
