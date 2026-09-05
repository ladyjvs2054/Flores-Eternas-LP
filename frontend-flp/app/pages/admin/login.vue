<!--
  @author esteban
  Página de inicio de sesión del administrador.
  Formulario para que el administrador acceda al panel de gestión.
  Protegida por middleware: si ya hay sesión, redirige a /admin/dashboard.
-->
<template>
  <div class="min-h-screen" style="background-color: #FFFCF6;">
    <header
      class="relative bg-cover bg-center h-95"
      style="background-image: url('/assets/images/FondoPruebaLogin.jpeg'); background-position: center 35%;"
    >
      <div class="bg-black/30 h-full">
        <nav class="max-w-6xl mx-auto flex items-center justify-between px-4 py-6 h-full">
          <div class="flex-1" />
          <div class="flex items-center gap-6">


          </div>
        </nav>
      </div>
    </header>

    <div class="flex justify-center -mt-90 relative z-10">
      <img
        :src="logoUrl"
        alt="Flores Eternas"
        class="h-80 object-contain"
      />
    </div>

    <main class="relative -mt-10 px-4 pb-12">
      <div class="max-w-sm mx-auto">
        <UiCard :elevated="true" :padding="8">
          <div class="max-w-xs mx-auto flex flex-col justify-center py-8">
            <form @submit.prevent="handleLogin" class="space-y-8">
              <UiInput
                v-model="form.correo"
                type="email"
                placeholder="Correo electrónico"
                :error="errores.correo"
              />

              <UiInput
                v-model="form.contrasena"
                type="password"
                placeholder="Contraseña"
                :error="errores.contrasena"
              />

              <div
                v-if="store.error"
                class="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600 font-['Poppins']"
              >
                {{ store.error }}
              </div>

              <div class="flex justify-center pt-4">
                <UiButton
                  type="submit"
                  variant="primary"
                  size="lg"
                  :loading="store.loading"
                  :disabled="!formValido || store.loading"
                  class="w-full"
                >
                  {{ store.loading ? 'Iniciando sesión...' : 'Iniciar Sesión' }}
                </UiButton>
              </div>
            </form>

            <div class="flex justify-center pt-4">
              <NuxtLink
                to="/admin/recuperar-contrasena"
                data-testid="link-olvide-contrasena"
                class="select-text no-underline text-sm font-medium text-[#83572E] hover:text-[#5C3A21] hover:underline transition-colors font-['Poppins'] cursor-pointer"
              >
                He olvidado mi contraseña
              </NuxtLink>
            </div>
          </div>
        </UiCard>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
/**
 * @author esteban
 * Página de login del administrador.
 * Utiliza el store de autenticación para validar credenciales
 * y redirigir al dashboard en caso de éxito.
 */

import { reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '~/stores/auth.store'

definePageMeta({ layout: false })

const store = useAuthStore()
const router = useRouter()

/**
 * @author esteban
 * URL del logo que funciona tanto en local como en producción.
 * Usa new URL() para resolución correcta en ambos entornos.
 */
const logoUrl = computed(() => {
  if (typeof window !== 'undefined') {
    return new URL('/assets/images/flplogowhite.png', window.location.origin).href
  }
  return '/assets/images/flplogowhite.png'
})

/**
 * @author esteban
 * Datos del formulario de login.
 */
const form = reactive({
  correo: '',
  contrasena: '',
})

/**
 * @author esteban
 * Errores de validación de campos.
 */
const errores = reactive({
  correo: '',
  contrasena: '',
})

/**
 * @author esteban
 * Valida el formulario antes de enviar.
 * @returns true si el formulario es válido, false en caso contrario.
 */
function validarFormulario(): boolean {
  errores.correo = ''
  errores.contrasena = ''

  let valido = true

  if (!form.correo.trim()) {
    errores.correo = 'El correo es obligatorio'
    valido = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.correo)) {
    errores.correo = 'Formato de correo inválido'
    valido = false
  }

  if (!form.contrasena) {
    errores.contrasena = 'La contraseña es obligatoria'
    valido = false
  }

  return valido
}

/**
 * @author esteban
 * Computed que verifica si el formulario tiene datos mínimos para habilitar el botón.
 */
const formValido = computed(() => {
  return form.correo.trim().length > 0 && form.contrasena.length > 0
})

/**
 * @author esteban
 * Maneja el envío del formulario de login.
 * Valida los campos y llama al store para autenticarse.
 */
async function handleLogin() {
  if (!validarFormulario()) return

  const success = await store.login({
    correo: form.correo,
    contrasena: form.contrasena,
  })

  if (success) {
    router.push('/admin/pedidos')
  }
}
</script>