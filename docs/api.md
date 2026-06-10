# resume-lcode 接口说明

## 通用约定

- 后端统一前缀：`/api`
- Swagger UI：`/api/swagger-ui.html`
- 所有接口返回统一结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "success": true
}
```

## 错误码

| code | message | 说明 |
|---:|---|---|
| 400 | 参数错误 | 入参校验失败 |
| 401 | 登录状态无效 | 登录或 token 预留错误 |
| 460 | 会员权限不足 | 会员权限预留错误码，当前不主动拦截 |
| 461 | 功能额度不足 | AI / 导出额度预留错误码，当前不主动拦截 |
| 520 | AI 服务暂不可用 | AI 调用异常 |
| 500 | 系统异常 | 未知异常 |

## 用户模块

### POST `/api/user/login`

演示登录，当前不做真实密码校验，返回用户资料和会员预留字段。

请求：

```json
{
  "username": "demo",
  "password": "demo123"
}
```

响应 `data`：

```json
{
  "id": 1,
  "username": "demo",
  "nickname": "求职者 Demo",
  "avatar": "https://api.dicebear.com/7.x/initials/svg?seed=resume",
  "vipLevel": "FREE",
  "vipExpireTime": "2026-07-10 12:00:00",
  "remainingAiQuota": 5,
  "remainingExportQuota": 3,
  "token": "demo-token-resume-lcode"
}
```

### GET `/api/user/profile`

获取当前演示用户资料，字段同登录响应。

## 简历模块

### GET `/api/resumes?userId=1`

获取我的简历列表。

响应 `data[]`：

```json
{
  "id": 1,
  "title": "Java 全栈工程师简历",
  "targetJob": "Java / Vue 全栈工程师",
  "templateId": 1,
  "draft": true,
  "components": [],
  "updateTime": "2026-06-10 12:00:00"
}
```

### POST `/api/resumes`

保存简历草稿或正式简历。会员组件数量、免费用户上限等仅预留校验入口，当前不拦截。

请求：

```json
{
  "id": 1,
  "title": "Java 全栈工程师简历",
  "targetJob": "Java 后端工程师",
  "templateId": 1,
  "draft": true,
  "userId": 1,
  "components": [
    {
      "id": "summary",
      "type": "text",
      "label": "个人优势",
      "content": "熟悉 Spring Boot 与 Vue3。",
      "x": 40,
      "y": 110,
      "width": 620,
      "height": 92,
      "vipOnly": false,
      "style": { "fontSize": 14 }
    }
  ]
}
```

### POST `/api/resumes/apply-template/{templateId}?userId=1`

根据模板创建一份简历草稿。会员模板仅返回标识，不做访问拦截。

## 模板模块

### GET `/api/templates/categories`

获取模板分类。

### GET `/api/templates?categoryCode=tech&keyword=Java`

获取模板列表。

响应 `data[]`：

```json
{
  "id": 1,
  "name": "高级后端工程师模板",
  "industry": "tech",
  "styleTag": "深色专业",
  "coverUrl": "linear-gradient(135deg,#312e81,#0f766e)",
  "vipTemplate": false,
  "favoriteCount": 128,
  "viewCount": 2048,
  "components": []
}
```

### GET `/api/templates/{templateId}`

获取模板详情。

## AI 模块

### POST `/api/ai/optimize`

AI 简历优化统一入口。API Key、接口地址、模型参数仅由后端读取，前端不传递密钥。未配置 `RESUME_AI_ENDPOINT` 或 `RESUME_AI_API_KEY` 时，后端返回本地模拟结果，方便零配置演示。

请求：

```json
{
  "featureType": "POLISH",
  "content": "负责项目开发...",
  "jobDescription": "Java 后端工程师",
  "userId": 1
}
```

`featureType` 可选值：`POLISH`、`EXPERIENCE`、`GRAMMAR`、`JOB_MATCH`、`SCORE`。

响应 `data`：

```json
{
  "featureType": "POLISH",
  "optimizedContent": "已将内容改写为更专业、结果导向的表达...",
  "score": null,
  "suggestions": ["补充量化指标", "突出岗位关键词", "强化个人贡献边界"],
  "remainingAiQuota": 4,
  "showUpgradeTip": false
}
```

## 导出模块

### POST `/api/export/record`

记录导出行为。当前不生成真实文件流；高清导出、水印、导出次数限制均为会员体系预留能力。

请求：

```json
{
  "userId": 1,
  "resumeId": 1,
  "exportType": "PDF",
  "highDefinition": true
}
```

## 会员模块

### GET `/api/member/packages`

获取会员套餐展示数据。

### POST `/api/member/orders`

创建会员支付订单。当前阶段仅支持模拟支付，是否允许创建订单由管理员系统配置 `paymentEnabled` 控制。

请求：

```json
{
  "userId": 1,
  "packageId": 2,
  "payChannel": "MOCK"
}
```

响应 `data`：

```json
{
  "id": 1,
  "orderNo": "MOCK17180000000001",
  "userId": 1,
  "packageId": 2,
  "packageName": "专业会员",
  "levelCode": "PRO",
  "amount": 49.90,
  "payChannel": "MOCK",
  "status": "PENDING",
  "paymentEnabled": true,
  "mockPaymentEnabled": true,
  "validDays": 30,
  "benefits": ["每日 AI 100 次", "高级组件预留", "岗位适配优先级预留"],
  "createTime": "2026-06-10 12:00:00",
  "paidTime": null
}
```

### POST `/api/member/orders/{orderNo}/mock-pay?userId=1`

模拟支付成功并开通对应会员。需要管理员开启 `paymentEnabled` 和 `mockPaymentEnabled`。

### GET `/api/member/orders?userId=1`

查询当前用户会员支付订单列表。

## 后台模块

### GET `/api/admin/dashboard`

获取后台统计概览。

响应 `data`：

```json
{
  "userCount": 1,
  "resumeCount": 1,
  "templateCount": 3,
  "todayAiCalls": 0,
  "vipUserCount": 0,
  "packageCount": 3
}
```
