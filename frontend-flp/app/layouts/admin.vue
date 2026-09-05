<template>
  <div class="min-h-screen bg-cover bg-center bg-fixed relative" style="background-image: url('/assets/images/fondoAdmin.jpeg'); background-color: #FFFCF6;">
    <header class="relative">
      <div class="bg-transparent">
        <nav class="w-full grid grid-cols-[1fr_auto_1fr] items-center px-2 sm:px-4 py-3 sm:py-6">
          <div class="flex flex-wrap gap-x-2 sm:gap-x-6 gap-y-1 min-w-0">
            <NuxtLink
              to="/admin/pedidos"
              class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide whitespace-nowrap"
              active-class="font-bold"
            >
              Pedidos
            </NuxtLink>
            <NuxtLink
              to="/admin/inventario"
              class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide whitespace-nowrap"
              active-class="font-bold"
            >
              Inventario
            </NuxtLink>
            <NuxtLink
              to="/admin/catalogo"
              class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide whitespace-nowrap"
              active-class="font-bold"
            >
              Catálogo
            </NuxtLink>
            <NuxtLink
              to="/admin/eventos"
              class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide whitespace-nowrap"
              active-class="font-bold"
            >
              Eventos
            </NuxtLink>
            <NuxtLink
              to="/admin/personalizados"
              class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide whitespace-nowrap"
              active-class="font-bold"
            >
              Personalizados
            </NuxtLink>
          </div>

          <NuxtLink to="/admin/pedidos">
            <img src="/assets/images/flplogowhite.png" alt="Flores Eternas" class="h-12 sm:h-20 md:h-36 w-auto justify-self-center" width="1536" height="1024" />
          </NuxtLink>

          <div class="flex items-center gap-2 sm:gap-6 justify-self-end relative">
            <span class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide hidden md:inline">Empleados</span>
            <span class="text-white text-[13px] sm:text-[15px] md:text-[17px] tracking-wide hidden md:inline">Clientes</span>
            <span class="text-white text-[11px] sm:text-[13px] md:text-[17px] tracking-wide hidden md:inline">Cierre de caja</span>
            
            <!-- Botón perfil de administradora -->
            <button
              type="button"
              class="text-white text-xl sm:text-2xl hover:text-[#FCE8EB] transition cursor-pointer p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-white/40"
              @click="menuPerfilAbierto = !menuPerfilAbierto"
              aria-label="Menú de perfil de administración"
            >
              <Icon icon="mdi:account-circle-outline" />
            </button>

            <!-- Menú flotante desplegable -->
            <Transition name="fade">
              <div
                v-if="menuPerfilAbierto"
                class="absolute right-0 top-full mt-3 z-50 w-72 sm:w-80 bg-white rounded-3xl p-5 shadow-2xl border border-stone-100/80"
              >
                <!-- Botón 'X' para cerrar menú -->
                <div class="flex justify-end mb-1">
                  <button
                    type="button"
                    class="w-7 h-7 rounded-full bg-stone-200/80 hover:bg-stone-300 text-stone-600 flex items-center justify-center transition cursor-pointer"
                    @click="menuPerfilAbierto = false"
                    aria-label="Cerrar menú"
                  >
                    <Icon icon="mdi:close" class="text-base" />
                  </button>
                </div>

                <!-- Cabecera con avatar y nombre -->
                <div class="flex items-center gap-3.5 mb-5 px-1">
                  <div class="w-14 h-14 rounded-full bg-[#E5E5E5] flex items-center justify-center text-stone-500 shrink-0">
                    <Icon icon="mdi:account-outline" class="text-3xl" />
                  </div>
                  <div class="min-w-0">
                    <h3 class="font-serif text-2xl text-[#6B3F24] leading-tight truncate font-normal capitalize">
                      {{ nombreAdministrador }}
                    </h3>
                    <p class="text-xs sm:text-sm text-stone-700 font-serif tracking-tight lowercase">
                      flores eternas
                    </p>
                  </div>
                </div>

                <!-- Lista de opciones -->
                <div class="space-y-3">
                  <button
                    type="button"
                    class="w-full py-2.5 px-4 rounded-2xl bg-[#FCE8EB] hover:bg-[#FACDD1] text-stone-800 flex items-center gap-3.5 transition shadow-xs cursor-pointer active:scale-[0.98]"
                    @click="abrirConfiguracion"
                  >
                    <Icon icon="mdi:cog-outline" class="text-2xl text-stone-700 shrink-0" />
                    <span class="text-base text-stone-800 font-normal">Configuración</span>
                  </button>

                  <button
                    type="button"
                    class="w-full py-2.5 px-4 rounded-2xl bg-[#FCE8EB] hover:bg-[#FACDD1] text-stone-800 flex items-center gap-3.5 transition shadow-xs cursor-pointer active:scale-[0.98]"
                    @click="cambiarTema"
                  >
                    <Icon icon="mdi:weather-night" class="text-2xl text-stone-700 shrink-0" />
                    <span class="text-base text-stone-800 font-normal">Tema</span>
                  </button>

                  <button
                    type="button"
                    class="w-full py-2.5 px-4 rounded-2xl bg-[#FCE8EB] hover:bg-[#FACDD1] text-stone-900 flex items-center gap-3.5 transition shadow-xs cursor-pointer active:scale-[0.98]"
                    @click="abrirConfirmacionLogout"
                  >
                    <Icon icon="mdi:logout" class="text-2xl text-stone-900 shrink-0" />
                    <span class="text-base font-bold text-stone-900">Cerrar sesión</span>
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </nav>

        <div class="text-center pb-12 sm:pb-16 md:pb-32 pt-6 sm:pt-8 md:pt-14">
          <h1
            class="text-white font-serif text-2xl sm:text-3xl md:text-4xl md:text-[43px] font-light"
            style="text-shadow: 0 2px 8px rgba(0,0,0,0.4);"
          >
            ¡Bienvenida de nuevo, {{ nombreAdministrador }}!
          </h1>
        </div>
      </div>
    </header>

    <main class="relative -mt-12 px-6 pb-6">
      <div class="max-w-6xl mx-auto">
        <slot />
      </div>
    </main>

    <!-- Overlay para cerrar el menú flotante al hacer clic afuera -->
    <div
      v-if="menuPerfilAbierto"
      class="fixed inset-0 z-30"
      @click="menuPerfilAbierto = false"
    />

    <!-- Ventana emergente (Modal) de confirmación de cierre de sesión -->
    <Transition name="fade">
      <div
        v-if="modalConfirmacionAbierto"
        class="fixed inset-0 bg-black/45 backdrop-blur-[2px] z-[60] flex items-center justify-center p-4"
        @click.self="modalConfirmacionAbierto = false"
      >
        <div class="relative bg-white rounded-3xl p-6 sm:p-8 max-w-sm sm:max-w-md w-full shadow-2xl text-center border border-stone-100 animate-in fade-in zoom-in-95 duration-150">
          <!-- Botón 'X' para cerrar modal -->
          <button
            type="button"
            class="absolute top-4 right-4 w-7 h-7 rounded-full bg-stone-200/80 hover:bg-stone-300 text-stone-600 flex items-center justify-center transition cursor-pointer"
            @click="modalConfirmacionAbierto = false"
            aria-label="Cerrar ventana"
          >
            <Icon icon="mdi:close" class="text-base" />
          </button>

          <h2 class="font-serif text-3xl sm:text-4xl text-[#7B4425] font-normal mt-2">
            Cerrar sesión
          </h2>

          <p class="text-stone-800 text-base sm:text-lg mt-3 font-serif">
            ¿esta seguro que desea cerrar sesión?
          </p>

          <div class="flex items-center justify-center gap-4 sm:gap-6 mt-6">
            <button
              type="button"
              class="px-8 py-2 rounded-2xl bg-[#FCE8EB] hover:bg-[#FACDD1] text-stone-800 font-serif text-base sm:text-lg lowercase transition shadow-xs hover:shadow active:scale-95 cursor-pointer"
              @click="confirmarLogout"
            >
              confirmar
            </button>

            <button
              type="button"
              class="px-8 py-2 rounded-2xl bg-[#FCE8EB] hover:bg-[#FACDD1] text-stone-800 font-serif text-base sm:text-lg lowercase transition shadow-xs hover:shadow active:scale-95 cursor-pointer"
              @click="modalConfirmacionAbierto = false"
            >
              cancelar
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
  <Toast />
