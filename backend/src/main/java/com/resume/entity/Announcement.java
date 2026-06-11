package com.resume.entity;

import java.time.LocalDateTime;

/**
 * 站内公告实体
 * 功能：后台维护、用户进站弹窗展示的公告。前端按 id + updateTime 组成的版本指纹做「已读不再弹」。
 * @author 开发人员
 * @date 2026-06-11
 */
public class Announcement {
    /** 公告主键 */
    private Long id;
    /** 公告标题 */
    private String title;
    /** 公告正文（支持多段纯文本，前端按换行渲染） */
    private String content;
    /** 是否启用：仅启用中的公告会下发给用户端弹窗 */
    private Boolean enabled = true;
    /** 是否置顶/强提醒（前端可加重样式，预留） */
    private Boolean pinned = false;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 最后更新时间：参与版本指纹，内容更新后用户会重新看到弹窗 */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
