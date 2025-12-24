import request from '../utils/request'

export const getDailyStats = (courseId: number, date?: string) => {
    return request({
        url: '/statistics/daily',
        method: 'get',
        params: { courseId, date }
    })
}

export const getCompletionTrend = (courseId: number, days: number = 30) => {
    return request({
        url: '/statistics/trend',
        method: 'get',
        params: { courseId, days }
    })
}
