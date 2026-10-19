package com.universidad.constancias.services;

import com.universidad.constancias.models.Solicitud;
import com.universidad.constancias.repositories.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class GenerarConstanciaService {
    private static final Logger log = LoggerFactory.getLogger(GenerarConstanciaService.class);
    private final SolicitudRepository repositorio;

    public GenerarConstanciaService(SolicitudRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Solicitud generarConstancia(Long id) {
        Solicitud solicitud = repositorio.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Solicitud no encontrada"));
        if (!solicitud.isPagada()) {
            throw new ResponseStatusException(FORBIDDEN, "Primero debe completarse el pago");
        }
        if (solicitud.isGenerada()) {
            return solicitud;
        }

        // texto de la constancia
        String documento = "CONSTANCIA DE ESTUDIOS\n"
                + "Folio: " + solicitud.getId() + "\n"
                + "Se hace constar que " + solicitud.getEstudiante().getNombre()
                + ", con matrícula " + solicitud.getEstudiante().getMatricula()
                + ", estuvo inscrito y activo en el periodo " + solicitud.getPeriodo() + ".";
        solicitud.setDocumento(documento);
        solicitud.setGenerada(true);
        log.info("[constancia] documento de solicitud {} generado", id);
        return repositorio.save(solicitud);
    }
}
