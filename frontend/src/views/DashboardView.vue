<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { currentUser, isAuthenticated } from '@/stores/auth'
import { Edit, Search, Delete, User, Timer } from '@element-plus/icons-vue'
import {
  getInbox,
  getSent,
  searchInbox,
  getMailDetail,
  sendMail,
  markMailAsRead,
  trashMail,
} from '@/services/mail'

const user = computed(() => currentUser.value || {})
const tabs = [
  { label: '收件箱', key: 'inbox' },
  { label: '已发送', key: 'sent' },
]
const activeTab = ref('inbox')
const mails = ref([])
const selectedMail = ref(null)
const searchKeyword = ref('')
const loadingMails = ref(false)
const loadingDetail = ref(false)
const loadingSend = ref(false)
const composeDrawer = ref(false)
const socketRef = ref(null)

const composeForm = reactive({
  subject: '',
  content: '',
  to: '',
  cc: '',
})

const loadMails = async () => {
  loadingMails.value = true
  selectedMail.value = null
  try {
    if (activeTab.value === 'inbox') {
      mails.value = searchKeyword.value ? await searchInbox(searchKeyword.value) : await getInbox()
    } else {
      mails.value = await getSent()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载邮件失败')
  } finally {
    loadingMails.value = false
  }
}

const openMail = async (mail) => {
  loadingDetail.value = true
  try {
    const detail = await getMailDetail(mail.id)
    selectedMail.value = detail
    if (activeTab.value === 'inbox' && mail.isRead === 0) {
      await markMailAsRead(mail.id)
      mail.isRead = 1
    }
  } catch (error) {
    ElMessage.error(error.message || '加载邮件详情失败')
  } finally {
    loadingDetail.value = false
  }
}

const handleSearch = async () => {
  if (activeTab.value !== 'inbox') {
    searchKeyword.value = ''
    return
  }
  await loadMails()
}

const openCompose = () => {
  composeDrawer.value = true
}

const resetCompose = () => {
  composeForm.subject = ''
  composeForm.content = ''
  composeForm.to = ''
  composeForm.cc = ''
}

const handleSend = async () => {
  if (!composeForm.subject.trim()) {
    ElMessage.warning('请填写邮件主题')
    return
  }
  if (!composeForm.to.trim() && !composeForm.cc.trim()) {
    ElMessage.warning('请填写一个收件人')
    return
  }
  loadingSend.value = true
  try {
    const payload = {
      subject: composeForm.subject,
      content: composeForm.content,
      to: composeForm.to.split(',').map((item) => item.trim()).filter(Boolean),
      cc: composeForm.cc.split(',').map((item) => item.trim()).filter(Boolean),
    }
    await sendMail(payload)
    ElMessage.success('发送成功')
    composeDrawer.value = false
    resetCompose()
    if (activeTab.value === 'sent') {
      await loadMails()
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    loadingSend.value = false
  }
}

const handleTrash = async (mail) => {
  try {
    await trashMail(mail.id)
    ElMessage.success('已移入垃圾箱')
    await loadMails()
    if (selectedMail.value?.id === mail.id) {
      selectedMail.value = null
    }
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleSocketMessage = async (event) => {
  let payload = null
  try {
    payload = JSON.parse(event.data)
  } catch {
    return
  }
  if (payload?.type !== 'NEW_MAIL') {
    return
  }
  ElNotification({
    title: '新邮件提醒',
    message: payload.subject || '你收到一封新邮件',
    type: 'success',
  })
  if (activeTab.value === 'inbox') {
    await loadMails()
  }
}

const connectSocket = () => {
  if (!isAuthenticated.value) {
    return
  }
  if (socketRef.value && socketRef.value.readyState <= 1) {
    return
  }
  const socket = createMailSocket()
  if (!socket) {
    return
  }
  socket.onmessage = handleSocketMessage
  socket.onclose = () => {
    socketRef.value = null
  }
  socket.onerror = () => {
    socketRef.value = null
  }
  socketRef.value = socket
}

const closeSocket = () => {
  if (socketRef.value) {
    socketRef.value.close()
    socketRef.value = null
  }
}

watch(activeTab, loadMails)
onMounted(loadMails)
</script>

<template>
  <div class="dashboard-container">
    <div class="dashboard-layout">
      
      <div class="dashboard-sidebar">
        <el-card shadow="always" class="user-profile-card">
          <div class="user-avatar-wrap">
            <el-avatar :size="64" :icon="User" class="custom-avatar" />
            <h3 class="username-display">{{ user.username || '未知用户' }}</h3>
            <span class="user-email-tag">{{ user.email || '-' }}</span>
          </div>
          <div class="user-meta-info">
            <div class="meta-item"><strong>用户账户 ID:</strong> {{ user.id || '-' }}</div>
          </div>
        </el-card>

        <div class="action-box">
          <el-button type="primary" size="large" @click="openCompose" :icon="Edit" class="write-btn">
            撰写新邮件
          </el-button>
        </div>
      </div>

      <div class="dashboard-content">
        <el-card shadow="always" class="main-content-card">
          <div class="mail-toolbar">
            <el-radio-group v-model="activeTab" size="large" class="custom-tabs">
              <el-radio-button label="inbox">📥 收件箱</el-radio-button>
              <el-radio-button label="sent">📤 已发送</el-radio-button>
            </el-radio-group>
            
            <div class="mail-search" v-if="activeTab === 'inbox'">
              <el-input
                v-model="searchKeyword"
                placeholder="按主题/发件人全局搜索..."
                clearable
                @clear="handleSearch"
                @keyup.enter="handleSearch"
                style="max-width: 360px;"
              >
                <template #append>
                  <el-button :icon="Search" @click="handleSearch" />
                </template>
              </el-input>
            </div>
          </div>

          <div class="mail-body-grid">
            <div class="mail-list-panel">
              <el-table
                :data="mails"
                v-loading="loadingMails"
                style="width: 100%"
                height="580"
                highlight-current-row
                @row-click="openMail"
                class="custom-mail-table"
              >
                <el-table-column label="状态" width="85" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" effect="light" round size="small">
                      {{ row.isRead === 1 ? '已读' : '未读' }}
                    </el-tag>
                  </template>
                </el-table-column>
                
                <el-table-column prop="senderName" label="发信人" width="130" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span :class="{ 'unread-bold': row.isRead !== 1 }">{{ row.senderName }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="subject" label="邮件主题" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span :class="{ 'unread-bold': row.isRead !== 1 }">{{ row.subject }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="sendTime" label="时间" width="160" show-overflow-tooltip />

                <el-table-column label="动作" width="85" align="center">
                  <template #default="{ row }">
                    <el-button size="small" type="danger" plain :icon="Delete" circle @click.stop="handleTrash(row)" />
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="mail-detail-panel">
              <el-card shadow="never" class="detail-inner-card" v-loading="loadingDetail">
                <div v-if="selectedMail" class="detail-view">
                  <div class="detail-header">
                    <h2 class="mail-subject-title">{{ selectedMail.subject }}</h2>
                    <div class="sender-info-block">
                      <el-avatar :size="36" :icon="User" style="background-color: #e4e7ed; color: #409eff" />
                      <div class="sender-meta">
                        <span class="sender-name"><strong>{{ selectedMail.senderName }}</strong></span>
                        <span class="sender-email">&lt;{{ selectedMail.senderEmail }}&gt;</span>
                      </div>
                    </div>
                    <div class="time-stamp-line">
                      <el-icon><Timer /></el-icon> <span>{{ selectedMail.sendTime }}</span>
                    </div>
                  </div>
                  
                  <el-divider style="margin: 16px 0;" />
                  
                  <div class="detail-recipients">
                    <span class="recipients-label">收件关系：</span>
                    <el-tag v-for="receiver in selectedMail.receivers" :key="receiver.userId" size="small" type="warning" effect="plain" class="mx-1">
                      {{ receiver.type === 'to' ? '收件' : '抄送' }}: {{ receiver.username || receiver.email }}
                    </el-tag>
                  </div>

                  <div class="mail-content-display">
                    {{ selectedMail.content }}
                  </div>

                  <div v-if="selectedMail.attachments?.length" class="attachments-section">
                    <div class="attach-title">📎 随信附件 ({{ selectedMail.attachments.length }})</div>
                    <div v-for="file in selectedMail.attachments" :key="file.id" class="file-item-badge">
                      <span class="file-name">{{ file.fileName }}</span>
                      <span class="file-size">({{ (file.fileSize / 1024).toFixed(1) }} KB)</span>
                    </div>
                  </div>
                </div>
                <div v-else class="detail-empty-placeholder">
                  <el-empty :image-size="120" description="点击左侧列表中的行，即可在此查阅详细信件正文" />
                </div>
              </el-card>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>

  <el-drawer
    title="新建邮件正文撰写"
    v-model="composeDrawer"
    direction="rtl"
    size="35%"
    :destroy-on-close="true"
  >
    <el-form label-position="top" :model="composeForm">
      <el-form-item label="🚀 收件人账号 (多个用英文逗号 , 隔开)">
        <el-input v-model="composeForm.to" placeholder="example@mail.com" />
      </el-form-item>
      <el-form-item label="👥 抄送人 (可选)">
        <el-input v-model="composeForm.cc" placeholder="cc@mail.com" />
      </el-form-item>
      <el-form-item label="📝 邮件核心主题">
        <el-input v-model="composeForm.subject" placeholder="请输入这封邮件的主题..." />
      </el-form-item>
      <el-form-item label="✉️ 邮件正文叙述">
        <el-input type="textarea" v-model="composeForm.content" :rows="12" placeholder="在此编辑您的邮件正文细节内容..." />
      </el-form-item>
      <div class="drawer-footer-actions">
        <el-button type="primary" size="large" :loading="loadingSend" @click="handleSend" style="width: 130px;">
          发送邮件
        </el-button>
        <el-button size="large" @click="composeDrawer = false">取消</el-button>
      </div>
    </el-form>
  </el-drawer>
</template>

<style scoped>
.dashboard-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 48px);
}

.dashboard-layout {
  display: flex;
  gap: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.dashboard-sidebar {
  width: 320px;
  flex-shrink: 0;
}

.user-profile-card {
  text-align: center;
  border-radius: 8px;
}

.user-avatar-wrap {
  padding: 12px 0;
}

.custom-avatar {
  background-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 159, 255, 0.3);
}

.username-display {
  margin: 12px 0 4px 0;
  color: #303133;
}

.user-email-tag {
  font-size: 13px;
  color: #909399;
}

.user-meta-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f2f6fc;
  text-align: left;
  font-size: 14px;
  color: #606266;
}

.action-box {
  margin: 20px 0;
}

.write-btn {
  width: 100%;
  height: 44px;
  font-weight: bold;
  letter-spacing: 1px;
  box-shadow: 0 4px 10px rgba(64, 159, 255, 0.2);
}

.dashboard-content {
  flex: 1;
  min-width: 0;
}

.main-content-card {
  border-radius: 8px;
}

.mail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f6fc;
  margin-bottom: 16px;
}

.mail-body-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 20px;
}

.custom-mail-table {
  border-radius: 6px;
  overflow: hidden;
}

.custom-mail-table :deep(.el-table__row) {
  cursor: pointer;
}

.unread-bold {
  font-weight: 700;
  color: #1f2f3d;
}

.detail-inner-card {
  height: 100%;
  box-sizing: border-box;
  background-color: #fafafa;
  border: 1px dashed #dcdfe6;
}

.detail-view {
  padding: 4px;
}

.mail-subject-title {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 20px;
}

.sender-info-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sender-meta {
  display: flex;
  flex-direction: column;
}

.sender-name {
  font-size: 14px;
  color: #303133;
}

.sender-email {
  font-size: 12px;
  color: #909399;
}

.time-stamp-line {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-recipients {
  margin-bottom: 16px;
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.recipients-label {
  color: #606266;
}

.mx-1 {
  margin: 2px;
}

.mail-content-display {
  margin-top: 20px;
  padding: 16px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  min-height: 220px;
  line-height: 1.6;
  color: #4c4d4f;
  white-space: pre-wrap;
}

.attachments-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.attach-title {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 8px;
}

.file-item-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f0f2f5;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  color: #409eff;
  margin-right: 8px;
  margin-bottom: 8px;
}

.file-size {
  color: #909399;
  font-size: 11px;
}

.detail-empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 540px;
}

.drawer-footer-actions {
  margin-top: 30px;
  display: flex;
  gap: 12px;
}
</style>