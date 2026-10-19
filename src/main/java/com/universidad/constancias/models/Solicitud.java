package com.universidad.constancias.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud")
public class Solicitud {

    // id automático de la solicitud
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // un estudiante puede tener varias solicitudes
    @ManyToOne
    @JoinColumn(name = "matricula", nullable = false)
    private Estudiante estudiante;

    @Column(nullable = false)
    private String periodo;

    // importe del trámite
    @Column(nullable = false)
    private BigDecimal importe;

    private boolean pagada;

    private boolean generada;
    private boolean enviada;
    private LocalDateTime fecha;

    // texto de la constancia
    @Lob
    private String documento;

    public Solicitud() {
    }

    public Solicitud(Estudiante estudiante, String periodo, BigDecimal importe) {
        this.estudiante = estudiante;
        this.periodo = periodo;
        this.importe = importe;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public boolean isPagada() { return pagada; }
    public void setPagada(boolean pagada) { this.pagada = pagada; }

    public boolean isGenerada() { return generada; }
    public void setGenerada(boolean generada) { this.generada = generada; }

    public boolean isEnviada() { return enviada; }
    public void setEnviada(boolean enviada) { this.enviada = enviada; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
}
