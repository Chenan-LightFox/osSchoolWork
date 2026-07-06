<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { currentUser, isAuthenticated } from '@/stores/auth'
import { Edit, Search, Delete, User, Timer, RefreshRight, Paperclip, Upload, Download, Warning } from '@element-plus/icons-vue'
import {
  getInbox,
  getSent,
  getTrash,
  getDrafts,
  searchInbox,
  getMailDetail,
  sendMail,
  sendMailWithAttachments,
  saveDraft,
  sendDraft,
  deleteDraft,
  markMailAsRead,
  trashMail,
  restoreMail,
  permanentDelete,
  downloadAttachment,
  getSpam,
  markAsSpam,
  markAsNotSpam,
} from '@/services/mail'
import { createMailSocket } from '@/services/ws'

const user = computed(() => currentUser.value || {})
const tabs = [
  { label: '收件箱', key: 'inbox' },
  { label: '已发送', key: 'sent' },
  { label: '草稿箱', key: 'draft' },
  { label: '垃圾箱', key: 'trash' },
  { label: '垃圾邮件', key: 'spam' },
]
const activeTab = ref('inbox')
const mails = ref([])
const selectedMail = ref(null)
const searchKeyword = ref('')
const loadingMails = ref(false)
const loadingDetail = ref(false)
const loadingSend = ref(false)
const composeDrawer = ref(false)
const draftId = ref(null)
const socketRef = ref(null)

const composeForm = reactive({
  subject: '',
  content: '',
  to: '',
  cc: '',
})
const pendingFiles = ref([])
const fileInputRef = ref(null)

const handleFileSelect = () => {
  fileInputRef.value?.click()
}

