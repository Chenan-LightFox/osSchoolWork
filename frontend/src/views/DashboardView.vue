<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { currentUser, isAuthenticated } from '@/stores/auth'
import { createMailSocket } from '@/services/ws'
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
watch(isAuthenticated, (authed) => {
  if (authed) {
    connectSocket()
  } else {
    closeSocket()
  }
})

onMounted(() => {
  loadMails()
  connectSocket()
})

onBeforeUnmount(closeSocket)
</script>

<template>
  <div class="dashboard-layout">
    <div class="dashboard-sidebar">
      <el-card shadow="hover" class="dashboard-card">
        <template #header>
          <div>用户信息</div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户ID">{{ user.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ user.username || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="hover" class="dashboard-card" style="margin-top: 24px">
        <template #header>
          <div>邮件操作</div>
        </template>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCompose" icon="el-icon-edit">写邮件</el-button>
        </div>
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="当前页面已接入收件箱、发件箱和邮件详情接口。"
        />
      </el-card>
    </div>

    <div class="dashboard-content">
      <el-card shadow="hover" class="dashboard-card">
        <template #header>
          <div>邮件中心</div>
        </template>

        <div class="mail-toolbar">
          <el-tabs v-model="activeTab" type="border-card">
            <el-tab-pane v-for="tab in tabs" :key="tab.key" :label="tab.label" :name="tab.key" />
          </el-tabs>
          <div class="mail-search" v-if="activeTab === 'inbox'">
            <el-input
              v-model="searchKeyword"
              placeholder="按主题/发件人搜索"
              clearable
              @clear="handleSearch"
              @keyup.enter.native="handleSearch"
            >
              <template #append>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>
        </div>

        <div class="mail-body">
          <div class="mail-list">
            <el-table
              :data="mails"
              v-loading="loadingMails"
              style="width: 100%"
              height="560"
              @row-click="openMail"
            >
              <el-table-column prop="senderName" label="发件人" width="140" />
              <el-table-column prop="subject" label="主题" />
              <el-table-column prop="sendTime" label="发送时间" width="200" />
              <el-table-column
                label="状态"
                width="120"
                :formatter="(row) => row.isRead === 1 ? '已读' : '未读'"
              />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click.stop="handleTrash(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="mail-detail">
            <el-card shadow="hover" style="height: 100%">
              <template #header>
                <div>邮件详情</div>
              </template>
              <div v-if="selectedMail">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="主题">{{ selectedMail.subject }}</el-descriptions-item>
                  <el-descriptions-item label="发件人">{{ selectedMail.senderName }} &lt;{{ selectedMail.senderEmail }}&gt;</el-descriptions-item>
                  <el-descriptions-item label="发送时间">{{ selectedMail.sendTime }}</el-descriptions-item>
                  <el-descriptions-item label="正文">{{ selectedMail.content }}</el-descriptions-item>
                  <el-descriptions-item label="收件人">
                    <div v-for="receiver in selectedMail.receivers" :key="receiver.userId">
                      {{ receiver.type }}: {{ receiver.username || receiver.email }}
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="附件" v-if="selectedMail.attachments?.length">
                    <div v-for="attachment in selectedMail.attachments" :key="attachment.id">
                      {{ attachment.fileName }} ({{ attachment.fileSize }} 字节)
                    </div>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
              <div v-else class="empty-state">
                <el-empty description="请选择一封邮件查看详情" />
              </div>
            </el-card>
          </div>
        </div>
      </el-card>
    </div>
  </div>

  <el-drawer
    title="写邮件"
    :model-value="composeDrawer"
    direction="rtl"
    size="40%"
    :destroy-on-close="true"
  >
    <el-form label-position="top">
      <el-form-item label="收件人 (逗号分隔)">
        <el-input v-model="composeForm.to" placeholder="输入收件人邮箱，多个请用逗号分隔" />
      </el-form-item>
      <el-form-item label="抄送 (逗号分隔)">
        <el-input v-model="composeForm.cc" placeholder="输入抄送人邮箱，多个请用逗号分隔" />
      </el-form-item>
      <el-form-item label="主题">
        <el-input v-model="composeForm.subject" placeholder="请输入主题" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input type="textarea" v-model="composeForm.content" :rows="8" placeholder="请输入邮件正文" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loadingSend" @click="handleSend">发送</el-button>
        <el-button @click="composeDrawer = false">取消</el-button>
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  gap: 24px;
}

.dashboard-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
}

.dashboard-content {
  flex: 1;
}

.dashboard-card {
  padding: 16px;
}

.toolbar-actions {
  margin-bottom: 16px;
}

.mail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.mail-search {
  flex: 1;
}

.mail-body {
  display: grid;
  grid-template-columns: 2fr 1.5fr;
  gap: 16px;
}

.mail-list {
  min-height: 620px;
}

.mail-detail {
  min-height: 620px;
}

.empty-state {
  display: grid;
  place-items: center;
  height: 100%;
}
</style>
