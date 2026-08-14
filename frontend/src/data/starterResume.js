import { layoutStructuredResume, defaultPageStyle } from '../utils/resumeLayout'

/** 一页可直接改的示例简历，降低空白画布起步成本 */
export const buildStarterResume = () => ({
  title: '示例简历',
  targetJob: '前端开发工程师',
  templateId: null,
  draft: true,
  style: defaultPageStyle(),
  components: layoutStructuredResume({
    name: '林晓舟',
    title: '前端开发工程师 ｜ 3 年经验 ｜ 求职意向：Web 前端 / 全栈',
    contacts: [
      { icon: 'phone', label: '电话', content: '138-0000-0000' },
      { icon: 'email', label: '邮箱', content: 'xiaozhou@example.com' },
      { icon: 'github', label: 'GitHub', content: 'github.com/xiaozhou' },
      { icon: 'address', label: '城市', content: '杭州 · 可出差' }
    ],
    sections: [
      {
        title: '教育经历',
        body: '某某大学  ·  软件工程  ·  本科  ·  2019.09 – 2023.06\n主修数据结构、计算机网络、软件工程；GPA 3.6 / 4.0'
      },
      {
        title: '工作经历',
        body: '某某科技  ·  前端开发工程师  ·  2023.07 – 至今\n• 负责招聘与成长业务的 Web 工作台，支撑日活约 8 万\n• 将首屏从 3.2s 降到 1.4s，核心接口成功率提升到 99.9%\n• 搭建组件库与发布流水线，需求交付周期缩短约 25%'
      },
      {
        title: '项目经历',
        body: '履历工坊  ·  负责人  ·  2025.03 – 2026.06\n可视化简历编辑、模板套用、AI 润色与导出。负责画布交互、版本与分享链路。\n技术栈：Vue 3、Spring Boot、MySQL'
      },
      {
        title: '专业技能',
        body: 'Vue 3 / JavaScript / CSS  ·  Spring Boot / MySQL  ·  组件化与性能优化  ·  能独立完成需求拆解到上线'
      },
      {
        title: '自我评价',
        body: '习惯用数据和交付结果说话，能把复杂流程收成别人愿意用的界面。这份示例可直接改姓名、经历和数字。'
      }
    ]
  })
})
