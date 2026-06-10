/**
 * 简历组件库定义
 * 功能：定义编辑器左侧面板的所有组件，按功能分类组织
 * 每个组件含：type（渲染类型）、label（显示名）、content（默认内容）、
 *   width/height（默认尺寸）、style（默认样式）、icon（侧栏小图标可选）
 */

/** 文本类组件 */
const TEXT_GROUP = {
  key: 'text',
  label: '文本',
  icon: 'T',
  children: [
    { type: 'text', label: 'H1 大标题', content: '张三', width: 420, height: 56, style: { fontSize: 32, fontWeight: 700, color: '#1d1d1f', textAlign: 'left' } },
    { type: 'text', label: 'H2 章节标题', content: '工作经历', width: 420, height: 42, style: { fontSize: 20, fontWeight: 700, color: '#1d1d1f' } },
    { type: 'text', label: 'H3 小标题', content: '项目负责人', width: 420, height: 36, style: { fontSize: 16, fontWeight: 600, color: '#424245' } },
    { type: 'text', label: '副标题', content: '高级前端开发工程师', width: 420, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#6e6e73' } },
    { type: 'text', label: '正文段落', content: '双击编辑这段文字，描述你的个人优势或经历。', width: 540, height: 72, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '引用文本', content: '"工程不只是写代码，更是对结果负责。"', width: 540, height: 60, style: { fontSize: 14, fontWeight: 400, color: '#6e6e73', borderLeft: '3px solid #0071e3', paddingLeft: 12 } },
    { type: 'text', label: '个人简介', content: '5 年互联网研发经验，擅长前端架构与性能优化，参与过千万级 DAU 产品迭代。', width: 540, height: 80, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '日期时间', content: '2024.06 - 至今', width: 200, height: 32, style: { fontSize: 13, fontWeight: 500, color: '#6e6e73' } }
  ]
}

/** 头像类组件 */
const AVATAR_GROUP = {
  key: 'avatar',
  label: '头像',
  icon: 'A',
  children: [
    { type: 'avatar', label: '圆形头像', content: '头像', width: 96, height: 96, style: { shape: 'circle', background: '#eef5ff', color: '#0071e3', borderColor: '#0071e3', borderWidth: 2 } },
    { type: 'avatar', label: '方形头像', content: '头像', width: 96, height: 96, style: { shape: 'square', background: '#eef5ff', color: '#0071e3', borderColor: '#0071e3', borderWidth: 2 } },
    { type: 'avatar', label: '圆角方形', content: '头像', width: 96, height: 96, style: { shape: 'rounded', borderRadius: 16, background: '#eef5ff', color: '#0071e3', borderColor: '#0071e3', borderWidth: 2 } },
    { type: 'avatar', label: '描边头像', content: '头像', width: 96, height: 96, style: { shape: 'circle', background: '#ffffff', color: '#0071e3', borderColor: '#0071e3', borderWidth: 3 } },
    { type: 'avatar', label: '阴影头像', content: '头像', width: 96, height: 96, style: { shape: 'circle', background: '#ffffff', color: '#0071e3', borderColor: 'transparent', borderWidth: 0, shadow: true } },
    { type: 'avatar', label: '字母头像', content: 'ZS', width: 80, height: 80, style: { shape: 'circle', background: '#0071e3', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '大尺寸方形', content: '头像', width: 140, height: 140, style: { shape: 'rounded', borderRadius: 12, background: '#f5f5f7', color: '#6e6e73', borderColor: '#e0e0e0', borderWidth: 1 } }
  ]
}

/** 联系方式类组件（含 SVG 图标） */
const CONTACT_GROUP = {
  key: 'contact',
  label: '联系方式',
  icon: 'C',
  children: [
    { type: 'contact', label: '手机号码', content: '138-0000-0000', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'phone' } },
    { type: 'contact', label: '邮箱地址', content: 'resume-lcode@mail.com', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'email' } },
    { type: 'contact', label: '微信号', content: 'resume_lcode', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#07c160', icon: 'wechat' } },
    { type: 'contact', label: 'QQ 号', content: '123456789', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0091ff', icon: 'qq' } },
    { type: 'contact', label: 'GitHub', content: 'github.com/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#1d1d1f', icon: 'github' } },
    { type: 'contact', label: 'LinkedIn', content: 'linkedin.com/in/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0a66c2', icon: 'linkedin' } },
    { type: 'contact', label: '个人网站', content: 'yourname.com', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'website' } },
    { type: 'contact', label: '所在地址', content: '北京市朝阳区', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'address' } },
    { type: 'contact', label: '出生日期', content: '1995-08-15', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'birthday' } },
    { type: 'contact', label: '年龄/性别', content: '28 岁 ｜ 男', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } }
  ]
}

