package com.universidad.constancias.services;

import com.universidad.constancias.models.PagoConfirmado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConstanciaListener {
    private static final Logger log = LoggerFactory.getLogger(ConstanciaListener.class);
    private final GenerarConstanciaService documentos;
    private final EnvioCorreoService correos;

    public ConstanciaListener(GenerarConstanciaService documentos, EnvioCorreoService correos) {
        this.documentos = documentos;
        this.correos = correos;
    }

    // generar y enviar después de confirmar el pago
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarsePago(PagoConfirmado evento) {
        try {
            documentos.generarConstancia(evento.getIdSolicitud());
            correos.enviarConstancia(evento.getIdSolicitud());
        } catch (RuntimeException error) {
            log.error("[constancia] no se completó la solicitud {}: {}",
                    evento.getIdSolicitud(), error.getMessage());
        }
    }
}