</template>

<script setup lang="ts">
/**
 * @author esteban
 * Layout principal del panel de administración.
 * Contiene la barra de navegación superior, el menú flotante de perfil de administración,
 * la modal de confirmación de cierre de sesión y las alertas Toast.
 */
import { ref, computed } from 'vue'
import { useAuthStore } from '~/stores/auth.store'
import { useToast } from '~/composables/useToast'
import Toast from '~/components/shared/Toast.vue'

const auth = useAuthStore()
const toast = useToast()

const menuPerfilAbierto = ref(false)
const modalConfirmacionAbierto = ref(false)

/**
 * Nombre de la administradora obtenido reactivamente desde la sesión autenticada.
 */
const nombreAdministrador = computed(() => {
  return auth.nombre?.trim() || 'Luisa'
})

/**
 * Cierra el menú desplegable y abre la modal de confirmación.
 */
function abrirConfirmacionLogout() {
  menuPerfilAbierto.value = false
  modalConfirmacionAbierto.value = true
}

/**
 * Notificación temporal para la opción de configuración.
 */
function abrirConfiguracion() {
  menuPerfilAbierto.value = false
  toast.warning('Módulo de configuración en desarrollo')
}

/**
 * Notificación temporal para el cambio de tema.
 */
function cambiarTema() {
  menuPerfilAbierto.value = false
  toast.warning('Selector de tema en desarrollo')
}

/**
 * Confirma el cierre de sesión: limpia el token y datos en Pinia y localStorage,
 * notifica al usuario y redirige a la pantalla de login.
 */
function confirmarLogout() {
  modalConfirmacionAbierto.value = false
  auth.logout()
  toast.success('Sesión cerrada correctamente')
  navigateTo('/admin/login')
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.97);
}
</style>
