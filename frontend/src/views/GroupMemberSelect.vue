<template>
  <div>
    <a-table :columns="columns" :data-source="dataSource">
      <template #role="{ text }">
        {{ roleMap[text] || '' }}
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const roleMap = {
  'groupmember': '小组成员',
  'groupleader': '综测负责人',
  'user': ''
};

const columns = [
  {
    title: '学号',
    dataIndex: 'studentId',
    key: 'studentId',
  },
  {
    title: '姓名',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '专业',
    dataIndex: 'major',
    key: 'major',
  },
  {
    title: '班级',
    dataIndex: 'classId',
    key: 'classId',
  },
  {
    title: '状态',
    dataIndex: 'role',
    key: 'role',
    slots: { customRender: 'role' }
  }
];
</script>