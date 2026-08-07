<template>
  <div class="profile-page animate-fade-in-up">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理您的个人资料与账户安全设置</p>
      </div>
      <div class="header-decoration"></div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <div class="avatar-card">
          <div class="avatar-card-inner">
            <div class="avatar-gradient-bg"></div>
            <div class="avatar-section">
              <el-avatar :size="96" :src="avatarUrl" class="user-avatar">
                {{ userInfo.realName?.[0] || 'U' }}
              </el-avatar>
              <div class="avatar-name">{{ userInfo.realName || userInfo.username }}</div>
              <el-tag :type="roleMeta(userInfo.role).type" effect="plain" class="avatar-role-tag">
                {{ roleMeta(userInfo.role).label }}
              </el-tag>
            </div>
            <div class="avatar-actions">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :show-file-list="false"
                :before-upload="beforeUpload"
                :on-success="onAvatarSuccess"
                accept="image/*"
              >
                <el-button type="primary" size="small">更换头像</el-button>
              </el-upload>
              <div class="avatar-tip">支持 jpg、png 格式，大小不超过 2MB</div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="16">
        <div class="form-card animate-fade-in-up stagger-1">
          <div class="form-card-header">
            <div class="form-card-icon form-card-icon--info">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <span class="form-card-title">基本信息</span>
          </div>
          <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="80px" class="profile-form">
            <el-form-item label="用户名">
              <el-input :model-value="userInfo.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="roleMeta(userInfo.role).type" size="small">
                {{ roleMeta(userInfo.role).label }}
              </el-tag>
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="form-card animate-fade-in-up stagger-2">
          <div class="form-card-header">
            <div class="form-card-icon form-card-icon--warning">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </div>
            <span class="form-card-title">修改密码</span>
          </div>
          <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="80px" class="profile-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api'
import { useUserStore } from '../../store/user'
import { roleMeta } from '../../utils/status'

const store = useUserStore()
const userInfo = computed(() => store.userInfo)

const profileRef = ref()
const pwdRef = ref()
const saving = ref(false)
const changingPwd = ref(false)

const profileForm = reactive({ realName: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const avatarUrl = computed(() => {
  const avatar = store.userInfo.avatar
  return avatar ? avatar : ''
})

const uploadAction = ''
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${store.token}` }))

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  try {
    const res = await userApi.info()
    const data = res.data || {}
    profileForm.realName = data.realName || ''
    profileForm.phone = data.phone || ''
    profileForm.email = data.email || ''
    store.userInfo.realName = data.realName
    store.userInfo.avatar = data.avatar
  } catch {}
})

function beforeUpload(file) {
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过 2MB')
    return false
  }
  // 校验真实 MIME 类型（仅允许常见图片格式），防止伪装文件上传
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WebP 图片')
    return false
  }
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      await userApi.updateProfile({ avatar: e.target.result })
      store.userInfo.avatar = e.target.result
      sessionStorage.setItem('userInfo', JSON.stringify(store.userInfo))
      ElMessage.success('头像更新成功')
    } catch { ElMessage.error('头像更新失败') }
  }
  reader.readAsDataURL(file)
  return false
}

function onAvatarSuccess() {}

async function handleSaveProfile() {
  await profileRef.value.validate()
  saving.value = true
  try {
    const res = await userApi.updateProfile(profileForm)
    const updated = res.data || {}
    store.userInfo.realName = updated.realName || profileForm.realName
    store.userInfo.phone = updated.phone || profileForm.phone
    store.userInfo.email = updated.email || profileForm.email
    sessionStorage.setItem('userInfo', JSON.stringify(store.userInfo))
    ElMessage.success('个人信息更新成功')
  } catch {
    ElMessage.error('更新失败')
  } finally { saving.value = false }
}

async function handleChangePassword() {
  await pwdRef.value.validate()
  changingPwd.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '密码修改失败')
  } finally { changingPwd.value = false }
}
</script>

<style scoped>
.profile-page {
  padding: 0;
}

.page-header {
  position: relative;
  margin-bottom: 20px;
  padding: 28px 32px 24px;
  background: linear-gradient(135deg, var(--bg-card) 0%, var(--bg-elevated) 50%, var(--bg-card) 100%);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.header-content {
  position: relative;
  z-index: 1;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-subtitle {
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  font-weight: 400;
}

.header-decoration {
  position: absolute;
  top: -30px;
  right: -20px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.2) 0%, transparent 70%);
  pointer-events: none;
}

.header-decoration::after {
  content: '';
  position: absolute;
  bottom: -60px;
  right: 80px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.15) 0%, transparent 70%);
}

.avatar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition: box-shadow var(--transition-normal);
}

.avatar-card:hover {
  box-shadow: var(--shadow-md);
}

.avatar-card-inner {
  display: flex;
  flex-direction: column;
}

.avatar-gradient-bg {
  height: 100px;
  background: linear-gradient(135deg, var(--bg-card) 0%, var(--bg-elevated) 50%, var(--bg-card) 100%);
  position: relative;
}

.avatar-gradient-bg::after {
  content: '';
  position: absolute;
  top: -20px;
  right: -10px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.2) 0%, transparent 70%);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: -48px;
  position: relative;
  z-index: 1;
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%) !important;
  font-size: 36px !important;
  font-weight: 700;
  font-family: var(--font-display);
  border: 4px solid var(--bg-card);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-name {
  margin-top: 12px;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.avatar-role-tag {
  margin-top: 6px;
  font-size: 12px !important;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 20px 24px;
  gap: 8px;
}

.avatar-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
  overflow: hidden;
  transition: box-shadow var(--transition-normal);
}

.form-card:hover {
  box-shadow: var(--shadow-md);
}

.form-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-hover);
}

.form-card-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.form-card-icon--info {
  background: rgba(37, 99, 235, 0.1);
  color: var(--primary);
}

.form-card-icon--warning {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning);
}

.form-card-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.profile-form {
  padding: 20px;
  max-width: 450px;
}
</style>
