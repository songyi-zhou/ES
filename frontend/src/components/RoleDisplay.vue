<template>
  <el-tag v-if="displayRole" :type="tagType" size="small">{{ displayRole }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  role: string;
}>();

const displayRole = computed(() => {
  const roleMap: { [key: string]: string } = {
    'GROUP_MEMBER': '小组成员',
    'groupleader': '综测负责人',
    'user': ''
  };
  return roleMap[props.role] || '';
});

const tagType = computed(() => {
  const typeMap: { [key: string]: string } = {
    'GROUP_MEMBER': 'success',
    'groupleader': 'warning'
  };
  return typeMap[props.role] || '';
});
</script> 