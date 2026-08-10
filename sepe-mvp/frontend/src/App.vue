<template>
  <div class="container">
    <header>
      <h1>📅 SEPE Cita Previa - Buscador</h1>
      <p class="subtitle">Encuentra citas disponibles cerca de tu código postal</p>
    </header>

    <div class="search-form">
      <div class="form-group">
        <label for="dni">DNI/NIE:</label>
        <input 
          type="text" 
          id="dni" 
          v-model="form.dni" 
          placeholder="12345678A"
          maxlength="9"
        />
      </div>
      
      <div class="form-group">
        <label for="postalCode">Código Postal:</label>
        <input 
          type="text" 
          id="postalCode" 
          v-model="form.postalCode" 
          placeholder="28001"
          maxlength="5"
          pattern="[0-9]{5}"
        />
      </div>
      
      <div class="form-group">
        <label for="appointmentType">Tipo de Cita:</label>
        <select id="appointmentType" v-model="form.appointmentType">
          <option value="PRESENCIAL">Presencial</option>
          <option value="TELEFONICA">Telefónica</option>
        </select>
      </div>
      
      <button @click="searchAppointments" :disabled="loading" class="search-btn">
        {{ loading ? 'Buscando...' : '🔍 Buscar Citas' }}
      </button>
    </div>

    <div v-if="loading" class="loading">
      ⏳ Buscando disponibilidad en oficinas cercanas...
    </div>

    <div v-if="error" class="error">
      {{ error }}
    </div>

    <div v-if="results.length > 0" class="results">
      <h2>📍 Oficinas con Disponibilidad</h2>
      <div class="results-grid">
        <div 
          v-for="(office, index) in results" 
          :key="index" 
          class="result-card"
          :class="{ available: office.hasAppointments }"
        >
          <div class="status-dot" :class="office.hasAppointments ? 'green' : 'red'"></div>
          <h3>{{ office.officeName }}</h3>
          <p class="province">{{ office.province }}</p>
          <p class="address" v-if="office.address">{{ office.address }}</p>
          <p class="distance" v-if="office.distance">📏 {{ office.distance }}</p>
          <a 
            :href="office.bookingUrl" 
            target="_blank" 
            rel="noopener noreferrer"
            class="booking-btn"
          >
            Reservar Cita →
          </a>
        </div>
      </div>
    </div>

    <div v-if="!loading && results.length === 0 && searched" class="no-results">
      😞 No se encontraron oficinas con disponibilidad cercana.
      <p class="tip">Prueba con un código postal diferente o revisa más tarde.</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      form: {
        dni: '',
        postalCode: '',
        appointmentType: 'PRESENCIAL'
      },
      results: [],
      loading: false,
      error: null,
      searched: false
    }
  },
  methods: {
    async searchAppointments() {
      if (!this.form.postalCode || this.form.postalCode.length !== 5) {
        this.error = 'Por favor, introduce un código postal válido (5 dígitos)';
        return;
      }

      this.loading = true;
      this.error = null;
      this.searched = false;
      this.results = [];
      
      try {
        const response = await fetch('/api/find-appointments', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.form)
        });
        
        if (!response.ok) throw new Error('Error al buscar citas');
        
        this.results = await response.json();
        this.searched = true;
      } catch (err) {
        this.error = 'No se pudo conectar con el servidor.';
        console.error(err);
      } finally {
        this.loading = false;
      }
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
  max-width: 800px;
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
}

.search-form {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #444;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0,123,255,0.25);
}

.search-btn {
  width: 100%;
  background: #007bff;
  color: white;
  border: none;
  padding: 12px 24px;
  font-size: 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  font-weight: 500;
}

.search-btn:hover:not(:disabled) {
  background: #0056b3;
}

.search-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error {
  background: #f8d7da;
  color: #721c24;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  text-align: center;
}

.results {
  margin-top: 20px;
}

.results h2 {
  margin-bottom: 16px;
  font-size: 1.5rem;
}

.results-grid {
  display: grid;
  gap: 16px;
}

.result-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: relative;
  padding-left: 50px;
}

.result-card.available {
  border-left: 4px solid #28a745;
}

.status-dot {
  position: absolute;
  left: 16px;
  top: 20px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.status-dot.green {
  background: #28a745;
}

.status-dot.red {
  background: #dc3545;
}

.result-card h3 {
  font-size: 1.1rem;
  margin-bottom: 4px;
}

.result-card .province {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 8px;
}

.result-card .address {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 4px;
}

.result-card .distance {
  font-size: 0.85rem;
  color: #007bff;
  font-weight: 500;
  margin-bottom: 12px;
}

.booking-btn {
  display: inline-block;
  background: #007bff;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: background 0.2s;
}

.booking-btn:hover {
  background: #0056b3;
}

.no-results {
  text-align: center;
  padding: 40px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.no-results .tip {
  margin-top: 12px;
  color: #666;
  font-size: 0.9rem;
}
</style>
