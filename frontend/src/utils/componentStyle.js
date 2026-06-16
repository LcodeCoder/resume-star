/**
 * 简历组件样式工具
 * 功能：将组件 style 配置转换为内联样式对象，画布编辑器与模板缩略图共用，保证所见即所得
 */

/** 联系方式小图标：使用内联 SVG 路径，避免 emoji 图标带来的跨平台风格不一致 */
export const CONTACT_ICON_MAP = {
  phone: {
    label: '电话',
    viewBox: '0 0 24 24',
    paths: ['M22 16.92v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.35 1.9.66 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.31 1.85.53 2.81.66A2 2 0 0 1 22 16.92z']
  },
  email: {
    label: '邮箱',
    viewBox: '0 0 24 24',
    paths: ['M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'm22 6-10 7L2 6']
  },
  wechat: {
    label: '微信',
    viewBox: '0 0 24 24',
    paths: ['M8.5 15.5c-3.31 0-6-2.24-6-5s2.69-5 6-5 6 2.24 6 5-2.69 5-6 5z', 'M15.5 10.5c3.31 0 6 2.02 6 4.5s-2.69 4.5-6 4.5c-.76 0-1.49-.11-2.16-.31L10 20l.81-2.43C9.99 16.86 9.5 15.96 9.5 15c0-2.48 2.69-4.5 6-4.5z', 'M6.5 9.5h.01', 'M10.5 9.5h.01', 'M13.5 14.5h.01', 'M17.5 14.5h.01']
  },
  qq: {
    label: 'QQ',
    viewBox: '0 0 24 24',
    paths: ['M12 2a7 7 0 0 0-7 7c0 2 1 4 1 6s-2 4-2 5h16c0-1-2-3-2-5s1-4 1-6a7 7 0 0 0-7-7z', 'M9 10h.01', 'M15 10h.01']
  },
  github: {
    label: 'GitHub',
    viewBox: '0 0 24 24',
    paths: ['M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22']
  },
  linkedin: {
    label: 'LinkedIn',
    viewBox: '0 0 24 24',
    paths: ['M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-4 0v7h-4v-7a6 6 0 0 1 6-6z', 'M2 9h4v12H2z', 'M4 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4z']
  },
  website: {
    label: '网站',
    viewBox: '0 0 24 24',
    paths: ['M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z', 'M2 12h20', 'M12 2a15 15 0 0 1 4 10 15 15 0 0 1-4 10', 'M12 2a15 15 0 0 0-4 10 15 15 0 0 0 4 10']
  },
  address: {
    label: '地址',
    viewBox: '0 0 24 24',
    paths: ['M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z', 'M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6z']
  },
  birthday: {
    label: '生日',
    viewBox: '0 0 24 24',
    paths: ['M20 21v-8H4v8', 'M4 13V8h16v5', 'M12 8V2', 'M9 5l3-3 3 3']
  },
  age: {
    label: '人',
    viewBox: '0 0 24 24',
    paths: ['M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z']
  },
  mobile: {
    label: '手机',
    viewBox: '0 0 24 24',
    paths: ['M16 2H8a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z', 'M12 18h.01']
  },
  fax: {
    label: '传真/打印',
    viewBox: '0 0 24 24',
    paths: ['M6 9V2h12v7', 'M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2', 'M6 14h12v8H6z']
  },
  message: {
    label: '消息',
    viewBox: '0 0 24 24',
    paths: ['M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z']
  },
  link: {
    label: '链接',
    viewBox: '0 0 24 24',
    paths: ['M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71', 'M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71']
  },
  calendar: {
    label: '日历',
    viewBox: '0 0 24 24',
    paths: ['M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z', 'M16 2v4', 'M8 2v4', 'M3 10h18']
  },
  clock: {
    label: '时间',
    viewBox: '0 0 24 24',
    paths: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M12 6v6l4 2']
  },
  male: {
    label: '男',
    viewBox: '0 0 24 24',
    paths: ['M10.5 21a6 6 0 1 0 0-12 6 6 0 0 0 0 12z', 'M14.5 9.5 20 4', 'M15 4h5v5']
  },
  female: {
    label: '女',
    viewBox: '0 0 24 24',
    paths: ['M12 16a6 6 0 1 0 0-12 6 6 0 0 0 0 12z', 'M12 16v6', 'M9 19h6']
  },
  flag: {
    label: '旗帜',
    viewBox: '0 0 24 24',
    paths: ['M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z', 'M4 22v-7']
  },
  heart: {
    label: '爱心',
    viewBox: '0 0 24 24',
    paths: ['M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z']
  },
  star: {
    label: '星标',
    viewBox: '0 0 24 24',
    paths: ['M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14l-5-4.87 6.91-1.01z']
  },
  award: {
    label: '奖章',
    viewBox: '0 0 24 24',
    paths: ['M12 15a7 7 0 1 0 0-14 7 7 0 0 0 0 14z', 'M8.21 13.89 7 23l5-3 5 3-1.21-9.12']
  },
  trophy: {
    label: '奖杯',
    viewBox: '0 0 24 24',
    paths: ['M6 9H4.5a2.5 2.5 0 0 1 0-5H6', 'M18 9h1.5a2.5 2.5 0 0 0 0-5H18', 'M4 22h16', 'M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22', 'M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22', 'M18 2H6v7a6 6 0 0 0 12 0V2z']
  },
  target: {
    label: '目标',
    viewBox: '0 0 24 24',
    paths: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12z', 'M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z']
  },
  briefcase: {
    label: '工作',
    viewBox: '0 0 24 24',
    paths: ['M20 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z', 'M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16']
  },
  building: {
    label: '公司',
    viewBox: '0 0 24 24',
    paths: ['M3 21h18', 'M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16', 'M9 7h.01', 'M15 7h.01', 'M9 11h.01', 'M15 11h.01', 'M9 21v-4h6v4']
  },
  education: {
    label: '学历',
    viewBox: '0 0 24 24',
    paths: ['M22 10 12 5 2 10l10 5 10-5z', 'M6 12v5c0 1.66 2.69 3 6 3s6-1.34 6-3v-5']
  },
  book: {
    label: '书籍/博客',
    viewBox: '0 0 24 24',
    paths: ['M4 19.5A2.5 2.5 0 0 1 6.5 17H20', 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z']
  },
  code: {
    label: '代码',
    viewBox: '0 0 24 24',
    paths: ['m16 18 6-6-6-6', 'm8 6-6 6 6 6']
  },
  terminal: {
    label: '终端',
    viewBox: '0 0 24 24',
    paths: ['m4 17 6-6-6-6', 'M12 19h8']
  },
  database: {
    label: '数据库',
    viewBox: '0 0 24 24',
    paths: ['M12 8c4.97 0 9-1.34 9-3s-4.03-3-9-3-9 1.34-9 3 4.03 3 9 3z', 'M21 12c0 1.66-4 3-9 3s-9-1.34-9-3', 'M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5']
  },
  languages: {
    label: '语言',
    viewBox: '0 0 24 24',
    paths: ['m5 8 6 6', 'M4 14l6-6 2-3', 'M2 5h12', 'M7 2h1', 'm22 22-5-10-5 10', 'M14 18h6']
  },
  idcard: {
    label: '证件',
    viewBox: '0 0 24 24',
    paths: ['M3 4h18a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z', 'M15 9h3', 'M15 13h3', 'M9 11a2 2 0 1 0 0-4 2 2 0 0 0 0 4z', 'M7 17a3 3 0 0 1 6 0']
  },
  salary: {
    label: '薪资',
    viewBox: '0 0 24 24',
    paths: ['M2 6h20v12H2z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z', 'M6 12h.01', 'M18 12h.01']
  },
  dollar: {
    label: '金额',
    viewBox: '0 0 24 24',
    paths: ['M12 1v22', 'M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6']
  },
  certificate: {
    label: '证书',
    viewBox: '0 0 24 24',
    paths: ['M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z', 'M14 2v6h6', 'M16 13H8', 'M16 17H8', 'M10 9H8']
  },
  camera: {
    label: '摄影',
    viewBox: '0 0 24 24',
    paths: ['M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z', 'M12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z']
  },
  music: {
    label: '音乐',
    viewBox: '0 0 24 24',
    paths: ['M9 18V5l12-2v13', 'M6 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z', 'M18 13a3 3 0 1 0 0 6 3 3 0 0 0 0-6z']
  },
  coffee: {
    label: '咖啡',
    viewBox: '0 0 24 24',
    paths: ['M18 8h1a4 4 0 0 1 0 8h-1', 'M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z', 'M6 1v3', 'M10 1v3', 'M14 1v3']
  },
  gift: {
    label: '礼物',
    viewBox: '0 0 24 24',
    paths: ['M20 12v10H4V12', 'M2 7h20v5H2z', 'M12 22V7', 'M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z', 'M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z']
  },
  twitter: {
    label: 'Twitter/X',
    viewBox: '0 0 24 24',
    paths: ['M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z']
  },
  facebook: {
    label: 'Facebook',
    viewBox: '0 0 24 24',
    paths: ['M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z']
  },
  instagram: {
    label: 'Instagram',
    viewBox: '0 0 24 24',
    paths: ['M17 2H7a5 5 0 0 0-5 5v10a5 5 0 0 0 5 5h10a5 5 0 0 0 5-5V7a5 5 0 0 0-5-5z', 'M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z', 'M17.5 6.5h.01']
  },
  youtube: {
    label: 'YouTube',
    viewBox: '0 0 24 24',
    paths: ['M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-2 29 29 0 0 0 .46-5.25 29 29 0 0 0-.46-5.33z', 'm9.75 15.02 5.75-3.27-5.75-3.27z']
  },
  telegram: {
    label: 'Telegram',
    viewBox: '0 0 24 24',
    paths: ['M22 2 11 13', 'M22 2 15 22 11 13 2 9 22 2z']
  },
  gitlab: {
    label: 'GitLab',
    viewBox: '0 0 24 24',
    paths: ['M22.65 14.39 12 22.13 1.35 14.39a.84.84 0 0 1-.3-.94l1.22-3.78 2.44-7.51A.42.42 0 0 1 4.82 2a.43.43 0 0 1 .58 0 .42.42 0 0 1 .11.18l2.44 7.49h8.1l2.44-7.51A.42.42 0 0 1 18.6 2a.43.43 0 0 1 .58 0 .42.42 0 0 1 .11.18l2.44 7.51L23 13.45a.84.84 0 0 1-.35.94z']
  },
  dribbble: {
    label: 'Dribbble',
    viewBox: '0 0 24 24',
    paths: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z', 'M19.13 5.09C15.22 9.14 10 10.44 2.25 10.94', 'M21.75 12.84c-6.62-1.41-12.14 1-16.38 6.32', 'M8.56 2.75c4.37 6.03 6.02 9.42 8.03 17.72']
  }
}

/**
 * 判断组件是否为可编辑文字类组件
 */
export const isTextComponent = (component) => !['divider', 'avatar', 'contact', 'block', 'progress', 'rating', 'tag', 'qrcode', 'image', 'radar', 'ring', 'gauge', 'timeline', 'wordcloud', 'barchart', 'statcard'].includes(component?.type)

/** 会员高级可视化组件类型（统一由 ResumeVisual.vue 渲染） */
export const VISUAL_TYPES = ['radar', 'ring', 'gauge', 'timeline', 'wordcloud', 'barchart', 'statcard']

/** 判断组件是否为会员高级可视化组件 */
export const isVisualComponent = (component) => VISUAL_TYPES.includes(component?.type)

/**
 * 生成组件文字内联样式
 */
export const buildComponentStyle = (component) => {
  const style = component.style || {}
  const css = {
    fontSize: `${style.fontSize || 14}px`,
    fontWeight: style.fontWeight || 400,
    color: style.color || '#1d1d1f',
    textAlign: style.textAlign || 'left',
    lineHeight: String(style.lineHeight || 1.6),
    letterSpacing: style.letterSpacing || 'normal'
  }
  if (style.borderLeft) css.borderLeft = style.borderLeft
  if (style.paddingLeft) css.paddingLeft = `${style.paddingLeft}px`
  return css
}

/**
 * 生成组件容器内联样式（绝对定位 + 背景色等块面元素）
 */
export const buildBlockStyle = (component) => {
  const style = component.style || {}
  const isMedia = ['avatar', 'image', 'qrcode'].includes(component.type)
  const css = {
    left: `${component.x || 0}px`,
    top: `${component.y || 0}px`,
    width: `${component.width || 300}px`,
    // 媒体组件必须用 height 而非 minHeight，否则内部 img 的 height:100% 会导致循环依赖坍缩
    ...(isMedia
      ? { height: `${component.height || 60}px` }
      // 媒体类（头像/图片/二维码）的形状、底色、描边均由内层元素绘制，
      // 外层定位框保持透明无边框，避免圆形头像后面出现方形底色/边框
      : { minHeight: `${component.height || 60}px` }),
    background: isMedia ? 'transparent' : (style.background || 'transparent'),
    borderRadius: (!isMedia && style.borderRadius) ? `${style.borderRadius}px` : undefined
  }
  if (!isMedia && style.borderColor && style.borderWidth) {
    css.border = `${style.borderWidth}px solid ${style.borderColor}`
  }
  if (component.type === 'block') {
    css.height = `${component.height || 60}px`
  }
  // 文本组件：纵向用 flex 排布，让文字可贴顶/居中/贴底；横向对齐仍由 buildComponentStyle 的 textAlign 控制。
  // 仅当框比文字高时 justify-content 才有可分配空白，正好对应“框比文字大、下方留白”的场景。
  if (isTextComponent(component)) {
    css.display = 'flex'
    css.flexDirection = 'column'
    css.justifyContent = { middle: 'center', bottom: 'flex-end' }[style.alignV] || 'flex-start'
  }
  return css
}

/**
 * 生成分割线样式：支持 solid / dashed / dotted / double
 */
export const buildDividerStyle = (component) => {
  const style = component.style || {}
  const borderStyle = style.borderStyle || 'solid'
  const color = style.borderColor || style.color || '#d2d2d7'
  const width = style.lineWidth || 2
  if (borderStyle === 'solid') {
    return {
      height: `${width}px`,
      background: color,
      borderRadius: '999px'
    }
  }
  return {
    height: '0px',
    borderTopWidth: `${width}px`,
    borderTopStyle: borderStyle,
    borderTopColor: color,
    borderRadius: 0
  }
}

/**
 * 生成头像占位容器样式：支持 circle / square / rounded / 阴影 / 描边
 */
export const buildAvatarStyle = (component) => {
  const style = component.style || {}
  const shape = style.shape || 'circle'
  let borderRadius = '50%'
  if (shape === 'square') borderRadius = '0'
  else if (shape === 'rounded') borderRadius = `${style.borderRadius || 16}px`
  const borderWidth = style.borderWidth != null ? style.borderWidth : 0
  const isDefaultBlueBorder = component.src && borderWidth <= 2 && (style.borderColor || '#0071e3') === '#0071e3'
  const css = {
    width: '100%',
    height: `${component.height || component.width || 96}px`,
    borderRadius,
    background: style.background || '#eef5ff',
    color: style.color || '#0071e3',
    border: borderWidth && !isDefaultBlueBorder ? `${borderWidth}px solid ${style.borderColor || '#0071e3'}` : 'none'
  }
  if (style.shadow) {
    css.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.12)'
  }
  return css
}

/** 联系方式图标 */
export const getContactIcon = (component) => CONTACT_ICON_MAP[component.style?.icon || 'phone'] || CONTACT_ICON_MAP.phone

/**
 * 进度条样式构造器：返回 track + fill 双层样式
 */
export const buildProgressStyle = (component) => {
  const style = component.style || {}
  const height = style.height || 10
  return {
    track: {
      height: `${height}px`,
      background: style.background || '#e8eef5',
      borderRadius: `${height / 2}px`,
      overflow: 'hidden'
    },
    fill: {
      width: `${Math.max(0, Math.min(100, style.percent ?? 60))}%`,
      height: '100%',
      background: style.color || '#0071e3',
      borderRadius: `${height / 2}px`,
      transition: 'width 0.3s ease'
    },
    label: {
      fontSize: `${style.fontSize || 13}px`,
      fontWeight: style.fontWeight || 500,
      color: style.textColor || '#1d1d1f',
      marginBottom: '4px',
      display: 'flex',
      justifyContent: 'space-between'
    }
  }
}

/**
 * 评分/等级样式构造器
 */
export const buildRatingStyle = (component) => {
  const style = component.style || {}
  return {
    label: {
      fontSize: `${style.fontSize || 13}px`,
      fontWeight: style.fontWeight || 500,
      color: style.textColor || '#424245',
      marginRight: '8px'
    },
    item: {
      color: style.color || '#ffb800',
      fontSize: `${(style.fontSize || 13) + 4}px`
    }
  }
}

/**
 * 标签样式构造器
 */
export const buildTagStyle = (component) => {
  const style = component.style || {}
  const css = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0 12px',
    height: '100%',
    background: style.background || '#eef5ff',
    color: style.color || '#0071e3',
    fontSize: `${style.fontSize || 12}px`,
    fontWeight: style.fontWeight || 500,
    borderRadius: `${style.borderRadius != null ? style.borderRadius : 6}px`
  }
  if (style.borderColor && style.borderWidth) {
    css.border = `${style.borderWidth}px solid ${style.borderColor}`
  }
  return css
}

/**
 * 二维码占位样式
 */
export const buildQrcodeStyle = (component) => ({
  width: '100%',
  height: `${component.height || 100}px`,
  background: 'repeating-conic-gradient(#1d1d1f 0% 25%, #ffffff 0% 50%) 50% / 8px 8px',
  borderRadius: '6px',
  display: 'flex',
  alignItems: 'flex-end',
  justifyContent: 'center'
})

/**
 * 图片占位样式
 */
export const buildImageStyle = (component) => {
  const style = component.style || {}
  return {
    width: '100%',
    height: '100%',
    background: style.background || '#f5f5f7',
    border: style.borderColor && style.borderWidth ? `${style.borderWidth}px solid ${style.borderColor}` : 'none',
    borderRadius: `${style.borderRadius || 8}px`,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    color: '#9e9ea4',
    fontSize: '13px'
  }
}
