package com.universidad.constancias;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.models.Inscripcion;
import com.universidad.constancias.repositories.EstudianteRepository;
import com.universidad.constancias.repositories.InscripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL}",
    "spring.datasource.username=${DB_USERNAME}",
    "spring.datasource.password=${DB_PASSWORD}",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false",
    "CONTROL_USUARIO=control",
    "CONTROL_PASSWORD=control123"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConstanciasApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired EstudianteRepository estudiantes;
    @Autowired InscripcionRepository inscripciones;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    @BeforeEach
    void preparar() {
        Estudiante estudiante = new Estudiante(
                "209900001", "Estudiante de prueba", "estudiante@example.com", false);
        estudiante.setContrasena(encoder.encode("practica123"));
        estudiantes.save(estudiante);
        inscripciones.save(new Inscripcion(estudiante, "2026-2", true));
    }

    private String acceso(String usuario, String clave) {
        return "Basic " + java.util.Base64.getEncoder().encodeToString(
                (usuario + ":" + clave).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private org.springframework.test.web.servlet.ResultActions ejecutar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.header("Authorization", acceso("209900001", "practica123")));
    }

    private String datosSolicitud(String periodo, String importe) {
        return """
                {"estudiante":{"matricula":"209900001","activo":false},
                 "periodo":"%s","importe":%s}
                """.formatted(periodo, importe);
    }

    private Long solicitud() throws Exception {
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2026-2", "150.00")))
                .andExpect(status().is2xxSuccessful());
        return jdbc.queryForObject("select max(id) from solicitud", Long.class);
    }

    private String datosPago(Long id, String monto, String tipo) {
        return """
                {"solicitud":{"id":%d},"monto":%s,"tipo":"%s"}
                """.formatted(id, monto, tipo);
    }

    private void esperarEnvio(Long id) throws Exception {
        for (int i = 0; i < 100; i++) {
            Boolean enviada = jdbc.queryForObject("select enviada from solicitud where id = ?", Boolean.class, id);
            if (Boolean.TRUE.equals(enviada)) {
                return;
            }
            Thread.sleep(50);
        }
        fail("No se completó el envío simulado después del pago");
    }

    @Test
    void guardaAbonoSinMarcarPagadaNiGenerarDocumento() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "50.00", "EFECTIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(50))
                .andExpect(jsonPath("$.fecha").isNotEmpty());
        ejecutar(get("/api/v1/solicitudes/" + id))
                .andExpect(jsonPath("$.pagada").value(false))
                .andExpect(jsonPath("$.generada").value(false))
                .andExpect(jsonPath("$.enviada").value(false));
        assertNull(jdbc.queryForObject("select documento from solicitud where id = ?", String.class, id));
    }

    @Test
    void alCompletarAbonosGeneraYEnviaLaConstancia() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "50", "DEBITO"))).andExpect(status().isCreated());
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "100.00", "CREDITO"))).andExpect(status().isCreated());
        esperarEnvio(id);
        ejecutar(get("/api/v1/solicitudes/" + id))
                .andExpect(jsonPath("$.pagada").value(true))
                .andExpect(jsonPath("$.generada").value(true))
                .andExpect(jsonPath("$.enviada").value(true))
                .andExpect(jsonPath("$.documento").value(org.hamcrest.Matchers.containsString("209900001")))
                .andExpect(jsonPath("$.documento").value(org.hamcrest.Matchers.containsString("2026-2")));
        assertEquals(2L, jdbc.queryForObject("select count(*) from pago", Long.class));
    }

    @Test
    void rechazaMontoCeroNegativoYSuperiorAlImporte() throws Exception {
        Long id = solicitud();
        for (String monto : new String[]{"0", "-10", "151", "0.001", "149.999"}) {
            ejecutar(post("/api/v1/pagos").contentType("application/json")
                    .content(datosPago(id, monto, "EFECTIVO"))).andExpect(status().isBadRequest());
        }
        assertEquals(0L, jdbc.queryForObject("select count(*) from pago", Long.class));
    }

    @Test
    void rechazaAbonoQueSuperaElSaldoPendiente() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "100", "OTRO"))).andExpect(status().isCreated());
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "60", "OTRO"))).andExpect(status().isBadRequest());
        assertEquals(1L, jdbc.queryForObject("select count(*) from pago", Long.class));
        assertFalse(jdbc.queryForObject("select pagada from solicitud where id = ?", Boolean.class, id));
    }

    @Test
    void rechazaSolicitudAusenteInexistenteYTipoInvalido() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content("{\"monto\":10,\"tipo\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(999999L, "10", "EFECTIVO"))).andExpect(status().isNotFound());
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "10", "CHEQUE"))).andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("select count(*) from pago", Long.class));
    }

    @Test
    void rechazaPagosPosterioresAlPagoCompleto() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "150", "EFECTIVO"))).andExpect(status().isCreated());
        esperarEnvio(id);
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "1", "EFECTIVO"))).andExpect(status().isConflict());
        assertEquals(1L, jdbc.queryForObject("select count(*) from pago", Long.class));
    }

    @Test
    void validaPeriodoYDatosAntesDeRegistrarSolicitud() throws Exception {
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2025-1", "150"))).andExpect(status().isBadRequest());
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content("""
                        {}
                        """)).andExpect(status().isBadRequest());
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2026-2", "0"))).andExpect(status().isBadRequest());
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2026-2", "150.001"))).andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("select count(*) from solicitud", Long.class));
    }

    @Test
    void permitePeriodoHistoricoYNoAceptaEstadosEnviadosPorCliente() throws Exception {
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2026-2", "150").replace("150}",
                        "150,\"pagada\":true,\"generada\":true,\"enviada\":true,\"documento\":\"inventado\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estudiante.activo").value(false))
                .andExpect(jsonPath("$.pagada").value(false))
                .andExpect(jsonPath("$.generada").value(false))
                .andExpect(jsonPath("$.enviada").value(false));
    }

    @Test
    void generarYEnviarExigenCompletarLosPasosPrevios() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/solicitudes/" + id + "/generar")).andExpect(status().isForbidden());
        ejecutar(post("/api/v1/solicitudes/" + id + "/enviar")).andExpect(status().isForbidden());
        ejecutar(get("/api/v1/solicitudes/999999")).andExpect(status().isNotFound());
        ejecutar(get("/api/v1/pagos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void pagosSimultaneosNoSuperanElSaldo() throws Exception {
        Long id = solicitud();
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarea = (java.util.concurrent.Callable<Integer>) () -> {
                preparados.countDown();
                assertTrue(iniciar.await(5, TimeUnit.SECONDS));
                return ejecutar(post("/api/v1/pagos").contentType("application/json")
                        .content(datosPago(id, "100", "EFECTIVO")))
                        .andReturn().getResponse().getStatus();
            };
            var primero = executor.submit(tarea);
            var segundo = executor.submit(tarea);
            assertTrue(preparados.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            var resultados = java.util.stream.Stream.of(
                    primero.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS)).sorted().toList();
            assertEquals(java.util.List.of(201, 400), resultados);
        }
        assertEquals(1L, jdbc.queryForObject("select count(*) from pago", Long.class));
    }

    @Test
    void generarYEnviarOtraVezConservaElDocumento() throws Exception {
        Long id = solicitud();
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "150", "OTRO"))).andExpect(status().isCreated());
        esperarEnvio(id);
        String documento = jdbc.queryForObject("select documento from solicitud where id = ?", String.class, id);
        ejecutar(post("/api/v1/solicitudes/" + id + "/generar")).andExpect(status().isOk());
        ejecutar(post("/api/v1/solicitudes/" + id + "/enviar")).andExpect(status().isOk());
        assertEquals(documento, jdbc.queryForObject("select documento from solicitud where id = ?", String.class, id));
    }

    @Test
    void rechazaConsultaSinAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/estudiantes")).andExpect(status().isUnauthorized());
    }


    @Test
    void autenticaConClaveCorrectaYRechazaClaveIncorrecta() throws Exception {
        ejecutar(get("/api/v1/estudiantes/autenticar"))
                .andExpect(status().isOk()).andExpect(content().string("209900001"));
        mvc.perform(get("/api/v1/estudiantes/autenticar")
                .header("Authorization", acceso("209900001", "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registraContrasenaCifradaSinExponerlaEnLasRespuestas() throws Exception {
        mvc.perform(post("/api/v1/estudiantes").contentType("application/json")
                .content("""
                        {"matricula":"209900002","nombre":"Otro estudiante","correo":"otro@example.com",
                         "contrasena":"practica123","activo":true,"rol":"CONTROL_ESCOLAR"}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contrasena").doesNotExist());
        String clave = jdbc.queryForObject("select contrasena from estudiante where matricula = '209900002'", String.class);
        assertNotEquals("practica123", clave);
        assertTrue(encoder.matches("practica123", clave));
        mvc.perform(get("/api/v1/estudiantes/209900002")
                .header("Authorization", acceso("209900002", "practica123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contrasena").doesNotExist());
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .header("Authorization", acceso("209900002", "practica123"))
                .content("""
                        {"estudiante":{"matricula":"209900002"},"periodo":"2026-2","activo":true}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloControlEscolarPuedeRegistrarInscripciones() throws Exception {
        String datos = """
                {"estudiante":{"matricula":"209900001"},"periodo":"2026-1","activo":true}
                """;
        ejecutar(post("/api/v1/inscripciones").contentType("application/json").content(datos))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json").content(datos)
                .header("Authorization", acceso("control", "control123")))
                .andExpect(status().isCreated());
    }

    @Test
    void unEstudianteNoPuedeConsultarNiSolicitarDatosDeOtro() throws Exception {
        Estudiante otro = new Estudiante("209900002", "Otro estudiante", "otro@example.com", true);
        otro.setContrasena(encoder.encode("practica123"));
        estudiantes.save(otro);
        inscripciones.save(new Inscripcion(otro, "2026-2", true));
        ejecutar(get("/api/v1/estudiantes/209900002")).andExpect(status().isForbidden());
        ejecutar(post("/api/v1/solicitudes").contentType("application/json")
                .content(datosSolicitud("2026-2", "150").replace("209900001", "209900002")))
                .andExpect(status().isForbidden());
        ejecutar(get("/api/v1/estudiantes")).andExpect(jsonPath("$.length()").value(1));
        ejecutar(get("/api/v1/inscripciones")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void protegeSolicitudesYPagosDeOtrosEstudiantes() throws Exception {
        Estudiante otro = new Estudiante("209900002", "Otro estudiante", "otro@example.com", true);
        otro.setContrasena(encoder.encode("practica123"));
        estudiantes.save(otro);
        inscripciones.save(new Inscripcion(otro, "2026-2", true));
        mvc.perform(post("/api/v1/solicitudes").contentType("application/json")
                .header("Authorization", acceso("209900002", "practica123"))
                .content(datosSolicitud("2026-2", "150").replace("209900001", "209900002")))
                .andExpect(status().isCreated());
        Long id = jdbc.queryForObject("select max(id) from solicitud", Long.class);
        ejecutar(get("/api/v1/solicitudes/" + id)).andExpect(status().isForbidden());
        ejecutar(post("/api/v1/pagos").contentType("application/json")
                .content(datosPago(id, "50", "EFECTIVO"))).andExpect(status().isForbidden());
        ejecutar(post("/api/v1/solicitudes/" + id + "/generar")).andExpect(status().isForbidden());
        ejecutar(post("/api/v1/solicitudes/" + id + "/enviar")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pagos").contentType("application/json")
                .header("Authorization", acceso("209900002", "practica123"))
                .content(datosPago(id, "50", "EFECTIVO"))).andExpect(status().isCreated());
        Long pago = jdbc.queryForObject("select max(id) from pago", Long.class);
        ejecutar(get("/api/v1/pagos/" + pago)).andExpect(status().isForbidden());
        ejecutar(get("/api/v1/solicitudes")).andExpect(jsonPath("$.length()").value(0));
        ejecutar(get("/api/v1/pagos")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/solicitudes").header("Authorization", acceso("control", "control123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rechazaRegistroSinContrasenaYCuentaAntiguaSinClave() throws Exception {
        mvc.perform(post("/api/v1/estudiantes").contentType("application/json")
                .content("""
                        {"matricula":"209900002","nombre":"Otro","correo":"otro@example.com","activo":true}
                        """))
                .andExpect(status().isBadRequest());
        estudiantes.save(new Estudiante("209900002", "Otro", "otro@example.com", true));
        mvc.perform(get("/api/v1/estudiantes/autenticar")
                .header("Authorization", acceso("209900002", "practica123")))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void controlEscolarPuedeAutenticarseEnPeticionesConsecutivas() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/v1/inscripciones")
                    .header("Authorization", acceso("control", "control123")))
                    .andExpect(status().isOk());
        }
    }


    @Test
    void altasSimultaneasNoReemplazanLaContrasenaDeOtraCuenta() throws Exception {
        String datos = """
                {"matricula":"209900003","nombre":"Registro simultáneo","correo":"registro@example.com",
                 "contrasena":"practica123","activo":true}
                """;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primero = executor.submit(() -> mvc.perform(post("/api/v1/estudiantes")
                    .contentType("application/json").content(datos)).andReturn().getResponse().getStatus());
            Thread.sleep(35);
            var segundo = executor.submit(() -> mvc.perform(post("/api/v1/estudiantes")
                    .contentType("application/json").content(datos.replace("practica123", "otraClave123")))
                    .andReturn().getResponse().getStatus());
            var resultados = java.util.stream.Stream.of(
                    primero.get(10, TimeUnit.SECONDS), segundo.get(10, TimeUnit.SECONDS)).sorted().toList();
            assertEquals(java.util.List.of(201, 409), resultados);
        }
    }

}
