import type {
  InspirationKeyword,
  InspirationKeywordDto,
  InspirationResponse,
  Video,
  VideoDto,
} from '../types/inspiration'
import { request } from '../utils/request'

function toVideo(dto: VideoDto): Video {
  return {
    id: dto._id,
    author: dto.author,
    fans: dto.fans,
    duration: dto.duration,
    publishDate: dto.publishDate,
    title: dto.title,
    likes: dto.likes,
    coverUrl: dto.coverUrl,
    videoUrl: dto.videoUrl,
    lowFansHighLikes: dto.lowFansHighLikes,
  }
}

function toKeyword(dto: InspirationKeywordDto): InspirationKeyword {
  return {
    id: dto._id,
    keyword: dto.keyword,
    order: dto.order,
    cacheExpiresAt: dto.cacheExpiresAt,
    videos: (dto.videos ?? []).map(toVideo),
  }
}

export async function fetchInspiration(): Promise<InspirationKeyword[]> {
  const data = await request<InspirationResponse>('GET', '/api/inspiration')
  return data.data.keywords.map(toKeyword)
}

export async function addKeyword(keyword: string): Promise<InspirationKeyword> {
  const data = await request<InspirationKeywordDto>(
    'POST',
    '/api/inspiration/keywords',
    { keyword },
  )
  return toKeyword(data)
}

export async function deleteKeyword(id: number): Promise<void> {
  await request<void>('DELETE', `/api/inspiration/keywords/${id}`)
}

export async function refreshKeyword(id: number): Promise<InspirationKeyword> {
  const data = await request<InspirationKeywordDto>(
    'POST',
    `/api/inspiration/keywords/${id}/refresh`,
  )
  return toKeyword(data)
}
