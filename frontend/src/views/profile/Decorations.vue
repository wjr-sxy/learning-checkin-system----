<template>
  <div class="decorations-view">
    <h2 class="section-title">我的装扮</h2>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="头像框" name="avatarFrame">
        <div class="decoration-grid">
            <div v-if="avatarFrames.length === 0" class="empty-tip">暂无头像框，去商城看看吧</div>
            <div 
                v-for="item in avatarFrames" 
                :key="item.id" 
                class="decoration-item"
                :class="{ active: isEquipped('avatarFrame', item) }"
                @click="handleEquip('avatarFrame', item)"
            >
                <div class="img-wrapper">
                    <img :src="item.imageUrl || '/default-frame.png'" alt="frame" />
                </div>
                <div class="item-name">{{ item.name }}</div>
                <div class="item-status" v-if="isEquipped('avatarFrame', item)">当前使用</div>
            </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="皮肤" name="skin">
        <div class="decoration-grid">
            <div v-if="skins.length === 0" class="empty-tip">暂无皮肤，去商城看看吧</div>
             <div 
                v-for="item in skins" 
                :key="item.id" 
                class="decoration-item"
                :class="{ active: isEquipped('skin', item) }"
                @click="handleEquip('skin', item)"
            >
                <div class="img-wrapper">
                     <img :src="item.imageUrl || '/default-skin.png'" alt="skin" />
                </div>
                <div class="item-name">{{ item.name }}</div>
                <div class="item-status" v-if="isEquipped('skin', item)">当前使用</div>
            </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="勋章" name="badge">
        <div class="decoration-grid">
             <div v-if="badges.length === 0" class="empty-tip">暂无勋章</div>
             <div 
                v-for="item in badges" 
                :key="item.id" 
                class="decoration-item"
                :class="{ active: isEquipped('badge', item) }"
                @click="handleEquip('badge', item)"
            >
                <div class="img-wrapper">
                     <img :src="item.imageUrl || '/default-badge.png'" alt="badge" />
                </div>
                <div class="item-name">{{ item.name }}</div>
                <div class="item-status" v-if="isEquipped('badge', item)">当前使用</div>
            </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <div class="shop-link">
        <el-button type="primary" plain @click="openShop">前往商城获取更多装扮</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useUserStore } from '../../stores/user'
import { getOwnedProducts } from '../../api/shop'
import { equipDecoration } from '../../api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('avatarFrame')
const ownedItems = ref<any[]>([])
const loading = ref(false)

const fetchOwnedItems = async () => {
  if (!userStore.user) return
  loading.value = true
  try {
    const res: any = await getOwnedProducts(userStore.user.id)
    ownedItems.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(() => userStore.user, (newUser) => {
  if (newUser) {
    fetchOwnedItems()
  }
}, { immediate: true })

const avatarFrames = computed(() => ownedItems.value.filter(item => item.type === 'AVATAR_FRAME'))
const skins = computed(() => ownedItems.value.filter(item => item.type === 'SKIN'))
const badges = computed(() => ownedItems.value.filter(item => item.type === 'BADGE'))

const isEquipped = (type: string, item: any) => {
    if (!userStore.user) return false
    if (type === 'avatarFrame') return userStore.user.currentAvatarFrame === item.imageUrl
    if (type === 'skin') return userStore.user.currentSkin === item.imageUrl
    if (type === 'badge') return userStore.user.currentBadge === item.imageUrl
    return false
}

const handleEquip = async (type: string, item: any) => {
    if (!userStore.user) return
    
    try {
        const res: any = await equipDecoration({
            userId: userStore.user.id,
            type: type,
            value: item.imageUrl
        })
        userStore.setUser(res.data)
        ElMessage.success(`已更换${item.name}`)
    } catch (error) {
        console.error(error)
        ElMessage.error('更换失败')
    }
}

const openShop = () => {
    window.open('/shop', '_blank')
}
</script>

<style scoped>
.section-title {
  margin-bottom: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.decoration-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 20px;
    padding: 20px 0;
}

.decoration-item {
    border: 1px solid #eee;
    border-radius: 8px;
    padding: 10px;
    cursor: pointer;
    text-align: center;
    transition: all 0.3s;
    position: relative;
}

.decoration-item:hover {
    box-shadow: 0 2px 12px rgba(0,0,0,0.1);
    transform: translateY(-2px);
}

.decoration-item.active {
    border: 2px solid #FFD700; /* Gold border */
    background-color: #fffbf0;
}

.img-wrapper {
    width: 80px;
    height: 80px;
    margin: 0 auto 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    border-radius: 4px;
}

.img-wrapper img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

.item-name {
    font-size: 14px;
    color: #606266;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.item-status {
    font-size: 12px;
    color: #E6A23C;
    margin-top: 5px;
    font-weight: bold;
}

.empty-tip {
    grid-column: 1 / -1;
    text-align: center;
    color: #909399;
    padding: 40px 0;
}

.shop-link {
    margin-top: 30px;
    text-align: center;
}
</style>
