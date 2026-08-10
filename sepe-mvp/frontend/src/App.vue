<template>
  <div class="container">
    <header>
      <h1>📅 SEPE Cita Previa - Estado</h1>
      <p class="subtitle">Monitor de disponibilidad por provincia</p>
      <button @click="refreshStatus" :disabled="loading" class="refresh-btn">
        {{ loading ? 'Cargando...' : '🔄 Actualizar' }}
      </button>
    </header>

    <div v-if="lastUpdate" class="last-update">
      Última actualización: {{ new Date(lastUpdate).toLocaleTimeString() }}
    </div>

    <div class="grid">
      <div 
        v-for="province in provinces" 
        :key="province.code" 
        class="card"
        :class="province.status.toLowerCase()"
      >
        <div class="status-indicator"></div>
        <h3>{{ province.name }}</h3>
        <span class="status-badge">{{ getStatusText(province.status) }}</span>
        <div class="response-time" v-if="province.responseTimeMs > 0">
          ⏱ {{ province.responseTimeMs }}ms
        </div>
        <a 
          :href="province.bookingUrl" 
          target="_blank" 
          rel="noopener noreferrer"
          class="booking-link"
        >
          Ir a Cita Previa →
        </a>
      </div>
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      provinces: [],
      loading: false,
      error: null,
      lastUpdate: null
    }
  },
  mounted() {
    this.refreshStatus()
  },
  methods: {
    async refreshStatus() {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/status')
        if (!response.ok) throw new Error('Error al cargar datos')
        
        this.provinces = await response.json()
        this.lastUpdate = Date.now()
      } catch (err) {
        this.error = 'No se pudo conectar con el servidor. Asegúrate de que el backend está ejecutándose.'
        console.error(err)
      } finally {
        this.loading = false
      }
    },
    getStatusText(status) {
      const texts = {
        'AVAILABLE': '✅ Disponible',
        'BUSY': '❌ Saturado',
        'UNKNOWN': '⚠️ Desconocido'
      }
      return texts[status] || status
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  background: #f5f5f5;
  color: #333;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

header {
  text-align: center;
  margin-bottom: 30px;
}

h1 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.subtitle {
  color: #666;
  margin-bottom: 20px;
}

.refresh-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 12px 24px;
  font-size: 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: #0056b3;
}

.refresh-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.last-update {
  text-align: center;
  color: #666;
  margin-bottom: 20px;
  font-size: 0.9rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: relative;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
}

.card.available::before {
  background: #28a745;
}

.card.busy::before {
  background: #dc3545;
}

.card.unknown::before {
  background: #ffc107;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-bottom: 12px;
}

.available .status-indicator {
  background: #28a745;
}

.busy .status-indicator {
  background: #dc3545;
}

.unknown .status-indicator {
  background: #ffc107;
}

.card h3 {
  font-size: 1.1rem;
  margin-bottom: 8px;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
  margin-bottom: 8px;
}

.available .status-badge {
  background: #d4edda;
  color: #155724;
}

.busy .status-badge {
  background: #f8d7da;
  color: #721c24;
}

.unknown .status-badge {
  background: #fff3cd;
  color: #856404;
}

.response-time {
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 12px;
}

.booking-link {
  display: inline-block;
  color: #007bff;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
}

.booking-link:hover {
  text-decoration: underline;
}

.error {
  background: #f8d7da;
  color: #721c24;
  padding: 16px;
  border-radius: 6px;
  margin-top: 20px;
  text-align: center;
}
</style>