/** 布局/分隔类组件 */
const LAYOUT_GROUP = {
  key: 'layout',
  label: '布局分隔',
  icon: 'L',
  children: [
    { type: 'divider', label: '实线分割', content: '', width: 540, height: 4, style: { borderColor: '#d2d2d7', lineWidth: 2, borderStyle: 'solid' } },
    { type: 'divider', label: '虚线分割', content: '', width: 540, height: 4, style: { borderColor: '#d2d2d7', lineWidth: 2, borderStyle: 'dashed' } },
    { type: 'divider', label: '点线分割', content: '', width: 540, height: 4, style: { borderColor: '#d2d2d7', lineWidth: 2, borderStyle: 'dotted' } },
    { type: 'divider', label: '双线分割', content: '', width: 540, height: 6, style: { borderColor: '#d2d2d7', lineWidth: 3, borderStyle: 'double' } },
    { type: 'block', label: '章节色带', content: '', width: 794, height: 48, style: { background: '#0071e3' } },
    { type: 'block', label: '纯色块', content: '', width: 200, height: 80, style: { background: '#f5f5f7', borderRadius: 8 } },
    { type: 'block', label: '边框框', content: '', width: 540, height: 80, style: { background: 'transparent', borderColor: '#0071e3', borderWidth: 2, borderRadius: 8 } }
  ]
}

/** 简历章节类（标题+内容预设） */
const SECTION_GROUP = {
  key: 'section',
  label: '简历章节',
  icon: 'S',
  children: [
    { type: 'experience', label: '教育经历', content: '某某大学｜计算机科学与技术（本科）\n2018.09 - 2022.06\n主修课程：数据结构、操作系统、计算机网络、数据库系统', width: 540, height: 100, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '工作经历', content: '某某科技有限公司｜高级前端工程师\n2022.07 - 至今\n• 负责核心业务模块设计与交付\n• 推动前端工程化建设', width: 540, height: 120, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '项目经历', content: '项目名称｜担任角色（2024.01 - 2025.06）\n负责核心模块设计与交付，量化产出与业务效果。', width: 540, height: 96, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '自我评价', content: '具备扎实的编程基础与良好的团队协作能力，对新技术保持好奇心，注重代码质量与工程实践。', width: 540, height: 72, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '获奖经历', content: '• 2024 年 度优秀员工\n• 2023 年 ACM 校赛二等奖\n• 2022 年 国家奖学金', width: 540, height: 80, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'skills', label: '技能特长', content: 'Java / Spring Boot / Vue3 / MySQL / Redis', width: 540, height: 44, style: { fontSize: 13, fontWeight: 500, color: '#0071e3' } },
    { type: 'text', label: '兴趣爱好', content: '阅读、跑步、摄影、围棋', width: 540, height: 36, style: { fontSize: 14, fontWeight: 400, color: '#424245' } },
    { type: 'text', label: '语言能力', content: '英语 CET-6 ｜ 日语 N2 ｜ 普通话', width: 540, height: 36, style: { fontSize: 14, fontWeight: 400, color: '#424245' } },
    { type: 'text', label: '证书资质', content: '• AWS 认证解决方案架构师\n• PMP 项目管理认证', width: 540, height: 64, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } }
  ]
}

/** 图形/进度类组件 */
const GRAPHIC_GROUP = {
  key: 'graphic',
  label: '图形进度',
  icon: 'G',
  children: [
    { type: 'progress', label: '技能进度条', content: 'JavaScript', width: 280, height: 36, style: { percent: 85, color: '#0071e3', background: '#e8eef5', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: '细进度条', content: 'Python', width: 280, height: 28, style: { percent: 70, color: '#34c759', background: '#e8f5ec', height: 6, fontSize: 12, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'rating', label: '星级评分', content: '英语水平', width: 240, height: 36, style: { stars: 4, total: 5, color: '#ffb800', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'rating', label: '圆点等级', content: '团队协作', width: 240, height: 36, style: { stars: 4, total: 5, color: '#0071e3', shape: 'dot', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'tag', label: '技能标签', content: 'Vue3', width: 80, height: 28, style: { background: '#eef5ff', color: '#0071e3', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '胶囊标签', content: '前端', width: 80, height: 28, style: { background: '#0071e3', color: '#ffffff', fontSize: 12, fontWeight: 500, borderRadius: 999 } },
    { type: 'tag', label: '描边标签', content: 'TypeScript', width: 100, height: 28, style: { background: 'transparent', color: '#0071e3', borderColor: '#0071e3', borderWidth: 1, fontSize: 12, fontWeight: 500, borderRadius: 6 } }
  ]
}

/** 媒体类组件 */
const MEDIA_GROUP = {
  key: 'media',
  label: '媒体',
  icon: 'M',
  children: [
    { type: 'qrcode', label: '二维码占位', content: '扫码联系', width: 100, height: 100, style: { background: '#ffffff', color: '#1d1d1f', fontSize: 11 } },
    { type: 'image', label: '图片占位', content: '图片', width: 200, height: 140, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'image', label: '横幅图片', content: '横幅图片', width: 540, height: 120, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } }
  ]
}

/** 总组件树：48 个内置组件 */
export const COMPONENT_TREE = [
  TEXT_GROUP,
  AVATAR_GROUP,
  CONTACT_GROUP,
  LAYOUT_GROUP,
  SECTION_GROUP,
  GRAPHIC_GROUP,
  MEDIA_GROUP
]

/** 平铺所有组件，供搜索使用 */
export const flattenComponents = () => COMPONENT_TREE.flatMap((g) => g.children.map((c) => ({ ...c, groupKey: g.key, groupLabel: g.label })))
