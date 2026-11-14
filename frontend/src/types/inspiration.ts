export interface Video {
  id: number
  author: string
  fans: string
  duration: string
  publishDate: string
  title: string
  likes: string
  coverUrl: string | null
  videoUrl: string | null
  lowFansHighLikes: boolean
}

export interface VideoDto {
  _id: number
  author: string
  fans: string
  duration: string
  publishDate: string
  title: string
  likes: string
  coverUrl: string | null
  videoUrl: string | null
  lowFansHighLikes: boolean
}

export interface InspirationKeyword {
  id: number
  keyword: string
  order: number
  cacheExpiresAt: string | null
  videos: Video[]
}

export interface InspirationKeywordDto {
  _id: number
  keyword: string
  order: number
  cacheExpiresAt: string | null
  videos: VideoDto[]
}

export interface InspirationResponse {
  data: {
    keywords: InspirationKeywordDto[]
  }
}
