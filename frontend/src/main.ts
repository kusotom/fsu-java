import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import './styles/theme.css'
import './styles/status.css'
import './styles/layout.css'
import App from './App.vue'
import router from './router'
import { setupRouteGuard } from './auth/routeGuard'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)
setupRouteGuard(router)
app.mount('#app')
