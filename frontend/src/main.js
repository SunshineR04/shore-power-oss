import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './style.css'

// 按需注册模板中实际使用的图标，避免全量注册导致的 bundle 膨胀
import {
  ArrowDown, ArrowRight, Bell, BellFilled, Calendar, Check, CircleCheck, CircleClose,
  Coin, Connection, Cpu, Expand, Fold, HomeFilled, Lightning, Location, Message,
  Money, Monitor, Moon, Odometer, Plus, Search, SetUp, Setting,
  Ship, Sunny, Timer, Tools, TrendCharts, User, Wallet, Warning, WarningFilled
} from '@element-plus/icons-vue'

const app = createApp(App)

const icons = {
  ArrowDown, ArrowRight, Bell, BellFilled, Calendar, Check, CircleCheck, CircleClose,
  Coin, Connection, Cpu, Expand, Fold, HomeFilled, Lightning, Location, Message,
  Money, Monitor, Moon, Odometer, Plus, Search, SetUp, Setting,
  Ship, Sunny, Timer, Tools, TrendCharts, User, Wallet, Warning, WarningFilled
}
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn })
app.use(createPinia())
app.use(router)
app.mount('#app')
