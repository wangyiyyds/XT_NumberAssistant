import type { Component } from 'vue'

import {
  Aim,
  Bell,
  ChatDotRound,
  ChatLineSquare,
  Coin,
  Compass,
  Cpu,
  DataBoard,
  DataLine,
  Document,
  EditPen,
  Files,
  Film,
  Folder,
  Grid,
  HomeFilled,
  Loading,
  MagicStick,
  Mic,
  Money,
  Notebook,
  Odometer,
  Opportunity,
  PieChart,
  Promotion,
  Reading,
  Service,
  ShoppingCart,
  Star,
  Tickets,
  Timer,
  TrendCharts,
  Trophy,
  UserFilled,
  VideoCamera,
  VideoPlay,
  Wallet,
} from '@element-plus/icons-vue'

/**
 * 全站图标唯一来源。
 * 图标统一用 @element-plus/icons-vue 的线性图标，色调统一从 TONE 取；
 * 新增页面不要再写 emoji，也不要另找图标。
 *
 * 用法限制：这些图标组件渲染出的 <svg> 没有 width/height，只有 viewBox，
 * 所以必须放进 .app-icon（普通图标）或 .icon-tile（带柔和底色方块）里，
 * 由 style.css 统一给 1em 尺寸；裸放会在 Safari 下塌成 0×0（看不见）。
 */

/**
 * "R G B" 三元组，配合 CSS `rgb(var(--tone) / .1)` 使用。
 * 色相取自 Element Plus 官方规范（主色/成功/警告/危险），再加几个同饱和度的扩展色，
 * 保证浅色底上 10% 底色 + 实色图标都够清楚。
 */
export const TONE = {
  blue: '64 158 255', // #409eff 主色
  amber: '230 162 60', // #e6a23c 警告
  purple: '146 84 222', // #9254de
  teal: '19 194 194', // #13c2c2
  red: '245 108 108', // #f56c6c 危险
  green: '103 194 58', // #67c23a 成功
  indigo: '91 107 217', // #5b6bd9
  yellow: '247 186 42', // #f7ba2a
  sky: '23 182 217', // #17b6d9
  pink: '237 76 123', // #ed4c7b
  slate: '144 147 153', // #909399
} as const

export interface NavItem {
  label: string
  path: string
  icon: Component
  tone: string
}

/** 导航项：侧边栏与首页快捷入口共用 */
export const NAV_ITEMS: NavItem[] = [
  { label: '首页', path: '/', icon: HomeFilled, tone: TONE.blue },
  { label: '流量部门', path: '/traffic', icon: Loading, tone: TONE.amber },
  { label: '视频部门', path: '/video', icon: VideoCamera, tone: TONE.purple },
  { label: '文案编导组', path: '/copywriting', icon: Document, tone: TONE.teal },
  { label: '直播增长组', path: '/live', icon: TrendCharts, tone: TONE.red },
  { label: 'AI销售部门', path: '/sales', icon: Wallet, tone: TONE.green },
  { label: '职能成本部门', path: '/cost', icon: Folder, tone: TONE.indigo },
  { label: 'BOSS决策部门', path: '/boss', icon: Trophy, tone: TONE.yellow },
  { label: '智能体中心', path: '/agent-center', icon: Grid, tone: TONE.sky },
  { label: '灵感中心', path: '/inspiration', icon: Opportunity, tone: TONE.pink },
]

/** 首页快捷入口：除首页以外的全部导航项 */
export const QUICK_ENTRIES: NavItem[] = NAV_ITEMS.filter((item) => item.path !== '/')

export interface AgentTile {
  icon: Component
  tone: string
}

/**
 * 取图标只需要这几个字段。
 * `department` 必须允许 null —— 后端 DTO 里部门就是 `string | null`，
 * 写成 `department?: string` 会让所有 `Agent[]` 调用点报 TS2345。
 */
export interface AgentIconSource {
  id: string
  name: string
  department?: string | null
}

const tile = (icon: Component, tone: string): AgentTile => ({ icon, tone })

/**
 * 各部门的智能体候选图标池。
 * 参照图里同一部门的智能体图标各不相同，所以按名字哈希在「本部门池子」里取：
 * 既保证同部门不重样，又保证图标跟部门业务对得上（不会给剪辑顾问配个 CPU）。
 * 池子顺序变化会让部分智能体换图标，但同一份数据下结果始终稳定。
 */
