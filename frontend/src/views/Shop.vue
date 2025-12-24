<template>
  <div class="shop-container">
    <el-header class="main-header">
      <div class="header-content container">
        <div class="logo-section">
            <h1 @click="router.push('/')" style="cursor: pointer;">学习打卡系统</h1>
        </div>
        <div class="header-actions">
            <div class="user-info" v-if="userStore.user">
                <span class="points-badge">
                    <el-icon><Coin /></el-icon>
                    {{ userStore.user.points }}
                </span>
            </div>
            <el-button @click="router.push('/')">返回仪表盘</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="container">
      <div class="page-header mb-4 flex-between">
          <h2>积分商城</h2>
          <div class="header-controls">
              <el-input
                  v-model="searchQuery"
                  placeholder="搜索商品..."
                  prefix-icon="Search"
                  class="search-input mr-2"
                  clearable
              />
              <el-button type="info" @click="showHistory">
                  <el-icon class="el-icon--left"><List /></el-icon>兑换记录
              </el-button>
          </div>
      </div>

      <el-tabs v-model="activeTab" class="mb-4">
        <el-tab-pane label="全部" name="ALL"></el-tab-pane>
        <el-tab-pane label="头像框" name="AVATAR_FRAME"></el-tab-pane>
        <el-tab-pane label="皮肤" name="SKIN"></el-tab-pane>
        <el-tab-pane label="实物" name="PHYSICAL"></el-tab-pane>
        <el-tab-pane label="优惠券" name="COUPON"></el-tab-pane>
      </el-tabs>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="product in filteredProducts" :key="product.id" class="mb-4">
          <el-card :body-style="{ padding: '0px' }" class="product-card h-100" shadow="hover">
            <div class="image-container">
                 <img :src="product.imageUrl" class="image" />
                 <div class="product-type-tag">
                     <el-tag size="small" effect="dark">{{ getTypeName(product.type) }}</el-tag>
                 </div>
            </div>
            <div style="padding: 14px">
              <span class="product-title">{{ product.name }}</span>
              <div class="bottom">
                <span class="price">{{ product.price }} 积分</span>
                <el-button 
                    type="primary" 
                    class="button" 
                    size="small" 
                    @click="handleExchange(product)"
                    :disabled="userStore.user && userStore.user.points < product.price"
                >
                    {{ userStore.user && userStore.user.points < product.price ? '积分不足' : '立即兑换' }}
                </el-button>
              </div>
              <div class="desc">{{ product.description }}</div>
              <div class="stock mt-2 text-muted small">库存: {{ product.stock }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-empty v-if="filteredProducts.length === 0" description="暂无相关商品" />

      <!-- History Dialog -->
      <el-dialog v-model="historyVisible" title="兑换记录" width="700px">
          <el-table :data="exchangeHistory" style="width: 100%" stripe empty-text="暂无兑换记录">
              <el-table-column label="商品名称">
                  <template #default="scope">
                      {{ getProductName(scope.row.productId) }}
                  </template>
              </el-table-column>
              <el-table-column prop="price" label="消耗积分" width="100">
                  <template #default="scope">
                      <span class="text-danger">-{{ scope.row.price }}</span>
                  </template>
              </el-table-column>
              <el-table-column prop="createTime" label="兑换时间" width="160">
                   <template #default="scope">
                       {{ formatTime(scope.row.createTime) }}
                   </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'info' : 'success'">
                        {{ scope.row.status === 1 ? '已退款' : '成功' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
                <template #default="scope">
                    <el-button 
                        v-if="scope.row.status !== 1" 
                        type="danger" 
                        size="small" 
                        link 
                        @click="handleRefund(scope.row)"
                    >
                        退款
                    </el-button>
                </template>
            </el-table-column>
          </el-table>
      </el-dialog>
    </el-main>
    <el-dialog v-model="shippingDialogVisible" title="填写收货信息" width="500px">
        <el-form :model="shippingForm" label-width="100px">
            <el-form-item label="收货人姓名">
                <el-input v-model="shippingForm.name" placeholder="请输入收货人姓名" />
            </el-form-item>
            <el-form-item label="联系电话">
                <el-input v-model="shippingForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="收货地址">
                <el-input v-model="shippingForm.address" type="textarea" placeholder="请输入详细收货地址" />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="shippingDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmShippingExchange">确认兑换</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getProducts, exchangeProduct, getExchangeHistory, refundProduct } from '../api/shop'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Coin, Search, List } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const products = ref<any[]>([])
const searchQuery = ref('')
const activeTab = ref('ALL')
const historyVisible = ref(false)
const exchangeHistory = ref<any[]>([])

const shippingDialogVisible = ref(false)
const shippingForm = ref({
    name: '',
    phone: '',
    address: ''
})
const currentExchangeProduct = ref<any>(null)

const loadProducts = async () => {
    try {
        const res: any = await getProducts()
        products.value = res.data
    } catch (error) {
        console.error(error)
    }
}

const filteredProducts = computed(() => {
    let result = products.value
    if (activeTab.value !== 'ALL') {
        result = result.filter(p => p.type === activeTab.value)
    }
    if (searchQuery.value) {
        result = result.filter(p => p.name.toLowerCase().includes(searchQuery.value.toLowerCase()))
    }
    return result
})

const getTypeName = (type: string) => {
    const map: any = {
        'AVATAR_FRAME': '头像框',
        'SKIN': '皮肤',
        'PHYSICAL': '实物',
        'REAL_GOODS': '实物', // Keep for compatibility if needed
        'COUPON': '优惠券',
        'VIRTUAL': '虚拟'
    }
    return map[type] || type
}

onMounted(() => {
    loadProducts()
})

const handleExchange = async (product: any) => {
    if (!userStore.user) return
    
    // Handle Physical Items
    if (product.type === 'PHYSICAL' || product.type === 'REAL_GOODS') {
        currentExchangeProduct.value = product
        shippingDialogVisible.value = true
        return
    }

    try {
        await ElMessageBox.confirm(
            `确定要消耗 ${product.price} 积分兑换 "${product.name}" 吗？`,
            '兑换确认',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )

        await exchangeProduct(userStore.user.id, product.id)
        ElMessage.success(`成功兑换 ${product.name}！`)
        // Update local points and stock
        userStore.user.points -= product.price
        product.stock -= 1
    } catch (error: any) {
        if (error !== 'cancel') {
             ElMessage.error(error.message || '兑换失败')
        }
    }
}

const confirmShippingExchange = async () => {
    if (!shippingForm.value.name || !shippingForm.value.phone || !shippingForm.value.address) {
        ElMessage.warning('请填写完整收货信息')
        return
    }
    
    try {
        await exchangeProduct(userStore.user!.id, currentExchangeProduct.value.id, shippingForm.value)
        ElMessage.success(`成功兑换 ${currentExchangeProduct.value.name}！`)
        // Update local points and stock
        if (userStore.user) {
            userStore.user.points -= currentExchangeProduct.value.price
        }
        if (currentExchangeProduct.value) {
            currentExchangeProduct.value.stock -= 1
        }
        shippingDialogVisible.value = false
        // clear form
        shippingForm.value = { name: '', phone: '', address: '' }
    } catch (error: any) {
        ElMessage.error(error.message || '兑换失败')
    }
}

const showHistory = async () => {
    if (!userStore.user) return
    historyVisible.value = true
    loadHistory()
}

const loadHistory = async () => {
    if (!userStore.user) return
    try {
        const res: any = await getExchangeHistory(userStore.user.id)
        exchangeHistory.value = res.data
    } catch (error) {
        console.error(error)
    }
}

const handleRefund = async (order: any) => {
    try {
        await ElMessageBox.confirm(
            '确定要申请退款吗？积分将退回账户。',
            '退款确认',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )

        await refundProduct(order.id)
        ElMessage.success('退款成功！积分已退回')
        
        // Update local points
        if (userStore.user) {
            userStore.user.points += order.price
        }
        
        // Reload history and products (to update stock)
        loadHistory()
        loadProducts()
        
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '退款失败')
        }
    }
}

const getProductName = (productId: number) => {
    const product = products.value.find(p => p.id === productId)
    return product ? product.name : '未知商品 (ID: ' + productId + ')'
}

const formatTime = (time: string) => {
    return dayjs(time).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.flex-between {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.header-controls {
    display: flex;
    align-items: center;
}
.search-input {
    width: 200px;
}
.product-card {
    transition: all 0.3s;
    position: relative;
}
.image-container {
    width: 100%;
    height: 200px;
    overflow: hidden;
    background: #f5f7fa;
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
}
.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.product-card:hover .image {
    transform: scale(1.05);
}
.product-type-tag {
    position: absolute;
    top: 10px;
    right: 10px;
}
.product-title {
    font-weight: 500;
    font-size: 16px;
    color: var(--text-primary);
    display: block;
    margin-bottom: 8px;
}
.bottom {
  margin-top: 13px;
  line-height: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.price {
    color: #f56c6c;
    font-weight: bold;
}
.desc {
    margin-top: 10px;
    font-size: 13px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}
</style>
