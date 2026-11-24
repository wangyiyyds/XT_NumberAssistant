/** 管理员（对应后端 AdminDto） */
export interface Admin {
  id: string
  username: string
  name?: string
  createdAt?: string
}

export interface LoginResult {
  token: string
  admin: Admin
}

/** 管理员新建请求体（对应后端 AdminSaveRequest） */
export interface AdminSaveRequest {
  username: string
  password: string
  name?: string
}