const onFilesChange = (e) => {
  const selected = Array.from(e.target.files || [])
  pendingFiles.value = [...pendingFiles.value, ...selected]
  // reset input so re-selecting same file works
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const removeFile = (index) => {
  pendingFiles.value.splice(index, 1)
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const loadMails = async () => {
  loadingMails.value = true
  selectedMail.value = null
  try {
    if (activeTab.value === 'inbox') {
      mails.value = searchKeyword.value
        ? await searchInbox(searchKeyword.value)
        : await getInbox()
    } else if (activeTab.value === 'sent') {
      mails.value = await getSent()
    } else if (activeTab.value === 'draft') {
      mails.value = await getDrafts()
    } else if (activeTab.value === 'trash') {
      mails.value = await getTrash()
    } else if (activeTab.value === 'spam') {
      mails.value = await getSpam()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载邮件失败')
  } finally {
    loadingMails.value = false
  }
}

const openMail = async (mail) => {
  if (activeTab.value === 'draft') return
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
  draftId.value = null
  resetCompose()
  composeDrawer.value = true
}

const resetCompose = () => {
  composeForm.subject = ''
  composeForm.content = ''
  composeForm.to = ''
  composeForm.cc = ''
  pendingFiles.value = []
}

const buildPayload = () => ({
  subject: composeForm.subject,
  content: composeForm.content,
  to: composeForm.to.split(',').map((s) => s.trim()).filter(Boolean),
  cc: composeForm.cc.split(',').map((s) => s.trim()).filter(Boolean),
})

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
    const payload = buildPayload()
    if (draftId.value) {
      await sendDraft(draftId.value, payload)
      ElMessage.success('草稿已发送')
    } else if (pendingFiles.value.length > 0) {
      await sendMailWithAttachments(payload, pendingFiles.value)
      ElMessage.success('发送成功（含 ' + pendingFiles.value.length + ' 个附件）')
    } else {
      await sendMail(payload)
      ElMessage.success('发送成功')
    }
    composeDrawer.value = false
    resetCompose()
    draftId.value = null
    if (activeTab.value === 'sent' || activeTab.value === 'draft') {
      await loadMails()
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    loadingSend.value = false
  }
}

const handleSaveDraft = async () => {
  loadingSend.value = true
  try {
    const id = await saveDraft(buildPayload(), draftId.value)
    draftId.value = id
    ElMessage.success('草稿已保存')
    if (activeTab.value === 'draft') {
      await loadMails()
    }
  } catch (error) {
    ElMessage.error(error.message || '保存草稿失败')
  } finally {
    loadingSend.value = false
  }
}

const editDraft = (mail) => {
  draftId.value = mail.id
  composeForm.subject = mail.subject || ''
  composeForm.content = '' // draft list only has preview, will need to load full content...
  // For now we load subject, content would need a detail call
  // But MailDetailView works for any mail, so let's just set what we have
  composeForm.to = ''
  composeForm.cc = ''
  composeDrawer.value = true
  // Load full draft content
  loadDraftContent(mail.id)
}

const loadDraftContent = async (id) => {
  try {
    const detail = await getMailDetail(id)
    composeForm.subject = detail.subject || ''
    composeForm.content = detail.content || ''
    // Try to reconstruct recipients from receiver list
    const toList = []
    const ccList = []
    if (detail.receivers) {
      for (const r of detail.receivers) {
        if (r.type === 'to') toList.push(r.email || r.username)
        else ccList.push(r.email || r.username)
      }
    }
    composeForm.to = toList.join(', ')
    composeForm.cc = ccList.join(', ')
  } catch {
    // ignore, keep what we have
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

const handleDeleteDraft = async (mail) => {
  try {
    await ElMessageBox.confirm('确定要删除这个草稿吗？', '确认', { type: 'warning' })
    await deleteDraft(mail.id)
    ElMessage.success('草稿已删除')
    await loadMails()
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleRestore = async (mail) => {
  try {
    await restoreMail(mail.id)
    ElMessage.success('已恢复到收件箱')
    await loadMails()
    if (selectedMail.value?.id === mail.id) {
      selectedMail.value = null
    }
  } catch (error) {
    ElMessage.error(error.message || '恢复失败')
  }
}

const handlePermanentDelete = async (mail) => {
  try {
    await ElMessageBox.confirm(
      '永久删除后无法恢复，确定要彻底删除该邮件吗？',
      '确认永久删除',
      { type: 'warning', confirmButtonText: '永久删除', cancelButtonText: '取消' }
    )
    await permanentDelete(mail.id)
    ElMessage.success('已永久删除')
    await loadMails()
    if (selectedMail.value?.id === mail.id) {
      selectedMail.value = null
    }
  } catch (error) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleMarkSpam = async (mail) => {
  try {
    await markAsSpam(mail.id)
    ElMessage.success('已标记为垃圾邮件')
    await loadMails()
    if (selectedMail.value?.id === mail.id) {
      selectedMail.value = null
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleMarkNotSpam = async (mail) => {
  try {
    await markAsNotSpam(mail.id)
    ElMessage.success('已移回收件箱')
    await loadMails()
    if (selectedMail.value?.id === mail.id) {
      selectedMail.value = null
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
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
  if (!isAuthenticated.value) return
  if (socketRef.value && socketRef.value.readyState <= 1) return
  const socket = createMailSocket()
  if (!socket) return
  socket.onmessage = handleSocketMessage
  socket.onclose = () => { socketRef.value = null }
  socket.onerror = () => { socketRef.value = null }
  socketRef.value = socket
}

const closeSocket = () => {
  if (socketRef.value) {
    socketRef.value.close()
    socketRef.value = null
  }
}

const handleDownload = async (file) => {
  try {
    await downloadAttachment(file.id, file.fileName)
  } catch (error) {
    ElMessage.error(error.message || '下载失败')
  }
}

const isDraftTab = computed(() => activeTab.value === 'draft')
const isTrashTab = computed(() => activeTab.value === 'trash')
const isSpamTab = computed(() => activeTab.value === 'spam')
const isInboxTab = computed(() => activeTab.value === 'inbox')
const showDetail = computed(() => !isDraftTab.value && !isTrashTab.value && !isSpamTab.value)

watch(activeTab, () => { searchKeyword.value = ''; loadMails() })
onMounted(() => { loadMails(); connectSocket() })
onBeforeUnmount(closeSocket)
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

        <el-menu
          :default-active="activeTab"
          @select="(key) => activeTab = key"
          class="sidebar-nav"
        >
          <el-menu-item index="inbox">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg></el-icon>
            <span>收件箱</span>
          </el-menu-item>
          <el-menu-item index="sent">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg></el-icon>
            <span>已发送</span>
          </el-menu-item>
          <el-menu-item index="draft">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg></el-icon>
            <span>草稿箱</span>
          </el-menu-item>
          <el-menu-item index="trash">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg></el-icon>
            <span>垃圾箱</span>
          </el-menu-item>
          <el-menu-item index="spam">
            <el-icon><svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M15.73 3H8.27L3 8.27v7.46L8.27 21h7.46L21 15.73V8.27L15.73 3zM12 17.3c-.72 0-1.3-.58-1.3-1.3 0-.72.58-1.3 1.3-1.3.72 0 1.3.58 1.3 1.3 0 .72-.58 1.3-1.3 1.3zm1-4.3h-2V7h2v6z"/></svg></el-icon>
            <span>垃圾邮件</span>
          </el-menu-item>
        </el-menu>

        <div class="action-box">
          <el-button type="primary" size="large" @click="openCompose" :icon="Edit" class="write-btn">
            撰写新邮件
          </el-button>
        </div>
      </div>

      <div class="dashboard-content">
        <el-card shadow="always" class="main-content-card">
          <div class="mail-toolbar">
            <h3 class="toolbar-title">{{ tabs.find(t => t.key === activeTab)?.label || '邮件' }}</h3>

            <div class="mail-search" v-if="isInboxTab">
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
            <div v-else style="min-width: 200px;"></div>
          </div>

          <div class="mail-body-grid" :class="{ 'mail-body-grid--full': !showDetail }">
            <div class="mail-list-panel" :class="{ 'mail-list-panel--full': !showDetail }">
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
                    <el-tag v-if="isInboxTab" :type="row.isRead === 1 ? 'info' : 'danger'" effect="light" round size="small">
                      {{ row.isRead === 1 ? '已读' : '未读' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="senderName" label="发信人" width="130" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span v-if="!isDraftTab" :class="{ 'unread-bold': row.isRead !== 1 && isInboxTab }">{{ row.senderName }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="subject" label="邮件主题" width="250" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span :class="{ 'unread-bold': row.isRead !== 1 && isInboxTab }">
                      {{ row.subject || '(无主题)' }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column prop="sendTime" label="时间" width="160" show-overflow-tooltip />

                <el-table-column label="" width="40" align="center">
                  <template #default="{ row }">
                    <el-icon v-if="row.hasAttachment" title="有附件" style="color: #909399;"><Paperclip /></el-icon>
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="130" align="center" fixed="right">
                  <template #default="{ row }">
                    <template v-if="isDraftTab">
                      <el-button size="small" type="primary" :icon="Edit" circle @click.stop="editDraft(row)" title="编辑草稿" />
                      <el-button size="small" type="danger" :icon="Delete" circle @click.stop="handleDeleteDraft(row)" title="删除草稿" style="margin-left: 4px;" />
                    </template>
                    <template v-else-if="isTrashTab">
                      <el-button size="small" type="success" :icon="RefreshRight" circle @click.stop="handleRestore(row)" title="恢复邮件" />
                      <el-button size="small" type="danger" :icon="Warning" circle @click.stop="handlePermanentDelete(row)" title="永久删除" style="margin-left: 4px;" />
                    </template>
                    <template v-else-if="isSpamTab">
                      <el-button size="small" type="primary" :icon="RefreshRight" circle @click.stop="handleMarkNotSpam(row)" title="非垃圾邮件" />
                      <el-button size="small" type="danger" :icon="Delete" circle @click.stop="handleTrash(row)" title="移到垃圾箱" style="margin-left: 4px;" />
                    </template>
                    <template v-else>
                      <el-button size="small" type="warning" plain :icon="Warning" circle @click.stop="handleMarkSpam(row)" title="标记为垃圾邮件" />
                      <el-button size="small" type="danger" plain :icon="Delete" circle @click.stop="handleTrash(row)" title="移到垃圾箱" style="margin-left: 4px;" />
                    </template>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="mail-detail-panel" v-if="showDetail">
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
                      <span class="file-size">({{ formatFileSize(file.fileSize) }})</span>
                      <el-button size="small" :icon="Download" circle @click="handleDownload(file)" title="下载附件" />
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

    <el-drawer
      :title="draftId ? '编辑草稿' : '新建邮件正文撰写'"
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
          <el-input type="textarea" v-model="composeForm.content" :rows="10" placeholder="在此编辑您的邮件正文细节内容..." />
        </el-form-item>

        <el-form-item label="📎 邮件附件">
          <input
            ref="fileInputRef"
            type="file"
            multiple
            style="display: none;"
            @change="onFilesChange"
          />
          <el-button :icon="Upload" @click="handleFileSelect" size="small" type="primary" plain>
            选择文件
          </el-button>
          <div v-if="pendingFiles.length" class="pending-files-list">
            <div v-for="(file, index) in pendingFiles" :key="index" class="pending-file-item">
              <el-icon style="color: #409eff;"><Paperclip /></el-icon>
              <span class="pending-file-name">{{ file.name }}</span>
              <span class="pending-file-size">({{ formatFileSize(file.size) }})</span>
              <el-button size="small" :icon="Delete" circle @click="removeFile(index)" title="移除此附件" />
            </div>
          </div>
        </el-form-item>

        <div class="drawer-footer-actions">
          <el-button type="primary" size="large" :loading="loadingSend" @click="handleSend" style="width: 130px;">
            {{ draftId ? '发送草稿' : '发送邮件' }}
          </el-button>
          <el-button size="large" :loading="loadingSend" @click="handleSaveDraft">
            💾 保存草稿
          </el-button>
          <el-button size="large" @click="composeDrawer = false">取消</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
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

.sidebar-nav {
  margin-top: 16px;
  border-right: none;
  border-radius: 8px;
  overflow: hidden;
}

.sidebar-nav .el-menu-item {
  height: 44px;
  line-height: 44px;
  font-size: 14px;
}

.sidebar-nav .el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.action-box {
  margin: 20px 0;
}

.toolbar-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
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

.mail-body-grid--full {
  grid-template-columns: 1fr;
}

.mail-list-panel--full {
  /* full width */
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

.pending-files-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pending-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}

.pending-file-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pending-file-size {
  color: #909399;
  font-size: 12px;
}
</style>
