<p align="center">
  <img src="assets/postman.svg" alt="Postman" width="160">
</p>

<h1 align="center">Constancias en Postman</h1>

Me apoyé en IA para preparar esta colección de peticiones y organizar las llamadas a la API.

Importar [Constancias.postman_collection.json](Constancias.postman_collection.json) desde **Import > Files**. Iniciar la API y ejecutar las 32 peticiones en orden, desde **Registrar estudiante**.

El estudiante usa **Basic Auth** con su matrícula y la contraseña de demostración `practica123`. El registro utiliza **No Auth**. La inscripción utiliza la cuenta de Control Escolar: `control` y `control123`; estos valores deben coincidir con `CONTROL_USUARIO` y `CONTROL_PASSWORD` en `.env`.

Si la matrícula ya está registrada, continuar con **Autenticar estudiante** usando la contraseña establecida.

El recorrido registra una solicitud de 150, un abono de 50 y otro de 100. Después consulta la constancia. También incluye rechazos por datos inválidos, generación anticipada y sobrepagos. Los códigos 400, 401, 403, 404 y 409 corresponden a los errores indicados en cada petición.

Los ejemplos se incluyen con fines prácticos para comprender el flujo de solicitudes y pagos. La generación es asíncrona; si la consulta final ocurre antes de terminar el evento, repetir la consulta.
