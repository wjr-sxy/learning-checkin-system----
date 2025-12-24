<template>
  <div class="dashboard-container">
    <el-header class="main-header">
      <div class="header-content container">
        <div class="logo-section">
          <h1>学习打卡系统</h1>
        </div>
        <div class="header-actions">
          <div class="user-info" v-if="userStore.user">
            <el-avatar :size="40" :src="userStore.user.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
            <span class="username">{{ userStore.user.username }}</span>
            <span class="points-badge">
              <el-icon><Coin /></el-icon>
              {{ userStore.user.points }}
            </span>
          </div>
          <el-button type="danger" plain size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="container">
      <div class="teacher-dashboard">
          <div class="flex-between mb-4">
               <div>
                    <h1>教师管理后台</h1>
                    <p class="text-muted">欢迎回来，管理您的课程和学生</p>
               </div>
          </div>

          <el-tabs v-model="activeTab" class="teacher-tabs" type="border-card">
            <!-- 1. 课程中心 (Course Center) -->
            <el-tab-pane label="课程中心" name="course">
                <div class="tab-actions mb-4">
                    <el-button type="primary" size="large" @click="createDialogVisible = true">
                        <el-icon class="mr-2"><Plus /></el-icon>新建课程
                    </el-button>
                </div>

                <el-row :gutter="20">
                    <el-col :span="24" v-if="courses.length === 0">
                        <el-empty description="暂无课程，快去创建吧" />
                    </el-col>
                    <el-col :xs="24" :sm="12" :md="8" v-for="course in courses" :key="course.id" class="mb-4">
                        <el-card shadow="hover" class="course-card">
                            <template #header>
                                <div class="card-header flex-between">
                                    <span class="course-title">{{ course.name }}</span>
                                    <el-tag type="success" effect="dark">{{ course.code }}</el-tag>
                                </div>
                            </template>
                            <div class="course-content">
                                <div class="course-info">
                                    <p><strong>学期:</strong> {{ course.semester || '默认学期' }}</p>
                                    <p class="description">{{ course.description || '暂无描述' }}</p>
                                </div>
                                <div class="course-footer mt-4 flex-between">
                                     <el-button type="primary" plain size="small" @click="switchToClassTab(course)">
                                         管理班级
                                     </el-button>
                                     <el-button type="warning" plain size="small" @click="handleRemind(course)">
                                         一键提醒
                                     </el-button>
                                </div>
                            </div>
                        </el-card>
                    </el-col>
                </el-row>
            </el-tab-pane>

            <!-- 2. 班级管理 (Class Mgmt) -->
            <el-tab-pane label="班级管理" name="class">
                <div class="filter-section mb-4 flex-between">
                    <div class="flex-start">
                        <span class="mr-2">选择课程:</span>
                        <el-select v-model="selectedCourseId" placeholder="请选择课程" @change="handleCourseChange" style="width: 200px">
                            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
                        </el-select>
                    </div>
                    <div v-if="selectedCourseId">
                        <el-input v-model="studentSearch" placeholder="搜索姓名/邮箱" style="width: 250px" prefix-icon="Search" />
                    </div>
                </div>

                <div v-if="selectedCourseId">
                    <el-table :data="filteredStudents" stripe style="width: 100%" v-loading="loadingStudents">
                        <el-table-column prop="username" label="姓名" />
                        <el-table-column prop="email" label="邮箱" />
                        <el-table-column prop="points" label="积分" width="100" />
                        <el-table-column label="状态" width="100">
                            <template #default="scope">
                                <el-tag :type="scope.row.status === 1 ? 'danger' : 'success'">
                                    {{ scope.row.status === 1 ? '已禁入' : '正常' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="joinTime" label="加入时间" width="180">
                            <template #default="scope">
                                {{ formatDate(scope.row.joinTime) }}
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="200" fixed="right">
                            <template #default="scope">
                                <el-button v-if="scope.row.status === 0" type="danger" link size="small" @click="handleBan(scope.row)">
                                    禁入
                                </el-button>
                                <el-button v-else type="success" link size="small" disabled>
                                    已禁入
                                </el-button>
                                <el-button type="warning" link size="small" @click="handleRemove(scope.row)">
                                    移除
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
                <el-empty v-else description="请先选择一个课程来管理学生" />
            </el-tab-pane>

            <!-- 3. 任务管理 (Task Mgmt) -->
            <el-tab-pane label="任务管理" name="task">
                <TaskManagement />
            </el-tab-pane>

            <!-- 3. 教学计划 (Plans) -->
            <el-tab-pane label="教学计划" name="plan">
                <div class="filter-section mb-4 flex-between">
                    <div class="flex-start">
                        <span class="mr-2">选择课程:</span>
                        <el-select v-model="planSelectedCourseId" placeholder="请选择课程" @change="handlePlanCourseChange" style="width: 200px">
                            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
                        </el-select>
                    </div>
                    <el-button type="primary" @click="openCreatePlanDialog" :disabled="!planSelectedCourseId">
                        <el-icon class="mr-2"><Plus /></el-icon>新建计划模板
                    </el-button>
                </div>

                <div v-if="planSelectedCourseId">
                    <el-table :data="plans" stripe style="width: 100%" v-loading="loadingPlans">
                        <el-table-column prop="title" label="计划标题" />
                        <el-table-column prop="description" label="描述" show-overflow-tooltip />
                        <el-table-column label="时间范围" width="220">
                             <template #default="scope">
                                {{ scope.row.startDate }} ~ {{ scope.row.endDate }}
                             </template>
                        </el-table-column>
                        <el-table-column label="操作" width="150">
                             <template #default="scope">
                                <el-button type="success" size="small" @click="handleDistributePlan(scope.row)">
                                    <el-icon class="mr-1"><Promotion /></el-icon>一键下发
                                </el-button>
                             </template>
                        </el-table-column>
                    </el-table>
                </div>
                <el-empty v-else description="请先选择一个课程进行计划管理" />
            </el-tab-pane>

            <!-- 4. 监控中心 (Monitoring) -->
            <el-tab-pane label="监控中心" name="monitor">
                <div class="filter-section mb-4 flex-between">
                    <div class="flex-start">
                        <span class="mr-2">选择课程:</span>
                        <el-select v-model="monitorSelectedCourseId" placeholder="请选择课程" @change="handleMonitorCourseChange" style="width: 200px">
                            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
                        </el-select>
                        <span class="ml-4 mr-2">日期:</span>
                        <el-date-picker
                            v-model="monitorDate"
                            type="date"
                            placeholder="选择日期"
                            :disabled-date="(time: Date) => time.getTime() > Date.now()"
                            @change="fetchMonitorData"
                        />
                    </div>
                    <el-button type="primary" plain @click="fetchMonitorData">
                        <el-icon class="mr-2"><Refresh /></el-icon>刷新数据
                    </el-button>
                </div>

                <div v-if="monitorSelectedCourseId">
                    <el-row :gutter="20">
                        <!-- Daily Check-in Rate -->
                        <el-col :span="12">
                            <el-card shadow="hover" class="mb-4">
                                <template #header>
                                    <div class="card-header">
                                        <span>当日打卡完成情况</span>
                                    </div>
                                </template>
                                <div ref="dailyChartRef" style="height: 300px;"></div>
                                <div class="text-center mt-2">
                                    <p>应打卡: {{ dailyStats.totalStudents }} 人 | 已打卡: {{ dailyStats.checkedInCount }} 人</p>
                                </div>
                            </el-card>
                        </el-col>

                        <!-- Abnormal Students -->
                        <el-col :span="12">
                            <el-card shadow="hover" class="mb-4" style="height: 400px; overflow-y: auto;">
                                <template #header>
                                    <div class="card-header flex-between">
                                        <span>异常学生 (完成率 < 60%)</span>
                                        <el-tag type="danger" effect="plain">{{ abnormalStudents.length }} 人</el-tag>
                                    </div>
                                </template>
                                <el-table :data="abnormalStudents" stripe style="width: 100%" size="small">
                                    <el-table-column prop="studentName" label="姓名" />
                                    <el-table-column prop="studentNumber" label="学号" />
                                    <el-table-column label="完成率" width="100">
                                        <template #default="scope">
                                            <el-tag type="danger">{{ scope.row.completionRate }}%</el-tag>
                                        </template>
                                    </el-table-column>
                                    <el-table-column prop="completedTasks" label="已完成任务" width="100" />
                                </el-table>
                            </el-card>
                        </el-col>
                    </el-row>

                    <!-- 30-Day Trend -->
                    <el-row>
                        <el-col :span="24">
                            <el-card shadow="hover">
                                <template #header>
                                    <div class="card-header">
                                        <span>近30天打卡率趋势</span>
                                    </div>
                                </template>
                                <div ref="trendChartRef" style="height: 350px;"></div>
                            </el-card>
                        </el-col>
                    </el-row>
                </div>
                <el-empty v-else description="请先选择一个课程查看监控数据" />
            </el-tab-pane>

            <!-- 5. 催学中心 (Reminders) -->
            <el-tab-pane label="催学中心" name="remind">
                <div class="filter-section mb-4 flex-between">
                    <div class="flex-start">
                        <span class="mr-2">选择课程:</span>
                        <el-select v-model="remindSelectedCourseId" placeholder="请选择课程" @change="handleRemindCourseChange" style="width: 200px">
                            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
                        </el-select>
                    </div>
                    <el-button type="warning" :disabled="selectedRemindStudents.length === 0" @click="openRemindDialog">
                        <el-icon class="mr-2"><Bell /></el-icon>一键提醒 ({{ selectedRemindStudents.length }}人)
                    </el-button>
                </div>

                <div v-if="remindSelectedCourseId">
                    <el-alert title="以下学生已超过3天未打卡" type="warning" show-icon class="mb-4" :closable="false" />
                    <el-table 
                        :data="lazyStudents" 
                        stripe 
                        style="width: 100%" 
                        v-loading="loadingRemind"
                        @selection-change="handleRemindSelectionChange"
                    >
                        <el-table-column type="selection" width="55" />
                        <el-table-column prop="username" label="姓名" />
                        <el-table-column prop="email" label="邮箱" />
                        <el-table-column prop="lastCheckinDate" label="上次打卡" width="180">
                             <template #default="scope">
                                {{ formatDate(scope.row.lastCheckinDate) }}
                             </template>
                        </el-table-column>
                        <el-table-column label="状态" width="120">
                             <template #default>
                                <el-tag type="danger">待提醒</el-tag>
                             </template>
                        </el-table-column>
                    </el-table>
                </div>
                <el-empty v-else description="请先选择一个课程进行催学管理" />
            </el-tab-pane>

            <!-- 6. 数据导出 (Export) -->
            <el-tab-pane label="数据导出" name="data">
                <div class="filter-section mb-4 flex-between">
                    <div class="flex-start">
                         <span class="mr-2">选择课程:</span>
                         <el-select v-model="exportSelectedCourseId" placeholder="请选择课程" style="width: 200px">
                            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
                         </el-select>
                    </div>
                    <el-button type="primary" :disabled="!exportSelectedCourseId" @click="handleExport">
                        <el-icon class="mr-2"><Download /></el-icon>导出Excel
                    </el-button>
                </div>
                
                <div v-if="exportSelectedCourseId">
                     <el-empty description="点击上方按钮导出班级数据" image-size="100">
                        <template #image>
                             <el-icon :size="60" color="#909399"><Document /></el-icon>
                        </template>
                     </el-empty>
                </div>
                <el-empty v-else description="请先选择一个课程" />
            </el-tab-pane>

             <!-- 7. 个人中心 (Profile) -->
             <el-tab-pane label="个人中心" name="profile">
                <div class="profile-container" style="max-width: 600px; margin: 0 auto;">
                    <el-card shadow="hover" class="mb-4">
                        <template #header>
                            <div class="card-header">
                                <span>基本资料</span>
                            </div>
                        </template>
                        <el-form :model="profileForm" label-width="100px">
                            <el-form-item label="用户名">
                                <el-input v-model="profileForm.username" disabled />
                            </el-form-item>
                            <el-form-item label="邮箱">
                                <el-input v-model="profileForm.email" />
                            </el-form-item>
                            <el-form-item label="头像URL">
                                <el-input v-model="profileForm.avatar" />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="handleUpdateProfile" :loading="updatingProfile">保存修改</el-button>
                            </el-form-item>
                        </el-form>
                    </el-card>

                    <el-card shadow="hover">
                        <template #header>
                            <div class="card-header">
                                <span>修改密码</span>
                            </div>
                        </template>
                        <el-form :model="passwordForm" label-width="100px">
                            <el-form-item label="旧密码">
                                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                            </el-form-item>
                            <el-form-item label="新密码">
                                <el-input v-model="passwordForm.newPassword" type="password" show-password />
                            </el-form-item>
                            <el-form-item label="确认新密码">
                                <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="danger" @click="handleUpdatePassword" :loading="updatingPassword">修改密码</el-button>
                            </el-form-item>
                        </el-form>
                    </el-card>
                </div>
            </el-tab-pane>
          </el-tabs>
      </div>

      <!-- Create Course Dialog -->
      <el-dialog v-model="createDialogVisible" title="新建课程" width="500px">
          <el-form :model="newCourse" label-width="80px">
              <el-form-item label="课程名称" required>
                  <el-input v-model="newCourse.name" placeholder="例如：Java程序设计" />
              </el-form-item>
              <el-form-item label="所属学期" required>
                  <el-select v-model="newCourse.semester" placeholder="请选择学期" style="width: 100%">
                      <el-option label="2025-2026 第一学期" value="2025-2026-1" />
                      <el-option label="2025-2026 第二学期" value="2025-2026-2" />
                  </el-select>
              </el-form-item>
              <el-form-item label="课程描述">
                  <el-input v-model="newCourse.description" type="textarea" placeholder="简要介绍课程内容" />
              </el-form-item>
          </el-form>
          <template #footer>
              <span class="dialog-footer">
                  <el-button @click="createDialogVisible = false">取消</el-button>
                  <el-button type="primary" @click="handleCreateCourse">生成邀请码并创建</el-button>
              </span>
          </template>
      </el-dialog>

      <!-- Send Reminder Dialog -->
      <el-dialog v-model="remindDialogVisible" title="发送催学提醒" width="500px">
          <el-form :model="remindForm" label-width="80px">
              <el-form-item label="提醒对象">
                  <span>已选择 {{ selectedRemindStudents.length }} 名学生</span>
              </el-form-item>
              <el-form-item label="标题" required>
                  <el-input v-model="remindForm.subject" placeholder="例如：学习打卡提醒" />
              </el-form-item>
              <el-form-item label="内容" required>
                  <el-input 
                    v-model="remindForm.content" 
                    type="textarea" 
                    :rows="4" 
                    placeholder="同学你好，检测到你已多日未打卡，请及时完成学习任务。" 
                  />
              </el-form-item>
          </el-form>
          <template #footer>
              <span class="dialog-footer">
                  <el-button @click="remindDialogVisible = false">取消</el-button>
                  <el-button type="primary" @click="handleSendReminders" :loading="sendingReminders">发送</el-button>
              </span>
          </template>
      </el-dialog>

      <!-- Create Plan Dialog -->
      <el-dialog v-model="createPlanDialogVisible" title="新建计划模板" width="600px">
          <el-form :model="newPlan" label-width="80px">
              <el-form-item label="计划标题" required>
                  <el-input v-model="newPlan.title" placeholder="例如：第一周学习计划" />
              </el-form-item>
              <el-form-item label="计划描述">
                  <el-input v-model="newPlan.description" type="textarea" placeholder="描述本阶段的学习目标" />
              </el-form-item>
              <el-form-item label="开始日期" required>
                  <el-date-picker v-model="newPlan.startDate" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
              <el-form-item label="结束日期" required>
                  <el-date-picker v-model="newPlan.endDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
              
              <el-divider content-position="left">任务列表</el-divider>
              
              <div v-for="(task, index) in newPlan.tasks" :key="index" class="mb-2 flex-between">
                  <span class="truncate flex-1 mr-2">{{ index + 1 }}. {{ task }}</span>
                  <el-button type="danger" circle size="small" @click="removeTaskFromForm(index)">
                      <el-icon><Delete /></el-icon>
                  </el-button>
              </div>
              
              <div class="flex-between mt-2">
                  <el-input v-model="newTaskContent" placeholder="输入任务内容，回车添加" @keyup.enter="addNewTaskToForm" class="mr-2" />
                  <el-button @click="addNewTaskToForm">添加</el-button>
              </div>
          </el-form>
          <template #footer>
              <span class="dialog-footer">
                  <el-button @click="createPlanDialogVisible = false">取消</el-button>
                  <el-button type="primary" @click="handleCreatePlan">创建</el-button>
              </span>
          </template>
      </el-dialog>

    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import TaskManagement from './TaskManagement.vue'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'
import { Coin, Plus, Document, Bell, Promotion, Delete } from '@element-plus/icons-vue'
import { createCourse, getTeacherCourses, getCourseStudentDetails, removeStudent, banStudent, remindStudents, checkStudentsToRemind, sendBatchReminders } from '../../api/course'
import { exportCourseData } from '../../api/export'
import { getCoursePlans, createPlan, distributePlan, addTask } from '../../api/plan'
import { getDailyStats, getCompletionTrend } from '../../api/statistics'
import { updateProfile, updatePassword } from '../../api/user'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

const activeTab = ref('course')
const courses = ref<any[]>([])
const createDialogVisible = ref(false)

// Class Management State
const selectedCourseId = ref<number | null>(null)
const students = ref<any[]>([])
const loadingStudents = ref(false)
const studentSearch = ref('')

// Plan Management State
const planSelectedCourseId = ref<number | null>(null)
const plans = ref<any[]>([])
const loadingPlans = ref(false)
const createPlanDialogVisible = ref(false)
const newPlan = ref({
    courseId: null as number | null,
    title: '',
    description: '',
    startDate: '',
    endDate: '',
    tasks: [] as string[]
})
const newTaskContent = ref('')

const newCourse = ref({
    name: '',
    semester: '',
    description: ''
})

const loadCourses = async () => {
    if (!userStore.user) return
    try {
        const res: any = await getTeacherCourses(userStore.user.id)
        courses.value = res.data
    } catch (error) {
        console.error(error)
    }
}

const handleCreateCourse = async () => {
    if (!newCourse.value.name || !newCourse.value.semester) {
        ElMessage.warning('请填写完整课程信息')
        return
    }
    if (!userStore.user) return

    try {
        await createCourse(userStore.user.id, newCourse.value.name, newCourse.value.description, newCourse.value.semester)
        ElMessage.success('创建成功，邀请码已生成')
        createDialogVisible.value = false
        newCourse.value = { name: '', semester: '', description: '' }
        loadCourses()
    } catch (error: any) {
        ElMessage.error(error.message || '创建失败')
    }
}

const switchToClassTab = (course: any) => {
    activeTab.value = 'class'
    selectedCourseId.value = course.id
    handleCourseChange(course.id)
}

const handleCourseChange = async (courseId: number) => {
    if (!courseId) return
    loadingStudents.value = true
    try {
        const res: any = await getCourseStudentDetails(courseId)
        students.value = res.data || []
    } catch (error) {
        ElMessage.error('获取学生列表失败')
        students.value = []
    } finally {
        loadingStudents.value = false
    }
}

const filteredStudents = computed(() => {
    if (!studentSearch.value) return students.value
    const keyword = studentSearch.value.toLowerCase()
    return students.value.filter(s => 
        s.username.toLowerCase().includes(keyword) || 
        (s.email && s.email.toLowerCase().includes(keyword))
    )
})

const handleRemove = async (student: any) => {
    try {
        await ElMessageBox.confirm(
            `确定要将学生 "${student.username}" 移出班级吗？该学生将失去课程访问权限。`,
            '确认移除',
            {
                confirmButtonText: '移除',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        await removeStudent(selectedCourseId.value!, student.id)
        ElMessage.success('已移除')
        handleCourseChange(selectedCourseId.value!)
    } catch (error: any) {
        if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
    }
}

const handleBan = async (student: any) => {
    try {
        await ElMessageBox.confirm(
            `确定要禁止学生 "${student.username}" 再次加入吗？`,
            '确认禁入',
            {
                confirmButtonText: '禁入',
                cancelButtonText: '取消',
                type: 'error',
            }
        )
        await banStudent(selectedCourseId.value!, student.id)
        ElMessage.success('已设置禁入')
        handleCourseChange(selectedCourseId.value!)
    } catch (error: any) {
         if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
    }
}

const handleRemind = async (course: any) => {
    if (!course) return
    try {
        await ElMessageBox.confirm(
            `确定要给 "${course.name}" 的所有学生发送打卡提醒吗？`,
            '发送提醒',
            {
                confirmButtonText: '发送',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        await remindStudents(course.id)
        ElMessage.success('提醒发送成功')
    } catch (error: any) {
        if (error !== 'cancel') {
             ElMessage.error(error.message || '发送失败')
        }
    }
}

// --- Plan Management ---

const handlePlanCourseChange = async (courseId: number) => {
    if (!courseId) return
    loadingPlans.value = true
    try {
        const res: any = await getCoursePlans(courseId, userStore.user?.id)
        // Fetch tasks for each plan (optional, maybe just show count)
        // For now, let's just show plans
        plans.value = res.data || []
    } catch (error) {
        ElMessage.error('获取计划列表失败')
    } finally {
        loadingPlans.value = false
    }
}

const handleCreatePlan = async () => {
    if (!newPlan.value.title || !newPlan.value.courseId) {
        ElMessage.warning('请填写完整计划信息')
        return
    }
    
    try {
        // 1. Create Plan
        const planRes: any = await createPlan({
            courseId: newPlan.value.courseId,
            userId: userStore.user?.id,
            title: newPlan.value.title,
            description: newPlan.value.description,
            startDate: newPlan.value.startDate,
            endDate: newPlan.value.endDate
        })
        const planId = planRes.data.id
        
        // 2. Add Tasks
        for (const taskContent of newPlan.value.tasks) {
             await addTask(planId, taskContent)
        }
        
        ElMessage.success('计划创建成功')
        createPlanDialogVisible.value = false
        handlePlanCourseChange(newPlan.value.courseId!)
        
        // Reset
        newPlan.value.title = ''
        newPlan.value.description = ''
        newPlan.value.startDate = ''
        newPlan.value.endDate = ''
        newPlan.value.tasks = []
    } catch (error: any) {
        ElMessage.error(error.message || '创建失败')
    }
}

const addNewTaskToForm = () => {
    if (newTaskContent.value.trim()) {
        newPlan.value.tasks.push(newTaskContent.value.trim())
        newTaskContent.value = ''
    }
}

const removeTaskFromForm = (index: number) => {
    newPlan.value.tasks.splice(index, 1)
}

const handleDistributePlan = async (plan: any) => {
    try {
        await ElMessageBox.confirm(
            `确定要将计划 "${plan.title}" 下发给班级所有学生吗？`,
            '确认下发',
            {
                confirmButtonText: '立即下发',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        await distributePlan(plan.id, plan.courseId)
        ElMessage.success('下发成功，学生端已更新')
    } catch (error: any) {
        if (error !== 'cancel') ElMessage.error(error.message || '下发失败')
    }
}

const openCreatePlanDialog = () => {
    if (!planSelectedCourseId.value) {
        ElMessage.warning('请先选择一个课程')
        return
    }
    newPlan.value.courseId = planSelectedCourseId.value
    createPlanDialogVisible.value = true
}

// --- Monitoring ---
const monitorSelectedCourseId = ref<number | null>(null)
const monitorDate = ref(new Date())
const dailyStats = ref({
    totalStudents: 0,
    checkedInCount: 0,
    checkinRate: 0,
    abnormalStudents: []
})
const abnormalStudents = ref<any[]>([])

const dailyChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let dailyChartInstance: echarts.ECharts | null = null
let trendChartInstance: echarts.ECharts | null = null

const handleMonitorCourseChange = async () => {
    if (monitorSelectedCourseId.value) {
        await fetchMonitorData()
    }
}

const fetchMonitorData = async () => {
    if (!monitorSelectedCourseId.value) return
    
    try {
        // 1. Daily Stats
        const dateStr = monitorDate.value.toISOString().split('T')[0]
        const dailyRes: any = await getDailyStats(monitorSelectedCourseId.value, dateStr)
        dailyStats.value = dailyRes.data
        abnormalStudents.value = dailyRes.data.abnormalStudents || []
        
        initDailyChart(dailyStats.value.checkinRate)

        // 2. Trend Stats
        const trendRes: any = await getCompletionTrend(monitorSelectedCourseId.value, 30)
        initTrendChart(trendRes.data)

    } catch (error) {
        console.error(error)
        ElMessage.error('获取监控数据失败')
    }
}

const initDailyChart = (rate: number) => {
    if (!dailyChartRef.value) return
    if (dailyChartInstance) dailyChartInstance.dispose()
    
    dailyChartInstance = echarts.init(dailyChartRef.value)
    const option = {
        tooltip: {
            formatter: '{a} <br/>{b} : {c}%'
        },
        series: [
            {
                name: '打卡率',
                type: 'gauge',
                progress: {
                    show: true
                },
                detail: {
                    valueAnimation: true,
                    formatter: '{value}%'
                },
                data: [
                    {
                        value: rate,
                        name: '完成率'
                    }
                ]
            }
        ]
    }
    dailyChartInstance.setOption(option)
}

const initTrendChart = (data: any[]) => {
    if (!trendChartRef.value) return
    if (trendChartInstance) trendChartInstance.dispose()

    trendChartInstance = echarts.init(trendChartRef.value)
    
    const dates = data.map(item => item.date)
    const rates = data.map(item => item.rate)

    const option = {
        tooltip: {
            trigger: 'axis'
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates
        },
        yAxis: {
            type: 'value',
            max: 100
        },
        series: [
            {
                name: '打卡率',
                type: 'line',
                stack: 'Total',
                areaStyle: {},
                emphasis: {
                    focus: 'series'
                },
                data: rates,
                smooth: true
            }
        ]
    }
    trendChartInstance.setOption(option)
}

// --- Reminders ---
const remindSelectedCourseId = ref<number | null>(null)
const lazyStudents = ref<any[]>([])
const loadingRemind = ref(false)
const selectedRemindStudents = ref<any[]>([])
const remindDialogVisible = ref(false)
const sendingReminders = ref(false)
const remindForm = ref({
    subject: '学习打卡提醒',
    content: ''
})

const handleRemindCourseChange = async () => {
    if (!remindSelectedCourseId.value) return
    loadingRemind.value = true
    try {
        const res: any = await checkStudentsToRemind(remindSelectedCourseId.value)
        lazyStudents.value = res.data || []
    } catch (error) {
        ElMessage.error('获取待提醒学生失败')
    } finally {
        loadingRemind.value = false
    }
}

const handleRemindSelectionChange = (val: any[]) => {
    selectedRemindStudents.value = val
}

const openRemindDialog = () => {
    if (selectedRemindStudents.value.length === 0) return
    remindForm.value.content = `同学你好，系统检测到你已连续多日未在课程中进行打卡。为了不影响你的平时成绩，请尽快完成学习任务并打卡。`
    remindDialogVisible.value = true
}

const handleSendReminders = async () => {
    if (!remindForm.value.subject || !remindForm.value.content) {
        ElMessage.warning('请填写完整提醒内容')
        return
    }
    
    sendingReminders.value = true
    try {
        const studentIds = selectedRemindStudents.value.map(s => s.id)
        await sendBatchReminders(
            remindSelectedCourseId.value!, 
            remindForm.value.subject, 
            remindForm.value.content,
            studentIds
        )
        ElMessage.success('发送成功')
        remindDialogVisible.value = false
        handleRemindCourseChange() // Refresh list
    } catch (error: any) {
        ElMessage.error(error.message || '发送失败')
    } finally {
        sendingReminders.value = false
    }
}

// --- Export ---
const exportSelectedCourseId = ref<number | null>(null)

const handleExport = async () => {
    if (!exportSelectedCourseId.value) return
    try {
        const res: any = await exportCourseData(exportSelectedCourseId.value)
        // Create a link and download
        const url = window.URL.createObjectURL(new Blob([res]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `课程数据_${exportSelectedCourseId.value}.xlsx`)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        ElMessage.success('导出成功')
    } catch (error) {
        ElMessage.error('导出失败')
    }
}

// --- Profile ---
const profileForm = ref({
    username: '',
    email: '',
    avatar: ''
})
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})
const updatingProfile = ref(false)
const updatingPassword = ref(false)

const initProfile = () => {
    if (userStore.user) {
        profileForm.value.username = userStore.user.username
        profileForm.value.email = userStore.user.email || ''
        profileForm.value.avatar = userStore.user.avatar || ''
    }
}

const handleUpdateProfile = async () => {
    if (!userStore.user) return
    updatingProfile.value = true
    try {
        const res: any = await updateProfile({
            id: userStore.user.id,
            email: profileForm.value.email,
            avatar: profileForm.value.avatar
        })
        userStore.setUser(res.data) // Assuming store has setUser or just user = res.data if reactive
        ElMessage.success('资料更新成功')
    } catch (error: any) {
        ElMessage.error(error.message || '更新失败')
    } finally {
        updatingProfile.value = false
    }
}

const handleUpdatePassword = async () => {
    if (!userStore.user) return
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致')
        return
    }
    if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
        ElMessage.warning('请填写完整密码信息')
        return
    }
    
    updatingPassword.value = true
    try {
        await updatePassword({
            id: userStore.user.id,
            oldPassword: passwordForm.value.oldPassword,
            newPassword: passwordForm.value.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')
        handleLogout()
    } catch (error: any) {
        ElMessage.error(error.message || '修改失败')
    } finally {
        updatingPassword.value = false
    }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const formatDate = (dateStr: string) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString()
}

onMounted(() => {
    loadCourses()
    initProfile()
    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (dailyChartInstance) dailyChartInstance.dispose()
    if (trendChartInstance) trendChartInstance.dispose()
})

const handleResize = () => {
    if (dailyChartInstance) dailyChartInstance.resize()
    if (trendChartInstance) trendChartInstance.resize()
}
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background-color: var(--bg-color-base);
}

.teacher-dashboard {
  padding: 0;
}

/* Header Section */
.teacher-dashboard > .flex-between {
  margin-bottom: 24px;
  padding: 24px 0;
}

.teacher-dashboard > .flex-between h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  background: var(--teacher-theme);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.teacher-dashboard > .flex-between p {
  margin: 0;
  font-size: 16px;
  color: var(--text-secondary);
}

/* Tabs */
.teacher-tabs {
  background: var(--bg-color-white);
  border-radius: 12px;
  box-shadow: 0 4px 20px var(--shadow-color-light);
  border: none !important;
  overflow: hidden;
}

.teacher-tabs :deep(.el-tabs__header) {
  background: var(--bg-color-card);
  border-bottom: 1px solid var(--border-color-lighter);
  padding: 0 24px;
}

.teacher-tabs :deep(.el-tabs__nav) {
  margin: 0;
  height: 60px;
}

.teacher-tabs :deep(.el-tabs__item) {
  height: 60px;
  line-height: 60px;
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-regular);
  transition: all 0.3s ease;
}

.teacher-tabs :deep(.el-tabs__item:hover) {
  color: var(--primary-color);
  background: rgba(64, 158, 255, 0.05);
}

.teacher-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
  background: var(--bg-color-white);
  border-bottom: 2px solid var(--primary-color);
  font-weight: 600;
}

.teacher-tabs :deep(.el-tabs__content) {
  padding: 24px;
}

/* Tab Actions */
.tab-actions {
  margin-bottom: 24px;
  display: flex;
  gap: 12px;
  align-items: center;
}

/* Filter Section */
.filter-section {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-color-card);
  border-radius: 12px;
  border: 1px solid var(--border-color-lighter);
}

.filter-section .flex-start {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-section span {
  font-weight: 500;
  color: var(--text-primary);
}

/* Card Styles */
.course-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  background: var(--bg-color-card) !important;
}

.course-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px var(--shadow-color-base) !important;
}

.course-card :deep(.el-card__header) {
  background: var(--bg-color-white) !important;
  border-bottom: 1px solid var(--border-color-lighter) !important;
  padding: 20px !important;
}

.course-card :deep(.el-card__body) {
  padding: 24px !important;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-title {
  font-weight: 600;
  font-size: 18px;
  color: var(--text-primary);
  margin: 0;
}

.course-info p {
  margin: 8px 0;
  color: var(--text-regular);
  font-size: 14px;
}

.course-info strong {
  color: var(--text-primary);
  font-weight: 500;
}

.description {
  min-height: 48px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 16px !important;
}

.course-footer {
  margin-top: auto;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

/* Table Styles */
.teacher-dashboard :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px var(--shadow-color-light);
  background: var(--bg-color-white);
}

.teacher-dashboard :deep(.el-table__header-wrapper) {
  background: var(--bg-color-card);
}

.teacher-dashboard :deep(.el-table__header th) {
  background: var(--bg-color-card);
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
  border-bottom: 2px solid var(--border-color-lighter);
}

.teacher-dashboard :deep(.el-table__body tr) {
  transition: all 0.3s ease;
}

.teacher-dashboard :deep(.el-table__body tr:hover) {
  background-color: rgba(64, 158, 255, 0.05) !important;
  transform: translateX(4px);
}

.teacher-dashboard :deep(.el-table__body td) {
  font-size: 14px;
  color: var(--text-regular);
  border-bottom: 1px solid var(--border-color-lighter);
}

.teacher-dashboard :deep(.el-table__empty-block) {
  padding: 60px 20px;
}

.teacher-dashboard :deep(.el-table__empty-text) {
  font-size: 16px;
  color: var(--text-secondary);
}

/* Dialog Styles */
.teacher-dashboard :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.teacher-dashboard :deep(.el-dialog__header) {
  background: var(--bg-color-card);
  border-bottom: 1px solid var(--border-color-lighter);
  padding: 24px !important;
}

.teacher-dashboard :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.teacher-dashboard :deep(.el-dialog__body) {
  padding: 24px !important;
}

.teacher-dashboard :deep(.el-dialog__footer) {
  background: var(--bg-color-card);
  border-top: 1px solid var(--border-color-lighter);
  padding: 16px 24px !important;
}

/* Form Styles */
.teacher-dashboard :deep(.el-form-item) {
  margin-bottom: 20px;
}

.teacher-dashboard :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
}

.teacher-dashboard :deep(.el-input__wrapper),
.teacher-dashboard :deep(.el-select__wrapper) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.teacher-dashboard :deep(.el-input__wrapper:hover),
.teacher-dashboard :deep(.el-select__wrapper:hover) {
  border-color: var(--primary-color-light);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.teacher-dashboard :deep(.el-input__wrapper.is-focus),
.teacher-dashboard :deep(.el-select__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* Statistics Cards */
.teacher-dashboard :deep(.el-card) {
  border-radius: 12px;
  box-shadow: 0 2px 12px var(--shadow-color-light);
  border: 1px solid var(--border-color-lighter);
  transition: all 0.3s ease;
}

.teacher-dashboard :deep(.el-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow-color-base);
}

.teacher-dashboard :deep(.el-card__header) {
  background: var(--bg-color-card);
  border-bottom: 1px solid var(--border-color-lighter);
  padding: 20px !important;
}

.teacher-dashboard :deep(.el-card__body) {
  padding: 24px !important;
}

/* Profile Section */
.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

/* Responsive Design */
@media (max-width: 768px) {
  .teacher-tabs :deep(.el-tabs__header) {
    padding: 0;
  }
  
  .teacher-tabs :deep(.el-tabs__nav) {
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
  }
  
  .teacher-tabs :deep(.el-tabs__content) {
    padding: 16px;
  }
  
  .filter-section {
    padding: 16px;
  }
  
  .filter-section .flex-start {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .filter-section .flex-start .el-select,
  .filter-section .flex-start .el-date-picker {
    width: 100%;
  }
  
  .course-footer {
    flex-direction: column;
    gap: 8px;
  }
  
  .course-footer .el-button {
    width: 100%;
  }
}

/* Chart Containers */
.teacher-dashboard .mb-4 {
  margin-bottom: 24px;
}
</style>
