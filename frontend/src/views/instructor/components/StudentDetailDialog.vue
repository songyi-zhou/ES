<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  visible: Boolean,
  student: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible'])

// 使用计算属性处理对话框的显示状态
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="dialogVisible"
    @update:model-value="dialogVisible = $event"
    title="学生详情"
    width="500px"
    @close="handleClose"
  >
    <div class="student-info">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学号">{{ student.userId }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ student.name }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ student.className }}</el-descriptions-item>
        <el-descriptions-item label="中队">{{ student.squad }}</el-descriptions-item>
        <el-descriptions-item label="当前角色">{{ student.role }}</el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ student.assignTime }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>
</template>

<style scoped>
.student-info {
  padding: 20px;
}
</style> 