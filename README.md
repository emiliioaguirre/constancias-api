<p align="center">
  <img src="docs/assets/constancias.png" alt="Icono de constancias" width="112">
</p>

<h1 align="center">API de Constancias</h1>

<p align="center">Solicitud de constancias de estudios, inscripción por periodo y registro de pagos.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=flat" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat" alt="MySQL">
  <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat" alt="Postman">
</p>

## Resumen

La API gestiona solicitudes de constancias de estudios, valida la inscripción por periodo y registra los pagos del trámite. El proceso acordado comprende seis actividades:

1. El estudiante se autentica.
2. El estudiante solicita una constancia de estudios.
3. Se verifica que el estudiante haya estado activo en el periodo solicitado.
4. Se realiza el cobro del trámite.
5. Se genera el documento PDF.
6. Se envía el documento PDF por correo electrónico.

El estudiante se autentica con su matrícula y contraseña mediante HTTP Basic. Solo puede operar sobre sus datos; Control Escolar registra las inscripciones. El cobro se representa mediante pagos y abonos, la constancia se guarda como texto y el envío se registra mediante logger.

## Estructura del proyecto

```text
src/main/java/com/universidad/constancias/
├── ConstanciasApplication.java        # Arranque de Spring Boot
├── controllers/
│   ├── EstudianteController.java      # Rutas de estudiantes
│   ├── InscripcionController.java     # Rutas de inscripciones
│   ├── PagoController.java            # Rutas de pagos
│   └── SolicitudController.java       # Rutas del trámite
├── models/
│   ├── Estudiante.java                # Datos del estudiante
│   ├── Inscripcion.java               # Estado por periodo
│   ├── Pago.java                      # Datos del abono
│   ├── PagoConfirmado.java            # Evento de pago completo
│   └── Solicitud.java                 # Datos y estados del trámite
├── repositories/
│   ├── EstudianteRepository.java      # Consultas de estudiantes
│   ├── InscripcionRepository.java     # Consultas de inscripciones
│   ├── PagoRepository.java            # Consultas de pagos
│   └── SolicitudRepository.java       # Consultas de solicitudes
└── services/
    ├── AutenticacionService.java      # Autenticación y permisos
    ├── ConstanciaListener.java        # Atención del evento de pago
    ├── EnvioCorreoService.java        # Envío simulado por consola
    ├── EstudianteService.java         # Operaciones de estudiantes
    ├── GenerarConstanciaService.java  # Texto de la constancia
    ├── InscripcionService.java        # Validación por periodo
    ├── PagoService.java               # Abonos y pago completo
    └── SolicitudService.java          # Registro de solicitudes
```

Los controladores reciben las peticiones HTTP, los servicios aplican las reglas de negocio y los repositorios acceden a MySQL. Los modelos representan los datos del trámite y el evento que comunica la confirmación del pago.

## Instalación y ejecución

Requiere JDK 21 y MySQL en ejecución.

```bash
git clone https://github.com/emiliioaguirre/constancias-api.git
cd constancias-api
cp .env.example .env
```

Crear la base desde MySQL Workbench o el cliente de MySQL:

```sql
CREATE DATABASE IF NOT EXISTS constancias CHARACTER SET utf8mb4;
```

Configurar las credenciales de MySQL en `.env`:

| Variable | Uso | Valor de ejemplo |
|---|---|---|
| `DB_URL` | Dirección de la base | `jdbc:mysql://localhost:3306/constancias` |
| `DB_USERNAME` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña local | Configurar en `.env` |
| `CONTROL_USUARIO` | Usuario de Control Escolar | `control` |
| `CONTROL_PASSWORD` | Contraseña de Control Escolar | Configurar en `.env` |

Ejecutar `ConstanciasApplication.java` con **Run Java**, o iniciar desde la terminal con:

```bash
./mvnw spring-boot:run
```

En Windows, usar `mvnw.cmd`. Ejecutar los comandos desde la raíz del proyecto. Hibernate crea o actualiza las tablas al iniciar.

**URL base:** `http://localhost:8080/api/v1`.

## Endpoints

