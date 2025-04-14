import axios from 'axios';

export const loadImage = async (url) => {
  try {
    const token = localStorage.getItem('token');
    const response = await axios.get(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      responseType: 'blob'
    });
    return URL.createObjectURL(response.data);
  } catch (error) {
    console.error('加载图片失败:', error);
    return null;
  }
};

export const getImageUrl = (fileName, defaultImage) => {
  if (!fileName) return defaultImage;
  return `/api/files/${fileName}`;
}; 