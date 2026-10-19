package com.universidad.constancias.controllers;

import com.universidad.constancias.models.Pago;
import com.universidad.constancias.services.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {
    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Pago> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Pago consultar(@PathVariable Long id) {
        return servicio.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pago registrarPago(@RequestBody Pago pago) {
        return servicio.registrarPago(pago);
    }
}
