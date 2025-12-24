<template>
  <div class="task-management">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="任务管理" name="list">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold">任务列表</span>
              <el-button type="primary" @click="showCreateDialog = true">发布任务</el-button>
            </div>
          </template>

          <el-table :data="tasks" style="width: 100%">
            <el-table-column prop="task.title" label="标题" />
            <el-table-column prop="task.deadline" label="截止时间">
              <template #default="scope">
                {{ formatDate(scope.row.task.deadline) }}
              </template>
            </el-table-column>
            <el-table-column prop="task.rewardPoints" label="积分奖励" width="100" />
            <el-table-column prop="submissionCount" label="提交数" width="100" />
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button size="small" @click="viewSubmissions(scope.row.task)">批改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="数据分析" name="analysis">
         <div class="p-4">
            <div class="flex items-center mb-4">
               <span class="mr-2">选择任务:</span>
               <el-select v-model="selectedTaskId" placeholder="请选择" @change="loadTaskStats" style="width: 300px">
                   <el-option v-for="t in tasks" :key="t.task.id" :label="t.task.title" :value="t.task.id" />
               </el-select>
            </div>
            
            <div v-if="stats" class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <el-card shadow="hover">
                    <div class="text-gray-500 mb-2">提交率</div>
                    <div class="text-2xl font-bold text-blue-600">
                       {{ (stats.submissionRate * 100).toFixed(1) }}%
                    </div>
                    <div class="text-sm text-gray-400 mt-2">
                       已提交: {{ stats.submittedCount }} / 总人数: {{ stats.totalStudents }}
                    </div>
                </el-card>
                
                <el-card shadow="hover">
                    <div class="text-gray-500 mb-2">平均提前提交</div>
                    <div class="text-2xl font-bold text-green-600">
                       {{ stats.avgTimeBeforeDeadline.toFixed(1) }} 小时
                    </div>
                    <div class="text-sm text-gray-400 mt-2">截止前平均时长</div>
                </el-card>
                
                <el-card shadow="hover" class="md:col-span-3">
                   <div class="text-gray-500 mb-2">每日提交趋势</div>
                   <div ref="chartRef" style="height: 300px; width: 100%"></div>
                </el-card>
            </div>
            <el-empty v-else description="请选择任务查看分析数据" />
         </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Task Dialog -->
    <el-dialog v-model="showCreateDialog" title="发布新任务" width="600px">
      <el-form :model="newTask" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="newTask.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="newTask.content" type="textarea" rows="4" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="newTask.deadline" type="datetime" placeholder="选择截止时间" />
        </el-form-item>
        <el-form-item label="奖励积分">
          <el-input-number v-model="newTask.rewardPoints" :min="0" />
        </el-form-item>
        <el-form-item label="提交类型">
          <el-select v-model="newTask.submitType">
            <el-option label="文本" value="TEXT" />
            <el-option label="文件" value="FILE" />
            <el-option label="图片" value="IMAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="重复打卡">
           <el-switch v-model="newTask.isRecurring" />
        </el-form-item>
        <template v-if="newTask.isRecurring">
           <el-form-item label="起止日期">
             <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="handleDateRangeChange"
             />
           </el-form-item>
           <el-form-item label="允许补卡">
             <el-input-number v-model="newTask.makeupCount" :min="0" :max="3" label="次数" />
             <span class="ml-2">次</span>
           </el-form-item>
           <el-form-item label="补卡积分">
             <el-input-number v-model="newTask.makeupCostPercent" :min="50" :max="100" step="10" />
             <span class="ml-2">% (相对于原积分)</span>
           </el-form-item>
           <el-form-item label="内容模板">
             <el-input v-model="newTask.contentTemplate" type="textarea" rows="3" placeholder="支持变量 [日期]" />
           </el-form-item>
        </template>
        <el-form-item label="截止时间" v-else>
          <el-date-picker v-model="newTask.deadline" type="datetime" placeholder="选择截止时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">发布</el-button>
      </template>
    </el-dialog>

    <!-- Submissions Drawer -->
    <el-drawer v-model="showSubmissions" title="提交管理" size="80%">
      <template #header>
          <div class="flex justify-between items-center">
              <span class="text-lg font-bold">提交管理</span>
              <el-button type="success" @click="handleBatchPass">批量通过 (满分)</el-button>
          </div>
      </template>
      <el-table :data="submissions" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="content" label="提交内容" />
        <el-table-column prop="submitTime" label="提交时间">
          <template #default="scope">
            {{ formatDate(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80" />
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openGradeDialog(scope.row)">评分</el-button>
            <el-button size="small" type="warning" @click="handleReturn(scope.row)">打回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- Grade Dialog -->
    <el-dialog v-model="showGradeDialog" title="评分" width="400px">
      <el-form :model="gradeForm">
        <el-form-item label="分数">
          <el-input-number v-model="gradeForm.score" :min="0" />
        </el-form-item>
        <el-form-item label="评级(1-5)">
          <el-rate v-model="gradeForm.rating" allow-half />
        </el-form-item>
        <el-form-item label="快捷评语">
            <el-select v-model="selectedQuickComment" placeholder="选择评语" @change="handleQuickComment" clearable>
                <el-option v-for="c in quickComments" :key="c" :label="c" :value="c" />
            </el-select>
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGradeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitGrade">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { getTeacherTasks, createTask, getSubmissions, gradeSubmission, returnSubmission, getTaskStats } from '../../api/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const activeTab = ref('list')
const tasks = ref([])
const submissions = ref([])
const showCreateDialog = ref(false)
const showSubmissions = ref(false)
const showGradeDialog = ref(false)
const selectedSubmissions = ref([])

// Analysis
const selectedTaskId = ref()
const stats = ref(null)
const chartRef = ref(null)
let chartInstance: any = null

const quickComments = ['优秀', '良好', '需改进', '未按要求完成', '请重新提交', '作业非常棒！', '内容详实', '格式规范']
const selectedQuickComment = ref('')

const dateRange = ref([])
const newTask = ref({
  title: '',
  content: '',
  deadline: '',
  rewardPoints: 10,
  submitType: 'TEXT',
  isRecurring: false,
  startDate: '',
  endDate: '',
  makeupCount: 0,
  makeupCostPercent: 100,
  contentTemplate: ''
})

const currentSubmission = ref(null)
const gradeForm = ref({
  score: 0,
  rating: 0,
  comment: ''
})

const loadTasks = async () => {
  try {
    const res = await getTeacherTasks()
    tasks.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const handleDateRangeChange = (val: any) => {
    if (val && val.length === 2) {
        newTask.value.startDate = dayjs(val[0]).format('YYYY-MM-DD')
        newTask.value.endDate = dayjs(val[1]).format('YYYY-MM-DD')
    } else {
        newTask.value.startDate = ''
        newTask.value.endDate = ''
    }
}

const handleCreateTask = async () => {
  try {
    // Create a payload copy to avoid modifying the form data directly and to format dates
    const payload: any = { ...newTask.value }

    if (payload.isRecurring) {
        if (!payload.startDate || !payload.endDate) {
            ElMessage.warning('请选择起止日期')
            return
        }
        // startDate/endDate are handled by handleDateRangeChange
    } else {
        if (!payload.deadline) {
            ElMessage.warning('请选择截止时间')
            return
        }
        // Format deadline to ISO string compatible with backend LocalDateTime
        payload.deadline = dayjs(payload.deadline).format('YYYY-MM-DDTHH:mm:ss')
    }
    
    // Set status to Published (1)
    payload.status = 1

    await createTask(payload)
    ElMessage.success('发布成功')
    showCreateDialog.value = false
    loadTasks()
  } catch (e: any) {
    console.error('Create task error:', e)
    ElMessage.error(e.message || '发布失败')
  }
}

const viewSubmissions = async (task: any) => {
  try {
    const res = await getSubmissions(task.id)
    submissions.value = res.data
    showSubmissions.value = true
  } catch (e) {
    console.error(e)
  }
}

const openGradeDialog = (submission: any) => {
  currentSubmission.value = submission
  gradeForm.value = {
    score: submission.score || 0,
    rating: submission.rating || 0,
    comment: submission.comment || ''
  }
  selectedQuickComment.value = ''
  showGradeDialog.value = true
}

const handleQuickComment = (val: string) => {
    if (val) {
        gradeForm.value.comment = val
    }
}

const submitGrade = async () => {
  if (!currentSubmission.value) return
  try {
    await gradeSubmission(currentSubmission.value.id, gradeForm.value)
    ElMessage.success('评分成功')
    showGradeDialog.value = false
    // Reload submissions
    const res = await getSubmissions(currentSubmission.value.taskId)
    submissions.value = res.data
  } catch (e) {
    ElMessage.error('评分失败')
  }
}

const handleReturn = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入打回原因', '打回作业', {
      confirmButtonText: '确认打回',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '原因不能为空'
    })
    
    await returnSubmission(row.id, { comment: value })
    ElMessage.success('已打回')
    // Reload
    const res = await getSubmissions(row.taskId)
    submissions.value = res.data
  } catch (e: any) {
      if (e !== 'cancel') {
          ElMessage.error('操作失败')
      }
  }
}

const handleSelectionChange = (val: any) => {
    selectedSubmissions.value = val
}

const handleBatchPass = async () => {
    if (selectedSubmissions.value.length === 0) {
        ElMessage.warning('请先选择提交记录')
        return
    }
    
    try {
        await ElMessageBox.confirm(`确定要批量通过 ${selectedSubmissions.value.length} 条记录吗？这将给予满分。`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        
        // Assume max score is 100 or fetch from task?
        // For now use 100 or task reward points? 
        // Requirement says "奖励积分（1-1000）".
        // Let's assume full score = 100 for grade, and points are separate.
        const score = 100
        
        let successCount = 0
        for (const sub of selectedSubmissions.value) {
            try {
                await gradeSubmission(sub.id, {
                    score: score,
                    rating: 5,
                    comment: '批量通过'
                })
                successCount++
            } catch (e) {
                console.error(e)
            }
        }
        
        ElMessage.success(`批量处理完成，成功 ${successCount} 条`)
        // Reload
        if (selectedSubmissions.value.length > 0) {
             const res = await getSubmissions(selectedSubmissions.value[0].taskId)
             submissions.value = res.data
        }
        
    } catch (e) {
        // Cancelled
    }
}

const loadTaskStats = async () => {
    if (!selectedTaskId.value) return
    try {
        const res = await getTaskStats(selectedTaskId.value)
        stats.value = res.data
        
        nextTick(() => {
            initChart()
        })
    } catch (e) {
        ElMessage.error('获取分析数据失败')
    }
}

const initChart = () => {
    if (!chartRef.value || !stats.value || !stats.value.trend) return
    
    if (chartInstance) {
        chartInstance.dispose()
    }
    
    chartInstance = echarts.init(chartRef.value)
    
    const dates = Object.keys(stats.value.trend).sort()
    const values = dates.map(d => stats.value.trend[d])
    
    const option = {
        title: { text: '每日提交数' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: dates },
        yAxis: { type: 'value' },
        series: [{
            data: values,
            type: 'line',
            smooth: true,
            areaStyle: {}
        }]
    }
    
    chartInstance.setOption(option)
}

const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusType = (status: number) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: number) => {
  switch (status) {
    case 0: return '待批改'
    case 1: return '已批改'
    case 2: return '已退回'
    default: return '未知'
  }
}

onMounted(() => {
  loadTasks()
})

watch(() => activeTab.value, () => {
    if (activeTab.value === 'analysis' && selectedTaskId.value) {
        nextTick(() => {
             initChart()
        })
    }
})
</script>

<style scoped>
.task-management {
    padding: 20px;
}
</style>