Los POST de registro reciben JSON con `Content-Type: application/json`. Las rutas de la tabla parten de `/api/v1`.

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/estudiantes` | Registrar estudiante | 201 Created |
| GET | `/estudiantes/autenticar` | Autenticar con matrícula y contraseña | 200 OK |
| GET | `/estudiantes` | Listar estudiantes | 200 OK |
| GET | `/estudiantes/{matricula}` | Consultar estudiante | 200 OK |
| POST | `/inscripciones` | Registrar inscripción | 201 Created |
| GET | `/inscripciones` | Listar inscripciones | 200 OK |
| GET | `/inscripciones/{id}` | Consultar inscripción | 200 OK |
| POST | `/solicitudes` | Registrar solicitud | 201 Created |
| GET | `/solicitudes` | Listar solicitudes | 200 OK |
| GET | `/solicitudes/{id}` | Consultar solicitud y sus estados | 200 OK |
| POST | `/pagos` | Registrar pago o abono | 201 Created |
| GET | `/pagos` | Listar pagos | 200 OK |
| GET | `/pagos/{id}` | Consultar pago | 200 OK |
| POST | `/solicitudes/{id}/generar` | Generar constancia de una solicitud pagada | 200 OK |
| POST | `/solicitudes/{id}/enviar` | Registrar envío de una constancia generada | 200 OK |

## Postman

Importar la [colección de Postman](postman/Constancias.postman_collection.json) y ejecutar las peticiones en orden. Las [instrucciones de uso](postman/README.md) explican cómo realizar las pruebas en Postman.

### Ejemplos de peticiones

Usar los IDs obtenidos en las respuestas.

**POST `/estudiantes`**

```json
{
  "matricula": "202360079",
  "nombre": "Emilio Alejandro Aguirre Tronco",
  "correo": "202360079@ucc.mx",
  "contrasena": "practica123",
  "activo": false
}
```

**POST `/inscripciones`**

```json
{
  "estudiante": { "matricula": "202360079" },
  "periodo": "2026-2",
  "activo": true
}
```

**POST `/solicitudes`**

```json
{
  "estudiante": { "matricula": "202360079" },
  "periodo": "2026-2",
  "importe": 150
}
```

**POST `/pagos`**, sustituyendo `1` por el ID de la solicitud:

```json
{
  "solicitud": { "id": 1 },
  "monto": 150,
  "tipo": "EFECTIVO"
}
```

### Pagos y abonos

- El importe puede cubrirse en un pago o en abonos, por ejemplo 50 y 100.
- Se admiten `DEBITO`, `CREDITO`, `EFECTIVO` y `OTRO`.
- Si no se envía la fecha del pago, se asigna automáticamente.

### Consulta de la constancia

Al completar el pago, se inicia la generación y el envío de la constancia. Consultar el resultado con `GET /solicitudes/{id}`. Si sigue procesándose, repetir la consulta.

Las rutas de generar y enviar permiten continuar esos pasos manualmente. Si ya se completaron, conservan el resultado.

## Manejo de errores

Los servicios validan los datos y el estado de la solicitud antes de guardar. Los rechazos incluyen el código HTTP y el motivo en `message`.

| Código HTTP | Significado | Casos en la API |
|---|---|---|
| `401 Unauthorized` | Autenticación requerida | Faltan credenciales o la matrícula y contraseña no son válidas. |
| `400 Bad Request` | Datos inválidos | Campos obligatorios ausentes, matrícula inválida, periodo sin inscripción activa, monto o tipo de pago inválido y abonos superiores al saldo. |
| `403 Forbidden` | Operación no permitida | Consultar o modificar registros ajenos, registrar inscripciones sin ser Control Escolar, generar sin completar el pago o enviar sin documento. |
| `404 Not Found` | Registro inexistente | No se encuentra el estudiante, la inscripción, la solicitud o el pago consultado. |
| `409 Conflict` | Conflicto con el estado actual | Registrar una matrícula duplicada o agregar un pago a una solicitud ya pagada. |

### Errores del registro de pagos

| Identificador | Código HTTP | Condición |
|---|---|---|
| `SOLICITUD_NO_INDICADA` | `400` | Falta la solicitud o su ID, o se envía un ID de pago al registrar. |
| `MONTO_INVALIDO` | `400` | El monto está ausente, no es positivo o contiene fracciones de centavo. |
| `TIPO_INVALIDO` | `400` | No se indica el tipo de pago. Los valores admitidos son `DEBITO`, `CREDITO`, `EFECTIVO` y `OTRO`. |
| `SOLICITUD_SIN_COSTO` | `400` | La solicitud no tiene un importe definido. |
| `MONTO_SUPERIOR` | `400` | La suma del nuevo pago y los abonos anteriores supera el importe del trámite. |
| `SOLICITUD_YA_PAGADA` | `409` | La solicitud ya tiene el pago completo. |