const AGENT_ICON_POOLS: Record<string, AgentTile[]> = {
  流量部门: [
    tile(Aim, TONE.red),
    tile(TrendCharts, TONE.sky),
    tile(Star, TONE.green),
    tile(DataLine, TONE.blue),
    tile(Promotion, TONE.amber),
  ],
  视频部门: [
    tile(VideoCamera, TONE.purple),
    tile(VideoPlay, TONE.blue),
    tile(Film, TONE.pink),
    tile(Cpu, TONE.teal),
    tile(MagicStick, TONE.indigo),
  ],
  文案编导组: [
    tile(Document, TONE.teal),
    tile(EditPen, TONE.indigo),
    tile(Notebook, TONE.green),
    tile(Reading, TONE.sky),
    tile(Tickets, TONE.yellow),
  ],
  直播增长组: [
    tile(Mic, TONE.red),
    tile(Bell, TONE.amber),
    tile(ChatLineSquare, TONE.blue),
    tile(TrendCharts, TONE.sky),
    tile(Timer, TONE.purple),
  ],
  AI销售部门: [
    tile(Wallet, TONE.green),
    tile(Coin, TONE.yellow),
    tile(ShoppingCart, TONE.amber),
    tile(Service, TONE.pink),
    tile(ChatDotRound, TONE.blue),
  ],
  职能成本部门: [
    tile(Folder, TONE.indigo),
    tile(Files, TONE.sky),
    tile(Tickets, TONE.yellow),
    tile(Money, TONE.green),
    tile(UserFilled, TONE.purple),
  ],
  BOSS决策部门: [
    tile(Trophy, TONE.yellow),
    tile(DataBoard, TONE.blue),
    tile(PieChart, TONE.red),
    tile(Odometer, TONE.teal),
    tile(Compass, TONE.pink),
  ],
}

/** 部门为空或不在池子里（比如自建智能体）时的通用池 */
const FALLBACK_AGENT_POOL: AgentTile[] = [
  tile(MagicStick, TONE.pink),
  tile(Cpu, TONE.sky),
  tile(Star, TONE.green),
  tile(ChatDotRound, TONE.blue),
  tile(Grid, TONE.indigo),
]

/**
 * 给同一部门内的智能体编号：按名称排序取序号，稳定可复现。
 * 列表页用「完整数据」（不是搜索后的）算一次，图标就能保证同部门不重样 ——
 * 纯哈希做不到这一点：5 个槽位放 3 个智能体照样撞（生日悖论）。
 */
export function departmentRanks(agents: AgentIconSource[]): Map<string, number> {
  const groups = new Map<string, Array<{ id: string; name: string }>>()

  for (const agent of agents) {
    const key = agent.department ?? ''
    const list = groups.get(key)
    if (list) list.push(agent)
    else groups.set(key, [{ id: agent.id, name: agent.name }])
  }

  const ranks = new Map<string, number>()
  for (const list of groups.values()) {
    list.sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
    list.forEach((agent, index) => ranks.set(agent.id, index))
  }
  return ranks
}

/**
 * 取智能体图标：优先用部门内序号（保证同部门不重样），
 * 拿不到序号时（对话页只有一个智能体）退回按名字哈希。
 * 哈希用 FNV-1a 再加一轮雪崩，每步都要 >>> 0 保持无符号，
 * 否则取模可能得到负数下标、拿到 undefined 图标。
 */
export function agentTile(name: string, department?: string | null, rank?: number): AgentTile {
  const pool = (department && AGENT_ICON_POOLS[department]) || FALLBACK_AGENT_POOL
  if (rank !== undefined && rank >= 0) {
    return pool[rank % pool.length] as AgentTile
  }

  let hash = 0x811c9dc5
  for (const ch of `${department ?? ''}#${name}`) {
    hash = Math.imul(hash ^ (ch.codePointAt(0) ?? 0), 0x01000193) >>> 0
  }
  hash = (hash ^ (hash >>> 5)) >>> 0
  hash = Math.imul(hash, 0x5bd1e995) >>> 0
  hash = (hash ^ (hash >>> 13)) >>> 0

  return pool[hash % pool.length] as AgentTile
}
