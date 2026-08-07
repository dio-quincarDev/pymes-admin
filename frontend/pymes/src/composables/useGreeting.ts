import { computed } from 'vue'

export function useGreeting() {
  const greeting = computed(() => {
    const h = new Date().getHours()
    if (h < 12) return 'Buenos días'
    if (h < 19) return 'Buenas tardes'
    return 'Buenas noches'
  })

  return { greeting }
}
