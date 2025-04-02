import request from '@/utils/request'

export const getGroupMembers = () => {
  return request({
    url: '/group-members',
    method: 'get'
  })
} 