# ALMORA - Banco de alimentos (API)

Aplicación web (frontend en HTML, CSS y JavaScript puro + API REST en **Spring Boot 4.1 + Java 21 + MongoDB Atlas**) para gestionar donantes, donaciones,
inventario por lotes con **asignación FEFO**, entregas a fundaciones con **seguimiento tipo guía de mensajería**,
certificados en PDF, reportes PDF/Excel, auditoría y respaldos. Seguridad con **JWT** (acceso + refresco rotativo).

## Arranque rápido (local)

1. Copia `.env.example` como `.env` y llena `MONGODB_URI`, `JWT_SECRET` y los datos del superadministrador.
2. Ejecuta `scripts/mongo-atlas-setup.js` una vez en mongosh (crea colecciones con validación).
3. `mvnw.cmd spring-boot:run` (Windows) o `./mvnw spring-boot:run` (Mac/Linux).
4. Abre **http://localhost:8080** e ingresa con el superadministrador del `.env`.
   La API se puede probar también en `http://localhost:8080/swagger-ui.html`.

El frontend vive en `src/main/resources/static` y lo sirve el mismo Spring Boot: un solo comando levanta todo.
Con `DATOS_DEMO=true`, el primer arranque crea productos, donantes, fundaciones, donaciones y entregas de ejemplo.

## Roles (herencia de la clase `Rol`)

```
            Rol (abstracta)
           /    \
     Donante    Admin
                  |
              SuperAdmin
```

| Rol | Puede |
|---|---|
| SUPERADMIN | Todo lo de ADMIN + crear/editar/activar administradores y cambiar roles |
| ADMIN | Donantes, productos, donaciones, inventario FEFO, fundaciones, entregas, reportes, auditoría |
| DONANTE | Registrar y ver sus donaciones, seguimiento, certificado PDF, su impacto |

Todos los roles ven el **seguimiento** de las donaciones (el donante solo las suyas).

## Estructura

```
com.almora
├── config       configuración, propiedades, FEFO, datos iniciales, medición de tiempos
├── controller   endpoints REST (/api/...)
├── dto          lo que entra y sale de la API (nunca expone contraseñas)
├── exception    errores en JSON uniforme
├── fefo         algoritmo FEFO y semáforo (Java puro, con pruebas)
├── model        documentos de MongoDB (usuario, inventario, donacion, entrega, auditoria)
├── repository   acceso a MongoDB
├── scheduler    vencimientos diarios y respaldo semanal
├── security     JWT, filtro, configuración de seguridad
├── service      reglas de negocio
└── util         códigos y textos
```

## Pruebas

`./mvnw test` ejecuta las pruebas del algoritmo FEFO, del semáforo y de la jerarquía de roles.
