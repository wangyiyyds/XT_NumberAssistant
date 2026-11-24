import type { Component } from 'vue'

import {
  Bell,
  ChatDotRound,
  Connection,
  Cpu,
  DataLine,
  Document,
  Files,
  Flag,
  Goods,
  Grid,
  HomeFilled,
  Key,
  List,
  Location,
  MagicStick,
  Menu as MenuIcon,
  Money,
  OfficeBuilding,
  Operation,
  PictureFilled,
  Promotion,
  Setting,
  Star,
  Ticket,
  Timer,
  TrendCharts,
  User,
  VideoCamera,
} from '@element-plus/icons-vue'

/**
 * 图标名 -> 组件映射。菜单表的 icon 列存 @element-plus/icons-vue 的导出名，
 * 前端在此解析成组件渲染。具名导入而非整包 import *，避免全量图标进 bundle。
 */
export const ICON_MAP: Record<string, Component> = {
  Bell,
  ChatDotRound,
  Connection,
  Cpu,
  DataLine,
  Document,
  Files,
  Flag,
  Goods,
  Grid,
  HomeFilled,
  Key,
  List,
  Location,
  MagicStick,
  Menu: MenuIcon,
  Money,
  OfficeBuilding,
  Operation,
  PictureFilled,
  Promotion,
  Setting,
  Star,
  Ticket,
  Timer,
  TrendCharts,
  User,
  VideoCamera,
}

/** 管理页图标下拉可选的名字（按导入顺序，即字母序） */
export const ICON_NAMES = Object.keys(ICON_MAP)

/** 未知名/空值回退到 Menu 图标 */
export function resolveIcon(name?: string): Component {
  return (name && ICON_MAP[name]) || MenuIcon
}
