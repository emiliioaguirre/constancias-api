package com.universidad.constancias.repositories;

import com.universidad.constancias.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findBySolicitudId(Long idSolicitud);
    List<Pago> findBySolicitudEstudianteMatricula(String matricula);
}
