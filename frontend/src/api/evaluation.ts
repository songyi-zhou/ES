import request from '@/utils/request';

export function getReviewMaterials() {
  return request({
    url: '/api/evaluation/review-materials',
    method: 'get'
  });
} 

