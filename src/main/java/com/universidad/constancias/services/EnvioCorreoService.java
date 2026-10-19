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
public class EnvioCorreoService {
    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoService.class);
    private final SolicitudRepository repositorio;

    public EnvioCorreoService(SolicitudRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Solicitud enviarConstancia(Long id) {
        Solicitud solicitud = repositorio.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Solicitud no encontrada"));
        if (!solicitud.isGenerada() || solicitud.getDocumento() == null) {
            throw new ResponseStatusException(FORBIDDEN, "Primero debe generarse la constancia");
        }
        if (!solicitud.isEnviada()) {
            // aviso por consola
            log.info("[correo simulado] solicitud {} para {}\n{}", id,
                    solicitud.getEstudiante().getCorreo(), solicitud.getDocumento());
            solicitud.setEnviada(true);
        }
        return repositorio.save(solicitud);
    }
}
