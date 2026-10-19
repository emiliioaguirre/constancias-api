package com.universidad.constancias.models;

// solicitud con pago completo
public class PagoConfirmado {
    private final Long idSolicitud;

    public PagoConfirmado(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public Long getIdSolicitud() { return idSolicitud; }
}
