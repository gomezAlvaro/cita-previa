# SEPE Cita Previa - MVP

Aplicación web sencilla que, a partir de tu código postal, te dice a qué provincia corresponde,
qué provincias cercanas puedes probar y si el portal de cita previa del SEPE está respondiendo ahora mismo.

> **Importante:** el SEPE atiende la cita previa desde un único portal. Esta versión solo comprueba
> si ese portal responde (operativo, lento, bloqueado, con errores o inaccesible). **No sabe si hay
> citas libres.** Detectar huecos reales está pendiente (ver `../PLAN_REVIEW.md`, sección 4).

## Características

- **Búsqueda por código postal**: identifica tu provincia y sugiere provincias vecinas.
- **Estado del portal**: `OK`, `SLOW`, `BLOCKED`, `DOWN` o `UNREACHABLE`.
- **Sin automatización de reservas**: te lleva a la web oficial del SEPE para pedir la cita.
- **Sin datos personales**: no se pide ni se guarda DNI/NIE.
- **Consulta limitada**: el portal del SEPE se consulta como máximo una vez por minuto, sin importar cuántas
  peticiones reciba la API.

## Stack tecnológico

- **Backend**: Java 17 + Spring Boot 3
- **Frontend**: Vue.js 3 + Vite

## Requisitos

- Java 17+
- Maven 3.9+ (`mvn`)
- Node.js 18+

## Ejecución

### Backend (puerto 8081)
```bash
cd backend
mvn spring-boot:run
```

### Frontend (puerto 5173)
```bash
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173. El frontend redirige `/api` al backend en el puerto 8081.

## Tests

```bash
cd backend
mvn test
```

## Endpoints API

- `GET /api/status` - Estado del portal para las 52 provincias (el mismo valor para todas)
- `POST /api/find-appointments` - Cuerpo: `{"postalCode": "28001"}`. Devuelve tu provincia y las cercanas.
  Responde `400` si el código postal no es válido.

## Cómo funciona

1. El usuario introduce su código postal.
2. El backend obtiene la provincia a partir de los dos primeros dígitos.
3. Comprueba (con caché de 60 s) si el portal del SEPE responde.
4. Devuelve su provincia y las provincias vecinas, con el estado del portal y un enlace a la web del SEPE.

## Limitaciones del MVP

- No confirma si hay citas libres.
- No automatiza la reserva.
- No guarda historial ni requiere registro.
- Las provincias cercanas son una lista fija de provincias vecinas, sin distancias reales.
