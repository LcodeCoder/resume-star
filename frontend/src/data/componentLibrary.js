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
    { type: 'text', label: '日期时间', content: '2024.06 - 至今', width: 200, height: 32, style: { fontSize: 13, fontWeight: 500, color: '#6e6e73' } },
    { type: 'text', label: '居中标题', content: '个人简历', width: 540, height: 48, style: { fontSize: 28, fontWeight: 700, color: '#1d1d1f', textAlign: 'center', letterSpacing: '0.1em' } },
    { type: 'text', label: '右对齐日期', content: '2024.06 - 2025.06', width: 240, height: 30, style: { fontSize: 13, fontWeight: 500, color: '#6e6e73', textAlign: 'right' } },
    { type: 'text', label: '大数字强调', content: '5+', width: 120, height: 60, style: { fontSize: 40, fontWeight: 800, color: '#0071e3', textAlign: 'center' } },
    { type: 'text', label: '关键词强调', content: '核心技能', width: 200, height: 32, style: { fontSize: 15, fontWeight: 700, color: '#0071e3' } },
    { type: 'text', label: '署名落款', content: '—— 张三', width: 240, height: 30, style: { fontSize: 13, fontWeight: 400, color: '#6e6e73', textAlign: 'right' } },
    { type: 'text', label: '双语标题', content: 'WORK EXPERIENCE 工作经历', width: 540, height: 36, style: { fontSize: 18, fontWeight: 700, color: '#1d1d1f', letterSpacing: '0.04em' } },
    { type: 'text', label: '页脚备注', content: '本简历由 resume-lcode 制作', width: 540, height: 26, style: { fontSize: 11, fontWeight: 400, color: '#9e9ea4', textAlign: 'center' } },
    { type: 'text', label: '岗位名称', content: '应聘岗位：前端开发工程师', width: 420, height: 34, style: { fontSize: 16, fontWeight: 600, color: '#1d1d1f' } },
    { type: 'text', label: '超大主标题', content: '李四', width: 480, height: 72, style: { fontSize: 44, fontWeight: 800, color: '#1d1d1f', letterSpacing: '0.04em' } },
    { type: 'text', label: '彩色章节标题', content: '专业技能', width: 420, height: 40, style: { fontSize: 20, fontWeight: 700, color: '#0071e3' } },
    { type: 'text', label: '下划线标题', content: '工作经历', width: 420, height: 42, style: { fontSize: 20, fontWeight: 700, color: '#1d1d1f', borderBottom: '2px solid #0071e3', paddingBottom: 6 } },
    { type: 'text', label: '编号小标题', content: '01 个人简介', width: 420, height: 36, style: { fontSize: 16, fontWeight: 600, color: '#0071e3' } },
    { type: 'text', label: '说明小字', content: '* 以上信息真实有效，欢迎核实', width: 420, height: 26, style: { fontSize: 12, fontWeight: 400, color: '#9e9ea4' } },
    { type: 'text', label: '高亮一句话', content: '让复杂的事情简单，让简单的事情极致。', width: 540, height: 40, style: { fontSize: 16, fontWeight: 600, color: '#1d1d1f' } },
    { type: 'text', label: '项目要点', content: '• 主导前端架构升级，首屏性能提升 40%', width: 540, height: 32, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '量化成果', content: '负责系统支撑日活 300 万，接口成功率 99.9%。', width: 540, height: 40, style: { fontSize: 14, fontWeight: 500, color: '#424245' } },
    { type: 'text', label: '英文标语', content: 'Less is more.', width: 360, height: 40, style: { fontSize: 18, fontWeight: 600, color: '#6e6e73', fontStyle: 'italic' } },
    { type: 'text', label: '居中副标题', content: '前端工程师 ｜ 全栈方向', width: 540, height: 32, style: { fontSize: 15, fontWeight: 500, color: '#6e6e73', textAlign: 'center' } },
    { type: 'text', label: '间隔字标题', content: 'R E S U M E', width: 420, height: 40, style: { fontSize: 22, fontWeight: 700, color: '#1d1d1f', letterSpacing: '0.4em', textAlign: 'center' } },
    { type: 'text', label: '浅色辅助文字', content: '更新于 2025 年 6 月', width: 280, height: 28, style: { fontSize: 12, fontWeight: 400, color: '#9e9ea4' } },
    { type: 'text', label: '加粗正文', content: '具备从 0 到 1 搭建产品的完整经验，能独立承担核心模块。', width: 540, height: 48, style: { fontSize: 14, fontWeight: 600, color: '#1d1d1f', lineHeight: 1.8 } }
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
    { type: 'avatar', label: '大尺寸方形', content: '头像', width: 140, height: 140, style: { shape: 'rounded', borderRadius: 12, background: '#f5f5f7', color: '#6e6e73', borderColor: '#e0e0e0', borderWidth: 1 } },
    { type: 'avatar', label: '小圆头像', content: '头像', width: 64, height: 64, style: { shape: 'circle', background: '#eef5ff', color: '#0071e3', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '超大圆形', content: '头像', width: 160, height: 160, style: { shape: 'circle', background: '#f5f5f7', color: '#6e6e73', borderColor: '#e0e0e0', borderWidth: 1 } },
    { type: 'avatar', label: '细描边方形', content: '头像', width: 96, height: 120, style: { shape: 'square', background: '#ffffff', color: '#6e6e73', borderColor: '#d2d2d7', borderWidth: 1 } },
    { type: 'avatar', label: '灰底字母', content: 'LS', width: 80, height: 80, style: { shape: 'circle', background: '#f5f5f7', color: '#6e6e73', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '深色字母', content: 'WS', width: 80, height: 80, style: { shape: 'rounded', borderRadius: 14, background: '#1d1d1f', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '绿色字母', content: 'ZL', width: 80, height: 80, style: { shape: 'circle', background: '#07c160', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '渐变字母', content: 'AI', width: 88, height: 88, style: { shape: 'circle', background: 'linear-gradient(135deg, #0071e3 0%, #34c759 100%)', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '双描边圆形', content: '头像', width: 100, height: 100, style: { shape: 'circle', background: '#ffffff', color: '#0071e3', borderColor: '#34c759', borderWidth: 4 } },
    { type: 'avatar', label: '证件照头像', content: '照片', width: 120, height: 150, style: { shape: 'square', background: '#f5f5f7', color: '#6e6e73', borderColor: '#d2d2d7', borderWidth: 1 } },
    { type: 'avatar', label: '大圆角头像', content: '头像', width: 120, height: 120, style: { shape: 'rounded', borderRadius: 24, background: '#eef5ff', color: '#0071e3', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '迷你圆头像', content: '头像', width: 48, height: 48, style: { shape: 'circle', background: '#f5f5f7', color: '#6e6e73', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '橙底字母', content: 'TX', width: 80, height: 80, style: { shape: 'circle', background: '#ff9500', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } },
    { type: 'avatar', label: '描边圆角字母', content: 'HR', width: 88, height: 88, style: { shape: 'rounded', borderRadius: 16, background: '#ffffff', color: '#0071e3', borderColor: '#0071e3', borderWidth: 2 } },
    { type: 'avatar', label: '阴影圆角头像', content: '头像', width: 110, height: 110, style: { shape: 'rounded', borderRadius: 18, background: '#ffffff', color: '#0071e3', borderColor: 'transparent', borderWidth: 0, shadow: true } },
    { type: 'avatar', label: '深蓝字母', content: 'CT', width: 80, height: 80, style: { shape: 'square', background: '#0a3d62', color: '#ffffff', borderColor: 'transparent', borderWidth: 0 } }
  ]
}

/** 图标类组件（SVG 图标 + 可编辑文字） */
const CONTACT_GROUP = {
  key: 'contact',
  label: '图标',
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
    { type: 'contact', label: '年龄/性别', content: '28 岁 ｜ 男', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } },
    { type: 'contact', label: '座机电话', content: '010-8888-6666', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'phone' } },
    { type: 'contact', label: '工作邮箱', content: 'work@company.com', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'email' } },
    { type: 'contact', label: '企业微信', content: 'work_wechat', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#07c160', icon: 'wechat' } },
    { type: 'contact', label: '技术博客', content: 'blog.yourname.com', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'website' } },
    { type: 'contact', label: '现居城市', content: '上海市浦东新区', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'address' } },
    { type: 'contact', label: '籍贯', content: '籍贯：浙江杭州', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'address' } },
    { type: 'contact', label: '政治面貌', content: '中共党员', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } },
    { type: 'contact', label: '求职状态', content: '离职｜随时到岗', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } },
    { type: 'contact', label: '备用手机', content: '139-1111-2222', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'phone' } },
    { type: 'contact', label: '个人邮箱', content: 'me@gmail.com', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'email' } },
    { type: 'contact', label: '微信公众号', content: '前端成长笔记', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#07c160', icon: 'wechat' } },
    { type: 'contact', label: '作品集', content: 'portfolio.yourname.com', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'website' } },
    { type: 'contact', label: '掘金主页', content: 'juejin.cn/user/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#1e80ff', icon: 'website' } },
    { type: 'contact', label: 'Gitee 主页', content: 'gitee.com/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#c71d23', icon: 'github' } },
    { type: 'contact', label: '期望薪资', content: '期望薪资：面议', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } },
    { type: 'contact', label: '期望城市', content: '期望城市：杭州', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'address' } },
    { type: 'contact', label: '婚姻状况', content: '未婚', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'age' } },
    { type: 'contact', label: '紧急联系人', content: '紧急联系：张先生 137-0000-0000', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'phone' } },
    { type: 'contact', label: '微信(手机)', content: 'wxid_resume', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#07c160', icon: 'mobile' } },
    { type: 'contact', label: '即时消息', content: '点击联系我', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'message' } },
    { type: 'contact', label: '入职时间', content: '可到岗时间：一周内', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'calendar' } },
    { type: 'contact', label: '工作年限', content: '工作年限：5 年', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'clock' } },
    { type: 'contact', label: '性别(男)', content: '性别：男', width: 180, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'male' } },
    { type: 'contact', label: '性别(女)', content: '性别：女', width: 180, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'female' } },
    { type: 'contact', label: '民族', content: '民族：汉族', width: 200, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'flag' } },
    { type: 'contact', label: '兴趣爱好', content: '爱好：阅读 / 跑步', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ff3b30', icon: 'heart' } },
    { type: 'contact', label: '核心亮点', content: '5 年大厂研发经验', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ffb800', icon: 'star' } },
    { type: 'contact', label: '荣誉奖项', content: '国家奖学金', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ff9500', icon: 'award' } },
    { type: 'contact', label: '获奖记录', content: '全国大赛一等奖', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ff9500', icon: 'trophy' } },
    { type: 'contact', label: '求职意向', content: '求职意向：前端开发', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'target' } },
    { type: 'contact', label: '工作经历', content: '5 年互联网研发', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'briefcase' } },
    { type: 'contact', label: '所在公司', content: '某某科技有限公司', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'building' } },
    { type: 'contact', label: '最高学历', content: '本科 ｜ 计算机科学', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'education' } },
    { type: 'contact', label: '毕业院校', content: '某某大学', width: 220, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'education' } },
    { type: 'contact', label: '技术博客', content: 'blog.yourname.com', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#0071e3', icon: 'book' } },
    { type: 'contact', label: '代码仓库', content: 'github.com/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#1d1d1f', icon: 'code' } },
    { type: 'contact', label: '语言能力', content: '英语 CET-6 ｜ 日语 N2', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'languages' } },
    { type: 'contact', label: '身份证号', content: '110********1234', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'idcard' } },
    { type: 'contact', label: '期望薪资', content: '期望薪资：20-30K', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#34c759', icon: 'salary' } },
    { type: 'contact', label: '专业证书', content: 'PMP 项目管理认证', width: 240, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#424245', icon: 'certificate' } },
    { type: 'contact', label: 'Twitter', content: 'x.com/yourname', width: 260, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#1d1d1f', icon: 'twitter' } },
    { type: 'contact', label: 'YouTube', content: 'youtube.com/@yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ff0000', icon: 'youtube' } },
    { type: 'contact', label: 'GitLab', content: 'gitlab.com/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#fc6d26', icon: 'gitlab' } },
    { type: 'contact', label: 'Dribbble', content: 'dribbble.com/yourname', width: 280, height: 32, style: { fontSize: 14, fontWeight: 500, color: '#ea4c89', icon: 'dribbble' } }
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
    { type: 'block', label: '边框框', content: '', width: 540, height: 80, style: { background: 'transparent', borderColor: '#0071e3', borderWidth: 2, borderRadius: 8 } },
    { type: 'block', label: '渐变色带', content: '', width: 794, height: 48, style: { background: 'linear-gradient(135deg, #0071e3 0%, #34c759 100%)' } },
    { type: 'block', label: '浅灰卡片', content: '', width: 540, height: 120, style: { background: '#f5f5f7', borderRadius: 12 } },
    { type: 'block', label: '圆角描边卡片', content: '', width: 540, height: 120, style: { background: 'transparent', borderColor: '#d2d2d7', borderWidth: 1, borderRadius: 12 } },
    { type: 'block', label: '左侧强调竖条', content: '', width: 6, height: 120, style: { background: '#0071e3', borderRadius: 999 } },
    { type: 'divider', label: '细实线', content: '', width: 540, height: 2, style: { borderColor: '#e0e0e0', lineWidth: 1, borderStyle: 'solid' } },
    { type: 'divider', label: '粗实线', content: '', width: 540, height: 6, style: { borderColor: '#1d1d1f', lineWidth: 4, borderStyle: 'solid' } },
    { type: 'divider', label: '短强调线', content: '', width: 200, height: 4, style: { borderColor: '#0071e3', lineWidth: 3, borderStyle: 'solid' } },
    { type: 'divider', label: '绿色细线', content: '', width: 540, height: 2, style: { borderColor: '#34c759', lineWidth: 1, borderStyle: 'solid' } },
    { type: 'divider', label: '居中短线', content: '', width: 120, height: 4, style: { borderColor: '#1d1d1f', lineWidth: 2, borderStyle: 'solid' } },
    { type: 'divider', label: '浅灰虚线', content: '', width: 540, height: 4, style: { borderColor: '#e0e0e0', lineWidth: 1, borderStyle: 'dashed' } },
    { type: 'divider', label: '渐隐分割线', content: '', width: 540, height: 3, style: { borderColor: '#0071e3', lineWidth: 2, borderStyle: 'solid' } },
    { type: 'block', label: '深色标题条', content: '', width: 794, height: 48, style: { background: '#1d1d1f' } },
    { type: 'block', label: '绿色色带', content: '', width: 794, height: 48, style: { background: '#34c759' } },
    { type: 'block', label: '左侧栏背景', content: '', width: 240, height: 1123, style: { background: '#f5f5f7' } },
    { type: 'block', label: '深色侧栏', content: '', width: 240, height: 1123, style: { background: '#1d1d1f' } },
    { type: 'block', label: '高亮信息块', content: '', width: 540, height: 100, style: { background: '#eef5ff', borderRadius: 12 } },
    { type: 'block', label: '圆形装饰点', content: '', width: 16, height: 16, style: { background: '#0071e3', borderRadius: 999 } },
    { type: 'block', label: '小方形标记', content: '', width: 14, height: 14, style: { background: '#34c759', borderRadius: 3 } },
    { type: 'block', label: '渐变卡片', content: '', width: 540, height: 120, style: { background: 'linear-gradient(135deg, #eef5ff 0%, #e7f6ec 100%)', borderRadius: 12 } },
    { type: 'block', label: '顶部强调横条', content: '', width: 540, height: 6, style: { background: '#0071e3', borderRadius: 999 } },
    { type: 'block', label: '底部页脚条', content: '', width: 794, height: 32, style: { background: '#f5f5f7' } }
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
    { type: 'text', label: '证书资质', content: '• AWS 认证解决方案架构师\n• PMP 项目管理认证', width: 540, height: 64, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '实习经历', content: '某某互联网公司｜前端开发实习生\n2021.06 - 2021.09\n• 参与活动页面开发与组件维护\n• 协助完成埋点与数据看板', width: 540, height: 110, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '校园经历', content: '校学生会技术部｜部长\n2019.09 - 2021.06\n负责校园活动技术支持与团队管理。', width: 540, height: 96, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '社团活动', content: '计算机协会｜核心成员\n组织技术沙龙与编程竞赛，累计参与人数超 500 人。', width: 540, height: 90, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '志愿服务', content: '社区公益编程课｜志愿讲师\n2023 年累计志愿服务 80 小时。', width: 540, height: 84, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'skills', label: '专业技能', content: 'Java ｜ Python ｜ Go ｜ Docker ｜ Kubernetes', width: 540, height: 44, style: { fontSize: 13, fontWeight: 500, color: '#0071e3' } },
    { type: 'text', label: '论文发表', content: '• 《基于深度学习的简历解析方法》，2024 计算机学报', width: 540, height: 48, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '专利成果', content: '• 一种智能简历排版方法及装置（发明专利，已授权）', width: 540, height: 48, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '培训认证', content: '阿里云开发者训练营｜结业\n系统学习云原生与微服务架构。', width: 540, height: 84, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '荣誉奖项', content: '• 公司年度技术之星（2024）\n• 优秀开源贡献者（2023）\n• 校级一等奖学金（2021）', width: 540, height: 88, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '出版物', content: '• 《Vue3 企业级实战》参编，电子工业出版社', width: 540, height: 48, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '开源项目', content: 'resume-lcode｜作者\nGitHub 开源简历编辑器，累计 Star 1.2k。', width: 540, height: 84, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '海外经历', content: '某大学交换生｜计算机学院\n2020.09 - 2021.01，参与跨文化协作项目。', width: 540, height: 88, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '职业目标', content: '希望在技术深度与团队影响力上持续成长，成长为技术专家。', width: 540, height: 56, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'skills', label: '工具技能', content: 'Git ｜ Webpack ｜ Vite ｜ Jenkins ｜ Linux', width: 540, height: 44, style: { fontSize: 13, fontWeight: 500, color: '#0071e3' } },
    { type: 'skills', label: '前端技能', content: 'HTML5 ｜ CSS3 ｜ JavaScript ｜ TypeScript ｜ Vue3', width: 540, height: 44, style: { fontSize: 13, fontWeight: 500, color: '#0071e3' } },
    { type: 'experience', label: '兼职经历', content: '某创业团队｜前端外包\n2022.01 - 2022.05，独立完成官网与后台开发。', width: 540, height: 88, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'experience', label: '竞赛经历', content: '全国大学生程序设计竞赛｜银奖\n2021 年，负责算法与系统设计模块。', width: 540, height: 88, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '推荐人', content: '推荐人：王经理｜技术总监｜138-0000-1111', width: 540, height: 40, style: { fontSize: 14, fontWeight: 400, color: '#424245' } },
    { type: 'text', label: '兴趣特长', content: '摄影、马拉松、吉他、写作', width: 540, height: 36, style: { fontSize: 14, fontWeight: 400, color: '#424245' } },
    { type: 'experience', label: '科研经历', content: '某实验室｜研究助理\n2020.09 - 2021.06，参与机器学习方向课题研究。', width: 540, height: 88, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } },
    { type: 'text', label: '其他信息', content: '可接受出差｜驾照 C1｜身体健康', width: 540, height: 36, style: { fontSize: 14, fontWeight: 400, color: '#424245' } },
    { type: 'experience', label: '行业经验', content: '互联网｜金融科技｜电商\n累计 5 年一线研发与团队管理经验。', width: 540, height: 84, style: { fontSize: 14, fontWeight: 400, color: '#424245', lineHeight: 1.8 } }
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
    { type: 'tag', label: '描边标签', content: 'TypeScript', width: 100, height: 28, style: { background: 'transparent', color: '#0071e3', borderColor: '#0071e3', borderWidth: 1, fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'progress', label: 'Vue 熟练度', content: 'Vue', width: 280, height: 36, style: { percent: 90, color: '#42b883', background: '#e7f6ec', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'React 熟练度', content: 'React', width: 280, height: 36, style: { percent: 80, color: '#0aa5c9', background: '#e6f5f9', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'SQL 熟练度', content: 'SQL', width: 280, height: 36, style: { percent: 75, color: '#f29111', background: '#fdf0db', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: '极细进度条', content: '设计', width: 280, height: 24, style: { percent: 60, color: '#8e8e93', background: '#ececec', height: 5, fontSize: 12, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'rating', label: '专业满级', content: '专业能力', width: 240, height: 36, style: { stars: 5, total: 5, color: '#ffb800', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'rating', label: '方点等级', content: '沟通能力', width: 240, height: 36, style: { stars: 3, total: 5, color: '#34c759', shape: 'dot', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'rating', label: '语言流利度', content: '日语', width: 240, height: 36, style: { stars: 4, total: 5, color: '#ff9500', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'tag', label: '成功色标签', content: '已认证', width: 90, height: 28, style: { background: '#e7f6ec', color: '#2f9e46', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '警告色标签', content: '在学', width: 80, height: 28, style: { background: '#fdf2dc', color: '#b9770a', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '大号胶囊', content: '推荐', width: 100, height: 32, style: { background: '#1d1d1f', color: '#ffffff', fontSize: 14, fontWeight: 600, borderRadius: 999 } },
    { type: 'progress', label: 'TypeScript 熟练度', content: 'TypeScript', width: 280, height: 36, style: { percent: 82, color: '#3178c6', background: '#e6eef7', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'Node 熟练度', content: 'Node.js', width: 280, height: 36, style: { percent: 78, color: '#3c873a', background: '#e8f1e6', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'Go 熟练度', content: 'Go', width: 280, height: 36, style: { percent: 65, color: '#00add8', background: '#e0f4fa', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'Docker 熟练度', content: 'Docker', width: 280, height: 36, style: { percent: 72, color: '#2496ed', background: '#e6f1fc', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: 'Redis 熟练度', content: 'Redis', width: 280, height: 36, style: { percent: 68, color: '#d82c20', background: '#fbe7e5', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'progress', label: '满格进度条', content: '专业基础', width: 280, height: 36, style: { percent: 100, color: '#34c759', background: '#e7f6ec', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } },
    { type: 'rating', label: '设计能力', content: '设计能力', width: 240, height: 36, style: { stars: 4, total: 5, color: '#af52de', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'rating', label: '红色满级', content: '抗压能力', width: 240, height: 36, style: { stars: 5, total: 5, color: '#ff3b30', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'rating', label: '蓝点四级', content: '学习能力', width: 240, height: 36, style: { stars: 4, total: 5, color: '#0071e3', shape: 'dot', fontSize: 13, fontWeight: 500, textColor: '#424245' } },
    { type: 'tag', label: '紫色标签', content: 'UI 设计', width: 90, height: 28, style: { background: '#f3e8fb', color: '#8e44ad', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '红色标签', content: '急招', width: 80, height: 28, style: { background: '#fde7e6', color: '#d82c20', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '灰色标签', content: '熟悉', width: 80, height: 28, style: { background: '#f0f0f2', color: '#6e6e73', fontSize: 12, fontWeight: 500, borderRadius: 6 } },
    { type: 'tag', label: '描边胶囊', content: '精通', width: 90, height: 28, style: { background: 'transparent', color: '#34c759', borderColor: '#34c759', borderWidth: 1, fontSize: 12, fontWeight: 500, borderRadius: 999 } },
    { type: 'progress', label: 'Linux 熟练度', content: 'Linux', width: 280, height: 36, style: { percent: 76, color: '#1d1d1f', background: '#ececec', fontSize: 13, fontWeight: 500, textColor: '#1d1d1f' } }
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
    { type: 'image', label: '横幅图片', content: '横幅图片', width: 540, height: 120, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'qrcode', label: '微信二维码', content: '微信扫码', width: 100, height: 100, style: { background: '#ffffff', color: '#07c160', fontSize: 11 } },
    { type: 'qrcode', label: '名片二维码', content: '电子名片', width: 120, height: 120, style: { background: '#ffffff', color: '#1d1d1f', fontSize: 11 } },
    { type: 'image', label: '证件照占位', content: '证件照', width: 120, height: 160, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 6 } },
    { type: 'image', label: 'Logo 占位', content: 'LOGO', width: 120, height: 120, style: { background: '#ffffff', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'qrcode', label: 'GitHub 二维码', content: 'GitHub 扫码', width: 100, height: 100, style: { background: '#ffffff', color: '#1d1d1f', fontSize: 11 } },
    { type: 'qrcode', label: '作品集二维码', content: '作品集', width: 110, height: 110, style: { background: '#ffffff', color: '#0071e3', fontSize: 11 } },
    { type: 'qrcode', label: '简历下载码', content: '下载简历', width: 100, height: 100, style: { background: '#ffffff', color: '#34c759', fontSize: 11 } },
    { type: 'image', label: '方形头像图', content: '头像图', width: 140, height: 140, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'image', label: '公司 Logo', content: '公司 LOGO', width: 160, height: 80, style: { background: '#ffffff', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'image', label: '项目截图', content: '项目截图', width: 320, height: 180, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'image', label: '作品缩略图', content: '作品图', width: 160, height: 120, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'image', label: '圆形图片', content: '图片', width: 120, height: 120, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 999 } },
    { type: 'image', label: '宽幅背景图', content: '背景图', width: 794, height: 200, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 0 } },
    { type: 'image', label: '证书图片', content: '证书', width: 200, height: 140, style: { background: '#f5f5f7', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 6 } },
    { type: 'image', label: '小图标占位', content: '图标', width: 64, height: 64, style: { background: '#ffffff', borderColor: '#e0e0e0', borderWidth: 1, borderRadius: 8 } },
    { type: 'qrcode', label: '领英二维码', content: 'LinkedIn', width: 100, height: 100, style: { background: '#ffffff', color: '#0a66c2', fontSize: 11 } }
  ]
}

/**
 * 会员高级组件（数据可视化）
 * 说明：全部用内联 SVG/HTML 渲染（见 ResumeVisual.vue），导出 PDF/PNG/Word 均可正确呈现。
 *       每个组件默认带 vipOnly:true，普通用户拦截弹升级；数据存于 style 下的专用字段，可在编辑器调整。
 */
const PREMIUM_GROUP = {
  key: 'premium',
  label: '会员高级',
  icon: '★',
  children: [
    {
      type: 'radar', label: '技能雷达图', vipOnly: true, content: '能力雷达', width: 300, height: 250,
      style: {
        color: '#0071e3', textColor: '#1d1d1f',
        indicators: [
          { name: '专业技能', value: 90 }, { name: '沟通协作', value: 75 }, { name: '学习能力', value: 85 },
          { name: '项目管理', value: 70 }, { name: '创新思维', value: 80 }, { name: '抗压能力', value: 78 }
        ]
      }
    },
    {
      type: 'radar', label: '四维雷达图', vipOnly: true, content: '核心能力', width: 280, height: 240,
      style: {
        color: '#34c759', textColor: '#1d1d1f',
        indicators: [
          { name: '技术能力', value: 92 }, { name: '业务理解', value: 80 },
          { name: '团队协作', value: 85 }, { name: '交付质量', value: 88 }
        ]
      }
    },
    {
      type: 'ring', label: '环形能力图', vipOnly: true, content: '综合能力', width: 160, height: 180,
      style: { color: '#0071e3', background: '#e8eef5', textColor: '#1d1d1f', percent: 86, ringWidth: 14 }
    },
    {
      type: 'ring', label: '绿色环形图', vipOnly: true, content: '岗位匹配度', width: 160, height: 180,
      style: { color: '#34c759', background: '#e7f6ec', textColor: '#1d1d1f', percent: 92, ringWidth: 14 }
    },
    {
      type: 'ring', label: '橙色环形图', vipOnly: true, content: '英语水平', width: 160, height: 180,
      style: { color: '#ff9500', background: '#fdf0db', textColor: '#1d1d1f', percent: 78, ringWidth: 14 }
    },
    {
      type: 'gauge', label: '仪表盘评分', vipOnly: true, content: '综合评分', width: 220, height: 130,
      style: { color: '#0071e3', background: '#e8eef5', textColor: '#1d1d1f', percent: 88, ringWidth: 16 }
    },
    {
      type: 'gauge', label: '绩效仪表盘', vipOnly: true, content: '年度绩效', width: 220, height: 130,
      style: { color: '#af52de', background: '#f3e8fb', textColor: '#1d1d1f', percent: 95, ringWidth: 16 }
    },
    {
      type: 'timeline', label: '履历时间线', vipOnly: true, content: '', width: 380, height: 220,
      style: {
        color: '#0071e3', background: '#e8eef5', textColor: '#1d1d1f',
        items: [
          { time: '2022.07 - 至今', title: '某科技公司 · 高级前端', desc: '负责核心业务架构与团队协作' },
          { time: '2020.06 - 2022.06', title: '某互联网公司 · 前端工程师', desc: '主导多个 C 端项目从 0 到 1' },
          { time: '2016.09 - 2020.06', title: '某大学 · 计算机科学与技术', desc: '主修数据结构、操作系统、网络' }
        ]
      }
    },
    {
      type: 'timeline', label: '教育时间线', vipOnly: true, content: '', width: 380, height: 160,
      style: {
        color: '#34c759', background: '#e7f6ec', textColor: '#1d1d1f',
        items: [
          { time: '2020.09 - 2023.06', title: '某大学 · 软件工程（硕士）', desc: 'GPA 3.8 / 4.0' },
          { time: '2016.09 - 2020.06', title: '某大学 · 计算机科学（本科）', desc: '校级一等奖学金' }
        ]
      }
    },
    {
      type: 'wordcloud', label: '技能词云', vipOnly: true, content: '', width: 360, height: 180,
      style: {
        words: [
          { text: 'JavaScript', weight: 3 }, { text: 'Vue', weight: 3 }, { text: 'TypeScript', weight: 2 },
          { text: 'Node.js', weight: 2 }, { text: 'Webpack', weight: 1 }, { text: '性能优化', weight: 2 },
          { text: '工程化', weight: 1 }, { text: 'React', weight: 2 }, { text: 'CSS', weight: 1 },
          { text: '架构设计', weight: 3 }, { text: 'Git', weight: 1 }, { text: '团队协作', weight: 2 }
        ]
      }
    },
    {
      type: 'wordcloud', label: '关键词标签云', vipOnly: true, content: '', width: 360, height: 160,
      style: {
        words: [
          { text: '高并发', weight: 3 }, { text: '微服务', weight: 2 }, { text: '云原生', weight: 2 },
          { text: 'DevOps', weight: 1 }, { text: '数据驱动', weight: 2 }, { text: '敏捷开发', weight: 1 },
          { text: '中台', weight: 2 }, { text: '可观测性', weight: 1 }
        ]
      }
    },
    {
      type: 'barchart', label: '柱状能力图', vipOnly: true, content: '', width: 320, height: 180,
      style: {
        color: '#0071e3', background: '#e8eef5', textColor: '#1d1d1f',
        bars: [
          { label: '后端开发', value: 90 }, { label: '前端开发', value: 80 },
          { label: '数据库', value: 75 }, { label: '运维部署', value: 65 }
        ]
      }
    },
    {
      type: 'barchart', label: '语言能力图', vipOnly: true, content: '', width: 320, height: 160,
      style: {
        color: '#ff9500', background: '#fdf0db', textColor: '#1d1d1f',
        bars: [
          { label: '英语', value: 88 }, { label: '日语', value: 70 }, { label: '普通话', value: 100 }
        ]
      }
    },
    {
      type: 'statcard', label: '数据统计卡', vipOnly: true, content: '', width: 360, height: 90,
      style: {
        color: '#0071e3', textColor: '#6e6e73',
        stats: [
          { value: '5+', label: '年经验' }, { value: '20+', label: '项目' }, { value: '99.9%', label: '可用性' }
        ]
      }
    },
    {
      type: 'statcard', label: '成就数据卡', vipOnly: true, content: '', width: 360, height: 90,
      style: {
        color: '#34c759', textColor: '#6e6e73',
        stats: [
          { value: '1.2k', label: 'GitHub Star' }, { value: '300万', label: '日活支撑' }, { value: '40%', label: '性能提升' }
        ]
      }
    }
  ]
}

/** 总组件树：内置组件（含图标组件与会员高级可视化组件） */
export const COMPONENT_TREE = [
  TEXT_GROUP,
  AVATAR_GROUP,
  CONTACT_GROUP,
  LAYOUT_GROUP,
  SECTION_GROUP,
  GRAPHIC_GROUP,
  MEDIA_GROUP,
  PREMIUM_GROUP
]

/** 平铺所有组件，供搜索使用 */
export const flattenComponents = () => COMPONENT_TREE.flatMap((g) => g.children.map((c) => ({ ...c, groupKey: g.key, groupLabel: g.label })))
