<template>
  <div class="teacher-tasks">
    <div class="mb-4 font-bold text-lg">教师任务</div>
    
    <el-row :gutter="20">
      <el-col :span="8" v-for="item in sortedTasks" :key="item.task.id">
        <el-card class="mb-4 task-card" :class="{ 'urgent': isUrgent(item.task.deadline) }">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="truncate font-bold" style="max-width: 150px;" :title="item.task.title">{{ item.task.title }}</span>
              <el-tag :type="getStatusType(item)">{{ getStatusText(item) }}</el-tag>
            </div>
          </template>
          
          <div v-if="item.task.isRecurring" class="flex flex-col items-center mb-4">
             <el-progress type="dashboard" :percentage="getProgress(item)" :color="getProgressColor(item)">
                <template #default="{ percentage }">
                  <span class="text-xl font-bold">{{ item.checkinCount }}/{{ item.totalDays }}</span>
                  <div class="text-xs text-gray-500">已打卡</div>
                </template>
             </el-progress>
             <div v-if="item.todayChecked" class="text-success font-bold mt-2">今日已打卡</div>
             <div v-else class="text-warning font-bold mt-2">今日未打卡</div>
          </div>

          <div v-else class="text-gray-600 mb-2 line-clamp-2" style="height: 40px; overflow: hidden;">{{ item.task.content }}</div>
          
          <div class="flex justify-between text-sm text-gray-500 mb-2">
            <span>{{ item.task.isRecurring ? '结束: ' + formatDate(item.task.endDate) : '截止: ' + formatDate(item.task.deadline) }}</span>
            <span>积分: {{ item.task.rewardPoints }}</span>
          </div>
          
          <div v-if="!item.submission && !item.task.isRecurring" class="text-orange-500 text-sm mb-2">
            倒计时: {{ getCountdown(item.task.deadline) }}
          </div>

          <div class="flex justify-between items-center mt-4">
             <el-button link type="primary" @click="viewLeaderboard(item.task)" v-if="!item.task.isRecurring">
                 <el-icon class="mr-1"><Trophy /></el-icon>排行榜
             </el-button>
             <div v-else></div> <!-- Spacer -->

             <el-button type="primary" size="small" @click="openSubmitDialog(item)" 
               :disabled="isSubmitDisabled(item)">
               {{ getButtonText(item) }}
             </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Submit Dialog -->
    <el-dialog v-model="showSubmitDialog" :title="currentTask?.title" width="500px">
      <el-form>
        <el-form-item label="内容">
           <el-input v-model="submitForm.content" type="textarea" rows="4" />
        </el-form-item>
        <el-form-item label="附件链接">
           <el-input v-model="submitForm.fileUrls" placeholder="输入文件链接 (JSON)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- Leaderboard Dialog -->
    <el-dialog v-model="showLeaderboardDialog" title="速度排行榜 (前10名)" width="600px">
        <el-alert title="仅展示截止前50%时间内完成的提交" type="info" show-icon :closable="false" class="mb-4" />
        <el-table :data="leaderboardData" stripe>
            <el-table-column type="index" label="排名" width="60">
                <template #default="scope">
                    <div class="flex justify-center">
                        <span v-if="scope.$index < 3" class="text-lg">
                            {{ scope.$index === 0 ? '🥇' : scope.$index === 1 ? '🥈' : '🥉' }}
                        </span>
                        <span v-else>{{ scope.$index + 1 }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="学生" width="150">
                <template #default="scope">
                    <div class="flex items-center">
                        <el-avatar :size="24" :src="scope.row.avatar" class="mr-2" />
                        <span class="truncate">{{ scope.row.studentName }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时" />
            <el-table-column prop="submitTime" label="提交时间">
                <template #default="scope">
                    {{ formatDate(scope.row.submitTime) }}
                </template>
            </el-table-column>
        </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getStudentTasks, submitTask, getTaskLeaderboard } from '../../api/task'
import { ElMessage } from 'element-plus'
import { Trophy } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const tasks = ref([])
const showSubmitDialog = ref(false)
const showLeaderboardDialog = ref(false)
const currentTask = ref(null)
const leaderboardData = ref([])
const submitForm = ref({
  content: '',
  fileUrls: ''
})

const loadTasks = async () => {
  try {
    const res = await getStudentTasks()
    tasks.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const sortedTasks = computed(() => {
  return tasks.value.slice().sort((a, b) => {
    // Urgent (<24h) first
    const aUrgent = isUrgent(a.task.deadline)
    const bUrgent = isUrgent(b.task.deadline)
    if (aUrgent && !bUrgent) return -1
    if (!aUrgent && bUrgent) return 1
    return 0
  })
})

const isUrgent = (deadline: string) => {
  if (!deadline) return false
  const diff = dayjs(deadline).diff(dayjs(), 'hour')
  return diff >= 0 && diff < 24
}

const getProgress = (item: any) => {
    if (!item.totalDays || item.totalDays === 0) return 0
    return Math.min(100, Math.floor((item.checkinCount / item.totalDays) * 100))
}

const getProgressColor = (item: any) => {
    const p = getProgress(item)
    if (p >= 100) return '#67C23A'
    if (p >= 60) return '#409EFF'
    return '#E6A23C'
}

const isSubmitDisabled = (item: any) => {
    if (item.task.isRecurring) {
        return item.todayChecked
    }
    return item.submission && item.submission.status !== 2
}

const getStatusType = (item: any) => {
  if (item.task.isRecurring) {
      if (item.todayChecked) return 'success'
      return 'warning'
  }
  if (item.submission) {
     if (item.submission.status === 1) return 'success' // Graded
     if (item.submission.status === 2) return 'danger' // Returned
     return 'success' // Submitted
  }
  return 'warning' // Ongoing
}

const getStatusText = (item: any) => {
  if (item.task.isRecurring) {
      if (item.todayChecked) return '今日已打卡'
      return '今日未打卡'
  }
  if (item.submission) {
     if (item.submission.status === 1) return '已完成'
     if (item.submission.status === 2) return '已退回'
     return '已提交'
  }
  return '进行中'
}

const getButtonText = (item: any) => {
  if (item.task.isRecurring) {
      if (item.todayChecked) return '已打卡'
      return '打卡'
  }
  if (item.submission) {
    if (item.submission.status === 2) return '重新提交'
    return '已提交'
  }
  return '去提交'
}

const formatDate = (date: string) => {
    return date ? dayjs(date).format('MM-DD') : '-' // Short format for cards
}

const getCountdown = (deadline: string) => {
    if (!deadline) return '-'
    const now = dayjs()
    const end = dayjs(deadline)
    const diff = end.diff(now)
    if (diff <= 0) return '已截止'
    
    const d = Math.floor(diff / (1000 * 60 * 60 * 24))
    const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    
    if (d > 0) return `${d}天 ${h}小时`
    return `${h}小时 ${m}分`
}

const openSubmitDialog = (item: any) => {
    currentTask.value = item.task
    
    if (item.task.isRecurring) {
        // Use template if available and new submission
        const date = dayjs().format('YYYY-MM-DD')
        let content = item.task.contentTemplate || ''
        content = content.replace('[日期]', date)
        submitForm.value = {
            content: content,
            fileUrls: ''
        }
    } else {
        submitForm.value = {
            content: item.submission ? item.submission.content : '',
            fileUrls: item.submission ? item.submission.fileUrls : ''
        }
    }
    showSubmitDialog.value = true
}

const handleSubmit = async () => {
    if (!currentTask.value) return
    try {
        const isRecurring = currentTask.value.isRecurring
        await submitTask(currentTask.value.id, submitForm.value)
        
        if (isRecurring) {
             ElMessage.success('打卡成功！坚持就是胜利！')
        } else {
             ElMessage.success('提交成功')
        }
        
        showSubmitDialog.value = false
        loadTasks()
    } catch (e: any) {
        ElMessage.error(e.message || '提交失败')
    }
}

const viewLeaderboard = async (task: any) => {
    try {
        const res = await getTaskLeaderboard(task.id)
        leaderboardData.value = res.data
        showLeaderboardDialog.value = true
    } catch (e) {
        ElMessage.error('获取排行榜失败')
    }
}

let timer: any
onMounted(() => {
    loadTasks()
    timer = setInterval(() => {
        // Force refresh for countdown
        tasks.value = [...tasks.value] 
    }, 60000)
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
})
</script>

<style scoped>
.urgent {
  border-left: 4px solid #ff4949;
}
.task-card {
  /* height: 220px; */
  margin-bottom: 20px;
}
</style>