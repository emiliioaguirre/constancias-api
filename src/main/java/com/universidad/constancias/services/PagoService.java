package com.universidad.constancias.services;

import com.universidad.constancias.models.Pago;
import com.universidad.constancias.models.PagoConfirmado;
import com.universidad.constancias.models.Solicitud;
import com.universidad.constancias.repositories.PagoRepository;
import com.universidad.constancias.repositories.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private final PagoRepository repositorio;
    private final SolicitudRepository solicitudes;
    private final ApplicationEventPublisher eventos;

    private final AutenticacionService autenticacion;

    public PagoService(PagoRepository repositorio, SolicitudRepository solicitudes,
            ApplicationEventPublisher eventos, AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
        this.repositorio = repositorio;
        this.solicitudes = solicitudes;
        this.eventos = eventos;
    }

    public List<Pago> listar() {
        if (autenticacion.esControlEscolar()) {
            return repositorio.findAll();
        }
        return repositorio.findBySolicitudEstudianteMatricula(autenticacion.usuarioActual());
    }

    public Pago consultar(Long id) {
        Pago pago = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pago no encontrado"));
        autenticacion.verificarPermisos(pago.getSolicitud().getEstudiante().getMatricula());
        return pago;
    }

    @Transactional
    public Pago registrarPago(Pago pago) {
        if (pago.getId() != null || pago.getSolicitud() == null || pago.getSolicitud().getId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "SOLICITUD_NO_INDICADA: se requiere una solicitud, sin ID de pago");
        }
        if (pago.getMonto() == null || pago.getMonto().signum() <= 0
                || pago.getMonto().stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(BAD_REQUEST, "MONTO_INVALIDO: use un monto positivo de hasta dos decimales");
        }
        if (pago.getTipo() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "TIPO_INVALIDO: indique DEBITO, CREDITO, EFECTIVO u OTRO");
        }

        Solicitud solicitud = solicitudes.buscarConBloqueo(pago.getSolicitud().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Solicitud no encontrada"));
        autenticacion.verificarPermisos(solicitud.getEstudiante().getMatricula());
        if (solicitud.getImporte() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "SOLICITUD_SIN_COSTO: falta el importe del trámite");
        }
        if (solicitud.isPagada()) {
            throw new ResponseStatusException(CONFLICT, "SOLICITUD_YA_PAGADA: no se permiten más pagos");
        }

        // sumar los abonos
        BigDecimal total = BigDecimal.ZERO;
        for (Pago anterior : repositorio.findBySolicitudId(solicitud.getId())) {
            total = total.add(anterior.getMonto());
        }
        total = total.add(pago.getMonto());
        if (total.compareTo(solicitud.getImporte()) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "MONTO_SUPERIOR: el pago supera el saldo pendiente");
        }

        pago.setSolicitud(solicitud);
        if (pago.getFecha() == null) {
            pago.setFecha(LocalDateTime.now());
        }
        Pago guardado = repositorio.save(pago);
        log.info("[pago] abono {} registrado para solicitud {}", pago.getMonto(), solicitud.getId());

        // publicar el evento al completar el pago
        if (total.compareTo(solicitud.getImporte()) == 0) {
            solicitud.setPagada(true);
            solicitudes.save(solicitud);
            eventos.publishEvent(new PagoConfirmado(solicitud.getId()));
        }
        return guardado;
    }
}
