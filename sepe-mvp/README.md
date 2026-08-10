# SEPE Cita Previa - MVP

Aplicación web simple para encontrar citas disponibles del SEPE cerca de tu código postal.

## Características

- **Búsqueda por Código Postal**: Introduce tu código postal y encuentra oficinas con disponibilidad
- **Recomendaciones Cercanas**: El sistema sugiere provincias cercanas con servidores disponibles
- **Sin Automatización**: Solo monitoriza el estado de los servidores provinciales del SEPE
- **Enlace Directo**: Te lleva directamente a la web oficial del SEPE para completar la cita

## Stack Tecnológico

- **Backend**: Java 17 + Spring Boot 3
- **Frontend**: Vue.js 3 + Vite
- **Cache**: Caffeine (60 segundos)

## Ejecución

### Backend (puerto 8081)
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (puerto 5173)
```bash
cd frontend
npm run dev
```

## Endpoints API

- `GET /api/status` - Estado de todas las provincias
- `POST /api/find-appointments` - Buscar citas por código postal

## Cómo Funciona

1. El usuario introduce DNI, código postal y tipo de cita
2. El backend identifica la provincia correspondiente
3. Verifica el estado del servidor SEPE de esa provincia
4. Busca provincias cercanas con servidores disponibles
5. Devuelve lista de oficinas recomendadas con enlaces directos

## Limitaciones MVP

- No automatiza la reserva (solo indica dónde hay disponibilidad)
- No guarda historial ni requiere registro
- Usa heurística simple de proximidad por códigos de provincia
