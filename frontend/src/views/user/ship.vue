<template>
  <div class="ship-container animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">我的船舶</h1>
        <p class="page-subtitle">管理您的船舶信息，维护船舶档案与状态</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索船名或MMSI"
          class="search-input"
          clearable
          @clear="loadShips"
          @keyup.enter="loadShips"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" class="add-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon>新增船舶
        </el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="ship in filteredShips" :key="ship.id" class="ship-col">
        <div class="ship-card">
          <div class="ship-card-header">
            <div class="ship-type-icon" :style="{'--icon-color': getTypeColor(ship.shipType)}">
              <el-icon :size="28" color="#fff"><Ship /></el-icon>
            </div>
            <div class="ship-card-title">
              <h3 class="ship-name">{{ ship.shipName }}</h3>
              <span class="ship-type-label">{{ getShipTypeText(ship.shipType) }}</span>
            </div>
            <el-tag size="small" :type="getStatusTagType(ship.status)" effect="dark" class="status-tag">
              {{ ship.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
          <div class="ship-card-divider"></div>
          <div class="ship-info">
            <div class="info-row">
              <span class="info-label">MMSI</span>
              <span class="info-value">{{ ship.mmsi || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">IMO编号</span>
              <span class="info-value">{{ ship.imo || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">船籍</span>
              <span class="info-value">{{ ship.nationality || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">总吨位</span>
              <span class="info-value">{{ ship.tonnage ? ship.tonnage + ' GT' : '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">电气参数</span>
              <span class="info-value">{{ ship.ratedVoltage ? ship.ratedVoltage + 'V' : '-' }} / {{ ship.ratedPower ? ship.ratedPower + 'kW' : '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">尺度</span>
              <span class="info-value">
                {{ ship.length ? ship.length + 'm' : '-' }} × {{ ship.width ? ship.width + 'm' : '-' }}
                <span v-if="ship.draft"> / 吃水{{ ship.draft }}m</span>
              </span>
            </div>
            <div class="info-row" v-if="getCompatiblePileLabels(ship.shipType)">
              <span class="info-label">适用桩</span>
              <span class="info-value" style="color: #00b894;">{{ getCompatiblePileLabels(ship.shipType) }}</span>
            </div>
            <div class="info-row" v-if="ship.remark">
              <span class="info-label">备注</span>
              <span class="info-value">{{ ship.remark }}</span>
            </div>
          </div>
          <div class="ship-card-actions">
            <el-button size="small" @click="openEditDialog(ship)">编辑</el-button>
            <el-button
              size="small"
              :type="ship.status === 1 ? 'warning' : 'success'"
              @click="handleToggle(ship)"
            >
              {{ ship.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(ship.id)">删除</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="filteredShips.length === 0" :description="ships.length === 0 ? '暂无船舶信息，点击右上角新增' : '未找到匹配的船舶'" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑船舶' : '新增船舶'" width="650px" append-to-body @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="船名" prop="shipName">
              <el-input v-model="form.shipName" placeholder="请输入船名" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="船舶类型" prop="shipType">
              <el-select v-model="form.shipType" placeholder="请选择船舶类型" style="width:100%;" @change="handleShipTypeChange">
                <el-option v-for="t in shipTypes" :key="t.value" :label="t.label" :value="t.value">
                  <span>{{ t.label }}</span>
                  <span style="float:right;color:#909399;font-size:12px;">
                    <span v-if="t.tonnage">{{ t.tonnage }}GT / {{ t.length }}m · </span>
                    → {{ getCompatiblePileLabels(t.value) }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="MMSI码">
              <el-input v-model="form.mmsi" placeholder="9位数字" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IMO编号">
              <el-input v-model="form.imo" placeholder="IMO开头的编号" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="船籍">
              <el-input v-model="form.nationality" placeholder="如：中国" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总吨位(GT)">
              <el-input-number v-model="form.tonnage" :min="0" :max="999999" :precision="2" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="船长(米)">
              <el-input-number v-model="form.length" :min="0" :max="999" :precision="2" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="船宽(米)">
              <el-input-number v-model="form.width" :min="0" :max="999" :precision="2" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="吃水(米)">
              <el-input-number v-model="form.draft" :min="0" :max="99" :precision="2" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="额定电压(V)">
              <el-input-number v-model="form.ratedVoltage" :min="0" :max="20000" :step="100" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="额定功率(kW)">
              <el-input-number v-model="form.ratedPower" :min="0" :max="5000" :step="50" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { shipApi, deviceApi } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Ship } from '@element-plus/icons-vue'

const ships = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const defaultForm = {
  id: null,
  shipName: '',
  shipType: '',
  mmsi: '',
  imo: '',
  nationality: '中国',
  tonnage: null,
  length: null,
  width: null,
  draft: null,
  ratedVoltage: null,
  ratedPower: null,
  remark: ''
}

const form = ref({ ...defaultForm })

const shipTypes = ref([])
const shipTypeLabels = ref({})
const shipTypeDefaults = ref({})
const shipToPileMap = ref({})
const pileTypeLabels = ref({})

const rules = {
  shipName: [{ required: true, message: '请输入船名', trigger: 'blur' }],
  shipType: [{ required: true, message: '请选择船舶类型', trigger: 'change' }]
}

const filteredShips = computed(() => {
  if (!keyword.value) return ships.value
  const kw = keyword.value.toLowerCase()
  return ships.value.filter(s =>
    s.shipName.toLowerCase().includes(kw) ||
    (s.mmsi && s.mmsi.toLowerCase().includes(kw)) ||
    (s.imo && s.imo.toLowerCase().includes(kw))
  )
})

const getShipTypeText = (type) => {
  return shipTypeLabels.value[type] || type || '未知'
}

const typeColors = {
  CARGO: '#409eff', CONTAINER: '#67c23a', TANKER: '#e6a23c',
  PASSENGER: '#f56c6c', BULK: '#909399', RO_RO: '#9b59b6',
  FISHING: '#1abc9c', OTHER: '#95a5a6', YACHT: '#00b894'
}
const getTypeColor = (type) => {
  return typeColors[type] || '#409eff'
}

const getStatusTagType = (status) => {
  return status === 1 ? 'success' : 'info'
}

async function loadTypes() {
  try {
    const res = await deviceApi.types()
    const data = res.data || {}
    shipTypeLabels.value = data.shipTypeLabels || {}
    shipTypeDefaults.value = data.shipTypeDefaults || {}
    shipToPileMap.value = data.shipToPileMap || {}
    const ptMap = {}
    ;(data.pileTypes || []).forEach(p => { ptMap[p.value] = p.label })
    pileTypeLabels.value = ptMap
    const labels = data.shipTypeLabels || {}
    const defaults = data.shipTypeDefaults || {}
    shipTypes.value = Object.keys(labels).map(key => ({
      value: key,
      label: labels[key],
      ...(defaults[key] || {})
    }))
  } catch {}
}

const getCompatiblePileLabels = (shipType) => {
  const pileList = shipToPileMap.value[shipType]
  if (!pileList) return ''
  return pileList.map(code => pileTypeLabels.value[code] || code).join('、')
}

const loadShips = async () => {
  try {
    const res = await shipApi.list()
    ships.value = res.data || []
  } catch {}
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

const handleShipTypeChange = (val) => {
  if (isEdit.value) return
  const t = shipTypes.value.find(s => s.value === val)
  if (t) {
    form.value.tonnage = t.tonnage ?? null
    form.value.length = t.length ?? null
    form.value.width = t.width ?? null
    form.value.draft = t.draft ?? null
    form.value.ratedVoltage = t.voltage ?? null
    form.value.ratedPower = t.power ?? null
  }
}

const openEditDialog = (ship) => {
  isEdit.value = true
  form.value = {
    id: ship.id,
    shipName: ship.shipName,
    shipType: ship.shipType,
    mmsi: ship.mmsi || '',
    imo: ship.imo || '',
    nationality: ship.nationality || '中国',
    tonnage: ship.tonnage,
    length: ship.length,
    width: ship.width,
    draft: ship.draft,
    ratedVoltage: ship.ratedVoltage,
    ratedPower: ship.ratedPower,
    remark: ship.remark || ''
  }
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (isEdit.value) {
      await shipApi.update(form.value)
      ElMessage.success('更新成功')
    } else {
      await shipApi.add(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadShips()
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该船舶信息吗？', '提示', { type: 'warning' })
    await shipApi.del(id)
    ElMessage.success('删除成功')
    loadShips()
  } catch {}
}

const handleToggle = async (ship) => {
  const action = ship.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}该船舶吗？`, '提示', { type: 'warning' })
    await shipApi.toggle(ship.id)
    ElMessage.success(`${action}成功`)
    loadShips()
  } catch {}
}

onMounted(async () => {
  await loadTypes()
  loadShips()
})
</script>

<style scoped>
.ship-container {
  padding: 0;
}

.toolbar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.add-btn {
  font-weight: 600;
}

.ship-col {
  margin-bottom: 20px;
}

.ship-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  box-shadow: var(--shadow-sm);
}

.ship-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary);
}

.ship-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
}

.ship-type-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--icon-color), var(--icon-color) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  opacity: 0.9;
}

.ship-type-icon::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(255,255,255,0.25) 0%, transparent 60%);
}

.ship-card-title {
  flex: 1;
  min-width: 0;
}

.ship-name {
  margin: 0 0 2px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ship-type-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.status-tag {
  flex-shrink: 0;
}

.ship-card-divider {
  height: 1px;
  background: var(--border-light);
  margin: 16px 0;
}

.ship-info {
  flex: 1;
}

.info-row {
  display: flex;
  padding: 5px 0;
  font-size: 13px;
  line-height: 1.5;
}

.info-label {
  color: var(--text-secondary);
  width: 70px;
  flex-shrink: 0;
  font-weight: 500;
}

.info-value {
  color: var(--text-primary);
  flex: 1;
  font-weight: 400;
}

.ship-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
</style>
