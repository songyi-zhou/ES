import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    userId: null,
    name: null,
    roleLevel: null
  }),
  persist: true,  // 持久化存储
  actions: {
    setUserInfo({ token, userId, name, roleLevel }) {
      this.token = token
      this.userId = userId
      this.name = name
      this.roleLevel = roleLevel
      
      // 可选：保存到 localStorage
      localStorage.setItem('userInfo', JSON.stringify({ token, userId, name, roleLevel }))
    },

    clearUserInfo() {
      this.token = null
      this.userId = null
      this.name = null
      this.roleLevel = null
      
      localStorage.removeItem('userInfo')
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    userRole: (state) => {
      switch (state.roleLevel) {
        case 4: return 'admin'
        case 3: return 'counselor'
        case 2: return 'leader'
        case 1: return 'member'
        default: return 'student'
      }
    }
  }
}) 