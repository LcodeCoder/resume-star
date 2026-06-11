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
      : { minHeight: `${component.height || 60}px` }),
    background: style.background || 'transparent',
    borderRadius: style.borderRadius ? `${style.borderRadius}px` : undefined
  }
  if (style.borderColor && style.borderWidth) {
    css.border = `${style.borderWidth}px solid ${style.borderColor}`
  }
  if (component.type === 'block') {
    css.height = `${component.height || 60}px`
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
