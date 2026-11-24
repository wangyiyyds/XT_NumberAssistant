/** 管理端用户列表行（对应后端 UserAdminDto：用户 + 套餐 + 积分三表组装） */
export interface UserAdmin {
  id: string
  email: string
  name?: string
  avatar?: string
  emailVerified?: boolean
  disabled?: boolean
  /** 所属套餐 publicId，未分配为空 */
  planId?: string
  planName?: string
  tokenCredits?: number
  autoRefillEnabled?: boolean
  expireAt?: string
  createdAt?: string
}
